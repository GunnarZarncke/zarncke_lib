package de.zarncke.lib.id;

import java.util.Collection;
import java.util.List;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.ctx.Context;
import de.zarncke.lib.value.Default;

/**
 * Functionality to find objects by their ids.
 *
 * @author Gunnar Zarncke
 */
public interface Resolver {
	Context<Resolver> CTX = Context.of(Default.of(new Factory(), Resolver.class));

	Resolver NULL_RESOLVER = new Resolver() {
		public <T> T get(final Gid<T> id) {
			return null;
		}

		@Override
		public <T> List<T> get(final Collection<? extends Gid<T>> ids) {
			// this illegally drops all ids, a List of nulls would be correct
			return L.e();
		}
	};

	/**
	 * @param <T> type of id and result
	 * @param id any for which a the resolver supports the type
	 * @return object of matching type or null if and only if the id is null
	 */
	<T> T get(final Gid<T> id);

	/**
	 * @param <T> type of ids and results
	 * @param ids List of ids for which the resolver supports the type
	 * @return List of corresponding objects (of matching type), may contain nulls for some ids
	 */
	<T> List<T> get(final Collection<? extends Gid<T>> ids);

}
