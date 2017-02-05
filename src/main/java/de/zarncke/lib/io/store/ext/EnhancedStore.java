package de.zarncke.lib.io.store.ext;

import java.util.Iterator;

import de.zarncke.lib.io.store.Store;

/**
 * A Store with elements which are of the same type.
 *
 * @author Gunnar Zarncke
 * @param <T> actual Store type
 */
public interface EnhancedStore<T extends EnhancedStore<T>> extends Store {
	@Override
	T element(String name);

	Iterator<T> enhancedIterator();
}