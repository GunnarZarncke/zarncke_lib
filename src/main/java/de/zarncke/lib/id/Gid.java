package de.zarncke.lib.id;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.codec.binary.Base64;

import de.zarncke.lib.coll.Elements;
import de.zarncke.lib.err.CantHappenException;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.id.Ids.HasGid;
import de.zarncke.lib.value.Type;

/**
 * General id type. Based on byte array.
 * Implements {@link Comparable}. Compare with respect to {@link #toHexString()}.
 *
 * @author Gunnar Zarncke
 * @param <T> type of id
 */
@XmlJavaTypeAdapter(value = GidXmlAdapter.class)
public class Gid<T> extends Type<T> implements HasGid<T>, Id, Comparable<Gid<T>> {
	private static final int HASH_PRIME = 37;
	private static final int BASE_16 = 16;
	private static final long serialVersionUID = 1L;
	public static final String ID_ENCODING = "UTF-8";

	/**
	 * Creates a Gid for a byte sequence specified in hex values e.g. "1f24" for the bytes [31, 36}. Uses big endian. Leading
	 * zeros are dropped.
	 * This is also the format returned by {@link #toHexString()}.
	 *
	 * @param <T> type of Gid
	 * @param idHex null or only letters 0-9 and a-z are allowed.
	 * @param clazz scope of id
	 * @return Gid null if idHex is null
	 */
	public static <T> Gid<T> ofHex(final String idHex, final Class<T> clazz) {
		return idHex == null ? null : idHex.length() == 0 ? of(Elements.NO_BYTES, clazz) : of(
				new BigInteger(idHex, BASE_16).toByteArray(), clazz);
	}

	/**
	 * Creates a Gid from a String. Uses encoding {@value #ID_ENCODING}.
	 *
	 * @param <T> type of Gid
	 * @param idUtf8 may be null
	 * @param clazz scope of id
	 * @return Gid null if idUtf8 is null
	 */
	public static <T> Gid<T> ofUtf8(final String idUtf8, final Class<T> clazz) {
		try {
			return idUtf8 == null ? null : of(idUtf8.getBytes(ID_ENCODING), clazz);
		} catch (UnsupportedEncodingException e) {
			throw Warden.spot(new CantHappenException(ID_ENCODING + " must exist.", e));
		}
	}

	/**
	 * Creates a Gid from a String. Uses base 64 encoding (url-save variant).
	 *
	 * @param <T> type of Gid
	 * @param idBase64 may be null
	 * @param clazz scope of id
	 * @return Gid null if idUtf8 is null
	 */
	public static <T> Gid<T> ofBase64(final String idBase64, final Class<T> clazz) {
		return idBase64 == null ? null : of(Base64.decodeBase64(idBase64), clazz);
	}

	/**
	 * Creates a Gid for an int. Uses big endian encoding.
	 *
	 * @param <T> type of Gid
	 * @param id int
	 * @param clazz scope of id
	 * @return Gid != null
	 */
	public static <T> Gid<T> of(final int id, final Class<T> clazz) {
		return of(Elements.toByteArray(id), clazz);
	}

	/**
	 * Creates a Gid for a long. Uses big endian encoding.
	 *
	 * @param <T> type of Gid
	 * @param id long
	 * @param clazz scope of id
	 * @return Gid != null
	 */
	public static <T> Gid<T> of(final long id, final Class<T> clazz) {
		return of(Elements.toByteArray(id), clazz);
	}

	/**
	 * Creates a Gid for a String. Uses encoding {@value #ID_ENCODING}.
	 *
	 * @param <T> type of Gid
	 * @param idBytes array, may be null or empty
	 * @param clazz scope of id
	 * @return Gid null if idBytes is null
	 */
	public static <T> Gid<T> of(final byte[] idBytes, final Class<T> clazz) {
		return idBytes == null ? null : new Gid<T>(idBytes, clazz);
	}

	private final byte[] id;

