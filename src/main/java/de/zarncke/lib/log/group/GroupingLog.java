package de.zarncke.lib.log.group;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.common.collect.MapMaker;

import de.zarncke.lib.ctx.Context;
import de.zarncke.lib.err.ExceptionNotIntendedToBeThrown;
import de.zarncke.lib.err.ShutdownPending;
import de.zarncke.lib.log.Log;
import de.zarncke.lib.log.Report;
import de.zarncke.lib.log.Report.Caller;
import de.zarncke.lib.sys.Health;
import de.zarncke.lib.value.Default;

/**
 * A {@link Log} which groups log messages before sending a {@link Report} about them to a {@link ReportListener}.
 * Grouping happens by {@link LogCaller source code location} by inspecting the stack trace.
 * Grouping is exponential, so you will receive one report about each first occurrence, then one for the next
 * {@value #REPORT_FACTOR} occurrences, then for the next {@value #REPORT_FACTOR}*{@value #REPORT_FACTOR} and so on
 * (up to a limit of {@link #MAX_PENDING_REPORTS_PER_CALLER}).
 * This will scale down every {@value #DEFAULT_TIME_BETWEEN_RESCALE_DAYS}, so you will typically receive one report
 * about the
 * occurrences every this period as long as the issue persists.
 * This is not very fast, so do not use this log class for very frequent log calls.
 * This takes some memory, even though massive message numbers are {@link #SAMPLING_FRACTION first sampled} and then
 * completely {@link #TRUNCATE_LIMIT truncated}.
 *
 * @author Gunnar Zarncke
 */
public class GroupingLog implements Log {

	/**
	 * Allows to supply a context for logging if {@link #isContextUser()} is on.
	 */
	public static final Context<Object> LOG_CONTEXT = Context.of(Default.of(null, Object.class,
			GroupingLog.class.getName()));

	public static final int MAX_STACKTRACE_NESTING_DEPTH = 10;

	private static final int DEFAULT_TIME_BETWEEN_RESCALE_DAYS = 7 * 24;
	private static final int DEFAULT_TIME_BETWEEN_RESCALE_MILLIS = DEFAULT_TIME_BETWEEN_RESCALE_DAYS * 60 * 60 * 1000;
	private static final double REPORT_FACTOR = 10.0;
	// TODO make these two params configurable
	/**
	 * Above this number of pending entries the caller details of the entry are truncated.
	 */
	public static final int TRUNCATE_LIMIT = 50;
	/**
	 * Above this number of pending entries the entry is not kept but only counted (except for
	 * {@link #SAMPLING_FRACTION
	 * sampling}).
	 */
	public static final int DISCARD_LIMIT = 200;
	/**
	 * Above this number of pending entries the report is sent in any case.
	 */
	public static final int MAX_PENDING_REPORTS_PER_CALLER = 5000;

	/**
	 * Above this number of total entries regular reports are heavily truncated to avoid excess mailing.
	 * Periodical reports are still send normally.
	 */
	public static final int TOTAL_COUNT_TO_TRUNCATE_ALL_RESULTs = 100000;
	/**
	 * Regular large reports are truncated to this size.
	 */
	public static final int TRUNCATED_SIZE = 100;

	/**
	 * Regular large reports are truncated to 1 entry per this duration.
	 */
	public static final int MAX_MILLIS_PER_REPORTED_ENTRY = 1000;

	/**
	 * Number of entries after which a message is sampled in any case.
	 */
	public static final int SAMPLING_FRACTION = 100;

	private static final int TYPICAL_LINE_LENGTH = 80;
	private static final int TYPICAL_LOG_PREFIX_LENGTH = 20;
	static final int SUMMARY_TEXT_LENGTH = 2 * TYPICAL_LINE_LENGTH - TYPICAL_LOG_PREFIX_LENGTH;
	static final int TRUNCATE_SUMMARY_TEXT_LIMIT = 100;
	static final int TO_STRING_SUMMARY_TEXT_LENGTH = TYPICAL_LINE_LENGTH;

