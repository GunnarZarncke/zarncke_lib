package de.zarncke.lib.value;

/**
 * An {@link Exception} which implies that the {@link IllegalArgumentException illegal argument} resulted from an externally and
 * thus untrusted source.
 * Thus this indicates less a programming error and more an attack of abuse and the reporting should differ accordingly.
 *
 * @author Gunnar Zarncke
 */
public class IllegalExternalException extends IllegalArgumentException {
	private static final long serialVersionUID = 1L;

	public IllegalExternalException(final String msg) {
		super(msg);
	}

	public IllegalExternalException(final String external, final String msg) {
		super("external value " + external + " is invalid: " + msg);
	}

	public IllegalExternalException(final String msg, final Throwable t) {
		super(msg, t);
	}

	public IllegalExternalException(final String external, final String msg, final Throwable t) {
		super("external value " + external + " is invalid: " + msg, t);
	}
}
