package de.zarncke.lib.log;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.helpers.MarkerIgnoringBase;

import de.zarncke.lib.coll.Elements;

/**
 * Reports SLF4J log messages over {@link Log#LOG}.
 *
 * @author Gunnar Zarncke
 */
public class Slf4jLoggerFactory implements ILoggerFactory {

	private static final class LogAdapter extends MarkerIgnoringBase {
		public void debug(final String msg) {
			if (debug) {
				Log.LOG.get().report(msg);
			}
		}

		public void debug(final String format, final Object arg) {
			if (debug) {
				Log.LOG.get().report(format(format, arg));
			}
		}

		public void debug(final String format, final Object[] argArray) {
			if (debug) {
				Log.LOG.get().report(format(format, argArray));
			}
		}

		public void debug(final String msg, final Throwable t) {
			if (debug) {
				Log.LOG.get().report(t);
			}
		}

		public void debug(final String format, final Object arg1, final Object arg2) {
			if (debug) {
				Log.LOG.get().report(format(format, arg1, arg2));
			}
		}

		public void error(final String msg) {
			Log.LOG.get().report(msg);
		}

		public void error(final String format, final Object arg) {
			Log.LOG.get().report(format(format, arg));
		}

		public void error(final String format, final Object[] argArray) {
			Log.LOG.get().report(format(format, argArray));
		}

		public void error(final String msg, final Throwable t) {
			Log.LOG.get().report(t);
		}

		public void error(final String format, final Object arg1, final Object arg2) {
			Log.LOG.get().report(format(format, arg1, arg2));
		}

		public String getName() {
			return null;
		}

		public void info(final String msg) {
			Log.LOG.get().report(msg);
		}

		public void info(final String format, final Object arg) {
			Log.LOG.get().report(format + " " + arg);
		}

		public void info(final String format, final Object[] argArray) {
			Log.LOG.get().report(argArray);
		}

		public void info(final String msg, final Throwable t) {
			Log.LOG.get().report(t);
		}

		public void info(final String format, final Object arg1, final Object arg2) {
			Log.LOG.get().report(format(format, arg1, arg2));
		}

		public boolean isDebugEnabled() {
			return debug;
		}

		public boolean isErrorEnabled() {
			return true;
		}

		public boolean isInfoEnabled() {
			return true;
		}

		public boolean isTraceEnabled() {
			return trace;
		}

		public boolean isWarnEnabled() {
			return true;
		}

		public void trace(final String msg) {
			if (trace) {
				Log.LOG.get().report((Object) msg);
			}
		}

		public void trace(final String format, final Object arg) {
			if (trace) {
				Log.LOG.get().report(format, arg);
			}
		}

		public void trace(final String format, final Object[] argArray) {
			if (trace) {
				Log.LOG.get().report(format, argArray);
			}
		}

		public void trace(final String msg, final Throwable t) {
			if (trace) {
				Log.LOG.get().report(t);
			}
		}

		public void trace(final String format, final Object arg1, final Object arg2) {
			if (trace) {
				Log.LOG.get().report(format, arg1, arg2);
			}
		}

		public void warn(final String msg) {
			Log.LOG.get().report(msg);
		}

		public void warn(final String format, final Object arg) {
			Log.LOG.get().report(format(format, arg));
		}

		public void warn(final String format, final Object[] argArray) {
			Log.LOG.get().report(format(format, argArray));
		}

		public void warn(final String msg, final Throwable t) {
			Log.LOG.get().report(t);
		}

		public void warn(final String format, final Object arg1, final Object arg2) {
			Log.LOG.get().report(format(format, arg1, arg2));
		}
	}

	public static final LogAdapter LOG_ADAPTER = new LogAdapter();

	public static boolean trace = false;
	public static boolean debug = false;

	public Logger getLogger(final String name) {
		return LOG_ADAPTER;
	}

	private static String format(final String format, final Object... args) {
		if (format.indexOf("{0") >= 0 || format.indexOf("{1") >= 0 || format.indexOf("{2") >= 0
				|| format.indexOf("{3") >= 0) {
			try {
				return java.text.MessageFormat.format(format, args);
			} catch (Exception e) {
				// fall back to simple output
			}
		}
		return format + " " + Elements.toString(args);
	}
}
