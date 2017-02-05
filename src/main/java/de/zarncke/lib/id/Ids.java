package de.zarncke.lib.id;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import com.google.common.base.Function;

import de.zarncke.lib.coll.Elements;
import de.zarncke.lib.coll.L;
import de.zarncke.lib.err.CantHappenException;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.util.Misc;

/**
 * Support for constructing normal or composite Ids.
 *
 * @author Gunnar Zarncke
 */
public final class Ids {
	private static final int VALUES_PER_BYTE = 128;
	private static final int MAX_VALID_ID_LENGTH = 128 * 128;
	private static final int MAX_SHORT_ID_LENGTH = 128;

	public interface HasIds {
		Collection<? extends Gid<?>> getIds();
	}

	/**
	 * @param <T> should always be the actual type of the object implementing this interface.
	 */
	public interface HasNid<T> extends HasId {
		@Override
		Nid<T> getId();
	}

	/**
	 * Only specifies the base type of the id.
	 * Useful for objects whose derived classes may have ids of different types.
	 *
	 * @param <T> the base type of the object implementing this interface.
	 */
	public interface HasSomeGid<T> extends HasId {
		@Override
		Gid<? extends T> getId();
	}

	/**
	 * Specifies the type of the id strictly such that it can be used to resolve the object type-safely.
	 *
	 * @param <T> should always be the actual type of the object implementing this interface.
	 */
	public interface HasGid<T> extends HasSomeGid<T> {
		/**
		 * Ensure more precise return type.
		 * See {@link HasSomeGid#getId()}.
		 */
		@Override
		Gid<T> getId();
	}

	/**
	 * Callback for objects which get their Id injected at some time after their construction. Example:
	 * {@link SerializingDbResolver}.
	 * Note: Consequently they must implement {@link #hashCode()} and {@link #equals(Object)} on their data, not on the
	 * id.
	 *
	 * @param <T> type of Gid, must match type of Has(Some)Gid on same class if present.
	 */
	public interface IdInjected<T> {
		void setId(Gid<? extends T> id);
	}

	public static final Function<HasId, String> TO_ID_UTF8 = new Function<HasId, String>() {
		@Override
		public String apply(@Nullable final HasId from) {
			return from.getId().toUtf8String();
		}
	};

	private Ids() {
		// helper
	}

	public static <T> List<Gid<T>> ofUtf8(final Collection<String> ids, final Class<T> type) {
		List<Gid<T>> typedIds = L.n(ids.size());
		for (String id : ids) {
			typedIds.add(Gid.ofUtf8(id, type));
		}
		return typedIds;
	}

	public static <T> Gid<T> of(final HasGid<T> withId) {
		return withId == null ? null : withId.getId();
	}

	public static <T> Nid<T> of(final HasNid<T> withId) {
		return withId == null ? null : withId.getId();
	}

	public static Id of(final HasId withId) {
		return withId == null ? null : withId.getId();
	}

	/**
	 * Creates a byte array for a {@link Gid} by concatenating the given byte arrays of part Gids.
	 * The byte array contains sufficient information (length bytes) to recover the original parts with the help of the
	 * {@link #getPartId(Gid, int, Class)} method.
	 *
	 * @param idAsBytes array of byte array.
	 * @return byte array != null
	 */
	public static byte[] appendIds(final byte[]... idAsBytes) {
		int total = 0;
		for (byte[] ba : idAsBytes) {
			total += ba.length;
		}
		if (total > 120 - '0') {
			StringBuilder sb = new StringBuilder();
			try {
				for (byte[] ba : idAsBytes) {
					sb.append(URLEncoder.encode(new String(ba, Misc.UTF_8), "UTF-8")).append("&");
				}
			} catch (UnsupportedEncodingException e) {
				throw Warden.spot(new CantHappenException("UTF-8 must be present", e));
			}
			return sb.toString().getBytes(Misc.UTF_8);
		}

		int l = 0;
		for (byte[] ba : idAsBytes) {
			if (ba.length >= MAX_VALID_ID_LENGTH) {
				throw Warden.spot(new IllegalArgumentException("max id length " + MAX_VALID_ID_LENGTH + " exceeded at "
						+ l + " in " + Elements.toString(idAsBytes)));
			}
			if (ba.length >= MAX_SHORT_ID_LENGTH) {
				l += 3;
			} else {
				l++;
			}
			l += ba.length;
		}
		byte[] resId = new byte[l];
		int p = l - 1;
		for (byte[] ba : idAsBytes) {
			if (ba.length > MAX_SHORT_ID_LENGTH) {
				resId[p--] = (byte) (-1 + Resolving.SIZE_OFFSET_TO_MOVE_INTO_READABLE_RANGE);
				resId[p--] = Resolving.toLengthByte(ba.length / VALUES_PER_BYTE);
				resId[p] = Resolving.toLengthByte(ba.length % VALUES_PER_BYTE);

			} else {
				resId[p] = Resolving.toLengthByte(ba.length);
			}
			p -= ba.length;
			System.arraycopy(ba, 0, resId, p, ba.length);
			p--;
		}
		return resId;
	}

