package de.zarncke.lib.err;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;

import de.zarncke.lib.ctx.Context;
import de.zarncke.lib.ctx.Context.Scope;
import de.zarncke.lib.log.BufferedLog;
import de.zarncke.lib.log.Log;
import de.zarncke.lib.time.Times;
import de.zarncke.lib.value.Default;

/**
 * Abstract {@link TestCase} which appoints a Warden for each test and intercepts logging for cleaner reporting.
 *
 * @author Gunnar Zarncke
 */
public abstract class GuardedTest extends TestCase {
	/**
	 * -D{@value #IGNORE_TEST_TIMEOUT_PROPERTY}=true means test timing failures are ignored
	 */
	public static final String IGNORE_TEST_TIMEOUT_PROPERTY = "de.zarncke.lib.IgnoreTestTimeout";
	/**
	 * -D{@value #SCALE_TEST_TIMEOUT_PROPERTY}=.5 means tests may take only half the time
	 */
	public static final String SCALE_TEST_TIMEOUT_PROPERTY = "de.zarncke.lib.ScaleTestTimeout";

	protected Warden testWarden;
	private boolean inError;
	private BufferedLog bufferLog;
	private Default<Log> previousLog = null;
	private Log delegateLog;

	Default<?>[] originalContext;

	@Before
	@Override
	public void setUp() throws Exception {
		Assert.assertNull(this.testWarden);
		Assert.assertNull(this.previousLog);

		this.originalContext = Context.bundleCurrentContext();

		this.testWarden = Warden.appointWarden();
		this.bufferLog = new BufferedLog();
		final Log defLog = Log.LOG.get();
		this.previousLog = Context.setFromNowOn(Default.of(this.bufferLog, Log.class), getScope());
		this.delegateLog = this.previousLog.getValue() == null ? defLog : this.previousLog.getValue();
		this.bufferLog.setDelegate(this.delegateLog);
		this.inError = false;
	}

	@Override
	protected void runTest() throws Throwable { // NOPMD junit
		if (this.testWarden == null) {
			throw Warden.spot(new IllegalStateException("setUp() wasn't called. Maybe you forgot super.setUp()?"));
		}
		try {
			final long start = System.currentTimeMillis();
			super.runTest();
			final long end = System.currentTimeMillis();
			final long dur = end - start;
			if (isTimeExceeded(dur)) {
				final String msg = "test took too long " + dur + "ms, allowed is only " + getMaximumTestMillis() + "ms";
				if (isTimingFailureEnabled()) {
					fail(msg);
				} else {
					getUnbufferedLog().report(msg);
				}
			}
		} catch (final Throwable e) { // NOPMD generic
			this.inError = true;
			throw e;
		}
	}

	private boolean isTimeExceeded(final long dur) {
		double scale = 1.0;
		final long maximumTestMillis = getMaximumTestMillis();
		String scaleStr = System.getProperty(SCALE_TEST_TIMEOUT_PROPERTY);
		if (scaleStr == null) {
			scaleStr = System.getenv(IGNORE_TEST_TIMEOUT_PROPERTY);
		}
		if (scaleStr != null) {
			try {
				scale = Double.parseDouble(scaleStr);
			} catch (final NumberFormatException e) {
				Warden.report(e);
			}
		}
		return dur > maximumTestMillis * scale;
	}

	private boolean isTimingFailureEnabled() {
		return System.getProperty(IGNORE_TEST_TIMEOUT_PROPERTY) == null && System.getenv(IGNORE_TEST_TIMEOUT_PROPERTY) == null;
	}

	/**
	 * Derived tests may declare that they take longer than the default
	 *
	 * @return {@link Times#MILLIS_PER_SECOND}
	 */
	protected long getMaximumTestMillis() {
		return Times.MILLIS_PER_SECOND;
	}

