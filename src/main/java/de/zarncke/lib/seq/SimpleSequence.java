package de.zarncke.lib.seq;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A simple atomic in memory integer sequence.
 * 
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
public class SimpleSequence implements Sequence {

	private final AtomicLong sequence = new AtomicLong();

	public SimpleSequence() {
	}

	@Override
	public long addAndGet(final long delta) {
		return this.sequence.addAndGet(delta);
	}

	@Override
	public long incrementAndGet() {
		return addAndGet(1);
	}
}
