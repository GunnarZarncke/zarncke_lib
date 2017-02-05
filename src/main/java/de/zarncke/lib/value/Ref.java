package de.zarncke.lib.value;

/**
 * Like {@link java.lang.ref.Reference} but without the GC connotations.
 *
 * @author Gunnar Zarncke
 * @param <T> type of referent
 */
public interface Ref<T> extends Value<T>, Target<T> {
	// aggregation of Value and Target
}
