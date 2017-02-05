package de.zarncke.lib.util;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Provides an atomic maximum tracker.
 * You can put as many values concurrently as you like and you will get the maximum
 *
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 * @param <T>
 */
public class AtomicMaximum<T extends Comparable<T>> implements Serializable {
	private static final long serialVersionUID = 1L;

	public static class Reverse<T extends Comparable<T>> extends AtomicMaximum<T> {
		private static final long serialVersionUID = 1L;

		@Override
		protected boolean isGreaterThan(final T a, final T b) {
			return a.compareTo(b) < 0;
		}
	}

	private final AtomicReference<T> maximum = new AtomicReference<T>();

	public void put(final T value) {
		while (true) {
			T current = this.maximum.get();
			// push up the value if the new value is new or higher
			if (current == null || value != null && isGreaterThan(value, current)) {
				if (this.maximum.compareAndSet(current, value)) {
					break;
				}
			}
			break;
		}
	}

	protected boolean isGreaterThan(final T a, final T b) {
		return a.compareTo(b) > 0;
	}

	public T getMaximum() {
		return this.maximum.get();
	}

	@Override
	public String toString() {
		return getMaximum().toString();
	}
}
