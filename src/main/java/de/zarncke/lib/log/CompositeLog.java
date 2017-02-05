package de.zarncke.lib.log;

import java.util.Arrays;

/**
 * A Log which delegates to multiple other {@link Log}s.
 *
 * @author Gunnar Zarncke
 */
public class CompositeLog implements Log {

	private final Log[] logs;

	public CompositeLog(final Log... logs) {
		this.logs = logs;
	}

	@Override
	public void report(final Throwable throwableToReport) {
		for (Log l : this.logs) {
			l.report(throwableToReport);
		}
	}

	@Override
	public void report(final CharSequence issue) {
		for (Log l : this.logs) {
			l.report(issue);
		}
	}

	@Override
	public void report(final Object... object) {
		for (Log l : this.logs) {
			l.report(object);
		}
	}

	@Override
	public String toString() {
		return Arrays.asList(this.logs).toString();
	}
}