	protected Gid(final byte[] id, final Class<T> clazz) { // NOPMD protected upwards
		super(clazz);
		this.id = id;
	}

	/**
	 * Converts into a Gid of different type but same value.
	 * Use if you have a 1 to 1 correspondence between objects, e.g. when one object decorates or builds on another.
	 * This allows to still distinguish between the objects (and e.g. {@link Resolving#resolve(Gid) resolve} them).
	 *
	 * @param <S> target type
	 * @param clazz != null
	 * @return Gid with same value but new type
	 */
	public <S> Gid<S> reinterpret(final Class<S> clazz) {
		return of(this.id, clazz);
	}

	public byte[] getIdAsBytes() {
		return this.id;
	}

	/**
	 * Pendant to {@link #ofUtf8(String, Class)}.
	 *
	 * @return String != null
	 */
	@Override
	public String toUtf8String() {
		try {
			return new String(this.id, ID_ENCODING);
		} catch (UnsupportedEncodingException e) {
			throw Warden.spot(new CantHappenException(ID_ENCODING + " must exists", e));
		}
	}

	/**
	 * Pendant to {@link #ofBase64(String, Class)}.
	 *
	 * @return String != null
	 */
	public String toBase64String() {
		return Base64.encodeBase64URLSafeString(this.id);
	}

	/**
	 * Warning: Less then 8 bytes are possible but are used as the least bytes. This means that e.g. [0x30, 0xff] become 0x30ff.
	 * And negative number are thus not possible with less the 8 bytes.
	 *
	 * @return long
	 */
	public Long toLong() {
		if (this.id.length > Elements.BYTES_PER_LONG) {
			throw Warden.spot(new IllegalArgumentException("id too long for long" + this));
		}
		long v = 0;
		for (byte element : this.id) {
			v = (v << Elements.BITS_PER_BYTE) + (element & Elements.BYTE_MASK);
		}
		return Long.valueOf(v);
	}

	/**
	 * Warning: Less then 4 bytes are possible but are used as the least bytes. This means that e.g. [0x30, 0xff] become 0x30ff.
	 *
	 * @return int
	 */
	public Integer toInteger() {
		if (this.id.length > Elements.BYTES_PER_INT) {
			throw Warden.spot(new IllegalArgumentException("id too long for int" + this));
		}
		int v = 0;
		for (byte element : this.id) {
			v = (v << Elements.BITS_PER_BYTE) + (element & Elements.BYTE_MASK);
		}
		return Integer.valueOf(v);
	}

	/**
	 * Uses the hex coding for the id. Corresponding to {@link #ofHex(String, Class)}.
	 *
	 * @return String != null, may be empty
	 */
	@Override
	public String toHexString() {
		return this.id.length == 0 ? "" : new BigInteger(this.id).toString(BASE_16);
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		for (byte element : this.id) {
			result = result * HASH_PRIME ^ element;
		}
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Gid<?> other = (Gid<?>) obj;
		if (!Elements.arrayequals(this.id, other.id)) {
			return false;
		}
		return true;
	}

	/**
	 * Intended only for debug output.
	 *
	 * @return human readable string, either byte array or utf-8 string
	 */
	@Override
	public String toString() {
		return Elements.byteArrayToHumanReadable(getIdAsBytes()) + "(" + super.toString() + ")";
	}

	@Override
	public int compareTo(final Gid<T> o) {
		if (!getType().equals(o.getType())) {
			throw Warden.spot(new IllegalArgumentException("cannot compare Gids of different kind " + getType() + "="
					+ o.getType()));
		}

		return toHexString().compareTo(o.toHexString());
	}

	/**
	 * Resolves this ID into a corresponding Object.
	 * Precondition is that a suitable {@link Resolver} is set and the ID can actually be resolved.
	 *
	 * @return T, null if it cannot be resolved
	 */
	public T resolve() {
		return Resolving.resolve(this);
	}

	@Override
	public Gid<T> getId() {
		return this;
	}
}
