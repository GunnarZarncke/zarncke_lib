package de.zarncke.lib.coll;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

/**
 * Simple pair, 2-tuple.
 *
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 * @param <F> first type
 * @param <S> second type
 */
public class Pair<F, S> {
	public static final class Swap<S, F> implements Function<Pair<S, F>, Pair<F, S>> {
		@Override
		public Pair<F, S> apply(@Nullable final Pair<S, F> input) {
			return input.swap();
		}
	}

	private final F first;
	private final S second;

	public static <F, S> Pair<F, S> pair(final F first, final S second) {
		return new Pair<F, S>(first, second);
	}

	public static <F, S> Pair<F, S> pair(final Map.Entry<F, S> me) {
		return pair(me.getKey(), me.getValue());
	}

	/**
	 * @param mapEntries {@link Map#entrySet()}
	 * @return Collection of Pair
	 */
	public static <F, S> Collection<Pair<F, S>> pairs(final Collection<? extends Map.Entry<F, S>> mapEntries) {
		return Collections2.transform(mapEntries, new Function<Map.Entry<F, S>, Pair<F, S>>() {
			@Override
			public Pair<F, S> apply(@Nullable final Entry<F, S> input) {
				return Pair.pair(input);
			}
		});
	}

	public static <F, S> Collection<Pair<F, S>> swap(final Collection<? extends Pair<S, F>> mapEntries) {
		return Collections2.transform(mapEntries, new Swap<S, F>());
	}

	public Pair(final F first, final S second) {
		this.first = first;
		this.second = second;
	}

	public F getFirst() {
		return this.first;
	}

	public S getSecond() {
		return this.second;
	}

	public Pair<S, F> swap() {
		return pair(getSecond(), getFirst());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.first == null ? 0 : this.first.hashCode());
		result = prime * result + (this.second == null ? 0 : this.second.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Pair<?, ?> other = (Pair<?, ?>) obj;
		if (this.first == null) {
			if (other.first != null) {
				return false;
			}
		} else if (!this.first.equals(other.first)) {
			return false;
		}
		if (this.second == null) {
			if (other.second != null) {
				return false;
			}
		} else if (!this.second.equals(other.second)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "(" + this.first + "," + this.second + ")";
	}
}
