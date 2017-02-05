package de.zarncke.lib.id;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.zarncke.lib.id.Ids.HasGid;

/**
 * Simple {@link Resolver} which uses a backing Map.
 * Not thread-save.
 *
 * @author Gunnar Zarncke
 */
public class MapResolver implements Resolver {

	private final Map<Gid<?>, Object> entries = new HashMap<Gid<?>, Object>();

	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(final Gid<T> id) {
		return (T) this.entries.get(id);
	}

	@Override
	public <T> List<T> get(final Collection<? extends Gid<T>> ids) {
		return Factory.getListByInteratingOverElements(ids, this);
	}

	public <T> void put(final Gid<T> id, final T obj) {
		this.entries.put(id, obj);
	}

	public void put(final HasGid<?> obj) {
		this.entries.put(obj.getId(), obj);
	}

	@Override
	public String toString() {
		return this.entries.toString();
	}
}