	@After
	@Override
	public void tearDown() throws Exception {
		if (this.testWarden == null) {
			throw new IllegalStateException("tearDown() called without setUp(), did you override without calling super?");
		}
		this.testWarden.finish();
		Context.setFromNowOn(this.previousLog, getScope());
		if (this.inError) {
			this.bufferLog.flush();
		} else {
			if (this.bufferLog.getNumberOfBufferedIssues() > 0 || this.bufferLog.getNumberOfBufferedThrowables() > 0) {
				final StringBuilder msg = new StringBuilder("Test ").append(getClass().getName()).append(" successful. ");
				if (this.bufferLog.getNumberOfBufferedIssues() > 0) {
					msg.append(" Didn't print ").append(this.bufferLog.getNumberOfBufferedIssues()).append(" issues.");
				}
				if (this.bufferLog.getNumberOfBufferedThrowables() > 0) {
					msg.append(" Didn't report ").append(this.bufferLog.getNumberOfBufferedThrowables()).append(" exceptions.");
				}
				Log.LOG.get().report(msg);
				this.bufferLog.clear();
			}
		}

		Guarded.assertAndCorrectRemainingContext(this.originalContext);

		this.testWarden = null;
		this.inError = false;
		this.previousLog = null;
		this.bufferLog = null;
	}

	/**
	 * @return default Log (always writes thru)
	 */
	public Log getUnbufferedLog() {
		if (this.delegateLog == null) {
			throw Warden.spot(new IllegalStateException("may only be called after setUp()"));
		}
		return this.delegateLog;
	}

	/**
	 * This method is for convenience and the few cases where the Log will again be changed by another context.
	 * Note: This Log is available in the current {@link Log#LOG} Context.
	 *
	 * @return Log which will only be written out to the default Log when tests failed
	 */
	public BufferedLog getBufferLog() {
		if (this.delegateLog == null) {
			throw Warden.spot(new IllegalStateException("may only be called after setUp()"));
		}
		return this.bufferLog;
	}

	/**
	 * Derived tests which start {@link Thread}s might use {@link Context#INHERITED}.
	 *
	 * @return Scope to use for all contexts including logging
	 */
	protected Scope getScope() {
		return Context.THREAD;
	}

	public static boolean failIfAfter(final int year, final int month, final int day) {
		return Asserts.failIfAfter(year, month, day);
	}

	public static void assertContentEquals(final List<?> a, final List<?> b) {
		Asserts.assertContentEquals(a, b);
	}

	public static void assertContentNestedEquals(final List<?> a, final List<?> b) {
		Asserts.assertContentNestedEquals(a, b);
	}

	public static void assertContentEquals(final Set<?> a, final Set<?> b) {
		Asserts.assertContentEquals(a, b);
	}

	public static void assertContentEquals(final Collection<?> a, final Collection<?> b) {
		Asserts.assertContentEquals(a, b);
	}

	public static void assertDistinct(final List<?> l) {
		Asserts.assertDistinct(l);
	}

	public static void assertEmpty(final Collection<?> list) {
		Asserts.assertEmpty(list);
	}

	public static void assertStartsWith(final String expectedPrefix, final String actualResult) {
		Asserts.assertStartsWith(expectedPrefix, actualResult);
	}

	public static void assertContains(final String expectedPart, final String actualResult) {
		Asserts.assertContains(expectedPart, actualResult);
	}

	public static void assertContains(final String msg, final String expectedPart, final String actualResult) {
		Asserts.assertContains(msg, expectedPart, actualResult);
	}

	public static void coverPrivateConstructorOf(final Class<?> clazz) {
		Asserts.coverPrivateConstructorOf(clazz);
	}

	public static void assertInstanceOf(final Class<?> type, final Object obj) {
		Asserts.assertInstanceOf(type, obj);
	}

	public static void assertInstanceOf(final String msg, final Class<?> type, final Object obj) {
		Asserts.assertInstanceOf(msg, type, obj);
	}
}
