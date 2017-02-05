package de.zarncke.lib.index.crit;

import java.util.Collection;

/**
 * Represents constraints on elements of a specific type.
 * Specialized for a specific key type.
 *
 * @author Gunnar Zarncke
 * @param <K> key type
 * @param <T> object type
 */
public interface Criteria<K, T> {

	/**
	 * Check whether a candidate matches.
	 *
	 * @param entry != null
	 * @return true if it matches out criteria
	 */
	boolean matches(T entry);

	/**
	 * Note: Used to match against an index.
	 *
	 * @return type of property tested
	 */
	Class<K> getType();

	// TODO add qualifier? there might be different keys of the same type

	/**
	 * @return key values which might match, null means: all might match (full scan)
	 */
	Collection<K> getKeys();
}
