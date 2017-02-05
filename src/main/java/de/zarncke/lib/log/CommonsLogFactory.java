package de.zarncke.lib.log;

import org.apache.commons.logging.LogConfigurationException;

/**
 * Intercepts commons logging calls and forwards them to {@link Log#LOG Log.LOG}.
 *
 * @author Gunnar Zarncke
 */
public class CommonsLogFactory extends org.apache.commons.logging.LogFactory {

	private static final org.apache.commons.logging.Log LOG_ADAPTER = new org.apache.commons.logging.Log() {

		@Override
		public void warn(final Object message, final Throwable t) {
			Log.LOG.get().report(t);
		}

		@Override
		public void warn(final Object message) {
			Log.LOG.get().report(message);
		}

		@Override
		public void trace(final Object message, final Throwable t) {
			if (trace) {
				Log.LOG.get().report(t);
			}
		}

		@Override
		public void trace(final Object message) {
			if (trace) {
				Log.LOG.get().report(message);
			}
		}

		@Override
		public boolean isWarnEnabled() {
			return true;
		}

		@Override
		public boolean isTraceEnabled() {
			return trace;
		}

		@Override
		public boolean isInfoEnabled() {
			return true;
		}

		@Override
		public boolean isFatalEnabled() {
			return true;
		}

		@Override
		public boolean isErrorEnabled() {
			return true;
		}

		@Override
		public boolean isDebugEnabled() {
			return debug;
		}

		@Override
		public void info(final Object message, final Throwable t) {
			Log.LOG.get().report(t);
		}

		@Override
		public void info(final Object message) {
			Log.LOG.get().report(message);
		}

		@Override
		public void fatal(final Object message, final Throwable t) {
			Log.LOG.get().report(t);
		}

		@Override
		public void fatal(final Object message) {
			Log.LOG.get().report(message);
		}

		@Override
		public void error(final Object message, final Throwable t) {
			Log.LOG.get().report(t);
		}

		@Override
		public void error(final Object message) {
			Log.LOG.get().report(message);
		}

		@Override
		public void debug(final Object message, final Throwable t) {
			if (debug) {
				Log.LOG.get().report(t);
			}
		}

		@Override
		public void debug(final Object message) {
			if (debug) {
				Log.LOG.get().report(message);
			}
		}
	};

	@Override
	public Object getAttribute(final String name) {
		return null;
	}

	@Override
	public String[] getAttributeNames() {
		return null;
	}

	// CHECKSTYLE:OFF defined by slf4j
	@Override
	public org.apache.commons.logging.Log getInstance(@SuppressWarnings("rawtypes"/* defined by slf4j! */) final Class clazz)
			throws LogConfigurationException {
		return LOG_ADAPTER;
	}

	@Override
	public org.apache.commons.logging.Log getInstance(final String name) throws LogConfigurationException {
		return LOG_ADAPTER;
	}

	// CHECKSTYLE:ON

	@Override
	public void release() {
		// ignore
	}

	@Override
	public void removeAttribute(final String name) {
		// ignore
	}

	@Override
	public void setAttribute(final String name, final Object value) {
		// ignore
	}

	public static boolean trace = false;
	public static boolean debug = false;

}
