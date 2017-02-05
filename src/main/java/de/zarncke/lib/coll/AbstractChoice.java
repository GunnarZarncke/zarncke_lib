package de.zarncke.lib.coll;

import java.util.Collection;
import java.util.Iterator;

import de.zarncke.lib.id.Ids.HasGid;

/**
 * Abstract {@link MultipleChoice}.
 * 
 * @author Gunnar Zarncke
 * @param <T> of elements
 */
public abstract class AbstractChoice<T extends HasGid<T>> implements MultipleChoice<T> {
	private static final long serialVersionUID = 1L;

	public void setAll(final Collection<T> newList) {
		clear();
		for (T v : newList) {
			add(v);
		}
	}

	public void set(final T value) {
		clear();
		add(value);
	}

	public void toggle(final T value) {
		if (!isDefined()) {
			set(value);
		} else if (getAll().contains(value)) {
			remove(value);
		} else {
			add(value);
		}
	}

	@Override
	public String toString() {
		return !isDefined() ? "undefined" : getAll().toString();
	}

	@Override
	public int hashCode() {
		return !isDefined() ? 0 : getAll().hashCode();
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
		AbstractChoice<?> other = (AbstractChoice<?>) obj;

		return getAll().equals(other.getAll());
	}

	@Override
	public void select(final T value, final Boolean enabled) {
		if (null == enabled) {
			toggle(value);
		} else if (Boolean.TRUE.equals(enabled)) {
			add(value);
		} else {
			remove(value);
		}
	}

	@Override
	public Iterator<T> iterator() {
		return !isDefined() ? EmptyIterator.<T> getInstance() : getAll().iterator();
	}

	@Override
	public boolean match(final Collection<T> values) {
		if (!isDefined()) {
			return true;
		}
		for (T v : values) {
			if (getAll().contains(v)) {
				return true;
			}
		}
		return false;
	}

}