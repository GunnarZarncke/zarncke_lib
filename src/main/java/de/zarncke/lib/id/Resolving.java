package de.zarncke.lib.id;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.base.Function;

import de.zarncke.lib.coll.Elements;
import de.zarncke.lib.coll.L;
import de.zarncke.lib.ctx.Context;
import de.zarncke.lib.err.CantHappenException;
import de.zarncke.lib.err.NotImplementedException;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.id.Ids.HasGid;
import de.zarncke.lib.id.Ids.HasNid;
import de.zarncke.lib.id.Ids.HasSomeGid;
import de.zarncke.lib.log.Log;
import de.zarncke.lib.value.BiTypeMapping;
import de.zarncke.lib.value.IllegalExternalException;
import de.zarncke.lib.value.TypeNameMapping;

/**
 * Misc functionality to resolve Object to an Id and back again.
 *
 * @author Gunnar Zarncke
 */
public final class Resolving {

	public static final String TYPE_ID_TYPE = "type";
	public static final String TYPE_ID_LIST = "list";
	public static final String TYPE_ID_INT = "i";
	public static final String TYPE_ID_LONG = "l";
	public static final String TYPE_ID_STRING = "s";
	public static final int MAX_EXTERNAL_ID_LENGTH = 0xfe - '0';
	public static final String EXTERNAL_VALUE_SEPARATOR = ":";
	public static final int SIZE_OFFSET_TO_MOVE_INTO_READABLE_RANGE = '0';

	private Resolving() {
		// helper
	}

	// unchecked because we do not know the Collections element type at any time but assume that the ListGid type matches it
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static final class CollectionResolver implements Function<Gid<Collection>, Collection> {
		@Override
		public Collection apply(final Gid<Collection> from) {
			if (!(from instanceof ListGid)) {
				throw Warden.spot(new IllegalArgumentException("this function can handle ListGids only " + from.getClass()));
			}
			ListGid lgid = (ListGid) from;
			Collection coll = new ArrayList();
			byte[] ba = lgid.getIdAsBytes();
			int i = 0;
			while (i < ba.length) {
				try {
					int l = ba[i++] - SIZE_OFFSET_TO_MOVE_INTO_READABLE_RANGE;
					if (l == -1) {
						// handle special null marker
						coll.add(null);
						continue;
					}
					if (l > ba.length + i || l < 0) {
						throw Warden.spot(new IllegalArgumentException("invalid id " + lgid + " at index " + i));
					}
					byte[] idBytes = new byte[l];
					System.arraycopy(ba, i, idBytes, 0, l);
					i += l;

					if (Collection.class.isAssignableFrom(lgid.getElementType())) {
						throw Warden.spot(new NotImplementedException("cannot handle Collections of Collections yet." + lgid));
					}
					Gid elementId = Gid.of(idBytes, lgid.getElementType());
					Object element = resolve(elementId);
					if (element != null) {
						coll.add(element);
					} else {
						Log.LOG.get().report(
								"resolving non-null id " + elementId + " to null - value is ignored from collection.");
					}
				} catch (IndexOutOfBoundsException e) {
					throw Warden.spot(new IllegalArgumentException("invalid byte array " + Elements.toString(ba) + " after "
							+ i, e));
				}
			}
			return coll;
		}
	}

	@SuppressWarnings("rawtypes" /* see above */)
	public static final Function<Gid<Collection>, Collection> COLLECTION_RESOLVER = new CollectionResolver();
	public static final Function<Gid<Long>, Long> LONG_RESOLVER = new Function<Gid<Long>, Long>() {
		@Override
		public Long apply(final Gid<Long> from) {
			return from.toLong();
		}
	};
	public static final Function<Gid<Integer>, Integer> INTEGER_RESOLVER = new Function<Gid<Integer>, Integer>() {
		@Override
		public Integer apply(final Gid<Integer> from) {
			return from.toInteger();
		}
	};
	public static final Function<Gid<String>, String> STRING_RESOLVER = new Function<Gid<String>, String>() {
		@Override
		public String apply(final Gid<String> from) {
			return from.toUtf8String();
		}
	};

	@SuppressWarnings("rawtypes"/* see above */)
	public static final Function<Gid<Class>, Class> CLASS_RESOLVER = new Function<Gid<Class>, Class>() {
		@Override
		public Class apply(final Gid<Class> from) {
			String name = from.toUtf8String();
			return getTypeForName(from, name);
		}

	};

