package de.zarncke.lib.time;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import de.zarncke.lib.ctx.Context;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.io.LineConsumer;
import de.zarncke.lib.io.OutputProducer;
import de.zarncke.lib.io.store.FileStore;
import de.zarncke.lib.io.store.Store;
import de.zarncke.lib.log.Log;
import de.zarncke.lib.value.Default;

/**
 * Class for collecting coarse structured timing information.
 * Syntax of profile files:
 * &lt;id&gt;;&lt;description&gt;;&lt;time ms&gt;;&lt;optional info&gt;
 *
 * @author Gunnar Zarncke
 */
public class Profiling {
	// TODO make Profiling an interface

	public static final class NoOpProfiling extends Profiling {
		@Override
		public Profiling createChild() {
			return this;
		}

		@Override
		public synchronized void time(final String item, final long millis) {
			// NOOP
		}

		@Override
		public synchronized void flush() {
			// NOOP
		}

		@Override
		public Store getStore() {
			throw Warden.spot(new UnsupportedOperationException("no store"));
		}
	}

	/**
	 * This Profiling is thread-save and uses the {@link Profiling#defaultProfilingStore}.
	 */
	public static class ThreadSaveProfiling extends Profiling {

		@Override
		public Profiling createChild() {
			return new ThreadSaveProfiling();
		}
		@Override
		public synchronized void time(final String item, final long millis) {
			super.time(item, millis);
		};

		@Override
		public Store getStore() {
			return defaultProfilingStore;
		}
	}

	private static final String PROFILING_FORMAT_COMMENT = "#";

	private static final String PROFILING_FORMAT_SEPARATOR = ";";

	private static final int REQUIRED_PROFILING_FORMAT_ENTRIES = 3;

	public static Store defaultProfilingStore = new FileStore(new File("profiling.txt"));

	/**
	 * Provides the current Profiling. This special Profiling records nothing.
	 */
	public static final Context<Profiling> CTX = Context.of(Default.of(new NoOpProfiling(), Profiling.class));

	private String lastId;
	protected final Map<String, AtomicLong> timings = new HashMap<String, AtomicLong>();

	private final Store store;

	protected Profiling() {
		this.store = null;
	}

	public Profiling(final Store profilingStore) {
		this.store = profilingStore;
	}

	/**
	 * Creates a profiling of the same type as the current profiling context.
	 * To be used by each process independently.
	 *
	 * @return {@link Profiling}
	 */
	public Profiling createChild() {
		return new Profiling(this.store);
	}

	/**
	 * @return current ID for which profile data are quired by this Profiling object; null if none yet
	 */
	public String getCurrentId() {
		return this.lastId;
	}

	/**
	 * @return earliest time; {@link Long#MAX_VALUE} if no timing present
	 */
	public long getCurrentEarliest() {
		long min = Long.MAX_VALUE;
		for (AtomicLong time : this.timings.values()) {
			min = Math.min(min, time.longValue());
		}
		return min;
	}

	/**
	 * Record timing.
	 * All recordings with the same ID are evaluated together (having save time reference).
	 * The item represents the named recording in the sequence of profiled events.
	 *
	 * @param id != null
	 * @param item != null
	 * @param millis unique clock source
	 */
	public final void time(final String id, final String item, final long millis) {
		if (this.lastId != null) {
			if (!this.lastId.equals(id)) {
				flush();
			}
		}
		this.lastId = id;
		time(item, millis);
	}

	/**
	 * Record timing.
	 * The item represents the named recording in the sequence of profiled events.
	 * Assumed to belong to the same time reference as the previous recording.
	 *
	 * @param item != null
	 * @param millis time of the event from the unique system clock source
	 */
	public void time(final String item, final long millis) {
		this.timings.put(item, new AtomicLong(millis));
	}

	/**
	 * Record timing.
	 * The item represents the named recording in the sequence of profiled events.
	 * Assumed to belong to the same time reference as the previous recording.
	 *
	 * @param item != null
	 */
	public final void time(final String item) {
		time(item, System.currentTimeMillis());
	}

	/**
	 * Stores the current recordings in the {@link #getStore() store}.
	 * Discards the in-memory recordings but remembers the last used id.
	 */
	public synchronized void flush() {
		if (this.timings.isEmpty()) {
			return;
		}
		try {
			Store outStore = getStore();
			if (outStore == null) {
				return;
			}
			new OutputProducer(true) {
				@Override
				protected Object produce() {
					StringBuilder sb = new StringBuilder();
					for (Map.Entry<String, AtomicLong> me : Profiling.this.timings.entrySet()) {
						sb.append(Profiling.this.lastId).append(PROFILING_FORMAT_SEPARATOR).append(me.getKey()).append(
								PROFILING_FORMAT_SEPARATOR).append(me.getValue()).append("\n");
					}
					return sb;
				}

				@Override
				protected void done() {
					Profiling.this.timings.clear();
				}
			}.produce(outStore);
		} catch (IOException e) {
			// we hope the next flush will save us
			Warden.disregardAndReport(e);
		}
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		flush();
	}

	/**
	 * @return {@link Store} used by this Profiling.
	 */
	public Store getStore() {
		return this.store;
	}

