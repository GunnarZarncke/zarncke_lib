package de.zarncke.lib.err;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.annotation.Nonnull;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.log.Log;

/**
 * Provides tool methods for dealing with Throwables and exceptional circumstances in general.
 *
 * @author Gunnar Zarncke
 */
public final class ExceptionUtil {

	/**
	 * At most {@value #MAX_TESTED_EXCEPTION_NESTING} levels of nested exceptions are tested.
	 */
	public static final int MAX_TESTED_EXCEPTION_NESTING = 32;

	private ExceptionUtil() {
		// helper
	}

	/**
	 * A ThrowableRewriterException is thrown if there is a problem while
	 * rewriting a throwable.
	 */
	public static final class ThrowableRewritingException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		/**
		 * Creates a ThrowableRewriterException.
		 */
		public ThrowableRewritingException() {
			super();
		}

		/**
		 * Creates a ThrowableRewriterException.
		 *
		 * @param message An additional message to the exception.
		 */
		public ThrowableRewritingException(final String message) {
			super(message);
		}
	}

	/**
	 * Creates a new message with both the original and the new throwable
	 * message and all of the stack trace.
	 *
	 * @param originalThrowable The original throwable, that was usually caught.
	 * @param newThrowable The new throwable you are about to throw.
	 * @return A throwable of the newThrowable's type, but with all of the stack trace.
	 * @exception ThrowableRewritingException If the exception cannot be rewritten.
	 */
	public static Throwable rewrite(final Throwable originalThrowable, final Throwable newThrowable) {
		// Get the original message and stack trace.
		StringTokenizer originalMessageLines = tokenize(originalThrowable);

		// Build new stack trace as message.
		StringBuffer cutMessage = new StringBuffer();
		cutMessage.append("Because of ");

		// Append just as many lines as required.
		while (originalMessageLines.hasMoreTokens()) {
			String messageLine = originalMessageLines.nextToken();

			// Get the new message and stack trace.
			boolean matchFound = false;
			StringTokenizer newMessageLines = tokenize(newThrowable);
			while (newMessageLines.hasMoreTokens()) {
				String newMessageLine = newMessageLines.nextToken();
				if (newMessageLine.equals(messageLine) && newMessageLine.indexOf("Exception") < 0
						&& newMessageLine.indexOf("Error") < 0 && newMessageLine.indexOf("Throwable") < 0) {
					matchFound = true;
					break;
				}
			}
			if (matchFound) {
				break;
			}

			cutMessage.append(messageLine).append('\n');
		}

		// Build the bridge between the original and the new throwable.
		cutMessage.append("there was the following exception: ");
		cutMessage.append(newThrowable.getClass().getName()).append(": ");
		cutMessage.append(newThrowable.getMessage());

		// Build the new throwable.
		try {
			Constructor<?> newThrowableConstructor = newThrowable.getClass().getConstructor(
					new Class[] { String.class });
			return (Throwable) newThrowableConstructor.newInstance(new Object[] { cutMessage.toString() });
		} catch (Exception e) {
			throw new ThrowableRewritingException("Cannot rewrite the exception "// NOPMD
					+ originalThrowable.getClass().getName() + " to "
					+ newThrowable.getClass().getName()
					+ " because of " + e.toString()
					+ ".\n"
					+ "Please make sure that a default constructor for exception is accessable.");
		}
	}

	private static StringTokenizer tokenize(final Throwable throwable) {
		return new StringTokenizer(getStackTrace(throwable), "\n");
	}

	/**
	 * Like {@link Throwable#initCause(Throwable)} but type safe and the cause is preserved in any case.
	 * If a cause is already set, then the cause chain is followed upwards
	 *
	 * @param <T> type of Throwable
	 * @param throwable != null
	 * @param cause != null
	 * @return throwable (T)
	 */
	public static <T extends Throwable> T preserveCause(final T throwable, final Throwable cause) {
		Throwable t = throwable;
		while (t.getCause() != null) {
			t = t.getCause();
		}
		t.initCause(cause);
		return throwable;
	}

	/**
	 * Extracts the stack trace of a throwable.
	 *
	 * @param t The throwable to determine the stack trace of
	 * @return A String representing the current stack trace.
	 */
	public static String getStackTrace(final Throwable t) {
		StringWriter traceWriter = new StringWriter();
		t.printStackTrace(new PrintWriter(traceWriter, true));
		return traceWriter.getBuffer().toString();
	}

	/**
	 * extract the current stack trace
	 *
	 * @return a String representing the current stacktrace
	 */
	public static String getStackTrace() {
		return getStackTrace(new ExceptionNotIntendedToBeThrown());
	}

	/**
	 * Does everything possible to notify someone, that an impossible
	 * condition occurred in a location, where nobody is about
	 * to catch an Exception.
	 * Examples:
	 * <ul>
	 * <li>static initializer</li>
	 * <li>Thread.run</li>
	 * <li>indirect call of the above</li>
	 * <li>finalizers</li>
	 * <li>shutdown code</li>
	 * </ul>
	 *
	 * @param msg msg
	 * @param ex cause
	 */
	public static void emergencyAlert(final String msg, final Throwable ex) {
		String sep = "\n\n\n!!!!!!!! THE IMPOSSIBLE HAPPENED !!!!!!!!\n\n\n";
		System.out.println(msg + sep); // NOPMD
		ex.printStackTrace(System.out);
		System.out.println(sep);// NOPMD
		System.err.println(msg + sep);// NOPMD
		ex.printStackTrace(System.err);
		System.err.println(sep);// NOPMD
		Log.LOG.get().report(msg);
	}

	/**
	 * Find caller.
	 *
	 * @param currentThread the current thread
	 * @return the stacktrace element of the caller outside of the warden, null if none found
	 */
	public static StackTraceElement findCallingElement(final Thread currentThread) {
		for (StackTraceElement ste : currentThread.getStackTrace()) {
			String classname = ste.getClassName();
			if (!classname.startsWith(Warden.class.getName()) && !classname.startsWith("java.lang.")) {
				return ste;
			}
		}
		return null;
	}

	/**
	 * Checks whether the stacktrace chain contains a given exception.
	 *
	 * @param throwable to check
	 * @param containedThrowableClass exception to check for presence
	 * @return true if the exception is contained in the {@link Throwable#getCause() chain}
	 */
	public static boolean stacktraceContainsException(final Throwable throwable,
			final Class<? extends Throwable> containedThrowableClass) {

		int n = 0;
		// count to avoid looping
		Throwable cause = throwable;
		while (cause != null && n < MAX_TESTED_EXCEPTION_NESTING) {
			if (containedThrowableClass.isInstance(cause)) {
				return true;
			}
			// special common loop
			if (cause == cause.getCause()) {
				break;
			}
			cause = cause.getCause();
			n++;
		}
		return false;
	}

	/**
	 * Checks whether the stacktrace chain contains a given exception.
	 *
	 * @param exception to check
	 * @param containedException exception to check for presence
	 * @return true if the exception is contained in the {@link Throwable#getCause() chain}
	 */
	public static boolean stacktraceContainsException(final Throwable exception, final Throwable containedException) {
		Throwable cause = exception;
		int n = 0;
		while (cause != null && n < MAX_TESTED_EXCEPTION_NESTING) {
			if (cause == containedException) { // NOPMD we want to test for same exception
				return true;
			}
			cause = cause.getCause();
			n++;
		}
		return false;
	}

	/**
	 * Extracts all unique message texts from a (nested) stacktrace.
	 *
	 * @param throwable to extract messages from
	 * @return List of messages, may be empty
	 */
	@Nonnull
	public static List<String> extractReasons(@Nonnull final Throwable throwable) {
		List<String> reasons = L.l();
		addReasons(throwable, reasons);
		return reasons;
	}

	private static void addReasons(final Throwable throwable, final List<String> reasons) {
		int n = 0;
		// count to avoid looping
		Throwable cause = throwable;
		while (cause != null && n++ < MAX_TESTED_EXCEPTION_NESTING && reasons.size() < 100) {
			if (cause instanceof MultiCauseException) {
				for (Throwable t : ((MultiCauseException) cause).getCauses()) {
					addReasons(t, reasons);
				}
			}
			String msg = cause.getMessage();
			if (msg == null || msg.trim().isEmpty()) {
				if (cause instanceof NullPointerException) {
					msg = "missing or null value";
				} else {
					msg = cause.getClass().getSimpleName();
				}
			}
			boolean found = false;
			Iterator<String> it = reasons.iterator();
			while (it.hasNext()) {
				String reason = it.next();
				if (reason.contains(msg)) {
					found = true;
					break;
				}
				if (msg.contains(reason)) {
					it.remove();
					break;
				}
			}
			if (!found) {
				reasons.add(msg);
			}
			// special common loop
			if (cause == cause.getCause()) {
				break;
			}
			cause = cause.getCause();
		}
	}
}
