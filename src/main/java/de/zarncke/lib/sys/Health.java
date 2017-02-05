/**
 *
 */
package de.zarncke.lib.sys;

/**
 * Defines levels of system health and associated messages indicating this health.
 *
 * @author Gunnar Zarncke
 */
public enum Health {
	/**
	 * No reports ever. May not be used by messages.
	 */
	VIRGIN,
	/**
	 * No reports since last cleaning. Should not be used by messages.
	 */
	CLEAN,
	/**
	 * Messages than can be discarded entirely or reported only summarily. Examples: Debug messages that occur too
	 * frequently and are not used currently.
	 */
	DISCARDABLE,
	/**
	 * Minor informational messages of which not even the first occurrence is of higher interest.
	 * This is the default level for debug output.
	 */
	MINOR,
	/**
	 * Only status messages, no incidents.
	 * This is the default when text messages have been logged.
	 * This first occurence of these messages is reported.
	 */
	INFO,
	/**
	 * Only incidents which are not currently considered problematic (no detailed logging).
	 */
	OK,
	/**
	 * Only warnings, no errors. Detail logging of causes starts at this level.
	 */
	WARNINGS,
	/**
	 * No problem, but important to be reported. Example: Server start.
	 */
	IMPORTANT,
	/**
	 * Errors, but operational. Suitable if only single requests fail.
	 * This is the default when unhandled Exceptions are logged.
	 */
	ERRORS,
	/**
	 * Failures, maybe partial, maybe transient. An operator should take immediate action.
	 * Suitable if all requests are affected.
	 */
	FAILURE,
	/**
	 * Not available in any way.
	 */
	DEAD;
}