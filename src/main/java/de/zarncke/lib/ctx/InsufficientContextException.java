package de.zarncke.lib.ctx;

/**
 * This Exception implies that a value was expected to be supplied by a {@link Context#get}, but was missing.
 * Probably a surrounding {@link Context#runWith} call is missing. Or it has supplied an invalid or null value.
 *
 * @author Gunnar Zarncke
 */
public class InsufficientContextException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public InsufficientContextException(final String msg) {
		super(msg);
	}

}
