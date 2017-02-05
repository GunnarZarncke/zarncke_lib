package de.zarncke.lib.log;

import java.util.Collection;

import de.zarncke.lib.sys.Health;

/**
 * Reports about an incident in the {@link de.zarncke.lib.sys.Installation}.
 * Care must be taken that all methods do never throw {@link RuntimeException}.
 * This is important because this is code for reporting errors which when failing will likely prevent the reporting
 * of errors within itself.
 * Callers (which are likely central log writers) should nonetheless catch exceptions,
 * try to fall back to a simpler method (e.g. toString()) and ultimately propagate exceptions to some admin
 * (e.g. system console).
 * 
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
public interface Report {

	enum Type {
		/**
		 * Report with a single immediate entry.
		 */
		SINGLE,
		/**
		 * Normal report due to grouping.
		 */
		REGULAR,
		/**
		 * Immediate report as configured.
		 */
		IMMEDIATE,
		/**
		 * Report because of a requested flushing.
		 */
		FLUSH,
		/**
		 * Report because of a periodical volume reduction.
		 */
		PERIODICAL,
		/**
		 * Final flusing of pending messages during shutdown/stop.
		 */
		FINAL
	}

	/**
	 * Identifies a causing agent (class/method) of an issue.
	 */
	interface Caller {
		String getClassName();
		String getMethodName();

		/**
		 * @return identification of the context of the call, may be null
		 */
		Object getContext();

		/**
		 * @return identification of the calling location (includes context).
		 */
		String getCallerKey();

		/**
		 * @return identification of the calling location for severity (excludes context).
		 */
		String getSeverityKey();

		/**
		 * File and line of current location of issue.
		 *
		 * @return filename and line number in stacktrace format
		 */
		String getSourceLocation();

		/**
		 * Other callers that might be relevant to this issue.
		 *
		 * @return Collection of <Caller>
		 */
		Collection<Caller> relatedCallers();

		/**
		 * @return causing caller, may be null
		 */
		Caller getCause();
	}

	long firstOccurenceMillis();

	long lastOccurenceMillis();

	int numberOfOccurences();

	Caller getCaller();

	/**
	 * Short one line summary of the issue(s).
	 *
	 * @return String != null
	 */
	String getSummary();

	/**
	 * Provides a detailed and lengthy report of the issue.
	 * 
	 * @return multiple lines
	 */
	CharSequence getFullReport();

	Health getEstimatedSeverity();

	Type getType();
}
