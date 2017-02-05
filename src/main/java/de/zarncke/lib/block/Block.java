package de.zarncke.lib.block;

/**
 * A unit of work yielding a result or failure.
 *
 * @author Gunnar Zarncke
 * @param <T> result type
 */
public interface Block<T> {

	T execute() throws Exception; // NOPMD
}
