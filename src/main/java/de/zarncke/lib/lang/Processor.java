package de.zarncke.lib.lang;


/**
 * A visitor pattern interface, that modifies elements of a container.
 */
public interface Processor
{
	/**
	 * @param object is the object to modify
	 * @param state is the current state (depends on container)
	 * @return the (possibly modified) Object
	 */
	public void process(Object object, Object state);
}


