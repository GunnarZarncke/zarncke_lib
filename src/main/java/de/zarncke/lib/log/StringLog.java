package de.zarncke.lib.log;

import de.zarncke.lib.coll.Elements;
import de.zarncke.lib.err.ExceptionUtil;

/**
 * A simple Log which aggregates the issues in a {@link StringBuffer}.
 * 
 * @author Gunnar Zarncke
 */
public class StringLog implements Log {

	private StringBuffer buffer = new StringBuffer();

	public void report(final Throwable throwableToReport) {
		if (throwableToReport == null) {
			report("no throwable given");
		} else {
			report(ExceptionUtil.getStackTrace(throwableToReport));
		}
	}

	public void report(final CharSequence issue) {
		this.buffer.append(issue).append("\n");
	}

	public void report(final Object... debugObject) {
		report("DEBUG: " + (debugObject == null ? "null" : Elements.toString(debugObject)));
	}

	/**
	 * Removes all buffered reports.
	 */
	public void clear() {
		this.buffer = new StringBuffer();
	}

	public CharSequence getBuffer() {
		return this.buffer;
	}

	@Override
	public String toString() {
		return this.buffer.toString();
	}

}
