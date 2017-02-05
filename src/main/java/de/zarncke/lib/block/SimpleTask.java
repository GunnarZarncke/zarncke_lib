package de.zarncke.lib.block;

/**
 * A Task which times the {@link #executeTimed() execute method} and returns a minimum {@link Task.Progress} info.
 *
 * @author Gunnar Zarncke
 */
public abstract class SimpleTask implements Task {

	@Override
	public Progress execute() throws InterruptedException {
		final long start = System.currentTimeMillis();
		final int count = executeTimed();
		final long end = System.currentTimeMillis();
		return new AbstractProgress() {
			@Override
			public int getCount() {
				return count;
			}

			@Override
			public long getDuration() {
				return end - start;
			}
		};
	}

	protected abstract int executeTimed() throws InterruptedException;

	@Override
	public String toString() {
		return getClass().getSimpleName() + " prio " + getPriority();
	}
}
