package de.zarncke.lib.math;

/**
 * Concrete mathematics helpers.
 * @author Gunnar Zarncke
 *
 */
public final class Concrete {

	private Concrete() {
		// helper
	}

	/**
	 * Truncated division.
	 *
	 * @param dividend any
	 * @param divisor any
	 * @return 0<=double<1
	 */
	public static int quot(final int dividend, final int divisor) {
		return (int) Math.floor((double) dividend / divisor);
	}

	/**
	 * mod by truncated division as defined in "The Art of Computer Programming".
	 *
	 * @param dividend any
	 * @param divisor any
	 * @return 0<=int<divisor
	 */
	public static int mod(final int dividend, final int divisor) {
		return dividend - divisor * quot(dividend, divisor);
	}
}
