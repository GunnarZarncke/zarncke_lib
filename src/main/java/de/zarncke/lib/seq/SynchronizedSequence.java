package de.zarncke.lib.seq;

public class SynchronizedSequence implements Sequence {
	private final Sequence delegate;

	public SynchronizedSequence(final Sequence delegate) {
		this.delegate = delegate;
	}

	@Override
	public synchronized long incrementAndGet() {
		return this.delegate.incrementAndGet();
	}

	@Override
	public synchronized long addAndGet(final long delta) {
		return this.delegate.addAndGet(delta);
	}

	@Override
	public String toString() {
		return "synchronized " + this.delegate;
	}
}
