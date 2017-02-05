package de.zarncke.lib.id;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import de.zarncke.lib.id.Ids.HasSomeGid;

public class GidRefXmlAdapter extends XmlAdapter<String, GidRef<?>> {

	@SuppressWarnings("unchecked")
	@Override
	public GidRef<?> unmarshal(final String v) throws Exception {
		return GidRef.of(v == null || v.isEmpty() ? null : Resolving.fromExternalForm(v, HasSomeGid.class).resolve());
	}

	@Override
	public String marshal(final GidRef<?> v) throws Exception {
		String ext = Resolving.toExternalForm(v.getId());
		return ext == null ? "" : ext;
	}

}
