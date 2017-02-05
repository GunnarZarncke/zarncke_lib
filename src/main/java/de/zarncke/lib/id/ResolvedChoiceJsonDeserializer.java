package de.zarncke.lib.id;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;

import de.zarncke.lib.coll.ResolvedChoice;
import de.zarncke.lib.id.Ids.HasGid;
import de.zarncke.lib.id.Ids.HasSomeGid;

public class ResolvedChoiceJsonDeserializer extends StdScalarDeserializer<ResolvedChoice<? extends HasSomeGid<?>>> {
	protected ResolvedChoiceJsonDeserializer() {
		super(ResolvedChoice.class);
	}

	private static final long serialVersionUID = 1L;

	@Override
	public ResolvedChoice<? extends HasSomeGid<?>> deserialize(final JsonParser jp, final DeserializationContext ctxt,
			final ResolvedChoice<? extends HasSomeGid<?>> intoValue) throws IOException, JsonProcessingException {
		String v = jp.readValueAs(String.class);
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
	public ResolvedChoice<? extends HasSomeGid<?>> deserialize(final JsonParser jp, final DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		String v = jp.readValueAs(String.class);
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

}
