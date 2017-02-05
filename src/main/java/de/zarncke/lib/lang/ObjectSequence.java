package de.zarncke.lib.lang;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A sequence of elements.
 */
public class ObjectSequence implements Sequence
{
	private final List list;

	public ObjectSequence()
	{
		this.list = new ArrayList();
	}

	public ObjectSequence(final ObjectSequence seq)
	{
		this.list = seq.list;
	}

	public ObjectSequence(final List list)
	{
		this.list = new ArrayList(list);
	}

	public void add(final Object a)
	{
		this.list.add(a);
	}

	public void remove(final Object a)
	{
		this.list.remove(a);
	}

	public void process(final Processor processor)
	{
		int s = this.list.size();
		for (int i = 0; i < s; i++)
		{
			throw new de.zarncke.lib.err.NotImplementedException
				("Processor changed semantics");
			//this.list.set(i, processor.process(new Integer(i),
			//								   this.list.get(i)));
		}
	}

	public int size()
	{
		return this.list.size();
	}

	public List toList()
	{
		return Collections.unmodifiableList(this.list);
	}


	@Override
	public int hashCode() {
		return this.list == null ? 0 : this.list.hashCode();
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
		ObjectSequence other = (ObjectSequence) obj;
		if (this.list == null) {
			if (other.list != null) {
				return false;
			}
		} else if (!this.list.equals(other.list)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString()
	{
		return this.list.toString();
	}
}


