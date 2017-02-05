package de.zarncke.lib.err;

import java.io.Serializable;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import de.zarncke.lib.block.ABlock;
import de.zarncke.lib.block.Block;
import de.zarncke.lib.block.Running;
import de.zarncke.lib.log.Log;

/**
 * Tracks exceptions and ensures their reporting.
 * Use as follows:
 *
 * <pre>
 * <code>
 *  		// this outer layer ensures that no exception goes unreported
 * 		Warden.guard(new Running() {
 * 			@Override
 * 			public void run() {
 * 				doComplexProcessing();
 * 			}
 * 			private void doComplexProcessing() {
 * 				// ...
 * 				try {
 * 					doDeeperProcessing();
 * 				}
 * 				catch(MyException e) {
 * 					// solve issue
 *
 * 					// this will ensure that the exception will not be reported
 * 					Warden.disregard(e);
 * 				}
 *
 * 				doDeeperProcessing();
 * 				// ...
 * 			}
 * 			private void doDeeperProcessing() {
 * 				// ...
 * 				// this will ensure that the exception is reported (by the Warden)
 * 				throw Warden.spot(new MyException("explain"));
 * 				// ...
 * 			}
 * 		});
 * </code>
 * </pre>
 *
 * Note that the Warden will handle chained exceptions (both ways) properly and report the most detailed one and only
 * once.
 *
 * @author Gunnar Zarncke
 */
public class Warden {

	static final class GuardedRunning extends Running implements Serializable {
		private static final long serialVersionUID = 1L; // serializable if nested elements are
		private Block<?> block;
		private boolean strict;

		@SuppressWarnings("unused" /* for serialization */)
		private GuardedRunning() {
		}

		public GuardedRunning(final Block<?> block, final boolean strict) {
			this.block = block;
			this.strict = strict;
		}

		@Override
		public void run() {
			guard(this.strict ? ABlock.wrapInReportingBlock(this.block, null, new UncaughtExceptionHandler() {
				@Override
				public void uncaughtException(final Thread t, final Throwable e) {
					Warden.report(e);
				}
			}) : this.block);
		}

		@Override
		public String toString() {
			return "guarding " + this.block + (this.strict ? " strictly" : "");
		}
	}

	private static final ThreadLocal<Warden> THREAD_WARDEN = new ThreadLocal<Warden>();

	private final Warden outerWarden;
	private final Set<Throwable> trackedThrowables = new HashSet<Throwable>();
	private List<Throwable> reportedThrowables = new LinkedList<Throwable>();

	Warden(final Warden outerWarden) {
		this.outerWarden = outerWarden;
	}

	/**
	 * Tell the Warden to disregard the {@link Throwable}, but take note of it.
	 * This means that this Throwable will not be reported later as "missing", but nonetheless as "occurred".
	 *
	 * @param throwableToIgnore this {@link Throwable} is "solved"
	 */
	public static void disregardAndReport(@Nonnull final Throwable throwableToIgnore) {
		Warden currentWarden = currentWarden();
		currentWarden.disregardThrowable(throwableToIgnore);
		log(throwableToIgnore);
	}

	/**
	 * Tell the system that an Exception was handled and doesn't need to be reported.
	 * Note: This logs this incident with low priority.
	 * If are going to report it anyway, it is recommended to use {@link #disregardAndReport(Throwable)}.
	 *
	 * @param throwableToIgnore exception != null
	 */
	public static void disregard(@Nonnull final Throwable throwableToIgnore) {
		logFromWithinLogging("disregarding " + throwableToIgnore);
		currentWarden().disregardThrowable(throwableToIgnore);
	}

	/**
	 * Definitely report a problem but do not throw/forward it.
	 * Reporting it means:
	 * <ul>
	 * <li>It will be logged,</li>
	 * <li>it will be remembered by the Warden and</li>
	 * <li>it will be returned by the {@link #guard(Block) guard call}.</li>
	 * </ul>
	 *
	 * @param spottedThrowable != null
	 */
	public static void report(@Nonnull final Throwable spottedThrowable) {
		currentWarden().logReport(spottedThrowable);
		log(spottedThrowable);
	}

