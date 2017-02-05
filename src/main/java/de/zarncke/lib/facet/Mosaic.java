package de.zarncke.lib.facet;

import java.util.Map;

import de.zarncke.lib.value.Default;

public interface Mosaic extends Map<Default<?>, Object> {
	/**
	 * Get a facet of the Mosaic
	 * 
	 * @param <T> type of piece
	 * @param facet specification of the facet
	 * @return value - if not preexisting the default specification determines the initial value (may be null)
	 */
	public <T> T get(final Default<T> facet);

	/**
	 * Explicitly set a facet.
	 * 
	 * @param <T> type of facet
	 * @param facet specification of facet
	 * @param value to set - if null the facet is removed!
	 */
	public <T> void set(final Default<T> facet, T value);
}
