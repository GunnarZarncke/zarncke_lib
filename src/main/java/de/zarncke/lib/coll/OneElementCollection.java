package de.zarncke.lib.coll;

import java.util.AbstractCollection;
import java.util.Iterator;

/**
 * contains exactly one Object.
 * 
 * @param <T> type of element
 */
public class OneElementCollection<T> extends AbstractCollection<T>
{
	protected T object;

	public OneElementCollection(final T obj)
	{
		this.object = obj;
	}

	@Override
	public int size()
	{
		return 1;
	}

	@Override
	public Iterator<T> iterator()
	{
		return new OneElementIterator<T>(this.object);
	}

	@Override
	public int hashCode() {
		return this.object == null ? 37 : this.object.hashCode();
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
		OneElementCollection<?> other = (OneElementCollection<?>) obj;
		if (this.object == null) {
			if (other.object != null) {
				return false;
			}
		} else if (!this.object.equals(other.object)) {
			return false;
		}
		return true;
	}

}

