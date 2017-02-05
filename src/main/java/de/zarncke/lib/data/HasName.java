package de.zarncke.lib.data;

/**
 * describes an Object, that has some kind of name (and can be ordered by it).
 * implementing classes should implement compareTo() as follows:
 *
 * <pre>
 * public int compareTo(HasName named) {
 * 	return getName().compareTo(named.getName());
 * }
 * </pre>
 *
 * see {@link NamedObject} for an example.
 */
public interface HasName extends Comparable<HasName>
{
	/**
	 * Get the name of this Object.
	 * @return the name as a String
	 */
	String getName();
}




