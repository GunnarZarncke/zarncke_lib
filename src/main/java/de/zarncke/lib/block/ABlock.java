package de.zarncke.lib.block;

import java.io.Serializable;
import java.lang.Thread.UncaughtExceptionHandler;

import de.zarncke.lib.err.TunnelException;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.time.Profiling;

/**
 * A {@link Block} with some convenience.
 *
 * @author Gunnar Zarncke
 * @param <T> or return
 */
public abstract class ABlock<T> implements Block<T> {

	public static final class ThreadNamedBlock<T> implements Block<T>, Serializable {
		private static final long serialVersionUID = 1L; // serializable if nested elements are
		private String name;
		private Block<T> block;

		@SuppressWarnings("unused")
		private ThreadNamedBlock() { // for serialization
		}

		public ThreadNamedBlock(final String name, final Block<T> block) {
			this.name = name;
			this.block = block;
		}

		@Override
		public T execute() throws Exception {
			final Thread currentThread = Thread.currentThread();
			final String currentThreadName = currentThread.getName();
			Thread.currentThread().setName(currentThreadName + " " + this.name);
			try {
				return this.block.execute();
			} finally {
				Thread.currentThread().setName(currentThreadName);
			}
		}

		@Override
		public String toString() {
			return this.block + " named " + this.name;
		}
	}

	public static final class ThreadNamedStrictBlock<T> implements StrictBlock<T>, Serializable {
		private static final long serialVersionUID = 1L; // serializable if nested elements are
		private String name;
		private StrictBlock<T> block;

		@SuppressWarnings("unused")
		private ThreadNamedStrictBlock() { // for serialization
		}

		public ThreadNamedStrictBlock(final String name, final StrictBlock<T> block) {
			this.name = name;
			this.block = block;
		}

		@Override
		public T execute() {
			final Thread currentThread = Thread.currentThread();
			final String currentThreadName = currentThread.getName();
			Thread.currentThread().setName(currentThreadName + " " + this.name);
			try {
				return this.block.execute();
			} finally {
				Thread.currentThread().setName(currentThreadName);
			}
		}

		@Override
		public String toString() {
			return this.block + " named " + this.name;
		}
	}

	public static final class IgnoringBlock<T> implements StrictBlock<T>, Serializable {
		private static final long serialVersionUID = 1L; // serializable if nested elements are
		private T valueForErrors;
		private Block<T> block;

		@SuppressWarnings("unused")
		private IgnoringBlock() { // for serialization
		}

		public IgnoringBlock(final T valueForErrors, final Block<T> block) {
			this.valueForErrors = valueForErrors;
			this.block = block;
		}

		@Override
		public T execute() {
			try {
				return this.block.execute();
			} catch (Exception e) { // NOPMD generic code
				Warden.report(e);
				return this.valueForErrors;
			}
		}

		@Override
		public String toString() {
			return "ignoring errors of " + this.block + " -> " + this.valueForErrors;
		}
	}

	public static final class TunnelingBlock<T> implements StrictBlock<T>, Serializable {
		private static final long serialVersionUID = 1L; // serializable if nested elements are
		private Block<T> block;

		public TunnelingBlock(final Block<T> block) {
			this.block = block;
		}

		@SuppressWarnings("unused")
		private TunnelingBlock() { // for serialization
		}

		@Override
		public T execute() {
			try {
				return this.block.execute();
			} catch (RuntimeException e) { // NOPMD generic code
				throw e;
			} catch (Exception e) { // NOPMD generic code
				throw new TunnelException(e);
			}
		}

		@Override
		public String toString() {
			return "tunneling " + this.block;
		}
	}

	public static final class ProfilingBlock<T> implements StrictBlock<T>, Serializable {
		private static final long serialVersionUID = 1L; // serializable if nested elements are
		private String profilingKey;
		private StrictBlock<T> block;

		private ProfilingBlock() { // for serialization
		}

		public ProfilingBlock(final String profilingKey, final StrictBlock<T> block) {
			this.profilingKey = profilingKey;
			this.block = block;
		}

		@Override
		public T execute() {
			long start = System.nanoTime();
			try {
				return this.block.execute();
			} finally {
				long end = System.nanoTime();
				Profiling.CTX.get().time(this.profilingKey, (end - start) / 1000000);
			}
		}

		@Override
		public String toString() {
			return "profiling " + this.block + " as " + this.profilingKey;
		}
	}