	public static void log(@Nonnull final Throwable spottedThrowable) {
		if (spottedThrowable instanceof ShutdownPending) {
			spottedThrowable.printStackTrace(System.err);
		} else {
			Log.LOG.get().report(spottedThrowable);
		}
	}

	/**
	 * Reports problems within this logging package. <em>Important:</em> The name of this method is handled specially by
	 * logging! All other methods in this class and package occurring in stacktraces are ignored when determining
	 * callers except for this one.
	 *
	 * @param message to log as occurring explicitly from within logging
	 */
	public static void logFromWithinLogging(final String message) {
		try {
			Log.LOG.get().report(message);
		} catch (ShutdownPending e) {
			// ignore, this is not a problem during shutdown
		}
	}

	private void logReport(@Nonnull final Throwable reportedThrowable) {
		this.reportedThrowables.add(reportedThrowable);
	}

	/**
	 * Observe the occurrence of the exception just before it is thrown.
	 * The Warden will ensure that it is ultimately logged if it is not handled (by {@link #disregard(Throwable)}.
	 * This should be called before <strong>every</strong> throw.
	 * Best enforce by a CheckStyle rule which forbids <code>throw new</code>.
	 *
	 * @param <T> to return properly
	 * @param spottedThrowable != null
	 * @return same Throwable
	 */
	public static <T extends Throwable> T spot(@Nonnull final T spottedThrowable) {
		return currentWarden().spotThrowable(spottedThrowable);
	}

	/**
	 * Record that an Exception was handled and doesn't need to be reported.
	 * Removes this exception and all of its causes from tracking.
	 *
	 * @param throwableToIgnore != null
	 */
	public void disregardThrowable(@Nonnull final Throwable throwableToIgnore) {
		Throwable cause = throwableToIgnore;
		int n = 0;
		while (cause != null && n < ExceptionUtil.MAX_TESTED_EXCEPTION_NESTING) {
			this.trackedThrowables.remove(cause);
			cause = cause.getCause();
			n++;
		}
	}

	/**
	 * See {@link #spot(Throwable)}.
	 *
	 * @param <T> of spottedThrowable
	 * @param spottedThrowable != null
	 * @return spottedThrowable
	 */
	public <T extends Throwable> T spotThrowable(@Nonnull final T spottedThrowable) {
		try {
			// check if it is already known indirectly ->
			for (Throwable t : this.trackedThrowables) {
				if (t != spottedThrowable && ExceptionUtil.stacktraceContainsException(t, spottedThrowable)) {
					report(new IllegalStateException("spotting <<" + spottedThrowable
							+ ">>,\n which was already spotted as cause of <<" + t + ">> (below).\n"
							+ "This is probably a problem", t));
					return spottedThrowable;
				}
			}
			Iterator<Throwable> it = this.trackedThrowables.iterator();
			while (it.hasNext()) {
				Throwable t = it.next();
				if (ExceptionUtil.stacktraceContainsException(spottedThrowable, t)) {
					it.remove();
				}
			}
			this.trackedThrowables.add(spottedThrowable);
			return spottedThrowable;
		} catch (ConcurrentModificationException e) {
			// can only happen if some other thread is reusing the Warden inappropriately
			logFromWithinLogging("Invalid use of Warden. A Warden may only be used by the same Thread. -> " + e.getMessage());
			return spottedThrowable;
		} catch (Exception e) {
			// this is a dangerous case and hopefully will not happen
			logFromWithinLogging(e.getMessage());
			return spottedThrowable;
		}
	}

	/**
	 * Return the currently appointed Warden.
	 * If no warden is appointed it appoints one on the fly which immediatly reports anything.
	 *
	 * @return
	 */
	private static Warden currentWarden() {
		Warden w = THREAD_WARDEN.get();
		if (w == null) {
			w = new Warden(null) {
				@Override
				public void disregardThrowable(final Throwable e) {
					report(new IllegalStateException("disregarding is impossible without a surrounding guard (ignored)", e));
				}

				@Override
				public <T extends Throwable> T spotThrowable(final T spottedThrowable) {
					report(spottedThrowable);
					return spottedThrowable;
				};
			};
			THREAD_WARDEN.set(w);
		}
		return w;
	}

