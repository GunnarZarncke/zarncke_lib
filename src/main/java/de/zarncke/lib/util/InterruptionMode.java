package de.zarncke.lib.util;

/**
 * Different ways to deal with interruption in a nested process.
 *
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
public enum InterruptionMode {
	/**
	 * Ignore any interruption; just go on.
	 */
	IGNORE,
	/**
	 * Pass on any received interruptions.
	 */
	PASS_ON,
	/**
	 * Check interruption directly (because called repeatedtly).
	 * If each call always calls an IO-method throwing {@link InterruptedException}, than the check is not
	 * neccessary.
	 * this will always also {@link #PASS_ON}.
	 */
	CHECK_SELF
}