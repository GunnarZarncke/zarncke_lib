package de.zarncke.lib.value;

/**
 * An abstraction of a data sink of type T. Allows to write an object of type T to some received.
 * This is the dual for {@link Value} which reads an object of type T.
 *
 * @author Gunnar Zarncke
 * @param <T>
 */
public interface Target<T> {
	void set(T value);
}