	/**
	 * Extracts the <code>pos</code>th {@link Gid} part from the given full Gid.
	 * The byte array of the given Gid must have been created with {@link #makeId(Class, Gid...)} (or more low level
	 * with {@link #appendIds}).
	 *
	 * @param <T> result Gid type
	 * @param fullId to extract from
	 * @param pos element - must be present in the byte array
	 * @param type of the expected result
	 * @return Gid (null not supported)
	 * @throws IllegalArgumentException if pos or byte array is invalid
	 */
	public static <T> Gid<T> getPartId(final Gid<?> fullId, final int pos, final Class<T> type) {
		byte[] id = fullId.getIdAsBytes();

		if (id.length > 120 - '0' && id[id.length - 1] == '&') {
			// assume work-around for size limitation
			String idstr = new String(id, Misc.UTF_8);
			String[] parts = idstr.split(Pattern.quote("&"), pos + 2);
			if (pos >= parts.length) {
				throw Warden.spot(new IllegalArgumentException("invalid id, " + pos + " field of " + fullId
						+ " out of bounds"));
			}
			try {
				return Gid.of(URLDecoder.decode(parts[pos], "UTF-8").getBytes(Misc.UTF_8), type);
			} catch (UnsupportedEncodingException e) {
				throw Warden.spot(new CantHappenException("UTF-8 must be present", e));
			}
		}

		int p = id.length - 1;
		for (int i = 0; i < pos; i++) {
			int skip = id[p] - Resolving.SIZE_OFFSET_TO_MOVE_INTO_READABLE_RANGE;
			if (skip == -2) {
				skip = 127 - Resolving.SIZE_OFFSET_TO_MOVE_INTO_READABLE_RANGE; // special control character case
			} else if (skip == -1) {
				int high = id[--p] - Resolving.SIZE_OFFSET_TO_MOVE_INTO_READABLE_RANGE;
				if (high == -2) {
					high = 127 - Resolving.SIZE_OFFSET_TO_MOVE_INTO_READABLE_RANGE;
				}
				int low = id[--p] - Resolving.SIZE_OFFSET_TO_MOVE_INTO_READABLE_RANGE;
				if (low == -2) {
					low = 127 - Resolving.SIZE_OFFSET_TO_MOVE_INTO_READABLE_RANGE;
				}
				skip = low + high * VALUES_PER_BYTE;
			} else if (skip < 0) {
				skip = skip & 0xff;
				// throw Warden.spot(new IllegalArgumentException("invalid id, skipped " + pos + " fields of " +
				// fullId));
			}
			p -= skip;
			p--;
			if (p < 0) {
				throw Warden.spot(new IllegalArgumentException("invalid id, skipped " + pos + " fields of " + fullId
						+ " out of bounds"));
			}
		}
		int skip = id[p] - Resolving.SIZE_OFFSET_TO_MOVE_INTO_READABLE_RANGE;
		if (skip == -2) {
			skip = 127 - Resolving.SIZE_OFFSET_TO_MOVE_INTO_READABLE_RANGE;
		} else if (skip == -1) {
			int high = id[--p] - Resolving.SIZE_OFFSET_TO_MOVE_INTO_READABLE_RANGE;
			if (high == -2) {
				high = 127 - Resolving.SIZE_OFFSET_TO_MOVE_INTO_READABLE_RANGE;
			}
			int low = id[--p] - Resolving.SIZE_OFFSET_TO_MOVE_INTO_READABLE_RANGE;
			if (low == -2) {
				low = 127 - Resolving.SIZE_OFFSET_TO_MOVE_INTO_READABLE_RANGE;
			}
			skip = low + high * VALUES_PER_BYTE;
		} else if (skip < 0) {
			skip = skip & 0xff;
			// throw Warden.spot(new IllegalArgumentException("invalid id, skipped " + pos + " fields of " + fullId));
		}
		byte[] rid = new byte[skip];
		System.arraycopy(id, p - skip, rid, 0, skip);
		return Gid.of(rid, type);
	}

