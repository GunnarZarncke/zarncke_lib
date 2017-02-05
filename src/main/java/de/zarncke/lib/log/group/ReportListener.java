package de.zarncke.lib.log.group;

import de.zarncke.lib.log.Report;

/**
 * Processes aggregated {@link Report}s.
 *
 * @author Gunnar Zarncke
 */
public interface ReportListener {

	/**
	 * Called when sufficient log messages have been aggregated.
	 * 
	 * @param report != null, typically contains multiple messages
	 */
	void notifyOfReport(final Report report);

	/**
	 * Called for every log message.
	 * 
	 * @param report != null, contains only a single message
	 */
	void notifyOfLog(final Report report);
}
