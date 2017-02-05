package de.zarncke.lib.facet;

import java.io.IOException;
import java.io.Serializable;

public class SerializableFacet extends Facet implements Serializable {
	private static final long serialVersionUID = 1L;

	protected SerializableFacet() {
		super();
	}

	public SerializableFacet(final Facetted identity) {
		super(identity);
	}
	/**
	 * For serialization
	 */
	private void writeObject(final java.io.ObjectOutputStream out) throws IOException {
		out.writeObject(this.identity);
		out.defaultWriteObject();
	}

	private void readObject(final java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		this.identity = (Facetted) in.readObject();
		in.defaultReadObject();
	}


}
