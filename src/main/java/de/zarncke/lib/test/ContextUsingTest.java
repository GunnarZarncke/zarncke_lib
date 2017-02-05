package de.zarncke.lib.test;

import de.zarncke.lib.block.Running;
import de.zarncke.lib.ctx.Context;
import de.zarncke.lib.err.GuardedTest;
import de.zarncke.lib.err.TunnelException;
import de.zarncke.lib.value.Default;

/**
 * A {@link GuardedTest} which provides easy use of
 * {@link Context#runWith(de.zarncke.lib.block.StrictBlock, Default...)}.
 *
 * @author Gunnar Zarncke
 */
public abstract class ContextUsingTest extends GuardedTest {
	@Override
	protected void runTest() throws Throwable {
		try {
			Context.runWith(new Running() {
				@Override
				public void run() {
					try {
						setUpInContext();
						try {
							superRunTest();
						} finally {
							tearDownInContext();
						}
					} catch (Throwable e) { // NOPMD
						throw new TunnelException(e);
					}
				}

			}, getScope(), getContextsToApply());
		} catch (TunnelException e) { // NOPMD no generic exceptions
			e.unpack();
		}
	}

	protected final void superRunTest() throws Throwable {
		super.runTest();
	}

	/**
	 * @return e.g. {@link Default#many(Default...)}.
	 */
	protected abstract Default<?>[] getContextsToApply();

	/**
	 * @throws Exception may be thrown by derived classes
	 */
	protected void tearDownInContext() throws Exception {
		// for derived
	}

	/**
	 * @throws Exception may be thrown by derived classes
	 */
	protected void setUpInContext() throws Exception {
		// for derived
	}

}