	static final DateTimeFormatter FORMATTER = DateTimeFormat.longDateTime().withZone(DateTimeZone.UTC)
			.withLocale(Locale.GERMANY);

	private Map<String, LogReport> knownReportsByKey = new MapMaker().makeMap();
	final ReportListener reportListener;
	final double reportFactor;
	final int reportLimit;
	long timeMillisBetweenRescale;
	private boolean contextUsed = true;

	private boolean stopped = false;

	public GroupingLog(final ReportListener reportListener) {
		this(reportListener, REPORT_FACTOR, MAX_PENDING_REPORTS_PER_CALLER);
	}

	public GroupingLog(final ReportListener reportListener, final double reportFactor, final int reportLimit) {
		this.reportListener = reportListener;
		this.reportFactor = reportFactor;
		this.reportLimit = reportLimit;
		this.timeMillisBetweenRescale = DEFAULT_TIME_BETWEEN_RESCALE_MILLIS;
	}

	public long getTimeMillisBetweenRescale() {
		return this.timeMillisBetweenRescale;
	}

	public void setTimeMillisBetweenRescale(final long timeMillisBetweenRescale) {
		this.timeMillisBetweenRescale = timeMillisBetweenRescale;
	}

	@Override
	public void report(final Throwable throwableToReport) {
		if (throwableToReport == null) {
			report("null Throwable passed");
			return;
		}
		if (this.stopped) {
			// this exception should not go over reporting
			throw new ShutdownPending("module already in shutdown or stepped.");
		}
		String qualifier = throwableToReport.getClass().getSimpleName();
		Object ctx = getContext();
		LogCaller caller = determineCaller(new ExceptionNotIntendedToBeThrown(), qualifier, ctx);
		LogCaller throwableCaller = determineCaller(throwableToReport, qualifier, ctx);
		if (caller == null) {
			caller = throwableCaller;
		}
		if (caller == null) {
			caller = new LogCaller(new ExceptionNotIntendedToBeThrown().getStackTrace()[0], null, qualifier, ctx);
		}

		LogReport r = determineReport(caller);
		r.report(caller, throwableCaller);
	}

	private Object getContext() {
		if (this.contextUsed) {
			return LOG_CONTEXT.get();
		}
		return null;
	}

	private static final Pattern EXCEPTION_PATTERN = Pattern.compile("[_\\p{javaLetterOrDigit}]*Exception");

	@Override
	public void report(final CharSequence issue) {
		checkStop();
		CharSequence effectiveIssue = issue;
		if (issue == null) {
			effectiveIssue = "null issue passed";
		} else if (issue.length() == 0) {
			effectiveIssue = "empty issue passed";
		}
		// TODO consider extracting a key from the issue
		String qualifier = null;
		Matcher matcher = EXCEPTION_PATTERN.matcher(effectiveIssue);
		if (matcher.matches()) {
			qualifier = matcher.group();
		}
		Object ctx = getContext();
		LogCaller caller = determineCaller(new ExceptionNotIntendedToBeThrown(), qualifier, ctx);
		if (caller == null) {
			caller = new LogCaller(new ExceptionNotIntendedToBeThrown().getStackTrace()[0], null, qualifier, ctx);
		}

		LogReport r = determineReport(caller);
		r.report(caller, issue);
	}

	@Override
	public void report(final Object... debugObjects) {
		checkStop();
		// TODO consider extracting a key from the issue
		String qualifier = debugObjects != null && debugObjects.length >= 1 ? debugObjects[0].getClass()
				.getSimpleName() : null;
		LogCaller caller = determineCaller(new ExceptionNotIntendedToBeThrown(), qualifier, getContext());
		if (caller == null) {
			caller = new LogCaller(new ExceptionNotIntendedToBeThrown().getStackTrace()[0], null, qualifier,
					getContext());
		}

		LogReport r = determineReport(caller);
		r.report(caller, debugObjects);
	}

