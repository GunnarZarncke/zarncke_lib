package de.zarncke.lib.value;

/**
 * An indirect Object.
 * Can be used when the actual object to be used should not be referenced directly, e.g. when
 * <ul>
 * <li>the Object should not be serialized</li>
 * <li>is large an should be fetched each time</li>
 * <li>is large and should be recomputed each time</li>
 * </ul>
 * In a way this is a generalized builder or factory interface.
 *
 * @author Gunnar Zarncke
 * @param <T> type of object
 */
public interface Value<T> {
	T get();
}
