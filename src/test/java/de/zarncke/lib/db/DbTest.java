package de.zarncke.lib.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assume;
import org.junit.Test;

import de.zarncke.lib.block.Running;
import de.zarncke.lib.db.Db.Utilizer;
import de.zarncke.lib.err.GuardedTest;

public final class DbTest extends GuardedTest {

	@Override
	public void tearDown() throws Exception {
		try {
			if (Db.isInitialized()) {
				Db.transactional(new Running() {
					public void run() {
						Db.utilize("DROP TABLE TEST_A", null);
					}
				});
			}
		} finally {
			super.tearDown();
		}
	}

	// TODO test that transaction boundaries work

	@Test
	public void testDb() {
		Assume.assumeTrue(Db.isInitialized());

		Db.transactional(new Running() {
			public void run() {
				Db.utilize("CREATE TABLE TEST_A ( S VARCHAR(64), I INTEGER )", null);
				for (int i = 1; i < 10; i++) {
					final int ii = i;
					Db.utilize("INSERT INTO TEST_A ( S, I) VALUES (?, ?)", new Utilizer<Void>() {
						@Override
						public void setParameters(final PreparedStatement preparedStatement) throws SQLException {
							preparedStatement.setString(1, "Hello World");
							preparedStatement.setInt(2, ii);
						}
					});
				}
				List<Integer> il = Db.utilize("SELECT DISTINCT I FROM TEST_A WHERE S = ? AND I < 5",
						new Utilizer<List<Integer>>() {
							@Override
							public void setParameters(final PreparedStatement preparedStatement) throws SQLException {
								preparedStatement.setString(1, "Hello World");
							}

							private final List<Integer> res = new ArrayList<Integer>();

							@Override
							public void addResult(final ResultSet resultSet) throws SQLException {
								this.res.add(Integer.valueOf(resultSet.getInt("I")));
							}

							@Override
							public List<Integer> result() {
								return this.res;
							}
						});
				assertEquals(4, il.size());
			}
		});
	}
}
