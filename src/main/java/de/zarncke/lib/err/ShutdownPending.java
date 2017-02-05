package de.zarncke.lib.err;

/**
 * This Exception indicates that the shutdown is pending and whatever functions could or should not execute or complete
 * therefore.
 * May be caught by appropriate handlers when this is deems to be unimportant during shutdown.
 * 
 * @author Gunnar Zarncke
 */
public class ShutdownPending extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public ShutdownPending(final String msg) {
		super(msg);
	}
}
