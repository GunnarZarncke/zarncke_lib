package de.zarncke.lib.test;

import de.zarncke.lib.block.Running;
import de.zarncke.lib.ctx.Context;
import de.zarncke.lib.db.DbUsingTest;
import de.zarncke.lib.err.TunnelException;
import de.zarncke.lib.value.Default;

public abstract class DbAndContextUsingTest extends DbUsingTest {
	@Override
	protected void runTest() throws Throwable {
		try {
			Context.runWith(new Running() {
				public void run() {
					try {
						superRunTest();
					} catch (Throwable e) { // NOPMD
						throw new TunnelException(e);
					}
				}
			}, getContextsToApply());
		} catch (TunnelException e) {
			e.unpack();
		}
	}

	protected void superRunTest() throws Throwable {
		super.runTest();
	}

	protected abstract Default<?>[] getContextsToApply();


}