	/**
	 * Assign default type mappings.
	 */
	public static void assignType() {
		BiTypeMapping mapping = (BiTypeMapping) TypeNameMapping.CTX.get();
		mapping.assign(String.class, TYPE_ID_STRING);
		mapping.assign(Long.class, TYPE_ID_LONG);
		mapping.assign(Integer.class, TYPE_ID_INT);
		mapping.assign(Collection.class, TYPE_ID_LIST);
		mapping.assign(Class.class, TYPE_ID_TYPE);
	}

	/**
	 * Registers all resolvers supported by this class on the given Factory.
	 * You can get the default Factory with <code>(Factory)Resolver.CTX.get()</code> or create your own and set it in a
	 * {@link Context}.
	 *
	 * @param factory != null
	 */
	public static void registerResolvers(final Factory factory) {
		factory.register(String.class, STRING_RESOLVER);
		factory.register(Long.class, LONG_RESOLVER);
		factory.register(Integer.class, INTEGER_RESOLVER);
		factory.register(Collection.class, COLLECTION_RESOLVER);
		factory.register(Class.class, CLASS_RESOLVER);
	}

	public static <T> T resolve(final Gid<? extends T> id) {
		return Resolver.CTX.get().get(id);
	}

	/**
	 * Converts any {@link Gid} into an external form which can be read back into an id.
	 * Precondition: The type of the id is converted with {@link #getId(Object)} which must have a resolver registered which
	 * returns ids without <code>{@value #EXTERNAL_VALUE_SEPARATOR}</code>!
	 *
	 * @param id any
	 * @return null if id is null, any UTF-8 string otherwise
	 */
	public static String toExternalForm(final Gid<?> id) {
		return id == null ? null : getId(id.getType()).toUtf8String() + EXTERNAL_VALUE_SEPARATOR + id.toUtf8String();
	}

	/*
	 * See #fromExternalForm() but type must match exactly
	 */
	@SuppressWarnings("unchecked")
	public static <T> Gid<T> fromExternalFormStrict(final String externalForm, final Class<T> expectedBaseClass) {
		Gid<? extends T> id = fromExternalForm(externalForm, expectedBaseClass);
		if (id != null && !id.getType().equals(expectedBaseClass)) {
			throw Warden.spot(new IllegalExternalException(externalForm, "has wrong type " + id.getType() + " (expected "
					+ expectedBaseClass + ")"));
		}
		return (Gid<T>) id;
	}

	/**
	 * Converts the external form of a {@link Gid} back to the type-safe gid.
	 *
	 * @param <T> expected type of the Gid
	 * @param externalForm any which was created from
	 * @param expectedBaseClass != null
	 * @return null if externalForm is null, Gid of the type otherwise
	 * @throws IllegalExternalException if the value is malformed or not of the expected clas
	 */
	public static <T> Gid<? extends T> fromExternalForm(final String externalForm, final Class<T> expectedBaseClass) {
		if (externalForm == null) {
			return null;
		}
		int p = externalForm.indexOf(EXTERNAL_VALUE_SEPARATOR);
		if (p < 0) {
			throw Warden.spot(new IllegalExternalException(externalForm, "no '" + EXTERNAL_VALUE_SEPARATOR + "'."));
		}

		String typeId = externalForm.substring(0, p);
		Class<?> type;
		try {
			type = resolve(Gid.ofUtf8(typeId, Class.class));
		} catch (IllegalArgumentException e) {
			throw Warden.spot(new IllegalExternalException(externalForm, "has unresolvable type " + typeId + ".", e));
		}

		if (expectedBaseClass != null && !expectedBaseClass.isAssignableFrom(type)) {
			throw Warden.spot(new IllegalExternalException(externalForm, "has wrong type " + type + " (expected "
					+ expectedBaseClass + ")"));
		}

		// we just checked
		return unsafeGid(type, externalForm.substring(p + 1));
	}

	private static <T> Gid<T> unsafeGid(final Class<?> type, final String id) {
		return (Gid<T>) Gid.ofUtf8(id, type);
	}

