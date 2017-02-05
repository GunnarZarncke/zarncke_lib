/**
 *
 */
package de.zarncke.lib.sys.mbean;

import org.joda.time.format.DateTimeFormatter;

import de.zarncke.lib.sys.Headquarters;
import de.zarncke.lib.sys.Headquarters.Status;
import de.zarncke.lib.sys.module.Module;

/**
 * Exposes Module state of a specific module for monitoring.
 *
 * @author Gunnar Zarncke
 */
public class ModuleAccess implements ModuleAccessMBean {
	private final DateTimeFormatter formatter;
	private final Module m;

	ModuleAccess(final DateTimeFormatter formatter, final Module m) {
		this.formatter = formatter;
		this.m = m;
	}

	@Override
	public void tryRecovery() {
		this.m.tryRecovery();
	}

	@Override
	public void startOrRestart() {
		this.m.startOrRestart();
	}

	@Override
	public void shutdown() {
		this.m.shutdown();
	}

	@Override
	public void kill() {
		this.m.kill();
	}

	private Status getStatus() {
		return Headquarters.HEADQUARTERS.get().getStatusFor(this.m);
	}

	@Override
	public String getTimeOfNextReset() {
		return this.formatter.print(getStatus().getTimeOfNextReset());
	}

	@Override
	public int getNumberOfInfoReports() {
		return getStatus().getNumberOfInfoReports();
	}

	@Override
	public int getNumberOfInfoMessages() {
		return getStatus().getNumberOfInfoMessages();
	}

	@Override
	public int getNumberOfErrorReports() {
		return getStatus().getNumberOfErrorReports();
	}

	@Override
	public int getNumberOfErrorMessages() {
		return getStatus().getNumberOfErrorMessages();
	}

	@Override
	public String getName() {
		return this.m.getName().getDefault();
	}

	@Override
	public String getLastErrorSummary() {
		return getStatus().getLastErrorSummary();
	}

	@Override
	public double getLoad() {
		try {
			return this.m.getLoad();
		} catch (Exception e) {
			return Double.NaN;
		}
	}

	@Override
	public String getHealth() {
		try {
			return this.m.getHealth().name();

			// Health mNow = this.m.getHealth();
			// Health mLast = getStatus().getHealth();
			// return mNow.ordinal() > mLast.ordinal() ? mNow.name() : mLast.name();
		} catch (Exception e) {
			return "EXCEPTION";
		}
	}

	@Override
	public String getState() {
		return this.m.getState().name();
	}
	@Override
	public void clearStatus() {
		getStatus().clear();
	}
}