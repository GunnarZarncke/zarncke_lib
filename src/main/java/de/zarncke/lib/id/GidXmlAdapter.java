package de.zarncke.lib.id;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Marshals a {@link Gid}.
 *
 * @author Gunnar Zarncke
 * @clean 28.02.2012
 */
public class GidXmlAdapter extends XmlAdapter<String, Gid<?>> {

	@Override
	public Gid<?> unmarshal(final String v) throws Exception {
		return Resolving.fromExternalForm(v, Object.class);
	}

	@Override
	public String marshal(final Gid<?> v) throws Exception {
		return Resolving.toExternalForm(v);
	}

}
