package de.zarncke.lib.err;

/**
 * An Exception, that indicates a call to an unimplemented or incompletely
 * implemented method. Intended for early testing, without the need to return
 * null values to make methods compile, which can quickly lead to searches,
 * whose those null values are created.
 */
public class NotImplementedException extends UnsupportedOperationException {
	private static final long serialVersionUID = 1L;

	public NotImplementedException() {
		super();
	}

	public NotImplementedException(final String msg) {
		super(msg);
	}

	public NotImplementedException(final String msg, final Throwable t) {
		super(msg, t);
	}

}
