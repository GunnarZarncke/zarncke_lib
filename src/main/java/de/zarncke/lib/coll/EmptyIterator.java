package de.zarncke.lib.coll;

import java.util.Iterator;

/**
 * empty.
 *
 * @param <T> any type
 */
public class EmptyIterator<T> implements Iterator<T>, Remaining {
	@SuppressWarnings("rawtypes")
	private static Iterator instance = new EmptyIterator();

	protected EmptyIterator() {
		// nothing
	}

	@SuppressWarnings("unchecked" /* no checks needed */)
	public static <T> Iterator<T> getInstance() {
		return instance;
	}

	@Override
	public boolean hasNext() {
		return false;
	}

	@Override
	public T next() {
		throw new IllegalStateException("beyond end of iteration!");
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("we are already empty!");
	}

	@Override
	public long available() {
		return 0;
	}
}
