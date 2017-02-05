package de.zarncke.lib.err;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runner.notification.StoppedByUserException;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

import com.google.common.base.Supplier;

import de.zarncke.lib.block.Running;
import de.zarncke.lib.coll.L;
import de.zarncke.lib.ctx.Context;
import de.zarncke.lib.log.BufferedLog;
import de.zarncke.lib.log.Log;
import de.zarncke.lib.test.Tests;
import de.zarncke.lib.time.Times;
import de.zarncke.lib.util.Chars;
import de.zarncke.lib.value.Default;

/**
 * JUnit 4 variant of {@link GuardedTest}.
 * To be used like this: <code>
 * <pre>
 *
 * @RunWith(Guarded.class)
 * @MaxTestTimeMillis(2*Times.MILLIS_PER_SECOND) // applies to all methods (optional)
 * @Scope(THREAD) // THREAD is default
 * public class MyTest {
 * @MaxTestTimeMillis(10*Times.MILLIS_PER_SECOND) // applies to this method
 * @Test
 * public void testMe() throws Exception {
 * }
 * ...
 * }
 * </pre></code>
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
public class Guarded extends BlockJUnit4ClassRunner {
	/**
	 * Allows to access the unbuffered Log (the default log outside of the test - usually StdOut).
	 */
	public static final Context<Log> UNBUFFERED = Context.of(Default.of((Log) null, Log.class, "unbuffered"));

	public static final class DelegateNotifier extends RunNotifier {
		private final RunNotifier notifier;
		private final boolean[] inError;
		RunNotifier delegate;

		public DelegateNotifier(final RunNotifier notifier, final boolean[] inError) {
			this.notifier = notifier;
			this.inError = inError;
			this.delegate = this.notifier;
		}

		@Override
		public void addListener(final RunListener listener) {
			this.delegate.addListener(listener);
		}

		@Override
		public void removeListener(final RunListener listener) {
			this.delegate.removeListener(listener);
		}

		@Override
		public void fireTestRunStarted(final Description description) {
			this.delegate.fireTestRunStarted(description);
		}

		@Override
		public void fireTestRunFinished(final Result result) {
			this.delegate.fireTestRunFinished(result);
		}

		@Override
		public void fireTestStarted(final Description description) throws StoppedByUserException {
			this.delegate.fireTestStarted(description);
		}

		@Override
		public void fireTestFailure(final Failure failure) {
			this.inError[0] = true;
			this.delegate.fireTestFailure(failure);
		}

		@Override
		public void fireTestAssumptionFailed(final Failure failure) {
			this.delegate.fireTestAssumptionFailed(failure);
		}

		@Override
		public void fireTestIgnored(final Description description) {
			this.delegate.fireTestIgnored(description);
		}

		@Override
		public void fireTestFinished(final Description description) {
			this.delegate.fireTestFinished(description);
		}

		@Override
		public void pleaseStop() {
			this.delegate.pleaseStop();
		}

		@Override
		public void addFirstListener(final RunListener listener) {
			this.delegate.addFirstListener(listener);
		}
	}

	/**
	 * Time-out for tests. Defaults to some sensible default time-out.
	 * Can be set on class and method.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.TYPE, ElementType.METHOD })
	@Inherited
	public @interface MaxTestTimeMillis {
		long value() default Times.MILLIS_PER_SECOND;
	}

	/**
	 * The scope of the test-{@link Log} established for the test.
	 * Default is {@link #THREAD} which makes the Log available in the current thread only.
	 */
	public enum ScopeType {
		THREAD(Context.THREAD), INHERITED(Context.INHERITED), CLASS_LOADER(Context.CLASS_LOADER), GLOBAL(Context.GLOBAL);
		private Context.Scope scope;

		ScopeType(final Context.Scope scope) {
			this.scope = scope;
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@Inherited
	public @interface Scope {
		/**
		 * @return ScopeType
		 */
		ScopeType value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@Inherited
	public @interface WithContext {
		Class<? extends Supplier<Default<?>[]>> value();
	}

	static Default<?>[] getContext(final Class<?> klass) throws InitializationError {
		final WithContext annotation = klass.getAnnotation(WithContext.class);
		if (annotation == null) {
			return Default.many();
		}
		try {
			return annotation.value().newInstance().get();
		} catch (final IllegalAccessException e) {
			throw new InitializationError(e);
		} catch (final InstantiationException e) {
			throw new InitializationError(e);
		}
	}

	private static long getMaxTestTimeMillis(final Class<?> klass) {
		final MaxTestTimeMillis annotation = klass.getAnnotation(MaxTestTimeMillis.class);
		if (annotation == null) {
			return Long.MAX_VALUE;
		}
		return annotation.value();
	}

	@SuppressWarnings("deprecation" /* for backward compatibility */)
	private long getMaxMethodTimeMillis(final FrameworkMethod method) {
		final MaxTestTimeMillis annotation = method.getAnnotation(MaxTestTimeMillis.class);
		if (annotation == null) {
			if (this.lastCreatedFixture instanceof GuardedTest4) {
				return ((GuardedTest4) this.lastCreatedFixture).getMaximumTestMillis();
			}
			return this.maxTestTimeMillis;
		}
		return annotation.value();
	}

	private static Context.Scope getScope(final Class<?> klass) {
		final Scope annotation = klass.getAnnotation(Scope.class);
		if (annotation == null) {
			return Context.THREAD;
		}
		return annotation.value().scope;
	}

	private final long maxTestTimeMillis;
	private final Context.Scope scope;

	private Object lastCreatedFixture;

	private final Class<?>[] categories;

	private final Default<?>[] context;

	public Guarded(final Class<?> klass) throws InitializationError {
		this(klass, null);
	}

	/**
	 * @param klass to run
	 * @param categories to filter by; null means run all tests, empty means run only tests without category
	 * @throws InitializationError
	 */
	public Guarded(final Class<?> klass, final Class<?>[] categories) throws InitializationError {
		super(klass);
		this.maxTestTimeMillis = getMaxTestTimeMillis(klass);
		this.scope = getScope(klass);
		this.categories = categories;
		this.context = getContext(klass);
	}

	/**
	 * Called reflectively on classes annotated with <code>@RunWith(Suite.class)</code>
	 *
	 * @param klass the root class
	 * @param builder builds runners for classes in the suite
	 * @param categories (optional) to use for filtering executed tests
	 * @throws InitializationError
	 */
	public Guarded(final Class<?> klass, final RunnerBuilder builder, final Class<?>[] categories) throws InitializationError {
		this(klass, categories);
	}

	@Override
	protected void runChild(final FrameworkMethod method, final RunNotifier notifier) {
		final Description methodDescription = Description.createTestDescription(getTestClass().getJavaClass(),
				testName(method), method.getAnnotations());
		if (this.categories != null
				&& !(Tests.hasMethodMatchingCategory(method.getMethod(), this.categories) || Tests.hasClassMatchingCategory(
						method.getMethod().getDeclaringClass(), this.categories))) {
			notifier.fireTestIgnored(methodDescription);
			return;
		}

		final Default<?>[] originalContext = Context.bundleCurrentContext();

		// set up
		Warden testWarden = Warden.appointWarden();
		BufferedLog bufferLog = new BufferedLog();
		final Log defLog = Log.LOG.get();
		Default<Log> previousLog = Context.setFromNowOn(Default.of(bufferLog, Log.class), this.scope);
		final Log delegateLog = previousLog.getValue() == null ? defLog : previousLog.getValue();
		bufferLog.setDelegate(delegateLog);
		Context.setFromNowOn(UNBUFFERED.getOtherDefault(delegateLog), this.scope);

		// execute guarded
		final boolean[] inError = new boolean[1];
		final RunNotifier xnotifier = new DelegateNotifier(notifier, inError);
		final long start = System.currentTimeMillis();

		Warden.guard(new Running() {
			@Override
			public void run() {
				if (Guarded.this.context != null && Guarded.this.context.length > 0) {
					Context.runWith(new Running() {
						@Override
						public void run() {
							superRunChild(method, xnotifier);
						}
					}, Guarded.this.context);
				} else {
					superRunChild(method, xnotifier);
				}
			}
		});

		// check timing
		final long dur = System.currentTimeMillis() - start;
		if (isTimeExceeded(method, dur)) {
			final String msg = "test took too long " + dur + "ms, allowed is only " + getMaxMethodTimeMillis(method) + "ms";
			if (isTimingFailureEnabled()) {
				notifier.fireTestFailure(new Failure(methodDescription, new TimeoutException(msg)));
			} else {
				delegateLog.report(msg);
			}
		}

		// clean up
		testWarden.finish();
		Context.setFromNowOn(previousLog, this.scope);
		Context.setFromNowOn(UNBUFFERED.getOtherDefault(null), this.scope);
		if (inError[0]) {
			bufferLog.flush();
		} else {
			if (bufferLog.getNumberOfBufferedIssues() > 0 || bufferLog.getNumberOfBufferedThrowables() > 0) {
				final StringBuilder msg = new StringBuilder("Test ").append(getClass().getName()).append(" successful. ");
				if (bufferLog.getNumberOfBufferedIssues() > 0) {
					msg.append(" Didn't print ").append(bufferLog.getNumberOfBufferedIssues()).append(" issues.");
				}
				if (bufferLog.getNumberOfBufferedThrowables() > 0) {
					msg.append(" Didn't report ").append(bufferLog.getNumberOfBufferedThrowables()).append(" exceptions.");
				}
				Log.LOG.get().report(msg);
				bufferLog.clear();
			}
		}

		assertAndCorrectRemainingContext(originalContext);

		testWarden = null;
		inError[0] = false;
		previousLog = null;
		bufferLog = null;
	}

	static void assertAndCorrectRemainingContext(final Default<?>[] originalContext) {
		final Default<?>[] remainingContext = Context.bundleCurrentContext();
		if (remainingContext.length != originalContext.length) {
			// correct Context
			for (final Default<?> remaining : remainingContext) {
				boolean exists = false;
				for (final Default<?> orig : originalContext) {
					if (orig.getType().equals(remaining.getType())) {
						exists = true;
						break;
					}
				}
				if (!exists) {
					Context.setFromNowOn(remaining.withOtherValue(null), Context.GLOBAL);
				}
			}

			Assert.assertEquals("Context after execution of test differs from Context before - missing cleanup",
					Chars.join(L.l(originalContext), "\n").toString(), Chars.join(L.l(remainingContext), "\n").toString());
		}
	}

	protected void superRunChild(final FrameworkMethod method, final RunNotifier notifier) {
		super.runChild(method, notifier);
	}

	@Override
	protected Object createTest() throws Exception {
		final Object fixture = super.createTest();
		this.lastCreatedFixture = fixture;
		return fixture;
	}

	public static Log getUnbufferedLog() {
		return UNBUFFERED.get();
	}

	private boolean isTimeExceeded(final FrameworkMethod method, final long dur) {
		double scale = 1.0;
		String scaleStr = System.getProperty(GuardedTest.SCALE_TEST_TIMEOUT_PROPERTY);
		if (scaleStr == null) {
			scaleStr = System.getenv(GuardedTest.SCALE_TEST_TIMEOUT_PROPERTY);
		}
		if (scaleStr != null) {
			try {
				scale = Double.parseDouble(scaleStr);
			} catch (final NumberFormatException e) {
				Warden.report(e);
			}
		}
		final long maxMethodTimeMillis = getMaxMethodTimeMillis(method);
		return dur > maxMethodTimeMillis * scale;
	}

	private boolean isTimingFailureEnabled() {
		return System.getProperty(GuardedTest.IGNORE_TEST_TIMEOUT_PROPERTY) == null
				&& System.getenv(GuardedTest.IGNORE_TEST_TIMEOUT_PROPERTY) == null;
	}

}
