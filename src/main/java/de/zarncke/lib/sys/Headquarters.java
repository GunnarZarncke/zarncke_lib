package de.zarncke.lib.sys;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.joda.time.DateTime;

import com.google.common.collect.MapMaker;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.ctx.Context;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.i18n.Translations;
import de.zarncke.lib.log.ExcerptableReport;
import de.zarncke.lib.log.Log;
import de.zarncke.lib.log.Report;
import de.zarncke.lib.log.Report.Type;
import de.zarncke.lib.log.SimpleReport;
import de.zarncke.lib.log.group.GroupingLog;
import de.zarncke.lib.log.group.ReportListener;
import de.zarncke.lib.sys.Installation.HealthStore;
import de.zarncke.lib.sys.module.Module;
import de.zarncke.lib.time.Times;
import de.zarncke.lib.value.Default;

/**
 * This is {@link Headquarters} of the application.
 * 
 * @author Gunnar Zarncke
 */
public class Headquarters implements Component, ReportListener {
	public static final Context<Headquarters> HEADQUARTERS = Context.of(Default.of(new Headquarters(), Headquarters.class));
	private static final int NUMBER_OF_LOGS_BEFORE_TIME_AND_RESCALE = 100;
	private static final long MAX_GRACE_WAIT_SLEEP_MILLIS = Times.MILLIS_PER_SECOND;
	private static final long MIN_CHECK_INTERVAL_MILLIS = Times.MILLIS_PER_MINUTE;
	private static final long STATUS_RESET_MILLIS = 24 * Times.MINUTES_PER_HOUR * Times.MILLIS_PER_MINUTE;
	public static final long ALLOWED_MS_PER_LOG = 100;
	public static final long LOG_RESCALE_PERIOD_MS = Times.MILLIS_PER_MINUTE;

	/**
	 * summarizes the system status of one {@link Module}.
	 */
	public class Status {
		private final Module module;
		private Health health = Health.VIRGIN;
		private int[] numberOfReports;
		private int[] numberOfMessages;
		private Health highestReportedLevel;
		private long nextReset;
		private final List<String> lastErrorSummaries = new ArrayList<String>(1);

		public Status(final Module module) {
			this.module = module;
			this.nextReset = System.currentTimeMillis() + Headquarters.this.statusResetPeriodMillis;
			clear();
		}

		public Module getModule() {
			return this.module;
		}

		public Health getHealth() {
			checkTime();
			return this.health;
		}

		public int getNumberOfReports(final Health level) {
			return this.numberOfReports[level.ordinal()];
		}

		private int getNumberOfMessages(final Health level) {
			return this.numberOfMessages[level.ordinal()];
		}

		public int getNumberOfErrorReports() {
			return getNumberOfReports(Health.ERRORS);
		}

		public int getNumberOfErrorMessages() {
			return getNumberOfMessages(Health.ERRORS);
		}

		public int getNumberOfInfoReports() {
			return getNumberOfReports(Health.INFO);
		}

		public int getNumberOfInfoMessages() {
			return getNumberOfMessages(Health.INFO);
		}

		public String getLastErrorSummary() {
			return this.lastErrorSummaries.isEmpty() ? "" : this.lastErrorSummaries.get(this.lastErrorSummaries.size() - 1);
		}

		/**
		 * @return in the order of occurrences
		 */
		public List<String> getLastErrorSummaries() {
			return this.lastErrorSummaries;
		}

		public DateTime getTimeOfNextReset() {
			return new DateTime(this.nextReset);
		}

