package de.zarncke.lib.id;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

import org.joda.time.DateTime;

import com.google.common.base.Function;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import de.zarncke.lib.block.Running;
import de.zarncke.lib.block.StrictBlock;
import de.zarncke.lib.cache.MemoryControl;
import de.zarncke.lib.cache.MemoryMonitor;
import de.zarncke.lib.coll.Elements;
import de.zarncke.lib.db.Db;
import de.zarncke.lib.db.Db.DbException;
import de.zarncke.lib.db.Db.Utilizer;
import de.zarncke.lib.err.CantHappenException;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.id.Ids.HasGid;
import de.zarncke.lib.id.Ids.IdInjected;
import de.zarncke.lib.log.Log;
import de.zarncke.lib.region.Region;
import de.zarncke.lib.region.RegionUtil;
import de.zarncke.lib.time.JavaClock;
import de.zarncke.lib.util.ObjectTool;

/**
 * Provides resolving capabilities for objects which can be serialized.
 * Keeps a serialized representation of the object in a database.
 * Supports nice ids by {@link IdProposer}.
 *
 * @author Gunnar Zarncke
 * @param <T> resolved type
 */
public final class SerializingDbResolver<T> implements Function<Gid<T>, T> {

	private static final String CANDIDATE_ID_SEPARATOR = "_";
	private final Class<T> clazz;

	private boolean cached = true;

	private final Cache<T, Gid<? extends T>> cacheIdByGroup = CacheBuilder.newBuilder().softValues().build();

	private final LoadingCache<Gid<? extends T>, T> cacheGroupById = CacheBuilder.newBuilder().softValues().build(
			new CacheLoader<Gid<? extends T>, T>() {
				@Override
				public T load(final Gid<? extends T> key) throws Exception {
					return ObjectTool.nullObjectInsteadOfNull(resolveUncached(key));
				}
			});

	private final MemoryControl idControl = MemoryMonitor.controlOf(this.cacheIdByGroup.asMap());
	private final MemoryControl groupControl = MemoryMonitor.controlOf(this.cacheGroupById.asMap());

	public static <T> SerializingDbResolver<T> of(final Class<T> clazz) {
		return new SerializingDbResolver<T>(clazz);
	}

	private SerializingDbResolver(final Class<T> clazz) {
		this.clazz = clazz;
		MemoryMonitor.CTX.get().register(
				MemoryMonitor.usageOf("Cache for resolver for " + clazz.getSimpleName(), this.cacheGroupById.asMap(), clazz),
				this.groupControl);
		MemoryMonitor.CTX.get().register(
				MemoryMonitor.usageOf("Cache for ids for " + clazz.getSimpleName(), this.cacheIdByGroup.asMap(), Gid.class),
				this.idControl);
		populateCache();
	}

	protected void populateCache() {
		if (!this.cached) {
			return;
		}
		Db.transactional(new Running() {
			@Override
			public void run() {
				Db.utilize("SELECT ID, DATA FROM ID_MAP WHERE TYPE=?", new Utilizer<Void>() {
					@Override
					public void setParameters(final PreparedStatement preparedStatement) throws SQLException {
						preparedStatement.setString(1, SerializingDbResolver.this.clazz.getName());
					}

					int count = 0;

					@Override
					public void addResult(final ResultSet resultSet) throws SQLException {
						String idStr = resultSet.getString(1);
						byte[] bytes = resultSet.getBytes(2);
						try {
							@SuppressWarnings("unchecked" /* we stored it thus */)
							T r = (T) ObjectTool.deserialize(RegionUtil.asRegion(bytes));
							Gid<T> id = Gid.ofUtf8(idStr, SerializingDbResolver.this.clazz);
							injectId(r, id);
							SerializingDbResolver.this.cacheIdByGroup.put(r, id);
							SerializingDbResolver.this.cacheGroupById.put(id, r);
							this.count++;
						} catch (Exception e) {
							// ignore failures, its only a cache anyway
							Warden.disregardAndReport(e);
						}
					}

					@Override
					public Void result() {
						Log.LOG.get().report("cached " + this.count + " for " + SerializingDbResolver.this.clazz);
						return super.result();
					}
				});
			}
		});
	}

