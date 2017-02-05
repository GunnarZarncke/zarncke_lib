package de.zarncke.lib.err;

/**
 * This exception indicates that a data inconsistency was encountered which cannot be resolved without causing further
 * inconsistencies.
 * This is intended for situations where read data doesn't conform to the format previously written and no explicit
 * handling is applicable.
 * Example: You write data and read/parse it later, you therefore expect no parsing errors (assuming proper format
 * version and IO error handling).
 * This is different from {@link CantHappenException} which doesn't strictly include external media (which could be
 * corrupt or otherwise be tampered with).
 * 
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
public class ConsistencyException extends IllegalStateException {
	private static final long serialVersionUID = 1L;

	public ConsistencyException() {
		super();
	}

	public ConsistencyException(final String msg) {
		super(msg);
	}

	public ConsistencyException(final Throwable thr) {
		super(thr);
	}

	public ConsistencyException(final String msg, final Throwable thr) {
		super(msg, thr);
	}
}