		public List<Report> getLastReports() {
			return L.e();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (this.module == null ? 0 : this.module.hashCode());
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
			Status other = (Status) obj;
			if (this.module == null) {
				if (other.module != null) {
					return false;
				}
			} else if (!this.module.equals(other.module)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return this.module + ":" + this.health + "\n" + getLastErrorSummary();
		}

		public Health inform(final Report report) {
			return inform(report, Integer.MAX_VALUE);
		}

		public Health inform(final Report report, final int maxErrorsKept) {
			checkTime();

			Health severity = getServeritiesByCallerKey().get(report.getCaller().getSeverityKey());
			if (severity == null) {
				severity = report.getEstimatedSeverity();
			}

			this.numberOfReports[severity.ordinal()]++;
			this.numberOfMessages[severity.ordinal()] += report.numberOfOccurences();

			if (severity.ordinal() >= this.highestReportedLevel.ordinal()) {
				String error;
				if (report instanceof ExcerptableReport) {
					error = ((ExcerptableReport) report).getExcerpt().toString();
				} else {
					error = report.getSummary();
				}
				this.lastErrorSummaries.add(error);
				while (this.lastErrorSummaries.size() > maxErrorsKept) {
					this.lastErrorSummaries.remove(0);
				}
				this.highestReportedLevel = severity;
			}
			if (severity.ordinal() > this.health.ordinal()) {
				this.health = severity;
			}

			return severity;
		}

		protected void checkTime() {
			if (System.currentTimeMillis() > this.nextReset) {
				clear();
			}
		}

		void informAboutHealthChange(final Health newHealth) {
			if (newHealth.ordinal() > this.health.ordinal()) {
				this.health = newHealth;
			}
		}

		public void clear() {
			this.numberOfReports = new int[Health.DEAD.ordinal() + 1];
			this.numberOfMessages = new int[Health.DEAD.ordinal() + 1];
			this.highestReportedLevel = Health.VIRGIN;
			this.lastErrorSummaries.clear();

			if (this.health != Health.VIRGIN) {
				this.health = Health.CLEAN;
			}
			this.nextReset = Math.max(System.currentTimeMillis(), this.nextReset) + Headquarters.this.statusResetPeriodMillis;
		}
	}

	private final Installation installation;
	private Thread shutdownHandler;
	private Thread killHandler;
	private long statusResetPeriodMillis;
	private long lastCheckMs;
	private Health lastHealth = Health.VIRGIN;
	private final Map<Module, Status> statusByModule = new MapMaker().makeMap();
	private boolean immediateLoggingEnabled = false;

	private int logsSince = 0;
	private long lastLogMs = System.currentTimeMillis();
	private int logsDiscarded;
	private long logsTotal;
	private int maxReportsKept = 10;

	public Headquarters() {
		this(new DefaultInstallation());
	}

	public Headquarters(final Installation installation) {
		this.installation = installation;
		this.statusResetPeriodMillis = STATUS_RESET_MILLIS;
	}

	/**
	 * Returns a snapshot of the aggregated system health.
	 * 
	 * @return Health of the overall system
	 */
	@Override
	public Health getHealth() {
		if (this.lastCheckMs > System.currentTimeMillis() - MIN_CHECK_INTERVAL_MILLIS) {
			return this.lastHealth;
		}

		// TODO 20121205 gunnar This seems to call module getHealth indirectly multiple times
		Health health = checkModuleHealthUpdateStatusAndGetAggregate();
		this.lastHealth = health;
		this.lastCheckMs = System.currentTimeMillis();
		return this.lastHealth;
	}

	/**
	 * Determines the overall system state.
	 * This operation may be quite expensive
	 * 
	 * @return aggregated health
	 */
	public Health checkModuleHealthUpdateStatusAndGetAggregate() {
		HealthStore hStore = getInstallation().getHealthStore();
		Map<Module, Health> healthByModule = hStore.getHealthByModule();
		int deaths = 0;
		Health health = Health.VIRGIN;
		Collection<Module> modules = getAllModules();
		for (Module m : modules) {
			try {
				Health mHealth;
				try {
					mHealth = m.getHealth();
					getStatusFor(m).informAboutHealthChange(mHealth);
				} catch (Throwable t) { // NOPMD generic
					Warden.disregardAndReport(t);
					mHealth = healthByModule.get(m);
				}

				// include status health in total system health
				Health mLast = getStatusFor(m).getHealth();
				if (mLast.ordinal() > mHealth.ordinal()) {
					mHealth = mLast;
				}

				try {
					hStore.remember(m, mHealth);
				} catch (Throwable t) { // NOPMD generic
					Warden.disregardAndReport(t);
				}
				if (mHealth.ordinal() > health.ordinal()) {
					if (mHealth == Health.DEAD) {
						health = Health.FAILURE;
						deaths++;
					}
				}
			} catch (Throwable t) { // NOPMD generic
				health = Health.FAILURE;
				deaths++;
			}
		}
		if (deaths == modules.size()) {
			health = Health.DEAD;
		}
		return health;
	}

