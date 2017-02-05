package de.zarncke.lib.log;

import de.zarncke.lib.ctx.Context;
import de.zarncke.lib.value.Default;

/**
 * General logging interface.
 * Intended to be accessing with a Context like this:
 *
 * <pre>
 * Log.LOG.get().report(&quot;Something happened&quot;);
 * </pre>
 *
 * Default implementation: {@link StdOut}.
 * Prominent implementation: {@link de.zarncke.lib.log.group.GroupingLog}.
 *
 * @author Gunnar Zarncke
 */
public interface Log {

	/**
	 * Default Log {@link Context} which uses {@link StdOut} as fallback.
	 */
	Context<Log> LOG = Context.of(Default.of(new StdOut(), Log.class));
	Log NULL_LOG = new Log() {

		@Override
		public void report(final Throwable throwableToReport) {
			// ignore
		}

		@Override
		public void report(final CharSequence issue) {
			// ignore
		}

		@Override
		public void report(final Object... object) {
			// ignore
		}
	};

	/**
	 * Reports an Exception.
	 * Consider calling {@link de.zarncke.lib.err.Warden#report(Throwable)} instead as that also records the problems in
	 * a "session".
	 * 
	 * @param throwableToReport
	 */
	void report(Throwable throwableToReport);

	/**
	 * Reports an issue.
	 *
	 * @param issue
	 */
	void report(CharSequence issue);

	/**
	 * Reports any objects which might be relevant, can include debug texts.
	 *
	 * @param debugObject one or more
	 */
	void report(Object... debugObject);
}
