package de.zarncke.lib.err;

/**
 * This exception indicates that the (wrapped) cause is not fatal but may go away for unspecified reasons.
 *
 * @author Gunnar Zarncke
 */
public class TransientFailure extends Exception {
	private static final long serialVersionUID = 1L;

	public TransientFailure(final String msg) {
		super(msg);
	}
	public TransientFailure(final String msg, final Throwable e) {
		super(msg, e);
	}
}