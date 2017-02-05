package de.zarncke.lib.data;

/**
 * Generic setter which can set a property of type V in an object of type B.
 *
 * @author Gunnar Zarncke
 * @param <B> base object type
 * @param <V> value type
 */
public interface Setter<B, V> {
	void set(B base, V value);
}
