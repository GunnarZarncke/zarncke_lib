package de.zarncke.lib.id;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Compositum pattern for {@link Resolver}.
 *
 * @author Gunnar Zarncke
 */
public class Resolvers implements Resolver {

	private final List<Resolver> resolvers = new LinkedList<Resolver>();

	public Resolvers(final List<Resolver> l) {
		this.resolvers.addAll(l);
	}

	public Resolvers add(final Resolver res) {
		this.resolvers.add(res);
		return this;
	}

	@Override
	public <T> T get(final Gid<T> id) {
		for (Resolver r : this.resolvers) {
			T v = r.get(id);
			if (v != null) {
				return v;
			}
		}
		return null;
	}

	@Override
	public <T> List<T> get(final Collection<? extends Gid<T>> ids) {
		// IDEA might distribute over resolvers first and merge later
		return Factory.getListByInteratingOverElements(ids, this);
	}

}
