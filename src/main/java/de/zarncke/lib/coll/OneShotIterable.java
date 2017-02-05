package de.zarncke.lib.coll;

import java.util.Iterator;

import de.zarncke.lib.err.Warden;

public class OneShotIterable<T> implements Iterable<T> {

	public static <T> OneShotIterable<T> wrap(final Iterator<T> iterator) {
		return new OneShotIterable<T>(iterator);
	}

	private Iterator<T> iterator;

	protected OneShotIterable(final Iterator<T> iterator) {
		this.iterator = iterator;
	}

	public Iterator<T> iterator() {
		if (this.iterator == null) {
			throw Warden.spot(new IllegalStateException("Iterator may only be used once"));
		}
		Iterator<T> it = this.iterator;
		this.iterator = null;
		return it;
	}

	@Override
	public String toString() {
		return this.iterator == null ? "already used" : this.iterator.toString();
	}
}
