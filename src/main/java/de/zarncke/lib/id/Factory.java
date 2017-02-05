package de.zarncke.lib.id;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.log.Log;

/**
 * A {@link Resolver} which uses registered functions to resolve the id.
 * Throws {@link IllegalArgumentException} if type is unknown.
 *
 * @author Gunnar Zarncke
 */
public class Factory implements Resolver, Cloneable {

	// the Maps are never modified. This ensures thread-safety. On changes anew Map is created and set atomically.
	private Map<Class<?>, Function<?, ?>> factories = new HashMap<Class<?>, Function<?, ?>>();
	private Map<Class<?>, Function<?, ?>> listFactories = new HashMap<Class<?>, Function<?, ?>>();

	public Factory() {
		// initially empty
	}

	/**
	 * @return a new Factory with the same bindings but which can be modified independently
	 */
	public Factory copy() {
		try {
			return (Factory) clone();
		} catch (CloneNotSupportedException e) {
			throw Warden.spot(new IllegalStateException(
					"cannot happen except if derived classes do something inconsistent", e));
		}
	}

	/**
	 * Resolves the given id.
	 *
	 * @param <T> type of result
	 * @param id may be null, type must be {@link #supports(Class) supported}
	 * @return matching object or null if id is null
	 */
	public <T> T get(final Gid<T> id) {
		if (id == null) {
			return null;
		}

		@SuppressWarnings("unchecked" /* we know that it has this type because we put it in that way */)
		Function<Gid<?>, ?> function = (Function<Gid<?>, ?>) this.factories.get(id.getType());
		if (function == null) {
			throw Warden.spot(new IllegalArgumentException("unknown id type " + id.getType()));
		}
		@SuppressWarnings("unchecked" /* we know that this type is OK because we put a matching function into the map */)
		T res = (T) function.apply(id);
		return res;
	}

	@Override
	public <T> List<T> get(final Collection<? extends Gid<T>> ids) {
		if (ids == null || ids.isEmpty()) {
			return L.e();
		}
		Gid<T> id0 = ids.iterator().next();
		@SuppressWarnings("unchecked" /* we know that it has this type because we put it in that way */)
		Function<Collection<? extends Gid<T>>, List<T>> function = //
		(Function<Collection<? extends Gid<T>>, List<T>>) this.listFactories.get(id0.getType());
		if (function == null) {
			return getListByInteratingOverElements(ids, this);
		}
		return function.apply(ids);
	}

	public static <S> Resolver forFunction(final Function<Gid<S>, S> func, final Class<S> type) {
		return new Resolver() {
			@SuppressWarnings("unchecked" /* we explicitly check */)
			@Override
			public <T> T get(final Gid<T> id) {
				if (id.getType().equals(type)) {
					return (T) func.apply((Gid<S>) id);
				}
				return null;
			}

			@Override
			public <T> List<T> get(final Collection<? extends Gid<T>> ids) {
				return getListByInteratingOverElements(ids, this);
			}
		};
	}

	public static <T> List<T> getListByInteratingOverElements(final Collection<? extends Gid<T>> ids,
			final Resolver resolverUsedToResolveSingleIds) {
		// TODO introduce block handling functions
		List<T> l = L.n(ids.size());
		for (Gid<T> id : ids) {
			l.add(resolverUsedToResolveSingleIds.get(id));
		}
		return l;
	}

	/**
	 * Registers a Function to resolve IDs by (replaces existing ones for this type).
	 *
	 * @param <T> type resolved
	 * @param type != null
	 * @param factory to use
	 */
	public <T> void register(final Class<T> type, final Function<Gid<T>, T> factory) {
		addByCopy(type, factory);
	}

	/**
	 * Registers a Function to resolve lists of IDs by (replaces existing ones for this type).
	 *
	 * @param <T> type resolved
	 * @param type != null
	 * @param factory to use
	 */
	public <T> void registerForList(final Class<T> type, final Function<Collection<? extends Gid<T>>, List<T>> factory) {
		addListByCopy(type, factory);
	}

	private <T> void addByCopy(final Class<T> type, final Function<Gid<T>, T> factory) {
		Map<Class<?>, Function<?, ?>> copy = new HashMap<Class<?>, Function<?, ?>>(this.factories);
		copy.put(type, factory);
		this.factories = copy;
	}

