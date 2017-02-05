package de.zarncke.lib.db;

import de.zarncke.lib.err.GuardedTest;

/**
 * A test which may use Cassandra.
 * All tests share the same config.
 * Important note: This starts a Cassandra daemon which cannot be stopped completely after the test!
 *
 * @author Gunnar Zarncke
 */
public abstract class CassandraUsingTest extends GuardedTest {

	// private static CassandraDaemon cassandra;
	//
	// @Override
	// public void setUp() throws Exception {
	// super.setUp();
	//
	// if (cassandra == null) {
	// cassandra = new CassandraDaemon();
	// cassandra.init(new String[0]);
	// }
	// cassandra.start();
	// }
	//
	// @Override
	// public void tearDown() throws Exception {
	// super.tearDown();
	// if (cassandra != null) {
	// cassandra.stop();
	// }
	// }

}
