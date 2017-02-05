package de.zarncke.lib.log;

import de.zarncke.lib.coll.Elements;

/**
 * Logs to {@link System#out stdout}.
 *
 * @author Gunnar Zarncke
 * @clean 29.03.2012
 */
public class StdOut implements Log {

	public void report(final Throwable throwableToReport) {
		if (throwableToReport == null) {
			report("no throwable given");
		} else {
			System.err.println(StackTraceCleaner.makeHumanReadableReport(throwableToReport)); // NOPMD StdOut may do so
		}
	}

	public void report(final CharSequence issue) {
		System.out.println(issue); // NOPMD StdOut may do so
	}

	public void report(final Object... debugObject) {
		report("DEBUG: " + (debugObject == null ? "null" : Elements.toString(debugObject)));
	}

}
