package de.zarncke.lib.id;

import java.util.Collection;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import de.zarncke.lib.coll.ResolvedChoice;
import de.zarncke.lib.id.Ids.HasGid;
import de.zarncke.lib.id.Ids.HasSomeGid;

public class ResolvedChoiceXmlAdapter extends XmlAdapter<String, ResolvedChoice<? extends HasSomeGid<?>>> {

	@SuppressWarnings("unchecked")
	@Override
	public ResolvedChoice<?> unmarshal(final String v) throws Exception {
		@SuppressWarnings("rawtypes")
		ResolvedChoice r = new ResolvedChoice();
		if (v == null || v.isEmpty()) {
			return r;
		}
		String[] ids = v.split(",");
		for (String id : ids) {
			r.add(Resolving.fromExternalForm(id, HasGid.class).resolve());
		}
		return r;
	}

	@Override
	public String marshal(final ResolvedChoice<? extends HasSomeGid<?>> v) throws Exception {
		StringBuilder sb = new StringBuilder();
		Collection<? extends HasSomeGid<?>> vals = v.getAll();
		for (HasSomeGid<?> hasid : vals) {
			sb.append(Resolving.toExternalForm(hasid.getId())).append(",");
		}
		return sb.toString();
	}

}
