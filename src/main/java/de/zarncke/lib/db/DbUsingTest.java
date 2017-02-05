package de.zarncke.lib.db;

import de.zarncke.lib.block.Running;
import de.zarncke.lib.block.StrictBlock;
import de.zarncke.lib.err.GuardedTest;
import de.zarncke.lib.err.TunnelException;

/**
 * All tests in test classes inheriting from this one will be run in a transactional context.
 * If you need a specific db instead of the {@link Db#DEFAULT_DB default}, you may set it up in the {@link #setUp()} method.
 *
 * @author Gunnar Zarncke
 */
public abstract class DbUsingTest extends GuardedTest {

	@Override
	public void setUp() throws Exception {
		super.setUp();
		Db.transactional(new StrictBlock<Void>() {
			public Void execute() {
				setUpInTransaction();
				return null;
			}
		});
	}

	protected void setUpInTransaction() {
		// filled by derived classes
	}

	@Override
	protected void runTest() throws Throwable { // NOPMD test
		try {
			Db.transactional(new Running() {
				public void run() {
					try {
						superRunTest();
					} catch (Throwable e) { // NOPMD library
						throw new TunnelException(e);
					}
				}
			});
		} catch (TunnelException e) {
			e.unpack();
		}
	}

	private void superRunTest() throws Throwable { // NOPMD test
		super.runTest();
	}

}
