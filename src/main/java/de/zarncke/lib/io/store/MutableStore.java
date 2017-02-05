package de.zarncke.lib.io.store;

/**
 * An interface which stores may implement which support mutation by replacing their behavior with that of another store.
 * Use with care.
 *
 * @author Gunnar Zarncke
 */
public interface MutableStore extends Store {
	void behaveAs(Store store);
}