package de.zarncke.lib.sys;

import de.zarncke.lib.io.store.Store;

/**
 * Optional interface for {@link Component}s. Allows to supply binary extra information.
 * 
 * @author Gunnar Zarncke
 */
public interface BinarySupplier {
	/**
	 * @param key well known keys of the Component (usually strings)
	 * @return Store with data or null if key unknown
	 */
	Store getBinaryInformation(Object key);
}