	/**
	 * Tries hard to get an {@link Gid} suitable to resolve the object by.
	 * The following cases are supported:
	 * <ul>
	 * <li>null</li>
	 * <li>String</li>
	 * <li>Long</li>
	 * <li>HasGid</li>
	 * <li>HasNid - a corresponding Gid is created</li>
	 * <li>HasId - a Gid with the type of the actual object is created</li>
	 * <li>Collection - a ListGid with an element type derived from the first element is created. Empty lists are not supported
	 * cleanly typed.</li>
	 * </ul>
	 *
	 * @param <T> type of object
	 * @param object see above
	 * @return Gid or null if object == null
	 */
	@SuppressWarnings("unchecked")
	public static <T> Gid<? extends T> getId(final T object) {
		// note: lots of unchecked warnings but I hope I know what I do
		if (object == null) {
			return null;
		}
		if (object instanceof String) {
			return (Gid<T>) Gid.ofUtf8((String) object, String.class);
		}
		if (object instanceof Integer) {
			return (Gid<T>) Gid.of(((Number) object).intValue(), Integer.class);
		}
		if (object instanceof Long) {
			return (Gid<T>) Gid.of(((Number) object).longValue(), Long.class);
		}
		if (object instanceof Class<?>) {
			return getIdForClass(object);
		}
		if (object instanceof HasGid<?>) {
			return ((HasSomeGid<T>) object).getId();
		}
		if (object instanceof HasNid<?>) {
			Nid<T> nid = ((HasNid<T>) object).getId();
			return Gid.of(nid.getId(), nid.getType());
		}
		if (object instanceof HasId) {
			Id nid = ((HasId) object).getId();
			return (Gid<? extends T>) Gid.ofUtf8(nid.toUtf8String(), object.getClass());
		}
		if (object instanceof Collection<?>) {
			return getIdForCollection((Collection<?>) object);
		}

		throw Warden.spot(new IllegalArgumentException("cannot handle objects of type" + object.getClass()));
	}

