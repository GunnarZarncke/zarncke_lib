package de.zarncke.lib.cache;

import java.util.List;
import java.util.Map;

import com.google.common.collect.MapMaker;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.ctx.Context;
import de.zarncke.lib.i18n.Translations;
import de.zarncke.lib.sys.Health;
import de.zarncke.lib.sys.module.Module;
import de.zarncke.lib.value.Default;

/**
 * Defines a global memory monitoring Module where {@link MemoryUsage memory using classes} may register and where the
 * overall
 * memory state may be monitored. A limited form of emmory control (clearing caches) is also provided.
 *
 * @author Gunnar Zarncke
 */
public class MemoryMonitor implements Module {

	public static final Context<MemoryMonitor> CTX = Context.of(Default.of(new MemoryMonitor(), MemoryMonitor.class));

	private final Map<MemoryUsage, MemoryControl> known = new MapMaker().weakValues().makeMap();

	private State state = State.UNDEFINED;

	/**
	 * Creates a simple MemoryControl for a Map holding significant amounts of memory.
	 * The refernce to the control should be stored together with the Map (otherwise the control will ge GCed shortly).
	 *
	 * @param map != null
	 * @return control (should be stored and then registered)
	 */
	public static MemoryControl controlOf(final Map<?, ?> map) {
		return new MemoryControl() {

			@Override
			public boolean clear() {
				map.clear();
				return true;
			}

			@Override
			public void cleanUp() {
				// noop
			}
		};
	}

	public static <T> MemoryUsage usageOf(final String name, final Map<?, T> map, final Class<? super T> elementClass) {
		return new BaseMapUsage(map, elementClass, name);
	}

	public static <T> MemoryUsage usageOf(final String name, final Map<?, ?> map, final int typicalObjectSize) {
		return new AbstractMapUsage(map, name) {
			@Override
			public int getTypicalObjectSize() {
				return typicalObjectSize;
			}
		};
	}

	public static <T> MemoryUsage usageOf(final String name, final Map<?, String> map) {
		return new StringMapUsage(map, name);
	}

	/**
	 * Registers memory.
	 * To work properly with garbage collection, the MemoryControl must be a part of the memory object (e.g. by letting
	 * the
	 * memory implement the MemoryControl interface).
	 * If the control is collected, then the mapping in the monitor will be collected too.
	 *
	 * @param memoryUser != null
	 * @param memoryControl != null (is kept as a weak reference)
	 */
	public void register(final MemoryUsage memoryUser, final MemoryControl memoryControl) {
		this.known.put(memoryUser, memoryControl);
	}

	/**
	 * Removes memory from the monitor.
	 * In case you don't trust the weak reference.
	 *
	 * @param memoryUser != null
	 */
	public void unregister(final MemoryUsage memoryUser) {
		this.known.remove(memoryUser);
	}

	public long getTotalMonitoredMemory() {
		maybeRefresh();
		long total = 0;
		for (MemoryUsage mc : getMemoryUsers()) {
			long l = mc.getAllocatedBytes();
			if (l < 0) {
				l = (long) mc.getAllocatedObjects() * mc.getTypicalObjectSize();
			}
			total += l;
		}
		return total;
	}

	public List<MemoryUsage> getMemoryUsers() {
		// TODO return a copy of the values to avoid too many calls
		maybeRefresh();
		return L.copy(this.known.keySet());
	}

	public void cleanUp() {
		for (MemoryControl mc : this.known.values()) {
			mc.cleanUp();
		}
	}

	public void clear(final MemoryUsage memory) {
		MemoryControl mc = this.known.get(memory);
		if (mc != null) {
			mc.clear();
		}
		refresh();
	}

	private void maybeRefresh() {
		// TODO check time for automatic refresh
	}

	public void refresh() {
		// TODO copy values
	}

	@Override
	public String toString() {
		return getTotalMonitoredMemory() + " used in " + this.known.size() + " memory stores.";
	}

	@Override
	public String getMetaInformation() {
		StringBuilder sb = new StringBuilder(toString());
		for (MemoryUsage mem : this.known.keySet()) {
			sb.append(mem.getMemoryName()).append("->")
					//
					.append(mem.getAllocatedObjects()).append(" objects ").append(mem.getTypicalObjectSize())
					.append(" bytes each ").append(mem.getAllocatedBytes()).append(" bytes total\n");
		}
		sb.append("health ").append(getHealth()).append(" due to load ").append(getLoad());
		return sb.toString();
	}

	@Override
	public Translations getName() {
		return new Translations("Memory Monitor");
	}

	@Override
	public List<Module> getSubModules() {
		return L.e();
	}

	@Override
	public State getState() {
		return this.state;
	}

	@Override
	public void shutdown() {
		this.state = State.GOING_DOWN;
		for (MemoryControl mc : this.known.values()) {
			mc.clear();
		}
		this.state = State.DOWN;
	}

	@Override
	public void kill() {
		// nop
	}

	@Override
	public void tryRecovery() {
		this.state = State.RECOVERING;
		cleanUp();
		this.state = State.UP;
	}

	@Override
	public void startOrRestart() {
		if (this.state == State.DOWN || this.state == State.UNINITIALIZED) {
			shutdown();
		}
		this.state = State.UP;
	}

	@Override
	public Health getHealth() {
		double l = getLoad();
		return l == 0.0 ? Health.VIRGIN : //
				l < 0.5 ? Health.CLEAN : //
						l < 1.0 ? Health.WARNINGS : //
								Health.ERRORS;
	}

	@Override
	public double getLoad() {
		double memFraction = (double) getTotalMonitoredMemory() / Runtime.getRuntime().totalMemory();
		return memFraction * 2;
	}
}