	public void registerWith(final Factory factory) {
		factory.register(this.clazz, this);
	}

	@Override
	public T apply(final Gid<T> from) {
		if (from == null) {
			return null;
		}
		if (!from.getType().equals(this.clazz)) {
			throw Warden.spot(new IllegalArgumentException("unexpected type " + from.getType() + " in " + from + " (expected "
					+ this.clazz + ")"));
		}
		if (this.cached) {
			T res;
			try {
				res = ObjectTool.nullInsteadOfNullObject(this.cacheGroupById.get(from));
			} catch (ExecutionException e) {
				throw Warden.spot(new CantHappenException("no runtime expected", e));
			}
			if (res == null) {
				// null values are removed
				this.cacheGroupById.invalidate(from);
			}
			return res;
		}
		return resolveUncached(from);
	}

	/**
	 * Ensure that the given object is remembered. Checks the cache.
	 *
	 * @param object != null
	 * @param proposedId propsal for the id, may not be used if collision; may be null to use auto id
	 * @return the Gid under which it can be retrieved later
	 */
	public Gid<? extends T> ensureRemembered(final T object, final String proposedId) {
		Gid<? extends T> id = this.cacheIdByGroup.getIfPresent(object);

		id = remember(object, proposedId);

		if (this.cached) {
			this.cacheIdByGroup.put(object, id);
			this.cacheGroupById.put(id, object);
		}
		return id;
	}

	/**
	 * Remember the given object.
	 *
	 * @param <T> type of object
	 * @param object != null
	 * @return the Gid under which it can be retrieved later
	 */
	public static <T> Gid<? extends T> remember(final T object) {
		return remember(object, null);
	}

	/**
	 * Remember the given object.
	 *
	 * @param <T> type of object
	 * @param object != null
	 * @param proposedId a proposal for an id of the object. May or may not be used or altered to make it unique, may be null
	 * @return the Gid under which it can be retrieved later
	 */
	public static <T> Gid<? extends T> remember(final T object, final String proposedId) {
		return Db.transactional(new StrictBlock<Gid<? extends T>>() {
			@Override
			public Gid<? extends T> execute() {
				final String sanitizedId = proposedId == null ? null : proposedId.replaceAll("[ +/:;]", "");
				if (object == null) {
					return null;
				}
				if (!(object instanceof Serializable)) {
					throw Warden.spot(new IllegalArgumentException("Cannot remember non-serializable objecte " + object
							+ ", resolving will be impossible "));
				}

				String firstCandidateId = getFirstCandidateId(object, sanitizedId);
				final Class<? extends T> type = getIdType(object);

				Resolver resolver = Resolver.CTX.get();
				if (resolver instanceof Factory && !((Factory) resolver).supports(type)) {
					throw Warden.spot(new IllegalArgumentException("no resolver registered for the type " + type
							+ ", resolving " + object + " will be impossible "));
				}

				Region serialized = ObjectTool.serialize((Serializable) object);
				// xxx injectId here (otherwise may be null; add test)
				final int hash = object.hashCode();
				final byte[] bytes = serialized.toByteArray();
				Result res;
				int cnt = 1;
				String candidateId = firstCandidateId;
				while (true) {
					res = findObjectIdRelationInTable(object, type, hash, bytes, candidateId);
					if (!res.alreadyUsed) {
						break;
					}
					candidateId = sanitizedId + CANDIDATE_ID_SEPARATOR + cnt;
					cnt++;
				}

				if (!res.found) {
					insertObjectIdRelationInTable(type, res.id, hash, bytes);
				}
				Gid<? extends T> id = Gid.ofUtf8(res.id, type);
				injectId(object, id);
				return id;
			}

		});
	}

