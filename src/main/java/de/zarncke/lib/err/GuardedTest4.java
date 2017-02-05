package de.zarncke.lib.err;

import org.junit.runner.RunWith;

import de.zarncke.lib.err.Guarded.MaxTestTimeMillis;
import de.zarncke.lib.log.Log;
import de.zarncke.lib.time.Times;

@RunWith(Guarded.class)
public abstract class GuardedTest4 extends Asserts {
	/**
	 * @return default Log (always writes thru)
	 */
	public Log getUnbufferedLog() {
		return Guarded.UNBUFFERED.getChecked();
	}

	/**
	 * This method is for convenience and the few cases where the Log will again be changed by another context.
	 * Note: This Log is available in the current {@link Log#LOG} Context.
	 *
	 * @return Log which will only be written out to the default Log when tests failed
	 */
	public Log getBufferLog() {
		return Log.LOG.get();
	}

	/**
	 * Derived tests may declare that they take longer than the default.
	 * Value can be overridden with the annotation.
	 * 
	 * @return {@link Times#MILLIS_PER_SECOND}
	 * @deprecated Use {@link MaxTestTimeMillis} instead
	 */
	@Deprecated
	protected long getMaximumTestMillis() {
		return Times.MILLIS_PER_SECOND;
	}

	/**
	 * present only to allow simple transition of JUnit 4 tests.
	 * 
	 * @deprecated use @RunBefore instead
	 * @throws Exception same signature
	 */
	@Deprecated
	protected void setUp() throws Exception {
		// empty
	}

	/**
	 * present only to allow simple transition of JUnit 4 tests.
	 *
	 * @deprecated use @RunAfetr instead
	 * @throws Exception same signature
	 */
	@Deprecated
	protected void tearDown() throws Exception {
		// empty
	}
}
