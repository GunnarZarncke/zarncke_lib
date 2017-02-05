package de.zarncke.lib.coll;

import java.util.Iterator;

/**
 * An Iterator over one given Element
 * 
 * @param <T> type of element
 */
public class OneElementIterator<T> implements Iterator<T>
{
	protected T object;

	public OneElementIterator(final T obj)
	{
		this.object = obj;
	}

	public boolean hasNext()
	{
		return this.object != null;
	}

	public T next()
	{
		T o = this.object;
		if(o == null)
		{
			throw new IllegalStateException("beyond end of iteration!");
		}
		this.object = null;
		return o;
	}

	public void remove()
	{
		throw new UnsupportedOperationException("can't remove the Object!");
	}
}

