package de.zarncke.lib.util;

/**
 * Provides generic support for normalizing (finding canonical instances of) objects.
 * 
 * @param <T> type to normalize
 */
public interface Normalizer<T>
{
    /**
     * Normalize.
     *
     * @param object
     *            the object to normalize
     * @return the canonical representation for the object
     */
    T normalize(T object);
}
