/**
 *
 */
package de.zarncke.lib.sys;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.i18n.Translations;
import de.zarncke.lib.io.store.Store;
import de.zarncke.lib.io.store.StoreUtil;
import de.zarncke.lib.lang.CodeResponsible;
import de.zarncke.lib.lang.Piece;
import de.zarncke.lib.sys.module.AbstractModule;
import de.zarncke.lib.sys.module.JavaVersionModule;
import de.zarncke.lib.sys.module.Module;
import de.zarncke.lib.util.Chars;
import de.zarncke.lib.util.Misc;

/**
 * This Module tracks the state of the Java VM and implements {@link #shutdown()}.
 *
 * @author Gunnar Zarncke
 */
public class JavaLangModule extends AbstractModule implements CodeResponsible, BinarySupplier {
	public static final String KEY_GARBAGE_COLLECT = "garbage_collect";
	public static final String KEY_USED_MEMORY = "used_memory";
	public static final String KEY_FREE_MEMORY = "free_memory";
	public static final String KEY_TOTAL_MEMORY = "total_memory";
	public static final String KEY_MEMORY_TEXT = "memory_text";

	private static final double REAL_FREE_MEM_FRACTION_TRESHOLD = 0.10;
	private static final double TOTAL_MEM_FRACTION_TRESHOLD = 0.95;
	private static final double FREE_MEM_FRACTION_TRESHOLD = 0.05;
	private static final int RECOVERY_INTER_GC_SLEEP_MILLIS = 10;

	private static final NumberFormat DFLT_MEMORY_FORMAT = NumberFormat.getInstance(Locale.GERMAN);

	private boolean virgin = true;
	private final boolean isRoot;

	protected final List<Module> subModules = L.l();

	/**
	 * If this module is the root it will forward kill and shutdown to the Java VM thus finishing the shutdown process.
	 *
	 * @param root true: {@link System#exit} on shutdown.
	 */
	public JavaLangModule(final boolean root) {
		this.isRoot = root;
		addModules(); // TODO breaks extensibility (inconsistent init)
	}

	/**
	 * Derived classes may change default module. Default: {@link JavaVersionModule} (default).
	 */
	protected void addModules() {
		this.subModules.add(new JavaVersionModule());
	}

	@Override
	public List<Module> getSubModules() {
		return this.subModules;
	}

	@Override
	public Health getHealth() {
		Health h = checkMemoryHealth();
		if (h != Health.VIRGIN) {
			this.virgin = false;
			return h;
		}
		return this.virgin ? h : Health.CLEAN;
	}

	@Override
	public Translations getName() {
		return new Translations(this.isRoot ? "default" : "java.lang");
	}

	@Override
	protected void doShutdown() {
		if (this.isRoot) {
			System.exit(0);
		}
	}

	@Override
	public void kill() {
		if (this.isRoot) {
			Runtime.getRuntime().halt(-1);
		}
	}

	@Override
	protected void doStartup() {
		super.doStartup();
	}

	@Override
	protected void doRecovery() {
		System.gc();
		try {
			Thread.sleep(RECOVERY_INTER_GC_SLEEP_MILLIS);
		} catch (InterruptedException e) {
			Warden.disregard(e);
		}
		System.runFinalization();
		System.gc();
	}

	private Health checkMemoryHealth() {
		Runtime r = Runtime.getRuntime();
		if (r.maxMemory() == Integer.MAX_VALUE) {
			if (r.freeMemory() < r.totalMemory() * FREE_MEM_FRACTION_TRESHOLD) {
				setLastMessage("low memory (free " + r.freeMemory() + " of total " + r.totalMemory() + ")");
				return Health.WARNINGS;
			}
		} else {
			if (r.totalMemory() > r.maxMemory() * TOTAL_MEM_FRACTION_TRESHOLD) {
				setLastMessage("total memory near max memory (total=" + r.totalMemory() + ", max " + r.maxMemory()
						+ ")");
				return Health.WARNINGS;
			}
			long realFree = r.maxMemory() - r.totalMemory() + r.freeMemory();
			if (realFree < r.maxMemory() * REAL_FREE_MEM_FRACTION_TRESHOLD) {
				setLastMessage("low memory (max=" + r.maxMemory() + ", total=" + r.totalMemory() + ", free="
						+ r.freeMemory() + ")");
				return Health.WARNINGS;
			}
		}

		return Health.VIRGIN;
	}

	@Override
	public double getLoad() {
		Runtime r = Runtime.getRuntime();
		if (r.maxMemory() == Integer.MAX_VALUE) {
			return 1.0 - r.freeMemory() / (double) r.totalMemory();
		}
		return 1.0 - (r.maxMemory() - r.totalMemory() + r.freeMemory()) / (double) r.maxMemory();
	}

	@Override
	public String toString() {
		return "java";
	}

	@Override
	public String getMetaInformation() {
		StringBuilder sb = new StringBuilder();
		sb.append(Chars.lineWrap(new TreeMap<String, String>(Misc.ENVIRONMENT.get()).toString(), 200));
		sb.append("\n");
		if (!Chars.isEmpty(getLastMessage())) {
			sb.append(getLastMessage());
		}
		return sb.toString();
	}

	@Override
	public boolean isResponsibleFor(final Piece code) {
		if (code.getName().startsWith("java.")) {
			return true;
		}
		if (code.getName().startsWith("javax.")) {
			return true;
		}
		return false;
	}

	@Override
	public Store getBinaryInformation(final Object key) {
		if (KEY_MEMORY_TEXT.equals(key)) {
			final long totalMemory = Runtime.getRuntime().totalMemory();
			final long freeMemory = Runtime.getRuntime().freeMemory();
			final long usedMemory = totalMemory - freeMemory;
			return StoreUtil.asStore(String.format("total memory: %s bytes\nused memory:  %s\nfree memory:  %s",
					DFLT_MEMORY_FORMAT.format(totalMemory), DFLT_MEMORY_FORMAT.format(usedMemory),
					DFLT_MEMORY_FORMAT.format(freeMemory)), null);
		}
		if (KEY_TOTAL_MEMORY.equals(key)) {
			final long totalMemory = Runtime.getRuntime().totalMemory();
			return StoreUtil.asStore(String.valueOf(totalMemory), null);
		}
		if (KEY_FREE_MEMORY.equals(key)) {
			final long freeMemory = Runtime.getRuntime().freeMemory();
			return StoreUtil.asStore(String.valueOf(freeMemory), null);
		}
		if (KEY_USED_MEMORY.equals(key)) {
			final long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			return StoreUtil.asStore(String.valueOf(usedMemory), null);
		}
		if (KEY_GARBAGE_COLLECT.equals(key)) {
			System.gc();
			final long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			return StoreUtil.asStore(
					String.format("Used memory %s bytes (after garbage collection)",
							DFLT_MEMORY_FORMAT.format(usedMemory)), null);
		}
		return null;
	}
}