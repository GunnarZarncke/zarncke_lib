package de.zarncke.lib.index.crit;

import java.util.Collection;

import de.zarncke.lib.coll.L;

/**
 * Basic criteria matching strings.
 *
 * @author Gunnar Zarncke
 * @param <T> type to match in
 */
public abstract class StringCriteria<T> implements Criteria<String, T> {

	protected final String str;

	public StringCriteria(final String str) {
		this.str = str;
	}

	public Collection<String> getKeys() {
		return L.s(this.str);
	}

	public Class<String> getType() {
		return String.class;
	}

	@Override
	public int hashCode() {
		return this.str.hashCode();
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
		StringCriteria<?> other = (StringCriteria<?>) obj;
		if (this.str == null) {
			if (other.str != null) {
				return false;
			}
		} else if (!this.str.equals(other.str)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return this.str;
	}
}