	static final class ReportingBlock<T> implements StrictBlock<T>, Serializable {
		private static final long serialVersionUID = 1L; // serializable if nested elements are
		private UncaughtExceptionHandler uncaughtExceptionHandler;
		private Block<T> block;
		private T valueForErrors;
		private boolean alsoHandleErrors;

		@SuppressWarnings("unused")
		private ReportingBlock() { // for serialization
		}

		public ReportingBlock(final UncaughtExceptionHandler uncaughtExceptionHandler, final Block<T> block, final T valueForErrors) {
			this(uncaughtExceptionHandler, block, valueForErrors, false);
		}

		public ReportingBlock(final UncaughtExceptionHandler uncaughtExceptionHandler, final Block<T> block,
				final T valueForErrors, final boolean alsoHandleErrors) {
			this.uncaughtExceptionHandler = uncaughtExceptionHandler;
			this.block = block;
			this.valueForErrors = valueForErrors;
			this.alsoHandleErrors = alsoHandleErrors;
		}

		@Override
		public T execute() {
			try {
				return this.block.execute();
			} catch (Error e) { // NOPMD generic code
				if (this.alsoHandleErrors) {
					this.uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), e);
					return this.valueForErrors;
				}
				throw e;
			} catch (Exception e) { // NOPMD generic code
				this.uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), e);
				return this.valueForErrors;
			}
		}

		@Override
		public String toString() {
			return "reporting errors of " + this.block + " to " + this.uncaughtExceptionHandler;
		}
	}

	public StrictBlock<T> logAndIgnoreExceptions(final T valueForErrors) {
		return wrapInIgnoringBlock(this, valueForErrors);
	}

	public StrictBlock<T> tunnelExceptions() {
		return wrapInTunnelingBlock(this);
	}

	public static <T> StrictBlock<T> wrapInProfilingBlock(final StrictBlock<T> block, final String profilingKey) {
		return new ProfilingBlock<T>(profilingKey, block);
	}

	public static <T> StrictBlock<T> wrapInTunnelingBlock(final Block<T> block) {
		return new TunnelingBlock<T>(block);
	}

	/**
	 * Reports but ignores Exceptions thrown by the wrapped block.
	 *
	 * @param block to wrap
	 * @param valueForErrors to return in case of exceptions
	 * @return StrictBlock
	 */
	public static <T> StrictBlock<T> wrapInIgnoringBlock(final Block<T> block, final T valueForErrors) {
		return new IgnoringBlock<T>(valueForErrors, block);
	}

	/**
	 * Exception thrown by the wrapped block are delegated to a handler..
	 *
	 * @param block to wrap
	 * @param valueForErrors to return in case of exceptions
	 * @param uncaughtExceptionHandler to handle exceptions
	 * @return StrictBlock
	 */
	public static <T> StrictBlock<T> wrapInReportingBlock(final Block<T> block, final T valueForErrors,
			final UncaughtExceptionHandler uncaughtExceptionHandler) {
		return new ReportingBlock<T>(uncaughtExceptionHandler, block, valueForErrors);
	}

	/**
	 * Same as {@link #wrapInReportingBlock(Block, Object, UncaughtExceptionHandler)} but also catches Errors.
	 *
	 * @param block to wrap
	 * @param valueForErrors to return in case of exceptions
	 * @param uncaughtExceptionHandler to handle exceptions
	 * @return StrictBlock
	 */
	public static <T> StrictBlock<T> wrapInStrongReportingBlock(final Block<T> block, final T valueForErrors,
			final UncaughtExceptionHandler uncaughtExceptionHandler) {
		return new ReportingBlock<T>(uncaughtExceptionHandler, block, valueForErrors, true);
	}

	public static <T> StrictBlock<T> wrapInThreadNamedBlock(final StrictBlock<T> block, final String name) {
		return new ThreadNamedStrictBlock<T>(name, block);
	}

	public static <T> Block<T> wrapInThreadNamedBlock(final Block<T> block, final String name) {
		return new ThreadNamedBlock<T>(name, block);
	}

	public static Runnable toRunnable(final StrictBlock<?> block) {
		return new Running() {
			@Override
			public void run() {
				block.execute();
			}
		};
	}

	public static Block<Void> toBlock(final Runnable runnable) {
		return new Running() {
			@Override
			public void run() {
				runnable.run();
			}
		};
	}
}
