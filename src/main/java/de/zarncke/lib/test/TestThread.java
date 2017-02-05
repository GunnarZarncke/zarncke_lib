package de.zarncke.lib.test;

import java.util.List;

import de.zarncke.lib.block.Running;
import de.zarncke.lib.coll.L;
import de.zarncke.lib.err.MultiCauseException;
import de.zarncke.lib.err.Warden;

public class TestThread extends Thread {

	private List<Throwable> uncaughtExceptions;

	static int n = 1;

	public TestThread(final Runnable runnable) {
		super(runnable, "test thread " + n++);
		setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			public void uncaughtException(final Thread t, final Throwable e) {
				TestThread.this.uncaughtExceptions = L.l(e);
			}
		});
		setDaemon(true);
	}

	@Override
	public void run() {
		this.uncaughtExceptions = Warden.guard(new Running() {
			public void run() {
				superRun();
			}
		});
	}

	public void superRun() {
		super.run();
	}

	public void reraise() throws Exception {
		if (this.uncaughtExceptions != null && !this.uncaughtExceptions.isEmpty()) {
			throw Warden.spot(new MultiCauseException("unhandled exceptions during run", this.uncaughtExceptions));
			/*
			 * if (this.uncaughtException instanceof RuntimeException) {
			 * throw new RuntimeException("reraised in Thread " + getName(), this.uncaughtException);
			 * }
			 * if (this.uncaughtException instanceof Error) {
			 * throw (Error) this.uncaughtException;
			 * }
			 * if (this.uncaughtException instanceof Exception) {
			 * throw new Exception("reraised in Thread " + getName(), this.uncaughtException);
			 * }
			 * throw new RuntimeException("reraised unexpected type of exception", this.uncaughtException);
			 */
		}
	}
}
