package de.zarncke.lib.util;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Executes with a delegate Executor or directly <em>if</em> the delegate is empty (no pending or running tasks).
 *
 * @author Gunnar Zarncke
 */
public class DirectIfEmptyExecutor implements Executor {
	private final Executor delegate;

	public DirectIfEmptyExecutor(final Executor delegate) {
		this.delegate = delegate;
	}

	private final AtomicInteger inProcess = new AtomicInteger();

	class Wrap implements Runnable {
		private final Runnable runnable;

		public Wrap(final Runnable runnable) {
			this.runnable = runnable;
		}

		@Override
		public void run() {
			try {
				DirectIfEmptyExecutor.this.inProcess.incrementAndGet();
				this.runnable.run();
			} finally {
				DirectIfEmptyExecutor.this.inProcess.decrementAndGet();
			}
		}
	}

	@Override
	public void execute(final Runnable command) {
		final Wrap wrap = new Wrap(command);
		if (this.inProcess.get() == 0) {
			wrap.run();
		} else {
			this.delegate.execute(wrap);
		}
	}

	@Override
	public String toString() {
		return "direct or with " + this.delegate.toString();
	}
}
