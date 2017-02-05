package de.zarncke.lib.index.crit;

import java.util.Collection;

import de.zarncke.lib.coll.L;

/**
 * Represents constraints on elements of a specific type by a single key.
 *
 * @author Gunnar Zarncke
 * @param <K> key type
 * @param <T> object type
 */
public abstract class SingleCriteria<K, T> implements Criteria<K, T> {

	private final K key;
	private final Class<K> type;

	protected SingleCriteria(final K key, final Class<K> type) {
		this.key = key;
		this.type = type;
	}

	@SuppressWarnings("unchecked")
	public Collection<K> getKeys() {
		return L.l(this.key);
	}

	public Class<K> getType() {
		return this.type;
	}

	public K getKey() {
		return this.key;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.key == null ? 0 : this.key.hashCode());
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
		SingleCriteria<?, ?> other = (SingleCriteria<?, ?>) obj;
		if (this.key == null) {
			if (other.key != null) {
				return false;
			}
		} else if (!this.key.equals(other.key)) {
			return false;
		}
		return this.type == other.type;
	}

	@Override
	public String toString() {
		return this.type.getSimpleName() + "=" + this.key;
	}
}
