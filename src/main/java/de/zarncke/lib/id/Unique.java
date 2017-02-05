package de.zarncke.lib.id;

import java.io.Serializable;

import de.zarncke.lib.id.Ids.HasGid;

/**
 * Base class for objects that are uniquely identified by an ID.
 *
 * @author Gunnar Zarncke
 * @param <T> type of object
 */
public abstract class Unique<T extends HasGid<T>> implements HasGid<T>, Serializable {
	private static final long serialVersionUID = 1L;

	private final Gid<T> id;

	public Unique(final Gid<T> id) {
		this.id = id;
	}

	public Gid<T> getId() {
		return this.id;
	}

	@Override
	public int hashCode() {
		return this.id == null ? 0 : this.id.hashCode();
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
		Unique<?> other = (Unique<?>) obj;
		if (this.id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!this.id.equals(other.id)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return this.id.toString();
	}
}
