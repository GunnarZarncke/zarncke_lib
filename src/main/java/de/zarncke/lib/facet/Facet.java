package de.zarncke.lib.facet;


/**
 * Represents one facet of information.
 *
 * @author Gunnar Zarncke
 */
public class Facet implements Facetted {
	protected Facetted identity;

	public Facet(final Facetted identity) {
		this.identity = identity;
	}

	protected Facet() {
		// for serializability extension
	}

	public <T extends Facet> T getFacet(final Nature<T> facet) {
		return this.identity.getFacet(facet);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.identity == null ? 0 : this.identity.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Facet other = (Facet) obj;
		if (this.identity == null) {
			if (other.identity != null) {
				return false;
			}
		} else if (!this.identity.equals(other.identity)) {
			return false;
		}
		return true;
	}

}
