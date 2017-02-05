package de.zarncke.lib.index.crit;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * Represents constraints on elements of a specific type by any of a number of keys.
 *
 * @author Gunnar Zarncke
 * @param <K> key type
 * @param <T> object type
 */
public abstract class CollectionCriteria<K, T> implements Criteria<K, T>, Serializable {
	private static final long serialVersionUID = 1L;

	private final Collection<K> keys;
	private final Class<K> type;

	protected CollectionCriteria(final Collection<K> keys, final Class<K> type) {
		this.keys = new LinkedHashSet<K>(keys);
		this.type = type;
	}

	@Override
	public Collection<K> getKeys() {
		return this.keys;
	}

	@Override
	public boolean matches(final T entry) {
		Collection<? extends K> values = getValues(entry);
		for (K value : values) {
			if (this.keys.contains(value)) {
				return true;
			}
		}
		return false;
	}

	protected abstract Collection<? extends K> getValues(T entry);

	@Override
	public Class<K> getType() {
		return this.type;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.keys == null ? 0 : this.keys.hashCode());
		result = prime * result + (this.type == null ? 0 : this.type.hashCode());
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
		CollectionCriteria<?, ?> other = (CollectionCriteria<?, ?>) obj;
		if (this.keys == null) {
			if (other.keys != null) {
				return false;
			}
		} else if (!this.keys.equals(other.keys)) {
			return false;
		}
		if (this.type == null) {
			if (other.type != null) {
				return false;
			}
		} else if (!this.type.equals(other.type)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "any of " + this.keys + "(" + this.type.getSimpleName() + ")";
	}
}
