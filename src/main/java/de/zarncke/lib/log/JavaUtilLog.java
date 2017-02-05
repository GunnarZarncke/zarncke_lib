package de.zarncke.lib.log;

import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.zarncke.lib.coll.Elements;
import de.zarncke.lib.err.ExceptionNotIntendedToBeThrown;

/**
 * A Log which forwards to a java.util.Logger.
 *
 * @author Gunnar Zarncke
 */
public class JavaUtilLog implements Log {

	static class LoggerCache {
		String lastName;
		Logger lastLogger;
	}

	private static ThreadLocal<LoggerCache> loggerCache = new ThreadLocal<LoggerCache>();

	@Override
	public void report(final Throwable throwableToReport) {
		getLogger(throwableToReport).throwing(null, null, throwableToReport);
	}

	@Override
	public void report(final CharSequence issue) {
		getLogger(new Exception()).info(issue.toString()); // NOPMD generic
	}

	@Override
	public void report(final Object... debugObject) {
		getLogger(new ExceptionNotIntendedToBeThrown()).fine(
				debugObject == null ? "null" : Elements.toString(debugObject));
	}

	private static Logger getLogger(final Throwable location) {
		LoggerCache lc = loggerCache.get();
		if (lc == null) {
			lc = new LoggerCache();
			loggerCache.set(lc);
		}
		Logger logger;
		String name = Slf4jLog.determineCallerName(location);
		if (name == lc.lastName) {
			logger = lc.lastLogger;
		} else {
			logger = LogManager.getLogManager().getLogger(name);
			lc.lastLogger = logger;
			lc.lastName = name;
		}
		return logger;
	}


}
