package de.zarncke.lib.log;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.zarncke.lib.coll.Elements;
import de.zarncke.lib.ctx.Context;
import de.zarncke.lib.err.ExceptionNotIntendedToBeThrown;
import de.zarncke.lib.value.Default;

/**
 * A Log which forwards to Slf4J.
 *
 * @author Gunnar Zarncke
 */
public class Slf4jLog implements Log {

	private static final int MAX_NESTING_DEPTH = 10;
	public static final Context<ILoggerFactory> SLF4J_LOG = Context.of(Default.of(LoggerFactory.getILoggerFactory(),
			ILoggerFactory.class));

	static class LoggerCache {
		String lastName;
		Logger lastLogger;
	}

	private static ThreadLocal<LoggerCache> loggerCache = new ThreadLocal<LoggerCache>();

	@Override
	public void report(final Throwable throwableToReport) {
		getLogger(throwableToReport).error("", throwableToReport);
	}

	@Override
	public void report(final CharSequence issue) {
		getLogger(new Exception()).info(issue.toString()); // NOPMD generic
	}

	@Override
	public void report(final Object... debugObject) {
		getLogger(new ExceptionNotIntendedToBeThrown()).debug(debugObject == null ? "null" : Elements.toString(debugObject));
	}

	private static Logger getLogger(final Throwable location) {
		LoggerCache lc = loggerCache.get();
		if (lc == null) {
			lc = new LoggerCache();
			loggerCache.set(lc);
		}
		Logger logger;
		String name = determineCallerName(location);
		if (name == lc.lastName) {
			logger = lc.lastLogger;
		} else {
			logger = SLF4J_LOG.get().getLogger(name);
			lc.lastLogger = logger;
			lc.lastName = name;
		}
		return logger;
	}

	static String determineCallerName(final Throwable throwable) {
		Throwable current = throwable;
		for (int j = 0; j < MAX_NESTING_DEPTH; j++) {
			StackTraceElement[] stackTrace = current.getStackTrace();
			for (int i = stackTrace.length - 1; i >= 0; i--) {
				String caller = stackTrace[i].getClassName();
				if (caller.startsWith("de.zarncke.lib.err.") || caller.startsWith("de.zarncke.lib.log.")) {
					continue;
				}
				return stackTrace[i].getClassName() + "." + stackTrace[i].getMethodName();
			}
			Throwable cause = current.getCause();
			if (current == cause || cause == null) { // NOPMD these object may be same
				return "unknown";
			}
			current = cause;
		}
		return "unknown";
	}
}
