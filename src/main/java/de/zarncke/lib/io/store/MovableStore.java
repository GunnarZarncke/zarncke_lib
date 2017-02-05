package de.zarncke.lib.io.store;

import java.io.IOException;

/**
 * Indicates that a Store supports move/rename operations.
 *
 * @author Gunnar Zarncke <gunnar@konzentrik.de>
 */
public interface MovableStore {
	/**
	 * Moves this store to the given target location.
	 * Note: Not em>into</em> but <em>to</em> the location.
	 * The target may need to be of the same type as this Store.
	 *
	 * @param target the new location of this store
	 * @throws IOException if move failed
	 * @throws UnsupportedOperationException if the target store doesn't match expectations
	 */
	void moveTo(MovableStore target) throws IOException;
}