	/**
	 * Returns all modules in the system (including sub children).
	 * 
	 * @return List of modules in depth first order
	 */
	public Collection<Module> getAllModules() {
		return getAllModules(this.installation.getRootModule());
	}

	/**
	 * @param topModule to get all (sub) modules for
	 * @return Collection of modules in depth first order with no duplicates
	 */
	public static Collection<Module> getAllModules(final Module topModule) {
		Collection<Module> mods = new LinkedHashSet<Module>();
		accumulateModules(topModule, mods);
		return Collections.unmodifiableCollection(mods);
	}

	/**
	 * Return the module with the given name (using the default translation).
	 * 
	 * @param name != null
	 * @return Module != null
	 * @throws IllegalArgumentException if module unknown
	 */
	public Module getModuleByName(final String name) {
		// first try to interpret name as path
		Module m = locateByPath(this.installation.getRootModule(), name);
		if (m != null) {
			return m;
		}
		// else try all directly
		for (Module mod : getAllModules()) {
			if (name.equals(mod.getName().getDefault())) {
				return mod;
			}
		}
		throw Warden.spot(new IllegalArgumentException("unknown module " + name));
	}

	private Module locateByPath(final Module rootModule, final String path) {
		int p = path.indexOf("/");
		if (p >= 0) {
			String firstLevel = path.substring(0, p);
			String remaining = path.substring(p + 1);
			for (Module mod : rootModule.getSubModules()) {
				if (firstLevel.equals(mod.getName().getDefault())) {
					return locateByPath(mod, remaining);
				}
			}
		} else {
			for (Module mod : rootModule.getSubModules()) {
				if (path.equals(mod.getName().getDefault())) {
					return mod;
				}
			}
		}
		return null;
	}

	/**
	 * Duplicates are not added.
	 * Modules are added together with all their children.
	 * 
	 * @param module to add all (sub) modules for
	 * @param modulesSoFar to add to
	 */
	public static void accumulateModules(final Module module, final Collection<Module> modulesSoFar) {
		if (modulesSoFar.contains(module)) {
			return;
		}
		modulesSoFar.add(module);
		for (Module m : module.getSubModules()) {
			accumulateModules(m, modulesSoFar);
		}
	}

	/**
	 * Registers {@link Runtime#addShutdownHook(Thread) Java shutdown hooks} for a orderly shutdown of the system.
	 * The shutdown hooks work as follows:
	 * First a {@link #shutdown()} will be tried. If this doesn't terminate within a
	 * {@link Installation#getShutdownGraceSeconds() grace time} {@link #kill()} will be executed.
	 */
	public synchronized void registerShutdownHook() {
		if (this.shutdownHandler != null) {
			Warden.report(new IllegalStateException("shutdownHook already registered!"));
			return;
		}
		this.shutdownHandler = new Thread("Headquarters.shutdown") {
			@Override
			public void run() {
				Log.LOG.get().report(
						"System " + getInstallation().getName() + " is going down (grace period is "
								+ getInstallation().getShutdownGraceSeconds() + " seconds).");
				shutdown();
			}
		};
		this.killHandler = new Thread("Headquarters.kill") {
			@Override
			public void run() {
				waitGrace(true);
				System.err.println("system is killed now!"); // NOPMD
				kill();
			}
		};
		// this allows the kill handler to terminate when the shutdown completes in time
		this.killHandler.setDaemon(true);

		// let them run in the callers context
		this.shutdownHandler.setContextClassLoader(Thread.currentThread().getContextClassLoader());
		this.killHandler.setContextClassLoader(Thread.currentThread().getContextClassLoader());

		Runtime.getRuntime().addShutdownHook(this.shutdownHandler);
		Runtime.getRuntime().addShutdownHook(this.killHandler);
	}

