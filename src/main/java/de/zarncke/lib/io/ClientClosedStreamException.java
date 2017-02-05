package de.zarncke.lib.io;

import java.io.EOFException;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;

import javax.annotation.Nonnull;

import de.zarncke.lib.err.ExceptionUtil;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.log.Log;

/**
 * This IOException indicates that the receiving client side closed the exception and sending further data is or should
 * be aborted.
 *
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
public class ClientClosedStreamException extends EOFException {
	public static final String DEFAULT_MSG = "the client closed the connection - aborting further sending";
	private static final long serialVersionUID = 1L;

	public ClientClosedStreamException() {
		super(DEFAULT_MSG);
	}

	public ClientClosedStreamException(final Throwable cause) {
		this(DEFAULT_MSG + "- aborting further processing.", cause);
	}

	public ClientClosedStreamException(final String msg, final Throwable cause) {
		super(msg);
		initCause(cause);
	}

	/**
	 * Helper to tests whether an exception indicates an EOF situation.
	 * Should only be used on exceptions from writing output.
	 *
	 * @param exception to test
	 * @return true: the exception indicates and EOF situation; false:
	 */
	public static boolean isClientClosedStream(@Nonnull final Exception exception) {
		Throwable nested = exception;
		for (int i = 0; i < ExceptionUtil.MAX_TESTED_EXCEPTION_NESTING && nested != null; i++) {
			if (nested instanceof EOFException) {
				return true;
			}
			if (nested instanceof ClosedChannelException) {
				return true;
			}
			if (nested instanceof IOException) {
				if ("Cloed".equals(nested.getMessage())) {
					return true;
				}
			}
			if (nested == nested.getCause()) {
				break;
			}
			nested = nested.getCause();
		}
		return false;
	}

	/**
	 * @param exception to test
	 * @return true if closed stream exception detected and logged, false otherwise
	 */
	public static boolean reportClientClosedStreamIfNecessary(@Nonnull final Exception exception) {
		if (isClientClosedStream(exception)) {
			reportClientClosedStream(exception);
			return true;
		}
		return false;
	}

	public static void reportClientClosedStream(final Exception cause, final String msg) {
		Warden.disregard(cause);
		Log.LOG.get().report(new ClientClosedStreamException(msg, cause));
	}

	public static void reportClientClosedStream(final Exception cause) {
		if (cause instanceof ClientClosedStreamException) {
			Warden.disregard(cause);
			Log.LOG.get().report(cause);
		} else {
			reportClientClosedStream(cause, ClientClosedStreamException.DEFAULT_MSG);
		}
	}

}
