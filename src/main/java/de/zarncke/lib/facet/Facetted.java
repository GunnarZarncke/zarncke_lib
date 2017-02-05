package de.zarncke.lib.facet;


public interface Facetted {
	<T extends Facet> T getFacet(final Nature<T> facetDefault);

}