	/**
	 * Must always be called in a finally block after {@link #appointWarden()}.
	 * Restores the Warden on duty when this warden was appointed.
	 *
	 * @return List of reported Throwables during this period (which have already been reported)
	 */
	public List<Throwable> finish() {
		if (!this.trackedThrowables.isEmpty()) {
			for (Throwable t : this.trackedThrowables) {
				encounter(t);
			}
			this.trackedThrowables.clear();
		}

		THREAD_WARDEN.set(this.outerWarden);

		List<Throwable> reportedThrowablesCopy = this.reportedThrowables;
		this.reportedThrowables = new LinkedList<Throwable>();
		return reportedThrowablesCopy;
	}

	/**
	 * Reports the Throwable as unhandled.
	 * Derived classes may do more with it.
	 *
	 * @param t {@link Throwable} != null
	 */
	protected void encounter(@Nonnull final Throwable t) {
		report(new UnhandledException(t));
	}

	/**
	 * Guard the execution of the {@link Runnable}.
	 * Reports any exceptions thrown but not handled.
	 * Exceptions thrown but not caught during execution (i.e. caught by this method) are only reported but not thrown
	 * further.
	 * If you want to propagate exceptions thru this construct you may use {@link TunnelException} which is forwarded.
	 * If you want to catch TunnelException also, then you need to wrap the Block with {@link ABlock#wrapInIgnoringBlock}.
	 * If you want to handle exceptions thrown by the block explicitly you could use {@link ABlock#wrapInReportingBlock} to
	 * handle these.
	 *
	 * @param runnable != null
	 * @return List of Exceptions reported (for reference only; they have already been reported).
	 */
	public static List<Throwable> guard(@Nonnull final Block<?> runnable) {
		Warden w = appointWarden();
		List<Throwable> unhandled;
		try {
			try {
				runnable.execute();
			} catch (TunnelException e) { // NOPMD special case
				throw e;
			} catch (Exception e) { // NOPMD generic code
				Warden.spot(e);
				Warden.report(e);
			}
		} finally {
			unhandled = w.finish();
		}
		return unhandled;
	}

	/**
	 * Must always be called in conjunction with {@link #finish}.
	 * It is recommended to use {@link #guard(Block)}.
	 *
	 * @return a new Warden which remembers the current warden
	 */
	public static Warden appointWarden() {
		Warden current = THREAD_WARDEN.get();
		Warden inner = new Warden(current);
		THREAD_WARDEN.set(inner);

		return inner;
	}

	/**
	 * Wraps the given block so that its exceptions will be properly reported.
	 *
	 * @param block != null
	 * @return protected Block not throwing any exception
	 */
	public static Running guarded(@Nonnull final Block<?> block) {
		return guarded(block, true);
	}

	/**
	 * Wraps the given block so that its exceptions will be properly reported.
	 *
	 * @param block != null
	 * @param strict false: only caught but unhandled exceptions will be reported exception declared by the block are
	 * reported; false: only
	 * @return protected Block not throwing any exception
	 */
	public static Running guarded(@Nonnull final Block<?> block, final boolean strict) {
		return new GuardedRunning(block, strict);
	}

	/**
	 * Wraps the given block so that its exceptions will be reported.
	 *
	 * @param block != null
	 * @param uncaughtExceptionHandler to use on exceptions
	 * @return protected Block not throwing any exception
	 */
	public static Running guarded(@Nonnull final Block<?> block, final UncaughtExceptionHandler uncaughtExceptionHandler) {
		return guarded(ABlock.wrapInReportingBlock(block, null, uncaughtExceptionHandler));
	}

	public static IllegalArgumentException illegal(final String string, final Object... args) {
		return Warden.spot(new IllegalArgumentException(String.format(string, args)));
	}

	public static IllegalArgumentException illegal(final Throwable t, final String string, final Object... args) {
		return Warden.spot(new IllegalArgumentException(String.format(string, args), t));
	}
}
