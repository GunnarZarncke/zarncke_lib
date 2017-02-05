package de.zarncke.lib.thread;

import java.util.Collection;

import de.zarncke.lib.block.Running;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.log.Log;

/**
 * A {@link Thread} which declares that it is {@link Interruptible}.
 * Also guards its {@link #run()} method.
 * If you overwrite {@link #run()}, then you are responsible for {@link Warden#guard guarding} yourself.
 *
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
public class InterruptibleThread extends Thread implements Interruptible {

	public InterruptibleThread() {
		super();
	}

	public InterruptibleThread(final Runnable target, final String name) {
		super(target, name);
	}

	public InterruptibleThread(final Runnable target) {
		super(target);
	}

	public InterruptibleThread(final String name) {
		super(name);
	}

	public InterruptibleThread(final ThreadGroup group, final Runnable target, final String name, final long stackSize) {
		super(group, target, name, stackSize);
	}

	public InterruptibleThread(final ThreadGroup group, final Runnable target, final String name) {
		super(group, target, name);
	}

	public InterruptibleThread(final ThreadGroup group, final Runnable target) {
		super(group, target);
	}

	public InterruptibleThread(final ThreadGroup group, final String name) {
		super(group, name);
	}

	/**
	 * @param threads to stop (filtered for interuptible ones)
	 * @param safe true: only interrupt those where interruption is save, i.e. doesn't permanently terminate
	 */
	public static void interruptAllInterruptibleThreads(final Collection<Thread> threads, final boolean safe) {
		for (Thread t : threads) {
			if (t instanceof Interruptible) {
				if (!safe || isInterruptionSave((Interruptible) t)) {
					Log.LOG.get().report("interrupting " + t.getName());
					t.interrupt();
				}

			}
		}
	}

	private static boolean isInterruptionSave(final Interruptible t) {
		switch (t.getSupport()) {
		case BEST_EFFORT:
		case AUTO_JOB_STOPS:
		case LOOP_CONTINUE:
		case LOOP_RETRY:
		case RECOVERING:
			return true;
		case LOOP_STOPS:
		case SINGLE_JOB_STOPS:
		case UNSUPPORTED:
		default:
			return false;
			// these are dangerous to interrupt
		}
	}

	@Override
	public Support getSupport() {
		return Support.BEST_EFFORT;
	}

	@Override
	public void run() {
		Warden.guard(new Running() {
			@Override
			public void run() {
				superRun();
			}
		});
	}

	protected void superRun() {
		super.run();
	}
}
