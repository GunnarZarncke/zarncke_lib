/**
 *
 */
package de.zarncke.lib.sys.module;

import java.io.File;
import java.util.List;

import javax.annotation.Nonnull;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.coll.Pair;
import de.zarncke.lib.i18n.Translations;
import de.zarncke.lib.io.IOTools;
import de.zarncke.lib.sys.Health;

/**
 * This Module tracks the system disk space.
 *
 * @author Gunnar Zarncke
 */
public class SystemDiskModule implements Module {
	private boolean virgin = true;
	private State state = State.UNINITIALIZED;
	private final File fileOnFileSystem;
	private final String name;

	public SystemDiskModule() {
		this(IOTools.getTempDir());
	}

	public SystemDiskModule(final File fileOnFileSystem) {
		this("DISK", fileOnFileSystem);
	}

	/**
	 * @param name name to use, default is "DISK"
	 * @param fileOnFileSystem to determine space of, defaults to temp
	 */
	public SystemDiskModule(@Nonnull final String name, @Nonnull final File fileOnFileSystem) {
		this.name = name;
		this.fileOnFileSystem = fileOnFileSystem;
	}

	@Override
	public State getState() {
		return this.state;
	}

	@Override
	public List<Module> getSubModules() {
		return L.e();
	}

	@Override
	public Health getHealth() {
		Health h = checkDiskHealth();
		if (h != Health.VIRGIN) {
			this.virgin = false;
			return h;
		}
		return this.virgin ? h : Health.CLEAN;
	}

	@Override
	public Translations getName() {
		return new Translations(this.name);
	}

	@Override
	public void shutdown() {
		this.state = State.DOWN;
	}

	@Override
	public void kill() {
		// nop
	}

	@Override
	public void startOrRestart() {
		this.state = State.UP;
	}

	@Override
	public void tryRecovery() {
		// nop
	}

	private Health checkDiskHealth() {
		double load = getLoad();
		if (load == Double.NaN) {
			return Health.VIRGIN;
		}
		if (load >= 0.99) {
			return Health.FAILURE;
		}
		if (load >= 0.95) {
			return Health.ERRORS;
		}
		if (load > 0.9) {
			return Health.WARNINGS;
		}
		if (load > 0.8) {
			return Health.OK;
		}
		return Health.CLEAN;
	}

	@Override
	public double getLoad() {
		Pair<Long, Long> disk = IOTools.getAvailableSystemDiskBytes(this.fileOnFileSystem);
		if (disk == null) {
			return Double.NaN;
		}
		return 1.0 - disk.getFirst().longValue() / (double) disk.getSecond().longValue();
	}

	@Override
	public String toString() {
		return "DISK";
	}

	@Override
	public String getMetaInformation() {
		Pair<Long, Long> disk = IOTools.getAvailableSystemDiskBytes(this.fileOnFileSystem);
		return disk == null ? "disk size unknown" : disk.getFirst() + " free of " + disk.getSecond() + " total";
	}
}