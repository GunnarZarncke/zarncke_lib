package de.zarncke.lib.err;

/**
 * This exception indicates that its cause was not handled.
 *
 * @author Gunnar Zarncke
 * @clean 19.03.2012
 */
public class UnhandledException extends Exception {
	private static final long serialVersionUID = 1L;

	public UnhandledException(final Throwable t) {
		super(t);
	}
}