	static void insertObjectIdRelationInTable(final Class<?> typeOfObject, final String chosenIdForObject,
			final int hashOfObject, final byte[] serializedBytesOfObject) {
		Db.utilize("INSERT INTO ID_MAP (ID, TYPE, DATA, HASH, LAST_ACCESS) VALUES (?,?,?,?,?)", new Utilizer<Void>() {
			@Override
			public void setParameters(final PreparedStatement preparedStatement) throws SQLException {
				int par = 1;
				preparedStatement.setString(par++, chosenIdForObject);
				preparedStatement.setString(par++, typeOfObject.getName());
				preparedStatement.setBytes(par++, serializedBytesOfObject);
				preparedStatement.setInt(par++, hashOfObject);
				long currentTimeMillis = JavaClock.getTheClock().getCurrentTimeMillis();
				preparedStatement.setLong(par++, currentTimeMillis);
			}
		});
	}

	static Result findObjectIdRelationInTable(final Object object, final Class<?> typeOfObject, final int hashOfObject,
			final byte[] serializedBytesOfObject, final String idToTryForObject) {
		Result res;
		res = Db.utilize("SELECT ID, DATA FROM ID_MAP WHERE (TYPE=? AND ID=?) OR (TYPE=? AND HASH=?)", new Utilizer<Result>() {
			private final Result result = new Result(idToTryForObject);

			@Override
			public void setParameters(final PreparedStatement preparedStatement) throws SQLException {
				int par = 1;
				preparedStatement.setString(par++, typeOfObject.getName());
				preparedStatement.setString(par++, idToTryForObject);
				preparedStatement.setString(par++, typeOfObject.getName());
				preparedStatement.setLong(par++, hashOfObject);
			}

			@Override
			public void addResult(final ResultSet resultSet) throws SQLException {
				String resultId = resultSet.getString("ID"); // NOPMD ID result
				byte[] resBytes = resultSet.getBytes("DATA");// NOPMD DATA result
				boolean same = Elements.arrayequals(resBytes, serializedBytesOfObject);
				if (!same) {
					if (object instanceof IdSerializable<?>) {
						same = true; // by definition
					} else {
						Object stored = deserializeRobustly(resBytes);
						if (stored != null) {
							same = stored.equals(object);
						}
					}
				}
				if (same) {
					if (this.result.found) {
						Log.LOG.get().report(
								"found more than one hit for id " + idToTryForObject + " (this indicates a "
										+ "race condition in the db which causes uni-unique ids, this is not "
										+ "critical as ids can still be resolved to objects.)!");
					}
					// we prefer equal ids
					if (resultId.equals(idToTryForObject)) {
						this.result.id = resultId;
					} else {
						// but we use other ids too
						if (this.result.id != null && !this.result.id.equals(idToTryForObject)) {
							this.result.id = resultId;
						}
					}
					this.result.found = true;
				} else {
					if (resultId.equals(idToTryForObject)) {
						// id already used for another object: retry with new id
						this.result.alreadyUsed = true;
					} else {
						// different bytes and different id, but same hash: ignore
					}
				}
			}

			@Override
			public Result result() {
				return this.result;
			}
		});
		return res;
	}

	/**
	 * @param resBytes bytes to deserialize
	 * @return Object or null if deserialization failed.
	 * @throws IllegalStateException if null was deserialized which is unexpected
	 */
	static Object deserializeRobustly(final byte[] resBytes) {
		Object stored = null;
		boolean failed = false;
		try {
			stored = ObjectTool.deserialize(RegionUtil.asRegion(resBytes));
		} catch (Exception e) {
			Warden.disregardAndReport(e);
			failed = true;
		}
		if (failed) {
			return null;
		}
		if (stored == null) {
			throw Warden.spot(new IllegalStateException("stored object cannot be null"));
		}
		return stored;
	}

	static String getFirstCandidateId(final Object object, final String sanitizedId) {
		String firstCandidateId = sanitizedId;
		if (object instanceof HasGid<?>) {
			Gid<?> gid = ((HasGid<?>) object).getId();
			if (firstCandidateId == null) {
				firstCandidateId = gid.toUtf8String();
			}
		} else {
			if (firstCandidateId == null) {
				if (object instanceof IdProposer) {
					firstCandidateId = ((IdProposer) object).getIdProposal();
				} else {
					firstCandidateId = String.valueOf(object.hashCode());
				}
			}
		}
		return firstCandidateId;
	}

