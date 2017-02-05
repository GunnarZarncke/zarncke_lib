package de.zarncke.lib.value;

import java.io.Serializable;

/**
 * Represents a type with the given class.
 * In comparison to using the class directly to indicate a type this class may be inherited from.
 *
 * @author Gunnar Zarncke
 * @param <T> type
 */
public class Type<T> implements Typed, Serializable {

	private static final long serialVersionUID = 2L;

	/**
	 * Creates a type for the given class.
	 *
	 * @param <T> type of default value
	 * @param clazz T
	 * @return Default
	 */
	public static <T> Type<T> of(final Class<T> clazz) {
		return new Type<T>(clazz);
	}

	private final Class<T> clazz;

	protected Type(final Class<T> clazz) {
		if (clazz == null) {
			throw new IllegalArgumentException("clazz==null");
		}
		this.clazz = clazz;
	}

	public Class<T> getType() {
		return this.clazz;
	}

	@Override
	public int hashCode() {
		return this.clazz.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Type<?> other = (Type<?>) obj;
		return this.clazz.equals(other.clazz);
	}

	@Override
	public String toString() {
		return this.clazz.getSimpleName();
	}
}