	public static <T> Gid<T> makeId(final Class<T> type, final Gid<?>... ids) {
		byte[][] idBytes = new byte[ids.length][];
		int i = 0;
		for (Gid<?> id : ids) {
			idBytes[i++] = id.getIdAsBytes();
		}
		return Gid.of(appendIds(idBytes), type);
	}

	public static final ObjectMapper MAPPER;
	static {
		MAPPER = new ObjectMapper();
		AnnotationIntrospector primary = new JaxbAnnotationIntrospector(TypeFactory.defaultInstance());
		AnnotationIntrospector secondary = new JacksonAnnotationIntrospector();
		AnnotationIntrospector pair = new AnnotationIntrospectorPair(primary, secondary);
		MAPPER.setAnnotationIntrospector(pair);
		MAPPER.setSerializationInclusion(Include.NON_NULL);
	}

	/**
	 * Creates a ID for the given object which is suitable to recover the object from it.
	 * The ID is a json-serialized form of the object.
	 * Use this method only if the ids a sufficiently short!
	 * Note: for json serialization the serialized (annotated) fields must be non-transient).
	 *
	 * @param <T> type of object
	 * @param object != null
	 * @param type type of object
	 * @return Gid of the given type
	 */
	public static <T> Gid<T> makeJsonIdFor(final T object, final Class<T> type) {
		StringWriter sw = new StringWriter();
		try {
			MAPPER.writeValue(sw, object);
		} catch (JsonGenerationException e) {
			throw Warden.spot(new IllegalArgumentException(object + " failed to marshal", e));
		} catch (JsonMappingException e) {
			throw Warden.spot(new IllegalArgumentException(type + " must have json/jaxb annotations", e));
		} catch (IOException e) {
			throw Warden.spot(new CantHappenException("we don't expect StringWriter to fail", e));
		}
		return Gid.ofUtf8(sw.toString(), type);
	}

	public static <T> void registerJsonResolverFor(final Class<T> type, final Factory factory) {
		factory.register(type, new Function<Gid<T>, T>() {
			@Override
			@SuppressWarnings("unchecked")
			public T apply(final Gid<T> from) {
				try {
					T res = MAPPER.readValue(new StringReader(from.toUtf8String()), from.getType());
					if (res instanceof IdInjected<?>) {
						((IdInjected<T>) res).setId(from);
					}
					return res;
				} catch (JsonParseException e) {
					throw Warden.spot(new IllegalArgumentException(from + " failed to parse", e));
				} catch (JsonMappingException e) {
					throw Warden.spot(new IllegalArgumentException(type + " must have json/jaxb annotations", e));
				} catch (IOException e) {
					throw Warden.spot(new CantHappenException("we don't expect StringReader to fail", e));
				}
			}
		});
	}

	public static <T extends HasGid<T>> Map<Gid<T>, T> index(final Collection<? extends T> elements) {
		Map<Gid<T>, T> map = new HashMap<Gid<T>, T>();
		for (T elem : elements) {
			map.put(elem.getId(), elem);
		}
		return map;
	}
}