	private void waitGrace(final boolean ultimo) {
		long begin = System.currentTimeMillis();
		long graceMs = getInstallation().getShutdownGraceSeconds() * Times.MILLIS_PER_SECOND;
		if (!ultimo) {
			graceMs = graceMs * 9 / 10;
		}
		while (System.currentTimeMillis() - begin < graceMs) {
			// TODO we may exit earlier if the Modules signal termination
			State total = getState(getInstallation().getRootModule(), false);
			if (total == State.DOWN) {
				return;
			}

			try {
				Thread.sleep(graceMs < MAX_GRACE_WAIT_SLEEP_MILLIS ? graceMs : MAX_GRACE_WAIT_SLEEP_MILLIS);
			} catch (InterruptedException e) {
				return;
			}
		}
	}

	/**
	 * Calls {@link Module#startOrRestart()} on each module in the order returned by {@link Module#getSubModules()} first
	 * calling start() on the parent module than on their children recursively.
	 */
	public void startOrRestart() {
		startOrRestart(Headquarters.this.installation.getRootModule());
	}

	private void startOrRestart(final Module module) {
		module.startOrRestart();
		for (Module m : module.getSubModules()) {
			startOrRestart(m);
		}
	}

	/**
	 * Calls {@link Module#shutdown()} on each module in the reverse {@link #startOrRestart()} order.
	 */
	public void shutdown() {
		Log.LOG.get().report(new Exception("System is shutting down"));
		shutdown(Headquarters.this.installation.getRootModule(), true);
	}

	private void shutdown(final Module module, final boolean root) {
		List<Module> subModules = L.copy(module.getSubModules());
		Collections.reverse(subModules);

		for (Module m : subModules) {
			try {
				shutdown(m, false);
			} catch (Exception e) {
				Warden.disregard(e);
				Log.LOG.get().report(e);
			}
		}
		// the root module gets some time to exit cleanly
		if (root) {
			waitGrace(false);
		}
		module.shutdown();
	}

	@Override
	public State getState() {
		State s = getState(getInstallation().getRootModule(), true);
		return s == null ? State.UNDEFINED : s;
	}

	private State getState(final Module module, final boolean includeRoot) {
		State agg = includeRoot ? module.getState() : null;
		for (Module m : module.getSubModules()) {
			State ms = m.getState();
			agg = State.aggregateStatus(agg, ms);
			State sub = getState(m, false);
			agg = State.aggregateStatus(agg, sub);
		}
		return agg;
	}

	public void kill() {
		kill(Headquarters.this.installation.getRootModule());
	}

	private void kill(final Module module) {
		for (Module m : module.getSubModules()) {
			kill(m);
		}
		module.kill();
	}

	public Status getStatusFor(final Module module) {
		Status status = this.statusByModule.get(module);
		if (status == null) {
			status = new Status(module);
			this.statusByModule.put(module, status);
		}
		return status;
	}

	public void setSeverity(final Report.Caller caller, final Health severity) {
		getInstallation().getSeverityStore().remember(caller.getSeverityKey(), severity, false, 0);
	}

	/**
	 * Hook for a Log processor ({@link GroupingLog}) to notify of an aggregated chunk of log messages.
	 * 
	 * @param report to report
	 */
	@Override
	public void notifyOfReport(final Report report) {
		// TODO handle Caller.getRelatedCallers
		Module module = getInstallation().determineModuleOf(report.getCaller().getClassName());
		if (module == null) {
			module = getInstallation().getRootModule();
		}
		Status status = getStatusFor(module);
		Health severityOfReport = status.inform(report, this.maxReportsKept);
		if (severityOfReport == Health.DISCARDABLE) {
			return;
		}
		if (report.getType() == Type.PERIODICAL) {
			// ignore periodical reports which are insignificant
			Integer minCount = getInstallation().getSeverityStore().getMinimumCounts().get(report.getCaller().getSeverityKey());
			if (minCount != null && report.numberOfOccurences() <= minCount.intValue()) {
				return;
			}
		}
		getInstallation().reportIncident(module, severityOfReport, report);
	}

