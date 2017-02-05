package de.zarncke.lib.util;

import java.util.Map;

import com.google.common.base.Function;

public class Memoize<A, T> implements Function<A, T> {

	private final Function<A, T> delegate;
	private final Map<? super A, ? super T> cache;

	/**
	 * @param <A> argument
	 * @param <T> return
	 * @param delegate to use if not memoized
	 * @param cache to store return in; may be reused among different Memoized if these have different argument types
	 * @return memoized Function
	 */
	public static <A, T> Function<A, T> memoize(final Function<A, T> delegate, final Map<? super A, ? super T> cache) {
		return new Memoize<A, T>(delegate, cache);
	}

	/**
	 * @param delegate to use if not memoized
	 * @param cache to store return in; may be reused among different Memoized if these have different argument types
	 */
	public Memoize(final Function<A, T> delegate, final Map<? super A, ? super T> cache) {
		this.delegate = delegate;
		this.cache = cache;
	}

	@Override
	public T apply(final A from) {
		if (from == null) {
			return this.delegate.apply(from);
		}

		@SuppressWarnings("unchecked" /* we put in matching values */)
		T val = (T) this.cache.get(from);

		if (val == null) {
			val = this.delegate.apply(from);
			if (val != null) {
				this.cache.put(from, val);
			}
		}
		return val;
	}

	@Override
	public String toString() {
		return "memoize " + this.delegate + " in " + this.cache;
	}
}
