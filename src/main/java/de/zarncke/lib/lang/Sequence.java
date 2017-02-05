package de.zarncke.lib.lang;

import java.util.List;

/**
 * A sequence of elements.
 */
public interface Sequence //extends Structure
{
	public void add(Object a);
	public void remove(Object a);

	public void process(Processor m);

	public int size();

	public List toList();
}


