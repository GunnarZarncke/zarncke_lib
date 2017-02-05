package de.zarncke.lib.id;

import de.zarncke.lib.id.Ids.HasSomeGid;

/**
 * Provides {@link #hashCode()} and {@link #equals(Object)} based on the id.
 *
 * @author Gunnar Zarncke
 * @param <T> type of HasSomeGid interface
 */
public abstract class IdEquality<T> implements HasSomeGid<T> {

	@Override
	public int hashCode() {
		Gid<?> thisId = getId();
		return thisId == null ? 31 : thisId.hashCode();
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
		IdEquality<?> other = (IdEquality<?>) obj;
		Gid<?> thisId = getId();
		Gid<?> otherId = other.getId();
		if (thisId == null) {
			if (otherId != null) {
				return false;
			}
		} else if (!thisId.equals(otherId)) {
			return false;
		}
		return true;
	}
}