	/**
	 * Helper to evaluate a (possibly merged) Profiling report file.
	 * Empty or comment lines are ignored; invalid lines are logged and skipped.
	 *
	 * @param reportStore Store where the reports come from != null
	 * @return Profiling result after normalizing and averaging all recordings
	 * @throws IOException on IO problems
	 */
	public static Profiling evaluate(final Store reportStore) throws IOException {
		final Map<String, Profiling> profs = new HashMap<String, Profiling>();
		new LineConsumer<Void>() {
			@Override
			protected void consume(final String line) {
				String tline = line.trim();
				if (tline.startsWith(PROFILING_FORMAT_COMMENT)) {
					return;
				}
				if (tline.length() == 0) {
					return;
				}
				String[] parts = tline.split(PROFILING_FORMAT_SEPARATOR);
				if (parts.length < REQUIRED_PROFILING_FORMAT_ENTRIES) {
					Log.LOG.get().report("invalid format in line " + getLineNumber() + "(" + getStore() + "): '" + line + "'.");
				}
				try {
				String id = parts[0];
				String item = parts[1];
				long ms = Long.parseLong(parts[2]);

				Profiling prof = profs.get(id);
				if (prof == null) {
					prof = new Profiling();
					profs.put(id, prof);
				}
				prof.time(item, ms);
				} catch (Exception e) {
					Warden.disregardAndReport(e);
				}
			}
		}.consume(reportStore);

		Profiling sum = new Profiling() {
			Map<String, AtomicInteger> counts = new HashMap<String, AtomicInteger>();
			@Override
			protected void change(final String key) {
				AtomicInteger c = this.counts.get(key);
				if (c == null) {
					c = new AtomicInteger();
					this.counts.put(key, c);
				}
				c.incrementAndGet();
			}

			@Override
			public void normalize() {
				for (Map.Entry<String, AtomicLong> me : this.timings.entrySet()) {
					me.getValue().set(me.getValue().get() / this.counts.get(me.getKey()).intValue());
				}
			}

			@Override
			public int getCount(final String item) {
				AtomicInteger cnt = this.counts.get(item);
				return cnt == null ? 0 : cnt.intValue();
			}

			@Override
			public synchronized void flush() {
				// NOOP
			}
			@Override
			public String toString() {
				return this.counts.toString() + "\n" + super.toString();
			}
		};
		for (Profiling entry : profs.values()) {
			entry.normalize();
			sum.add(entry);
		}
		sum.normalize();
		return sum;
	}

	public void add(final Profiling entry) {
		for (Map.Entry<String, AtomicLong> me : entry.timings.entrySet()) {
			AtomicLong presentVal = this.timings.get(me.getKey());
			if (presentVal == null) {
				this.timings.put(me.getKey(), new AtomicLong(me.getValue().longValue()));
			} else {
				presentVal.addAndGet(me.getValue().longValue());
			}
			change(me.getKey());
		}
	}

	/**
	 * @param key to change by derived class
	 */
	protected void change(final String key) {
		// for derived
	}

	public void normalize() {
		long min = Long.MAX_VALUE;
		for (AtomicLong ms : this.timings.values()) {
			min = Math.min(min, ms.longValue());
		}
		add(-min);
	}

	public void add(final long value) {
		for (Map.Entry<String, AtomicLong> me : this.timings.entrySet()) {
			me.getValue().addAndGet(value);
		}
	}

	/**
	 * @param item recording key != null
	 * @return Long ms or null if unknown
	 */
	public Number getTiming(final String item) {
		return this.timings.get(item);
	}

	protected class Report implements Comparable<Report> {
		private final long time;
		private final String item;

		protected Report(final long timeMs, final String item) {
			this.time = timeMs;
			this.item = item;
		}

		@Override
		public int compareTo(final Report o) {
			if (this.time < o.time) {
				return -1;
			}
			if (this.time > o.time) {
				return 1;
			}
			return this.item.compareTo(o.item);
		}

		@Override
		public String toString() {
			return this.time + " " + this.item;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + (this.item == null ? 0 : this.item.hashCode());
			result = prime * result + (int) (this.time ^ this.time >>> 32);
			return result;
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			Report other = (Report) obj;
			if (!getOuterType().equals(other.getOuterType())) {
				return false;
			}
			if (this.item == null) {
				if (other.item != null) {
					return false;
				}
			} else if (!this.item.equals(other.item)) {
				return false;
			}
			if (this.time != other.time) {
				return false;
			}
			return true;
		}

		private Profiling getOuterType() {
			return Profiling.this;
		}
	}

	@Override
	public String toString() {
		SortedSet<Report> items = new TreeSet<Report>();
		for (Map.Entry<String, AtomicLong> me : this.timings.entrySet()) {
			items.add(createReport(me.getValue().longValue(), me.getKey()));
		}
		StringBuilder sb = new StringBuilder();
		for (Report r : items) {
			sb.append(r).append("\n");
		}
		return sb.toString();
	}

	public int getCount(final String item) {
		return this.timings.containsKey(item) ? 1 : 0;
	}

	protected Report createReport(final long timeMs, final String item) {
		return new Report(timeMs, item);
	}
}