	public void reportExplicit(final CharSequence message, final Report.Caller caller, final long simulatedTime,
			final Health severity) {
		checkStop();
		LogReport r = determineReport(caller);
		r.reportExplicit(caller, message, simulatedTime, severity);
	}

	protected void checkStop() {
		if (this.stopped) {
			throw new ShutdownPending("module already in shutdown or stopped.");
		}
	}

	private LogReport determineReport(final Caller caller) {
		LogReport r = this.knownReportsByKey.get(caller.getCallerKey());
		if (r == null) {
			r = new LogReport(this, caller);
			this.knownReportsByKey.put(caller.getCallerKey(), r);
		}
		return r;
	}

	static LogCaller determineCaller(final Throwable throwable, final String qualifier, final Object context) {
		Throwable current = throwable;
		for (int j = 0; j < MAX_STACKTRACE_NESTING_DEPTH; j++) {
			StackTraceElement[] stackTrace = current.getStackTrace();
			for (StackTraceElement element : stackTrace) {
				if (isStackTraceElementGeneric(element)) {
					continue;
				}
				return new LogCaller(element, throwable, qualifier, context);
			}
			Throwable cause = current.getCause();
			if (current == cause || cause == null) { // NOPMD cycles happen sometimes
				return null;
			}
			current = cause;
		}
		return null;
	}

	private static boolean isStackTraceElementGeneric(final StackTraceElement element) {
		String caller = element.getClassName();
		// disregard caller stack-frames belonging to logging - except for those by specially named methods
		if (caller.startsWith("de.zarncke.lib.err.") || caller.startsWith("de.zarncke.lib.log.")
				|| caller.startsWith("java.util.logging.Logger")) {
			if (!element.getMethodName().equals("logFromWithinLogging")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Estimate (roughly) the held reporting volume.
	 * This is monitoring functionality and should be used by seldom.
	 *
	 * @return bytes kept for unsent reports
	 */
	public int estimatePendingReportVolume() {
		int numReports = this.knownReportsByKey.size();
		int size = 0;
		int sampleSize = (int) Math.sqrt(numReports);
		Iterator<LogReport> it = this.knownReportsByKey.values().iterator();
		for (int i = 0; i < sampleSize; i++) {
			LogReport r = it.next();
			size += r.estimateSize();
		}
		return size * numReports / sampleSize;
	}

	/**
	 * Send all pending reports and drop all information.
	 * This is recovery functionality and should be used if memory use gets prohibitive.
	 *
	 * @param type
	 */
	public synchronized void flushAllReportsAndDropRecencyInformation(final Report.Type type) {
		Map<String, LogReport> reports = this.knownReportsByKey;
		this.knownReportsByKey = new MapMaker().makeMap();

		for (LogReport report : reports.values()) {
			report.sendReport(type);
		}
	}

	/**
	 * Drop all information.
	 * This is emergency recovery functionality and should be used in dire circumstances only (module failing).
	 * All pending reports will be lost!!!
	 */
	public void emergencyReinit() {
		this.knownReportsByKey = new MapMaker().makeMap();
	}

	public void stop() {
		this.stopped = true;
	}

	@Override
	public String toString() {
		int total = 0;
		int max = 0;
		String maxKey = null;
		for (Map.Entry<String, LogReport> me : this.knownReportsByKey.entrySet()) {
			int size = me.getValue().size();
			total += size;
			if (size > max) {
				max = size;
				maxKey = me.getKey();
			}
		}
		return total + " entries in " + this.knownReportsByKey.size() + " categories. " + maxKey
				+ " occur smost often (" + max + ")";
	}

	public String getDetailedSummary() {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, LogReport> me : this.knownReportsByKey.entrySet()) {
			sb.append(me.getKey()).append("=").append(me.getValue().summary()).append("\n");
		}
		return sb.toString();
	}

	public boolean isContextUser() {
		return this.contextUsed;
	}

	public GroupingLog setContextUsed(final boolean contextUsed) {
		this.contextUsed = contextUsed;
		return this;
	}

}
