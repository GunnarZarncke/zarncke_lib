package de.zarncke.lib.io.store.ext;

import javax.annotation.Nonnull;

import de.zarncke.lib.io.store.Store;

/**
 * Allows customization of delegate stores.
 *
 * @author Gunnar Zarncke
 * @param <T> specific EnhancedStore
 */
public interface Enhancer<T extends EnhancedStore<T>> {
	/**
	 * @param store
	 * @return enhanced store
	 */
	@Nonnull
	T enhance(@Nonnull Store store);

	/**
	 * @param store to test for inclusion of a child store of a delegate
	 * @return true if the store is included
	 */
	boolean isIncluded(@Nonnull Store store);
}