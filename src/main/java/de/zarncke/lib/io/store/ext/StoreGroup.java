package de.zarncke.lib.io.store.ext;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.coll.Pair;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.io.store.AbstractStore;
import de.zarncke.lib.io.store.DelegateStore;
import de.zarncke.lib.io.store.MapStore;
import de.zarncke.lib.io.store.MapStore.CreateMode;
import de.zarncke.lib.io.store.Store;

/**
 * A logical grouping of {@link Store#element(String) elements} of a Store.
 * The elements are provided as one virtual Store with sub Stores of for each group.
 * Deleting a group deletes all contained Stores and removes the group from the virtual store.
 * This is only a snapshot of the structure at the moment the group is created.
 * Files created later will not show up as sub groups nor elements.
 *
 * @author Gunnar Zarncke
 */
public class StoreGroup extends AbstractStore {

	/**
	 * Strategy to define behavior of the artificial group Store.
	 */
	public interface Controller {
		String getName();

		long getLastModified(Collection<Store> elements);
	}

	/**
	 * Strategy to form groups of files in a directory.
	 */
	public interface Grouper {
		/**
		 * @param container which contains the selectedElement and the returned elements
		 * @param selectedElement any
		 * @return null indicates that the element should not appear;
		 * otherwise a Pair of the controller and matching elements
		 * from the store, may contain the selected element, may be empty
		 */
		Pair<Controller, ? extends Iterable<Store>> getMatchingsFor(Store container, Store selectedElement);
	}

	/**
	 * Groups files with common prefix.
	 *
	 * @param length of prefix (shorter names are returned solitary)
	 * @return Grouper
	 */
	public static final Grouper makePrefixLengthGrouper(final int length) {
		return new Grouper() {
			@Override
			public Pair<Controller, ? extends Iterable<Store>> getMatchingsFor(final Store container,
					final Store selectedElement) {
				String prefix = selectedElement.getName();
				if (prefix.length() < length) {
					return Pair.pair((Controller) new SimpleController(prefix), L.s(selectedElement));
				}
				prefix = prefix.substring(0, length);
				List<Store> elems = L.l();
				for (Store elem : container) {
					if (elem.getName().startsWith(prefix)) {
						elems.add(elem);
					}
				}
				return Pair.pair((Controller) new SimpleController(prefix), elems);
			}
		};
	}

	/**
	 * A controller with a fix name. Uses the least contained modification date.
	 *
	 * @author Gunnar Zarncke
	 */
	public static class SimpleController implements Controller {

		private final String name;

		public SimpleController(final String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public long getLastModified(final Collection<Store> elements) {
			if (elements.isEmpty()) {
				return Store.UNKNOWN_MODIFICATION;
			}
			long min = Long.MAX_VALUE;
			for (Store s : elements) {
				long lm = s.getLastModified();
				if (lm != Store.UNKNOWN_MODIFICATION) {
					min = Math.min(lm, min);
				}
			}
			return min == Long.MAX_VALUE ? Store.UNKNOWN_MODIFICATION : min;
		}

		@Override
		public String toString() {
			return this.name;
		}
	}

	static class Group {

		boolean resolved;
		Collection<Store> group;
		Controller controller;

		@Override
		public String toString() {
			return this.controller + "->" + this.group;
		}
	}

	public static Store group(final Store base, final Grouper grouper) {
		if (!base.iterationSupported()) {
			throw Warden.spot(new IllegalArgumentException(base + " must support iteration."));
		}
		// IDEA perform this grouping on-demand

		// this ensures that we have a fast and save iteration over children
		Store fastBase = new DelegateStore(base) {
			private List<Store> elements;

			@Override
			public Iterator<Store> iterator() {
				if (this.elements == null) {
					this.elements = L.copy(super.iterator());
				}
				return this.elements.iterator();
			}
		};

		Map<Store, Group> storeToGroup = setupGroupForEachStore(fastBase);

		processGroupings(fastBase, grouper, storeToGroup);

		return createResultStore(storeToGroup);
	}

	private static MapStore createResultStore(final Map<Store, Group> storeToGroup) {
		MapStore result = new MapStore();
		result.setCreateMode(CreateMode.LAZY_AUTOMATIC);
		for (final Map.Entry<Store, Group> me : storeToGroup.entrySet()) {
			Group group = me.getValue();
			if (!group.resolved || group.controller == null) {
				continue;
			}
			final Controller controller = group.controller;
			final Collection<Store> elements = group.group;
			MapStore element = new MapStore() {
				@Override
				public long getLastModified() {
					return controller.getLastModified(elements);
				}

				@Override
				public boolean delete() {
					boolean succ = true;
					for (Store e : elements) {
						succ = succ & e.delete();
					}
					super.delete();
					return succ;
				}
			};
			// element.setCreateMode(CreateMode.ALLOW_LEAFS);
			for (Store child : elements) {
				element.add(child.getName(), child);
			}
			// element.setCreateMode(CreateMode.DENY);

			if (result.element(group.controller.getName()).exists()) {
				throw Warden.spot(new IllegalArgumentException(
						"the grouping created multiple different groups with the same name " + group.controller.getName()
								+ " in " + storeToGroup));
			}
			result.add(group.controller.getName(), element);
		}
		result.setCreateMode(CreateMode.DENY);
		return result;
	}

	private static void processGroupings(final Store base, final Grouper grouper, final Map<Store, Group> storeToGroup) {
		for (Map.Entry<Store, Group> me : storeToGroup.entrySet()) {
			Group group = me.getValue();
			if (!group.resolved) {
				Store child = me.getKey();
				Pair<Controller, ? extends Iterable<Store>> controllerAndMatchings = grouper.getMatchingsFor(base, child);
				if (controllerAndMatchings == null) {
					continue;
				}

				// TODO check whether this matching overlaps with an already present group

				Collection<Store> matchings = L.copy(controllerAndMatchings.getSecond().iterator());
				for (Store mats : matchings) {
					Group matGroup = storeToGroup.get(mats);
					if (matGroup != null) {
						matGroup.resolved = true;
					}
				}

				if (!matchings.contains(child)) {
					matchings.add(child);
					group.resolved = true;
				}
				group.group = matchings;
				group.controller = controllerAndMatchings.getFirst();
			}
		}
	}

	private static Map<Store, Group> setupGroupForEachStore(final Store base) {
		Map<Store, Group> storeToGroup = new HashMap<Store, StoreGroup.Group>();
		for (Store child : base) {
			storeToGroup.put(child, new Group());
		}
		return storeToGroup;
	}

	Collection<Store> common;

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public String getName() {
		return null;
	}

	public static Store filterByName(final Store store, final String regEx) {
		final Pattern regPat = Pattern.compile(regEx);
		return new DelegateStore(store) {
			@Override
			public java.util.Iterator<Store> iterator() {
				return Iterators.filter(store.iterator(), new Predicate<Store>() {
					@Override
					public boolean apply(final Store input) {
						return regPat.matcher(input.getName()).matches();
					}
				});
			}
		};
	}
}
