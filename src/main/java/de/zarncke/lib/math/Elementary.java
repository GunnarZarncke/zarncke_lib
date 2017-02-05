package de.zarncke.lib.math;

import java.util.LinkedList;
import java.util.List;

/**
 * Provides elementary number theoretic functions.
 *
 * @author Gunnar Zarncke
 */
public final class Elementary {
	private Elementary() {
		// helper
	}

	/**
	 * Returns a list of factors of the given number.
	 * SLOW! Intended for small number (e.g. UI layout).
	 * 
	 * @param number >0
	 * @return List of factors
	 */
	public static List<Integer> factorize(final int number) {
		LinkedList<Integer> divider = new LinkedList<Integer>();
		if (number == 1) {
			divider.add(Integer.valueOf(1));
			return divider;
		}

		int rest = number;
		factors: while (rest > 1) {
			for (int div = 2; div <= rest / 2; div++) {
				if (rest % div == 0) {
					divider.add(Integer.valueOf(div));
					rest /= div;
					continue factors;
				}
			}
			divider.add(Integer.valueOf(rest));
			break;
		}
		return divider;
	}

}
