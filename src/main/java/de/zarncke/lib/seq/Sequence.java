package de.zarncke.lib.seq;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A monotonically rising sequence of integers.
 * Intended for providing unique IDs.
 * The idea is that each call 'reserves' one ore more IDs and returns the <em>first</em> of these values.
 * Implementations should start numbering with 0, but may provide other custom start values.
 * Implementations should be thread-safe.
 * Compare with {@link AtomicInteger}.
 *
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
public interface Sequence {
	/**
	 * @return the current number of the sequence (the value before the count is increased)
	 */
	long incrementAndGet();

	/**
	 * @param delta must be >= 0; for delta == 0 no change is done and the current value returned
	 * @return the current number of the sequence (the value before the count is increased)
	 */
	long addAndGet(long delta);
}