	static <T> Class<? extends T> getIdType(final T object) {
		final Class<? extends T> type;
		if (object instanceof HasGid<?>) {
			@SuppressWarnings("unchecked" /* according to contract of HasGid */)
			Gid<? extends T> gid = ((HasGid<? extends T>) object).getId();
			type = gid.getType();
		} else {
			@SuppressWarnings("unchecked" /* according to passed in object */)
			Class<? extends T> ctype = (Class<? extends T>) object.getClass();
			type = ctype;
		}
		return type;
	}

	@SuppressWarnings("unchecked")
	private static <T> void injectId(final T object, final Gid<? extends T> id) {
		if (object instanceof IdInjected<?>) {
			((IdInjected<T>) object).setId(id);
		}
	}

	protected static class Result {
		String id;
		boolean found = false;
		boolean alreadyUsed = false;

		public Result(final String id) {
			this.id = id;
		}
	}

	public static void createSchema() {
		execSqlNoErrors("CREATE TABLE ID_MAP ( ID VARCHAR(256), TYPE VARCHAR(256), DATA VARBINARY(8000), HASH INTEGER, LAST_ACCESS NUMERIC)");
		execSqlNoErrors("CREATE INDEX ID_MAP_IDX_TYPE_ID ON ID_MAP ( TYPE, ID )");
		execSqlNoErrors("CREATE INDEX ID_MAP_IDX_TYPE_HASH ON ID_MAP ( TYPE, HASH )");
	}

	private static void execSqlNoErrors(final String sql) {
		try {
			Db.utilize(sql, null);
		} catch (DbException e) {
			Warden.disregard(e);
			// ignore already existing schema
			// TODO check for exactly this case
		}
	}

	public static void dumpSchema() {
		try {
			Db.utilize("SELECT * FROM ID_MAP", new Utilizer<Void>() {
				@Override
				public void addResult(final ResultSet resultSet) throws SQLException {
					Log.LOG.get().report(
							"ID_MAP " + resultSet.getString("ID") + ":" + resultSet.getString("TYPE") + " "
									+ resultSet.getInt("HASH") + " " + new DateTime(resultSet.getLong("LAST_ACCESS")));
				}
			});
		} catch (DbException e) {
			Warden.disregard(e);
			// ignore already existing schema
			// TODO check for exactly this case
		}
	}

	public Class<T> getType() {
		return this.clazz;
	}

	protected T resolveUncached(final Gid<? extends T> from) {
		return Db.transactional(new StrictBlock<T>() {
			@Override
			public T execute() {
				T res = Db.utilize("SELECT DATA FROM ID_MAP WHERE ID=? AND TYPE=?", new Utilizer<T>() {
					private T result = null;

					@Override
					public void setParameters(final PreparedStatement preparedStatement) throws SQLException {
						preparedStatement.setString(1, from.toUtf8String());
						preparedStatement.setString(2, from.getType().getName());
					}

					@Override
					public void addResult(final ResultSet resultSet) throws SQLException {
						if (this.result != null) {
							Log.LOG.get().report("unexpected: more than one result for " + from);
							return;
						}
						byte[] bytes = resultSet.getBytes(1);
						try {
							@SuppressWarnings("unchecked" /* we stored it thus */)
							T r = (T) ObjectTool.deserialize(RegionUtil.asRegion(bytes));
							injectId(r, from);
							this.result = r;
						} catch (Exception e) {
							throw Warden.spot(new SQLException("for " + from + " cannot deserialize data "
									+ Elements.asList(bytes), e));
						}
					}

					@Override
					public T result() {
						return this.result;
					}
				});
				if (from != null && res != null && SerializingDbResolver.this.cached) {
					SerializingDbResolver.this.cacheIdByGroup.put(res, from);
				}
				return res;
			}
		});
	}

	/**
	 * @return true: results are cached; false: never cache results
	 */
	public boolean isCached() {
		return this.cached;
	}

	public SerializingDbResolver<T> setCached(final boolean cached) {
		this.cached = cached;
		return this;
	}

	/**
	 * @return number of cached entries
	 */
	public int size() {
		return this.cacheGroupById.asMap().size();
	}

}
