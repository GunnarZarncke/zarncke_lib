/**
 *
 */
package de.zarncke.lib.sys.module;

import java.io.FileInputStream;
import java.io.IOException;

import de.zarncke.lib.err.Warden;
import de.zarncke.lib.i18n.Translations;
import de.zarncke.lib.io.IOTools;
import de.zarncke.lib.jna.LinuxFunctions;
import de.zarncke.lib.sys.Health;
import de.zarncke.lib.util.Misc;

/**
 * This Module tracks the system RAM.
 * Only available on Linux.
 *
 * @author Gunnar Zarncke
 */
public class SystemRamModule extends AbstractModule {
	private boolean virgin;

	@Override
	public Health getHealth() {
		Health h = checkRamHealth();
		if (h != Health.VIRGIN) {
			this.virgin = false;
			return h;
		}
		return this.virgin ? h : Health.CLEAN;
	}

	@Override
	public Translations getName() {
		return new Translations("RAM");
	}


	private Health checkRamHealth() {
		double load = getLoad();
		if (load == Double.NaN) {
			return Health.VIRGIN;
		}
		if (load >= 0.99) {
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
		Long total = Misc.getTotalSystemRamBytes();
		Long free = Misc.getFreeSystemRamBytes();
		if (total == null || free == null) {
			return Double.NaN;
		}
		return 1.0 - free.longValue() / (double) total.longValue();
	}

	@Override
	public String toString() {
		return "RAM";
	}

	@Override
	public String getMetaInformation() {
		try {
			return new String(IOTools.getAllBytes(new FileInputStream(LinuxFunctions.PROC_MEMINFO)), Misc.ASCII);
		} catch (IOException e) {
			Warden.disregard(e);
			return null;
		}
	}
}