	/**
	 * Hook for a Log processor ({@link GroupingLog}) to notify of a single log message.
	 * 
	 * @param report to report (containing only a single log entry)
	 */
	@Override
	public void notifyOfLog(@Nonnull final Report report) {
		if (!this.immediateLoggingEnabled) {
			return;
		}
		// non-immediate logs are never reported
		if (!getInstallation().getSeverityStore().getImmediateCallerKeys().contains(report.getCaller().getSeverityKey())) {
			return;
		}

		// flood protection
		this.logsSince++;
		this.logsTotal++;
		if (this.logsTotal % NUMBER_OF_LOGS_BEFORE_TIME_AND_RESCALE == 0) {
			long nowMs = System.currentTimeMillis();
			this.logsSince -= (nowMs - this.lastLogMs) / ALLOWED_MS_PER_LOG;
			if (this.logsSince < 0) {
				this.logsSince = 0;
			}
			this.lastLogMs = nowMs;
		}
		if (this.logsSince < LOG_RESCALE_PERIOD_MS / ALLOWED_MS_PER_LOG) {
			if (this.logsDiscarded > 0) {
				final String summary = "discarded " + Headquarters.this.logsDiscarded
						+ " immediate logs because of contention (more than " + Times.MILLIS_PER_SECOND / ALLOWED_MS_PER_LOG
						+ " logs per second).";
				getInstallation().reportImmediate(getInstallation().getRootModule(), Health.ERRORS, new SimpleReport(summary));
				this.logsDiscarded = 0;
			}

			floodProtectedSingleLogReporting(report);
		} else {
			this.logsDiscarded++;
		}
	}

	protected void floodProtectedSingleLogReporting(final Report report) {
		Module module = getInstallation().determineModuleOf(report.getCaller().getClassName());
		if (module == null) {
			module = getInstallation().getRootModule();
		}
		Health severity = getServeritiesByCallerKey().get(report.getCaller().getSeverityKey());
		if (severity == null) {
			severity = report.getEstimatedSeverity();
		}
		if (severity != Health.DISCARDABLE) {
			getInstallation().reportImmediate(module, severity, report);
		}
	}

	private Map<String, Health> getServeritiesByCallerKey() {
		return getInstallation().getSeverityStore().getSeveritiesByCallerKey();
	}

	public Installation getInstallation() {
		return this.installation;
	}

	public void assignSeverity(final String callerKey, final Health severity) {
		Integer minimum = getInstallation().getSeverityStore().getMinimumCounts().get(callerKey);
		assignSeverityProperties(callerKey, severity, false, minimum == null ? 0 : minimum.intValue());
	}

	public void assignMinimumCount(final String callerKey, final int newMinimumCount) {
		Health severity = getInstallation().getSeverityStore().getSeveritiesByCallerKey().get(callerKey);
		assignSeverityProperties(callerKey, severity, false, newMinimumCount);
	}

	public void assignSeverityProperties(final String callerKey, final Health severity, final boolean immediate,
			final int newMinimumCount) {
		getInstallation().getSeverityStore().remember(callerKey, severity, immediate, newMinimumCount);
	}

	@Override
	public Translations getName() {
		return getInstallation().getName();
	}

	@Override
	public double getLoad() {
		// TODO consider aggregating the loads somehow
		return getInstallation().getRootModule().getLoad();
	}

	@Override
	public String getMetaInformation() {
		StringBuilder sb = new StringBuilder(getName().getDefault()).append("\n");
		for (Module m : getAllModules()) {
			sb.append(m.getName().getDefault()).append("\n");
		}
		return sb.toString();
	}

	public boolean isImmediateLoggingEnabled() {
		return this.immediateLoggingEnabled;
	}

	public Headquarters setImmediateLoggingEnabled(final boolean immediateLoggingEnabled) {
		this.immediateLoggingEnabled = immediateLoggingEnabled;
		return this;
	}

	public long getStatusResetPeriodMillis() {
		return this.statusResetPeriodMillis;
	}

	public Headquarters setStatusResetPeriodMillis(final long statusResetPeriodMillis) {
		this.statusResetPeriodMillis = statusResetPeriodMillis;
		return this;
	}

	public int getMaxReportsKept() {
		return this.maxReportsKept;
	}

	public void setMaxReportsKept(final int maxReportsKept) {
		this.maxReportsKept = maxReportsKept;
	}

}
