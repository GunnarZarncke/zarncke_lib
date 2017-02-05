package de.zarncke.lib.util;

/**
 * Indicates that an Object may be 'detached' from some container.
 * 
 * @author Gunnar Zarncke
 * @param <T> type of container (you might like to use Void)
 */
public interface Detachable<T> {
	void detach(T container);
}
