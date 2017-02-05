package de.zarncke.lib.sys.module;

import java.util.List;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.i18n.Translations;
import de.zarncke.lib.lang.CodeResponsible;
import de.zarncke.lib.lang.Piece;
import de.zarncke.lib.log.Log;
import de.zarncke.lib.log.Report;
import de.zarncke.lib.log.group.GroupingLog;
import de.zarncke.lib.sys.Health;
import de.zarncke.lib.util.Misc;

/**
 * A {@link Module} for monitoring the {@link Log logging system} with special support for {@link GroupingLog}.
 *
 * @author Gunnar Zarncke
 */
public class LogModule implements Module, CodeResponsible {

	private static final double LOAD_FOR_TOTAL_MEM_USE = 10.0;
	private static final double LOG_MEM_FRACTION_ERROR = .9;
	private static final double LOG_MEM_FRACTION_WARNING = 0.1;
	private State state = State.UNINITIALIZED;

	@Override
	public Health getHealth() {
		Log log = Log.LOG.get();
		if (log instanceof GroupingLog) {
			int size;
			try {
				size = ((GroupingLog) log).estimatePendingReportVolume();
			} catch (Exception e) {
				Log.LOG.get().report("Cannot determine size of Logging - inconsistency?");
				return Health.ERRORS;
			}
			if (size == 0) {
				return Health.VIRGIN;
			}
			long total = Runtime.getRuntime().totalMemory();
			if (size > total * LOG_MEM_FRACTION_WARNING) {
				Log.LOG.get().report(
						"Memory used by Logging is sizable (about " + size / (int) Misc.BYTES_PER_KB + "KB)");
				return Health.WARNINGS;
			}
			if (size > total * LOG_MEM_FRACTION_ERROR) {
				Log.LOG.get().report(
						"Memory used by Logging is >90% total (about " + size / (int) Misc.BYTES_PER_KB + "KB)");
				return Health.ERRORS;
			}
			return Health.OK;
		}
		return Health.VIRGIN;
	}

	@Override
	public double getLoad() {
		int size;
		try {
			Log log = Log.LOG.get();
			size = ((GroupingLog) log).estimatePendingReportVolume();
		} catch (Exception e) {
			return 1.0;
		}
		long total = Runtime.getRuntime().totalMemory();
		return LOAD_FOR_TOTAL_MEM_USE * size / total;
	}

	@Override
	public State getState() {
		return this.state;
	}

	@Override
	public Translations getName() {
		return new Translations("Logging");
	}

	@Override
	public List<Module> getSubModules() {
		return L.e();
	}

	@Override
	public void kill() {
		//
	}

	@Override
	public void shutdown() {
		this.state = State.GOING_DOWN;
		Log log = Log.LOG.get();
		if (log instanceof GroupingLog) {
			try {
				((GroupingLog) log).flushAllReportsAndDropRecencyInformation(Report.Type.FINAL);
			} finally {
				((GroupingLog) log).stop();
			}
		}
		this.state = State.DOWN;
	}

	@Override
	public void startOrRestart() {
		this.state = State.GOING_UP;
		Log log = Log.LOG.get();
		if (log instanceof GroupingLog) {
			try {
				((GroupingLog) log).flushAllReportsAndDropRecencyInformation(Report.Type.FLUSH);
			} catch (RuntimeException e) {
				((GroupingLog) log).emergencyReinit();
				throw e;
			}
		}
		this.state = State.UP;
	}

	@Override
	public void tryRecovery() {
		this.state = State.RECOVERING;
		Log log = Log.LOG.get();
		if (log instanceof GroupingLog) {
			try {
				((GroupingLog) log).flushAllReportsAndDropRecencyInformation(Report.Type.FLUSH);
			} finally {
				((GroupingLog) log).emergencyReinit();
			}
		}
		this.state = State.UP;
	}

	@Override
	public String toString() {
		return "log";
	}

	@Override
	public String getMetaInformation() {
		Log log = Log.LOG.get();
		return log instanceof GroupingLog ? ((GroupingLog) log).getDetailedSummary() : log.toString();
	}

	@Override
	public boolean isResponsibleFor(final Piece code) {
		if (code.getName().startsWith("de.zarncke.lib.log.")) {
			return true;
		}
		return false;
	}
}