	private <T> void addListByCopy(final Class<T> type, final Function<Collection<? extends Gid<T>>, List<T>> factory) {
		Map<Class<?>, Function<?, ?>> copy = new HashMap<Class<?>, Function<?, ?>>(this.listFactories);
		copy.put(type, factory);
		this.listFactories = copy;
	}

	/**
	 * Adds all Functions from the given factory (using it before the previous function is used).
	 *
	 * @param factory to use
	 */
	public void extend(final Factory factory) {
		extend(factory, null);
	}

	/**
	 * Adds all Functions from the given factory (using it before the previous function is used).
	 *
	 * @param factory to use
	 * @param errorLogger where to log errors when the new factory failed and the original one is used (null means
	 * normal)
	 */
	@SuppressWarnings({ "unchecked"/* we put it in thus */, "rawtypes" })
	public void extend(final Factory factory, final Log errorLogger) {
		for (Map.Entry<Class<?>, Function<?, ?>> me : factory.factories.entrySet()) {
			extend((Class) me.getKey(), (Function) me.getValue(), errorLogger);
		}
		for (Map.Entry<Class<?>, Function<?, ?>> me : factory.listFactories.entrySet()) {
			extendList((Class) me.getKey(), (Function) me.getValue(), errorLogger);
		}
	}

	/**
	 * Adds a Function to resolve IDs by (using it before the previous function is used).
	 *
	 * @param <T> type resolved
	 * @param type != null
	 * @param factory to use
	 */
	public synchronized <T> void extend(final Class<T> type, final Function<Gid<T>, T> factory) {
		extend(type, factory, null);
	}

	public synchronized <T> void extendList(final Class<T> type,
			final Function<Collection<? extends Gid<T>>, List<T>> factory) {
		extendList(type, factory, null);
	}

	/**
	 * Adds a Function to resolve IDs by (using it before the previous function is used).
	 *
	 * @param <T> type resolved
	 * @param type != null
	 * @param factory to use
	 * @param errorLogger where to log errors when the new factory failed and the original one is used (null means
	 * normal)
	 */
	public synchronized <T> void extend(final Class<T> type, final Function<Gid<T>, T> factory, final Log errorLogger) {
		@SuppressWarnings("unchecked"/* we put it in thus */)
		final Function<Gid<T>, T> oldFactory = (Function<Gid<T>, T>) this.factories.get(type);
		if (oldFactory == null) {
			addByCopy(type, factory);
		} else {
			addByCopy(type, new Function<Gid<T>, T>() {
				public T apply(final Gid<T> from) {
					T o;
					try {
						o = factory.apply(from);
					} catch (Exception e) {
						if (errorLogger != null) {
							Warden.disregard(e);
							errorLogger.report(e);
						} else {
							Warden.disregardAndReport(e);
						}
						o = null;
					}
					return o != null ? o : oldFactory.apply(from);
				}
			});
		}
	}

	/**
	 * Adds a Function to resolve IDs by (using it before the previous function is used).
	 *
	 * @param <T> type resolved
	 * @param type != null
	 * @param factory to use
	 * @param errorLogger where to log errors when the new factory failed and the original one is used (null means
	 * normal)
	 */
	public synchronized <T> void extendList(final Class<T> type,
			final Function<Collection<? extends Gid<T>>, List<T>> factory, final Log errorLogger) {
		@SuppressWarnings("unchecked"/* we put it in thus */)
		final Function<Collection<? extends Gid<T>>, List<T>> oldFactory = //
		(Function<Collection<? extends Gid<T>>, List<T>>) this.listFactories.get(type);
		if (oldFactory == null) {
			addListByCopy(type, factory);
		} else {
			addListByCopy(type, new Function<Collection<? extends Gid<T>>, List<T>>() {
				public List<T> apply(final Collection<? extends Gid<T>> from) {
					List<T> o;
					try {
						o = factory.apply(from);
					} catch (Exception e) {
						if (errorLogger != null) {
							Warden.disregard(e);
							errorLogger.report(e);
						} else {
							Warden.disregardAndReport(e);
						}
						o = null;
					}
					return o != null ? o : oldFactory.apply(from);
				}
			});
		}
	}

	/**
	 * @param type != null
	 * @return true: ids of given type can be mapped
	 */
	public boolean supports(final Class<?> type) {
		return this.factories.containsKey(type) || this.listFactories.containsKey(type);
	}

	@Override
	public String toString() {
		return this.factories.toString();
	}
}
