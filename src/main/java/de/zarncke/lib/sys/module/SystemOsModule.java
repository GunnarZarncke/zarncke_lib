/**
 *
 */
package de.zarncke.lib.sys.module;

import java.io.File;
import java.io.IOException;
import java.util.List;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.i18n.Translations;
import de.zarncke.lib.io.IOTools;
import de.zarncke.lib.io.StoreConsumer;
import de.zarncke.lib.io.store.Store;
import de.zarncke.lib.sys.Health;
import de.zarncke.lib.util.Misc;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * This Module monitors some aspects of the OS.
 * Only available on Linux.
 *
 * @author Gunnar Zarncke
 */
public class SystemOsModule implements Module {
	private boolean virgin = true;
	private State state = State.UNINITIALIZED;

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
		Health h = checkHealth();
		if (h != Health.VIRGIN) {
			this.virgin = false;
			return h;
		}
		return this.virgin ? h : Health.CLEAN;
	}

	private Health checkHealth() {
		double l = getLoad();
		return l == Double.NaN ? Health.VIRGIN //
				: l < .1 ? Health.INFO //
						: l > 1.0 ? Health.WARNINGS //
								: l > 5.0 ? Health.ERRORS //
										: l > 20.0 ? Health.FAILURE : Health.CLEAN;
	}

	@Override
	public Translations getName() {
		return new Translations("OS");
	}

	@Override
	public void shutdown() {
		this.state = State.DOWN;
	}

	@Override
	public void kill() {
		// nop - we might try to kill some processes (params)
	}

	@Override
	public void startOrRestart() {
		this.state = State.UP;
	}

	@Override
	public void tryRecovery() {
		// nop - we might try
		// - swapoff/on
		// - restart some processes
		// - clear /tmp
	}

	@Override
	@SuppressWarnings({ "DMI", "REC" })
	public double getLoad() {
		try {
			String loadavg = new String(IOTools.getAllBytes(new File("/proc/loadavg")), Misc.ASCII);
			int p = loadavg.indexOf(' ');
			return Double.parseDouble(p > 0 ? loadavg.substring(0, p) : loadavg);
		} catch (Exception e) { // NOPMD may not pass up, /proc only on Linux
			return Double.NaN;
		}
	}

	@Override
	public String toString() {
		return "OS";
	}

	@Override
	public String getMetaInformation() {
		try {
			return Misc.processCommand("/usr/bin/uname -a", new StoreConsumer<String>() {
				@Override
				public String consume(final Store storeToProcess) throws IOException {
					return new String(IOTools.getAllBytes(storeToProcess.getInputStream()), Misc.ASCII);
				}
			}).getFirst();
		} catch (IOException e) {
			Warden.disregard(e);
			return null;
		}
	}
}
