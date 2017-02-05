package de.zarncke.lib.facet;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A Manifold has many {@link Facet facets}. The facets are logical identical to the {@link Facetted manifold}.
 *
 * @author Gunnar Zarncke
 */
public class Manifold extends AbstractMap<Nature<?>, Facet> implements Facetted, Serializable {
	private static final long serialVersionUID = 1L;
	private final Map<Nature<?>, Facet> facets = Collections.synchronizedMap(new HashMap<Nature<?>, Facet>());

	public Manifold() {
		//
	}

	public <T extends Facet> T getFacet(final Nature<T> character) {
		Facet f = this.facets.get(character);
		if (f == null) {
			f = character.createFacet(this);
			this.facets.put(character, f);
		}
		@SuppressWarnings("unchecked")
		T t = (T) f;
		return t;
	}

	public void removeFacet(final Nature<?> character) {
		this.facets.remove(character);
	}

	@Override
	public Set<java.util.Map.Entry<Nature<?>, Facet>> entrySet() {
		return this.facets.entrySet();
	}

}
