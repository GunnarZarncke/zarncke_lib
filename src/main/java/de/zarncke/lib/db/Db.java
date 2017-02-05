package de.zarncke.lib.db;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import de.zarncke.lib.block.ABlock;
import de.zarncke.lib.block.Block;
import de.zarncke.lib.block.StrictBlock;
import de.zarncke.lib.coll.Elements;
import de.zarncke.lib.ctx.Context;
import de.zarncke.lib.err.CantHappenException;
import de.zarncke.lib.err.TunnelException;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.io.IOTools;
import de.zarncke.lib.io.store.Store;
import de.zarncke.lib.value.Default;

/**
 * Very simple wrapper around {@link Connection} to have clean transaction boundaries.
 * No pooling. A new connection is created for each request.
 *
 * @author Gunnar Zarncke
 */
public class Db implements Cloneable {
	/**
	 * Uses {@value #DEFAULT_DRIVER}.
	 */
	public static final String DEFAULT_DRIVER = "org.hsqldb.jdbcDriver";
	/**
	 * Uses {@value #DEFAULT_DB}.
	 */
	public static final String DEFAULT_DB = "jdbc:hsqldb:mem:zarncke";
	public static final Context<Db> DB = Context.of(Default.of(new Db(), Db.class));

	/**
	 * Indicates a runtime problem with the Db.
	 */
	public static class DbException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public DbException(final String msg) {
			super(msg);
		}
		public DbException(final String msg, final Exception e) {
			super(msg, e);
		}
	}

	public static final Db NO_DB = new Db() {
		@Override
		protected Connection createConnection() {
			throw Warden.spot(new DbException("no DB configured"));
		}
	};

	static int openConnections = 0;

	private static boolean initialized;

	public static boolean isInitialized() {
		return initialized;
	}

	static {
		try {
			DriverManager.registerDriver((Driver) Class.forName(DEFAULT_DRIVER).newInstance());
			initialized = true;
		} catch (Exception e) {
			Warden.disregard(e);
			initialized = false;
		}
	}

	private Connection connection = null;
	private boolean outer = true;

	private static boolean synced = true;

	/**
	 * Creates an outer instance of the Db.
	 * Note: nested instances are created in {@link #nest()} by {@link #clone()}.
	 */
	protected Db() {
		//
	}

	/**
	 * Runs the {@link StrictBlock} in a transaction.
	 * The transaction will be rolled back on any {@link RuntimeException} (or {@link Error}).
	 * The {@link StrictBlock} may now use e.g. {@link Db#getConnection()}
	 *
	 * @param runnable != null
	 * @return passed thru result
	 * @param <T> type of result
	 */
	public static <T> T transactional(final StrictBlock<T> runnable) {
		Db db = DB.get().nest();
		boolean aborted = false;
		try {
			return Context.runWith(runnable, Default.of(db, Db.class));
		} catch (Throwable t) { // NOPMD library
			aborted = true;
			db.rollback();
			if (t instanceof RuntimeException) {
				throw (RuntimeException) t;
			}
			throw (Error) t;
		} finally {
			if (!aborted) {
				db.commit();
			}
			db.close();
		}
	}

	/**
	 * Runs the {@link Block} in a transaction.
	 * The transaction will be rolled back on any {@link RuntimeException} (or {@link Error}) but not on checked
	 * {@link Exception}s or {@link TunnelException}s
	 * which are passed on.
	 * The {@link Block} may now use e.g. {@link Db#getConnection()}
	 *
	 * @param runnable != null
	 * @return passed thru result
	 * @param <T> type of result
	 * @throws Exception if the block threw any
	 */
	public static <T> T transactional(final Block<T> runnable) throws Exception { // NOPMD block API
		Db db = DB.get().nest();
		boolean abort = false;
		try {
			try {
				return Context.runWith(ABlock.wrapInTunnelingBlock(runnable), Default.of(db, Db.class));
			} catch (TunnelException t) {
				throw (Exception) t.getThrowable();
			} catch (RuntimeException t) {
				abort = true;
				throw t;
			} catch (Error t) {
				abort = true;
				throw t;
			}
		} finally {
			if (abort) {
				db.rollback();
			} else {
				db.commit();
			}
			db.close();
		}
	}

	protected void close() {
		if (this.connection != null) {
			try {
				this.connection.close();
				openConnections--;
			} catch (SQLException e) {
				// Warden.disregard(e);
				throw Warden.spot(new DbException("closing connection failed", e));
			}
		}
	}

	private void commit() {
		if (this.connection != null) {
			try {
				this.connection.commit();
			} catch (SQLException e) {
				throw Warden.spot(new DbException("commit failed", e));
			}
		}
	}

	private void rollback() {
		if (this.connection != null) {
			try {
				this.connection.rollback();
			} catch (SQLException e) {
				throw Warden.spot(new DbException("rollback failed", e));
			}
		}
	}

	public final Connection getConnection() {
		if (this.outer) {
			throw Warden.spot(new IllegalStateException("cannot create Connection outside of a call to transactional() "
					+ Context.currentContextReport()));
		}
		if (this.connection == null) {
			this.connection = createConnection();
			openConnections++;
		}
		return this.connection;
	}

	/**
	 * Creates a new (nested) instance. Derived classes must use this to provide instances.
	 *
	 * @return Db
	 */
	protected Db nest() {
		try {
			Db db = (Db) clone();
			// default nesting creates new conections
			db.connection = null;
			db.outer = false;
			return db;
		} catch (CloneNotSupportedException e) {
			throw Warden.spot(new CantHappenException("we implement Cloneable", e));
		}
	}

	/**
	 * Derived classes may use their own Driver. This default uses HSQLDB in memory (assuming it is registered).
	 *
	 * @return Connection != null
	 */
	protected Connection createConnection() {
		try {
			return DriverManager.getConnection(DEFAULT_DB, "sa", "");
		} catch (SQLException e) {
			throw Warden.spot(new DbException("failed to create connection by url " + DEFAULT_DB, e));
		}
	}

	/**
	 * Abstract operations on {@link PreparedStatement PreparedStatements}.
	 *
	 * @author Gunnar Zarncke
	 * @param <T> type of result (use Void for UPDATEs)
	 */
	public static class Utilizer<T> {

		/**
		 * Must be used to set query parameters.
		 *
		 * @param preparedStatement != null
		 * @throws SQLException may be thrown by implementers
		 */
		public void setParameters(final PreparedStatement preparedStatement) throws SQLException {
			// nothing
		}

		/**
		 * @param resultSet positioned on the row to process
		 * @throws SQLException may be thrown by implementers
		 */
		public void addResult(final ResultSet resultSet) throws SQLException {
			// nothing
		}

		public T result() {
			return null;
		}
	}

	/**
	 * Executes the given SQL.
	 * The Utilizers {@link Utilizer#addResult(ResultSet)} will only be called for SELECT statements.
	 *
	 * @param <T> result
	 * @param sql != null
	 * @param utilizer to parameterize the sql and process the ResultSet
	 * @return T
	 */
	public static <T> T utilize(final String sql, final Utilizer<T> utilizer) {
		if (synced) {
			synchronized (Db.class) {
				return utilizeInner(sql, utilizer);
			}
		}
		return utilizeInner(sql, utilizer);
	}

	private static final <T> T utilizeInner(final String sql, final Utilizer<T> utilizer) {
		Utilizer<T> theUtilizer;
		if (utilizer == null) {
			theUtilizer = new Utilizer<T>();
		} else {
			theUtilizer = utilizer;
		}
		PreparedStatement ps = null;
		ResultSet res = null;
		try {
			ps = DB.get().getConnection().prepareStatement(sql);
			theUtilizer.setParameters(ps);
			if (sql.startsWith("SELECT ")) {
				res = ps.executeQuery();
				while (res.next()) {
					theUtilizer.addResult(res);
				}
			} else {
				ps.execute();
			}
			return theUtilizer.result();
		} catch (SQLException e) {
			throw Warden.spot(new DbException("preparing failed", e));
		} finally {
			try {
				if (res != null) {
					try {
						res.close();
					} catch (SQLException e) {
						throw Warden.spot(new DbException("result closing failed", e));
					}
				}

			} finally {
				if (ps != null) {
					try {
						ps.close();
					} catch (SQLException e) {
						// Warden.disregard(e);
						throw Warden.spot(new DbException("", e));
					}
				}
			}
		}
	}

	public String getUrl() {
		return DEFAULT_DB;
	}

	public static void runAll(final Store store, final String suffix) {
		for (Store file : store) {
			if (store.canRead()) {
				if (suffix == null || file.getName().endsWith(suffix)) {
					Db.executeSql(file);
				}
			}
		}
	}

	/**
	 * Executes the given sql file line by line in the current transaction.
	 *
	 * @param sqlFileSource != null
	 */
	public static void executeSql(final Store sqlFileSource) {
		InputStream sqlIn = null;
		try {
			sqlIn = sqlFileSource.getInputStream();
			LineNumberReader lnr = new LineNumberReader(new InputStreamReader(sqlIn, "UTF-8"));
			String line;
			while ((line = lnr.readLine()) != null) {
				line = line.trim();
				if (line.length() == 0 || line.startsWith("#") || line.startsWith("--")) {
					continue;
				}

				try {
					Db.utilize(line, null);
				} catch (DbException e) { // NOPMD no flow control
					throw Warden.spot(new DbException("sql problem in " + line + " (line " + lnr.getLineNumber() + ").", e));
				}
			}
		} catch (UnsupportedEncodingException e) {
			throw Warden.spot(new CantHappenException("UTF-8 must exist", e));
		} catch (IOException e) {
			throw Warden.spot(new DbException("reading " + sqlFileSource + " failed.", e));
		} finally {
			IOTools.forceClose(sqlIn);
		}
	}

	public static boolean doesTableExist(final String tableName) {
		ResultSet tables = null;
		try {
			DatabaseMetaData dbm = DB.get().getConnection().getMetaData();
			tables = dbm.getTables(null, null, tableName, null);
			return tables.next();
		} catch (SQLException e) {
			throw Warden.spot(new DbException("cannot check presence of " + tableName + " in db meta data", e));
		} finally {
			try {
				if (tables != null) {
					tables.close();
				}
			} catch (SQLException e) {
				Warden.report(e);
			}
		}
	}

	/**
	 * For debugging.
	 *
	 * @param table name is not sanitized!
	 * @return String line-wise rows
	 */
	public static String dumpTableToString(final String table) {
		return Db.transactional(new StrictBlock<String>() {
			@Override
			public String execute() {
				// TODO add caching
				return Db.utilize("SELECT * FROM " + table, new Utilizer<String>() {
					private final StringBuilder result = new StringBuilder();

					@Override
					public void addResult(final ResultSet resultSet) throws SQLException {
						for (int i = 1; i < resultSet.getMetaData().getColumnCount(); i++) {
							this.result.append(resultSet.getMetaData().getColumnName(i));
							this.result.append("=");
							Object object = resultSet.getObject(i);
							// if(resultSet.wasNull()) object = null;
							this.result.append(Elements.toString(object));
							this.result.append(" ");
						}
						this.result.append("\n");
					}

					@Override
					public String result() {
						return this.result.toString();
					}
				});
			}
		});
	}

	/**
	 * Allows to work around limitations of the DB.
	 *
	 * @return true: access to db is synchronized; only one thread at a time may access the db; false: allow multiple threads
	 * (default)
	 */
	public static boolean isSynced() {
		return synced;
	}

	public static void setSynced(final boolean syncedNew) {
		synced = syncedNew;
	}
}
