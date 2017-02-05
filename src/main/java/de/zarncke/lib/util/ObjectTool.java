package de.zarncke.lib.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import de.zarncke.lib.err.Warden;
import de.zarncke.lib.io.RegionInputStream;
import de.zarncke.lib.region.Region;
import de.zarncke.lib.region.RegionUtil;

/**
 * Operations on Objects by reflection and other Java magic.
 *
 * @author Gunnar Zarncke
 */
public final class ObjectTool {
	/**
	 * {@link RuntimeException} for embedded serialization code.
	 */
	public static class NotSerializableException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public NotSerializableException(final String msg, final Throwable e) {
			super(msg, e);
		}
	}
	private ObjectTool() {
		//
	}

	public static final Object NULL = new Object();

	/**
	 * Returns {@link #NULL} instead of null.
	 * This violates the compilers generics contract! You will be caught by a {@link ClassCastException} earlier or later.
	 * You have been warned.
	 *
	 * @param <T> formal type of the
	 * @param object any object or null
	 * @return object or {@link #NULL} instead of null
	 */
	@SuppressWarnings("unchecked" /* very dangerous */)
	public static <T> T nullObjectInsteadOfNull(final T object) {
		return object == null ? (T) NULL : object;
	}

	/**
	 * Returns null instead of {@link #NULL}.
	 * This undoes {@link #nullObjectInsteadOfNull(Object)}.
	 *
	 * @param <T> formal type of the
	 * @param object any object or null or specifically {@link #NULL}
	 * @return object or null instead of {@link #NULL}
	 */
	public static <T> T nullInsteadOfNullObject(final T object) {
		return object == NULL ? null : object; // NOPMD intended
	}

	/**
	 * @param object to serialize, may be null
	 * @return bytes != null
	 */
	public static byte[] getSerializedBytes(final Serializable object) {
		// TODO COnsider serializing on demand (possibly optional) to save space.
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(object);
			oos.flush();
			oos.close();
		} catch (IOException e) {
			throw Warden.spot(new NotSerializableException("Serialization failed on " + object.getClass() + ":" + object, e));
		}
		return baos.toByteArray();
	}

	public static Region serialize(final Serializable object) {
		// TODO COnsider serializing on demand (possibly optional) to save space.
		return RegionUtil.asRegion(getSerializedBytes(object));
	}

	/**
	 * @param <T> type
	 * @param container yielding bytes
	 * @param expectedClass != null
	 * @return deserialized bytes of given type, may be null
	 * @throws ClassCastException if type not matching
	 */
	public static <T> T deserialize(final Region container, final Class<T> expectedClass) {
		Serializable res = deserialize(container);
		if (expectedClass.isAssignableFrom(expectedClass)) {
			@SuppressWarnings("unchecked")
			T t = (T) res;
			return t;
		}
		throw Warden.spot(new ClassCastException("Seserialized object " + res + " is not of expected type " + expectedClass));
	}

	public static Serializable deserialize(final Region container) {
		// TODO Consider adding a variant which checks that the object fully fills the region.
		final InputStream ris = new RegionInputStream(container);
		try {
			ObjectInputStream ois = new ObjectInputStream(ris);
			Object res = ois.readObject();
			ois.close();
			return (Serializable) res;
		} catch (IOException e) {
			throw Warden.spot(new RuntimeException("Deserialization (IO) failed on " + container, e));
		} catch (ClassNotFoundException e) {
			throw Warden.spot(new RuntimeException("Deserialization (class) failed on " + container, e));
		}
	}

	public static int estimateSize(final Serializable object) {
		try {
			return getSerializedBytes(object).length;
		} catch (Exception e) {
			Warden.disregard(e);
			return 64;
		}
	}

}
