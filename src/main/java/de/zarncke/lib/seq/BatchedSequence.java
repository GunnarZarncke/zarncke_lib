package de.zarncke.lib.seq;

import de.zarncke.lib.coll.Pair;
import de.zarncke.lib.util.Q;

/**
 * A Sequences which uses a backing Sequence to fetch multiple values at once thus batching lower calls.
 * Will lead to gaps in the backing sequence if non-multipele numbers of IDs are used up.
 * It is recommended to log {@link #terminateAndGetCurrentAndRemaining() the remaining values at program termination}.
 *
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
public class BatchedSequence implements Sequence {
	private static final int DEFAULT_BATCH_SIZE = 100;

	private Sequence backingSequence;
	private final long batchSize;
	private volatile long nextValue;
	private volatile long remainingValues = 0;

	public BatchedSequence(final Sequence backingSequence) {
		this(backingSequence, DEFAULT_BATCH_SIZE);
	}

	/**
	 * Create a sequence with the given backing Sequence and batch size.
	 *
	 * @param backingSequence
	 * @param batchSize
	 */
	public BatchedSequence(final Sequence backingSequence, final int batchSize) {
		this.backingSequence = backingSequence;
		this.batchSize = batchSize;
	}

	@Override
	public long incrementAndGet() {
		return addAndGet(1);
	}

	/**
	 * It is recommended to use batch sizes that are multiple of the delta used or deltas which are multiples of the
	 * batch size. Otherwise gaps may occur because contiguous regions cannot be ensured.
	 */
	@Override
	public synchronized long addAndGet(final long delta) {
		assert delta >= 0 : "delta " + delta + " must be >= 0";
		if (delta == 0) {
			if (this.remainingValues <= 0) {
				this.nextValue = this.backingSequence.addAndGet(this.batchSize);
				this.remainingValues = this.batchSize;
			}
			return this.nextValue;
		}
		if (this.remainingValues - delta < 0) {
			if (this.remainingValues > 0) {
				discard(this.nextValue, this.remainingValues);
			}

			long needToFetch = ((delta - 1) / this.batchSize + 1) * this.batchSize;
			this.nextValue = this.backingSequence.addAndGet(needToFetch);
			this.remainingValues = needToFetch;
		}

		long current = this.nextValue;
		this.remainingValues -= delta;
		this.nextValue += delta;
		return current;
	}

	/**
	 * This overridable method is called when {@link #addAndGet(long)} needs to discard values in the sequence.
	 * This can be the case when the batch size is a non-multiples of the delta.
	 * This leads to gaps (unused values) in the underlying sequence and thus derived classes may log this issue
	 * (this default method does nothing).
	 *
	 * @param firstDiscarded first number which is discarded.
	 * @param numberOfDiscarded
	 */
	protected void discard(final long firstDiscarded, final long numberOfDiscarded) {
		// do nothing
	}

	/**
	 * May only be called as the last method. The other methods will fail after this call.
	 *
	 * @return next value that would be returned by the sequence and the number of remaining values
	 */
	public synchronized Pair<Long, Long> terminateAndGetCurrentAndRemaining() {
		Pair<Long, Long> result = Pair.pair(Q.l(this.nextValue), Q.l(this.remainingValues));
		this.backingSequence = null;
		this.nextValue += this.remainingValues;
		this.remainingValues = 0;
		return result;
	}

	@Override
	public String toString() {
		return "batching " + this.batchSize + " of " + this.backingSequence.toString();
	}
}
