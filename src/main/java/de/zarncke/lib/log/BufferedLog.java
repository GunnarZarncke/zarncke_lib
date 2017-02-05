package de.zarncke.lib.log;

import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.concurrent.ThreadSafe;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.err.Warden;

/**
 * A Log which buffers all logged entries.
 * Supports flushing to a delegate Log.
 * Can be used to postpone actual logging.
 * 
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
@ThreadSafe
public class BufferedLog implements Log {

	private Log delegate = Log.NULL_LOG;

	private final Queue<Object> buffer;

	private int numberOfBufferedIssues;

	private int numberOfBufferedThrowables;

	public BufferedLog() {
		this.buffer = new ConcurrentLinkedQueue<Object>();
	}

	@Override
	public void report(final Throwable throwableToReport) {
		this.buffer.add(throwableToReport);
		this.numberOfBufferedThrowables++;
	}

	@Override
	public void report(final CharSequence issue) {
		this.buffer.add(issue);
		this.numberOfBufferedIssues++;
	}

	@Override
	public void report(final Object... debugObjects) {
		this.buffer.add(debugObjects);
	}

	public Log getDelegate() {
		return this.delegate;
	}

	public void setDelegate(final Log delegate) {
		this.delegate = delegate;
	}

	/**
	 * Removes all buffered reports.
	 */
	public void clear() {
		while (!this.buffer.isEmpty()) {
			Object elem = this.buffer.remove();
			if (elem instanceof Throwable) {
				this.numberOfBufferedThrowables--;
			} else if (elem instanceof CharSequence) {
				this.numberOfBufferedIssues--;
			}
		}
	}

	/**
	 * Removes all buffered reports.
	 *
	 * @return the removed entries
	 */
	public List<Object> removeAndReturn() {
		List<Object> removedEntries = L.l();
		while (!this.buffer.isEmpty()) {
			Object elem = this.buffer.remove();
			removedEntries.add(elem);
			if (elem instanceof Throwable) {
				this.numberOfBufferedThrowables--;
			} else if (elem instanceof CharSequence) {
				this.numberOfBufferedIssues--;
			}
		}
		return removedEntries;
	}

	/**
	 * Flushes out all reports to the delegate (which must have been set).
	 */
	public void flush() {
		if (this.delegate == null) {
			throw Warden.spot(new IllegalStateException("no delegate set."));
		}
		while (!this.buffer.isEmpty()) {
			Object elem = this.buffer.remove();
			if (elem instanceof Throwable) {
				this.delegate.report((Throwable) elem);
				this.numberOfBufferedThrowables--;
			} else if (elem instanceof CharSequence) {
				this.delegate.report(elem.toString());
				this.numberOfBufferedIssues--;
			} else {
				this.delegate.report((Object[]) elem);
			}
		}
	}

	/**
	 * Reports multiple issues at once.
	 * Throwables and CharSequences are reported as that.
	 *
	 * @param log to Log into
	 * @param entries to report
	 */
	public static void reportBatch(final Log log, final Collection<?> entries) {
		for (Object elem : entries) {
			if (elem instanceof Throwable) {
				log.report((Throwable) elem);
			} else if (elem instanceof CharSequence) {
				log.report((CharSequence) elem);
			} else {
				log.report(elem);
			}
		}
	}

	public void reportBatch(final Collection<?> entries) {
		reportBatch(entries);
	}

	public int getNumberOfBufferedIssues() {
		return this.numberOfBufferedIssues;
	}

	public int getNumberOfBufferedThrowables() {
		return this.numberOfBufferedThrowables;
	}

	@Override
	public String toString() {
		return "buffer for " + this.delegate + "with " + this.buffer.size() + " pending messages";
	}

}
