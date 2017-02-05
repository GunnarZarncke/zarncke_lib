package de.zarncke.lib.struct;

/**
 * defines a partial order relation among objects.
 */
// TODO generics
public interface PartialOrder
{
	/**
	 * @param a any
	 * @param b any
	 * @return true if <code>a R b</code> with R mostly written as <=.
	 */
	public boolean lessEqual(Object a, Object b);
}