	private static <T> Gid<? extends T> getIdForClass(final T object) {
		Class<?> type = (Class<?>) object;
		String name = TypeNameMapping.CTX.get().getNameForType(type);
		if (name == null) {
			name = type.getName();
		}
		return (Gid<? extends T>) Gid.ofUtf8(name, Class.class);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" /* we tell the compiler to use the intended type which we */})
	public static <T> Gid<T> getIdForCollection(final Collection<?> list) {
		if (list.isEmpty()) {
			return new ListGid(Elements.NO_BYTES, Object.class);
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Class<?> type = null;
		for (Object o : list) {
			if (o == null) {
				// write special -1 marker for null
				baos.write(-1 + SIZE_OFFSET_TO_MOVE_INTO_READABLE_RANGE);
				continue;
			}
			Gid<? extends Object> id = getId(o);
			if (id == null) {
				throw Warden.spot(new IllegalArgumentException("Collection " + list + " contains object " + o + " with null id"));
			}
			byte[] oid = id.getIdAsBytes();
			int lengthByte = toLengthByte(oid.length);
			baos.write(lengthByte);
			try {
				baos.write(oid);
			} catch (IOException e) {
				throw Warden.spot(new CantHappenException("cannot happen on small in-mem arrays", e));
			}
			type = o.getClass();
		}
		return new ListGid(baos.toByteArray(), type);
	}

	static byte toLengthByte(final int length) {
		if (length > MAX_EXTERNAL_ID_LENGTH - SIZE_OFFSET_TO_MOVE_INTO_READABLE_RANGE) {
			throw Warden.spot(new IllegalArgumentException("length of id " + length + " out of range"));
		}
		int lengthByte = length + SIZE_OFFSET_TO_MOVE_INTO_READABLE_RANGE;
		if (lengthByte == 127) {
			// 127 is a control character which is not printable/handlable everywhere, replace by '.'
			lengthByte = -2 + SIZE_OFFSET_TO_MOVE_INTO_READABLE_RANGE;
		}
		return (byte) lengthByte;
	}

	/**
	 * Maps over {@link Gid#ofUtf8(String, Class)}.
	 * Doesn't use {@link #toExternalForm(Gid) external form}!
	 *
	 * @param idStrings literal IDs
	 * @param type their type.
	 * @return List of Gids of that type
	 */
	public static <T> List<Gid<T>> getIdsForUtf8String(final Collection<String> idStrings, final Class<T> type) {
		List<Gid<T>> list = L.l();
		for (String idStr : idStrings) {
			list.add(Gid.ofUtf8(idStr, type));
		}
		return list;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T> Gid<Collection<T>> getIdForIds(final Collection<Gid<T>> list) {
		if (list == null) {
			return null;
		}
		if (list.isEmpty()) {
			return new ListGid(Elements.NO_BYTES, Object.class);
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Class<?> type = null;
		for (Gid<?> id : list) {
			if (id == null) {
				// write special -1 marker for null
				baos.write(-1 + SIZE_OFFSET_TO_MOVE_INTO_READABLE_RANGE);
				continue;
			}
			byte[] oid = id.getIdAsBytes();
			baos.write(toLengthByte(oid.length));
			try {
				baos.write(oid);
			} catch (IOException e) {
				throw Warden.spot(new CantHappenException("cannot happen on small in-mem arrays", e));
			}
			type = id.getType();
		}
		return new ListGid(baos.toByteArray(), type);
	}

	/**
	 * Can deal with any IDs, but does no block lookup.
	 *
	 * @param <T>
	 * @param ids
	 * @param skipResolvedNulls
	 * @return List of T
	 */
	public static <T> List<T> resolveMixed(final Collection<? extends Gid<? extends T>> ids, final boolean skipResolvedNulls) {
		List<T> res = new ArrayList<T>(ids.size());
		for (Gid<? extends T> id : ids) {
			T o = resolve(id);
			if (o != null || !skipResolvedNulls) {
				res.add(o);
			}
		}
		return res;
	}

	public static <T> List<T> resolve(final Collection<? extends Gid<T>> ids, final boolean skipResolvedNulls) {
		List<T> res = Resolver.CTX.get().get(ids);
		if (!skipResolvedNulls) {
			return res;
		}
		List<T> copy = L.n(res.size());
		for (T t : res) {
			if (t != null) {
				copy.add(t);
			}
		}
		return copy;
	}

	/**
	 * Converts lists of anything to Lists of {@link Gid}.
	 * Uses {@link Resolving#getId(Object)}.
	 * Caution: May remember objects and be slow.
	 *
	 * @param <T> type of elements.
	 * @param objects != null Collection of elements, may contain nulls and be empty
	 * @return List of Gids extending from T (null ids for null elements)
	 */
	public static <T> List<Gid<? extends T>> toIds(final Collection<T> objects) {
		List<Gid<? extends T>> res = new ArrayList<Gid<? extends T>>(objects.size());
		for (T o : objects) {
			res.add(o == null ? null : getId(o));
		}
		return res;
	}

	/**
	 * Converts Lists of {@link HasGid objects with an id} to Lists of precise {@link Gid Gids}.
	 *
	 * @param <T> type of elements.
	 * @param objects != null Collection of {@link HasGid}, may contain nulls and be empty
	 * @return List of Gids extending from T (null ids for null elements)
	 */
	public static <T extends HasGid<T>> List<Gid<T>> toPreciseIds(final Collection<T> objects) {
		List<Gid<T>> res = new ArrayList<Gid<T>>(objects.size());
		for (T o : objects) {
			res.add(o == null ? null : o.getId());
		}
		return res;
	}

	/**
	 * Converts Lists of {@link HasSomeGid objects with some id} to Lists of {@link Gid Gids}.
	 *
	 * @param <T> type of elements.
	 * @param objects != null Collection of {@link HasSomeGid}, may contain nulls and be empty
	 * @return List of Gids extending from T (null ids for null elements)
	 */
	public static <T extends HasSomeGid<T>> List<Gid<? extends T>> toDirectIds(final Collection<? extends T> objects) {
		List<Gid<? extends T>> res = new ArrayList<Gid<? extends T>>(objects.size());
		for (T o : objects) {
			res.add(o == null ? null : o.getId());
		}
		return res;
	}

	protected static Class<?> getTypeForName(@SuppressWarnings("rawtypes") final Gid<Class> from, final String name) {
		Class<?> type = TypeNameMapping.CTX.get().getTypeForName(name);
		if (type == null) {
			try {
				type = Class.forName(name);
			} catch (ClassNotFoundException e) {
				throw Warden.spot(new IllegalArgumentException("cannot load class " + name + " for which the id " + from
						+ " was created.", e));
			}
		}
		return type;
	}

}
