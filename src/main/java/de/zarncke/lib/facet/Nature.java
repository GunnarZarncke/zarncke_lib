package de.zarncke.lib.facet;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import de.zarncke.lib.err.Warden;

public class Nature<T extends Facet> implements Serializable {
	private static final long serialVersionUID = 1L;

	private final Class<T> clazz;

	public static <T extends Facet> Nature<T> of(final Class<T> clazz) {
		return new Nature<T>(clazz);
	}

	public Nature(final Class<T> clazz) {
		this.clazz = clazz;
	}

	public Class<T> getClazz() {
		return this.clazz;
	}

	/**
	 * Creates the Facet for the given {@link Facetted identity}.
	 * This default implementation uses the constructor with Facetted argument.
	 * Derived classes may call specific constructors.
	 *
	 * @param identity of the Facet
	 * @return specific Facet
	 */
	public T createFacet(final Facetted identity) {
		try {
			Constructor<T> constructor = this.clazz.getConstructor(Facetted.class);
			T newInstance = constructor.newInstance(identity);
			return newInstance;
		} catch (SecurityException e) {
			throw Warden.spot(new RuntimeException("", e));
		} catch (NoSuchMethodException e) {
			throw Warden.spot(new RuntimeException("", e));
		} catch (IllegalArgumentException e) {
			throw Warden.spot(new RuntimeException("", e));
		} catch (InstantiationException e) {
			throw Warden.spot(new RuntimeException("", e));
		} catch (IllegalAccessException e) {
			throw Warden.spot(new RuntimeException("", e));
		} catch (InvocationTargetException e) {
			throw Warden.spot(new RuntimeException("", e));
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.clazz == null ? 0 : this.clazz.hashCode());
		return result;
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
		Nature<?> other = (Nature<?>) obj;
		if (this.clazz == null) {
			if (other.clazz != null) {
				return false;
			}
		} else if (!this.clazz.equals(other.clazz)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "Character of " + this.clazz;
	}
}
