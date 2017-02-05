package de.zarncke.lib.test;

import junit.framework.AssertionFailedError;

/**
 * Indicates that an exception occurred during Unit-Testing and needs to be propagated.
 * Compare with {@link AssertionFailedError} which is thrown by assertions but which doesn't carry stack traces.
 *
 * @author Gunnar Zarncke
 */
public class TestFailedException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public TestFailedException(final String msg) {
		super(msg);
	}
	public TestFailedException(final String msg, final Throwable cause) {
		super(msg, cause);
	}
}
