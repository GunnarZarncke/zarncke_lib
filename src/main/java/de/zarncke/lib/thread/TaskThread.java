package de.zarncke.lib.thread;

import java.util.Collection;

import de.zarncke.lib.block.Running;
import de.zarncke.lib.block.Task;
import de.zarncke.lib.coll.L;
import de.zarncke.lib.err.ExceptionUtil;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.log.Log;
import de.zarncke.lib.util.Chars;

/**
 * A Thread which executes some {@link Task}s repeatedly until interrupted.
 *
 * @author Gunnar Zarncke
 */
public class TaskThread extends Thread {
	private static final int TASK_SUMMARY_LENGTH = 80;
	public static final int DEFAULT_SLEEP_MILLIS = 15 * 60 * 1000;
	private final Collection<? extends Task> tasks;
	private int sleepMillis = DEFAULT_SLEEP_MILLIS;
	private volatile boolean mayrun = true;

	private Task currentTask;

	public TaskThread(final String name, final Task... tasks) {
		this(name, L.l(tasks));
	}

	public TaskThread(final String name, final Collection<? extends Task> tasks) {
		super(name);

		setDaemon(true);
		this.tasks = tasks;
	}

	public int getSleepMillis() {
		return this.sleepMillis;
	}

	public void setSleepMillis(final int sleepMillis) {
		this.sleepMillis = sleepMillis;
	}

	@Override
	public void run() {
		outer: while (true) {
			for (final Task t : TaskThread.this.tasks) {
				if (!this.mayrun) {
					break outer;
				}
				if (t == null) {
					continue;
				}
				String name = getName();
				try {
					setName(name + "[" + Chars.summarize(t.toString(), TASK_SUMMARY_LENGTH) + "]");
					if (executeInterruptible(t)) {
						// interuption terminate the thread
						break;
					}
				} finally {
					setName(name);
				}
			}
			try {
				Thread.sleep(this.sleepMillis);
			} catch (InterruptedException e) {
				Log.LOG.get().report("interrupted - stopping " + this);
				break;
			}
		}
	}

	public boolean executeInterruptible(final Task t) {
		final boolean[] stop = new boolean[1];
		Warden.guard(new Running() {
			@Override
			public void run() {
				try {
					TaskThread.this.currentTask = t;
					t.execute();
				} catch (ThreadDeath e) { // NOPMD in a Thread may may
					throw e;
				} catch (InterruptedException e) {
					Warden.disregard(e);
					Log.LOG.get().report("interrupted, stopping " + this);
					stop[0] = true;
				} catch (OutOfMemoryError e) {
					Warden.disregardAndReport(e);
				} catch (StackOverflowError e) {
					Warden.disregardAndReport(e);
				} catch (Error e) {
					ExceptionUtil.emergencyAlert("unknown fatal error (stopping)", e);
					throw e;
				} catch (Throwable e) { // NOPMD we must continue
					Warden.disregardAndReport(e);
					Log.LOG.get().report("problems during execution - will retry");
				} finally {
					TaskThread.this.currentTask = null;
				}
			}
		});
		return stop[0];
	}

	public void shutdown() {
		this.mayrun = false;
		interrupt();
	}

	public Collection<? extends Task> getTasks() {
		return L.copy(this.tasks);
	}

	@Override
	public String toString() {
		return "run " + this.tasks.toString();
	}

	public Task getCurrentTask() {
		return this.currentTask;
	}
}
