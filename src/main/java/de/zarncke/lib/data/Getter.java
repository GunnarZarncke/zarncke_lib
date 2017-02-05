package de.zarncke.lib.data;

/**
 * Generic getter which can read a property of type V in an object of type B.
 *
 * @author Gunnar Zarncke
 * @param <B> base object type
 * @param <V> value type
 */
public interface Getter<B, V> {
	V get(B base);
}
