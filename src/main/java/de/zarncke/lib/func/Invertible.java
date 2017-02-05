package de.zarncke.lib.func;

import com.google.common.base.Function;

/**
 * Declares that a {@link Function} is (or may be) invertible.
 *
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 * @param <F> argument type
 * @param <T> return type
 */
public interface Invertible<F, T> extends Function<F, T> {

	/**
	 * @return true: function is in fact invertible; false: it is not (e.g. in case of function composition where the
	 * delegator doesn't know beforehand)
	 */
	boolean isInvertible();

	/**
	 * @return inverted Function taking results back to arguments
	 */
	Function<T, F> invert();
}
