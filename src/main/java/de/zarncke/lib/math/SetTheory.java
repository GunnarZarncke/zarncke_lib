package de.zarncke.lib.math;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.zarncke.lib.coll.L;

/**
 * Currently provides cartesian product only.
 *
 * @author Gunnar Zarncke
 */
public final class SetTheory {

	private SetTheory() {
		// helper
	}

	/**
	 * Returns the cartesian product of all elements.
	 *
	 * @param <T> element type
	 * @param expandedCriteria ((a,b),(x),(1,2,3))
	 * @return ((a,x,1),(a,x,2),(a,x,3),(b,x,1),(b,x,2),(b,x,3))
	 */
	public static <T> Collection<? extends Collection<T>> expandToCartesianProduct(
			final List<? extends Collection<T>> expandedCriteria) {
		return expandToCartesianProduct(expandedCriteria, L.l(L.<T> e()));
	}

	private static <T> Collection<? extends Collection<T>> expandToCartesianProduct(
			final List<? extends Collection<T>> expandedCriteria,
			final List<? extends Collection<T>> cartesianPrefix) {
		if (expandedCriteria.isEmpty()) {
			return cartesianPrefix;
		}

		List<? extends Collection<T>> remainingSuffix = expandedCriteria.subList(1, expandedCriteria.size());

		List<Collection<T>> moreCompleteProduct = L.l();

		for (Collection<T> partialProduct : cartesianPrefix) {
			for (T crit : expandedCriteria.get(0)) {
				List<T> l = new ArrayList<T>(partialProduct.size() + 1);
				l.addAll(partialProduct);
				l.add(crit);
				moreCompleteProduct.add(l);
			}
		}
		return expandToCartesianProduct(remainingSuffix, moreCompleteProduct);
	}

	/**
	 * Removes all sets from a list of sets which are supersets of another set in the list.
	 * Example: ((a,b,c), (a,b), (a,c), (b, d), (d)) is reduced to ((a,b, (a,c), (d)). (a,b,c) is removed because it contains
	 * (a,b) and (b,d) is removed because it contains (d) as subsets).
	 * Note: If the List contains the empty set then consequentially all other elements are removed.
	 * 
	 * @param <T> of elements
	 * @param listOfSets
	 * @return Collection of least Sets
	 */
	public static <T> Collection<? extends Collection<T>> absorbSuperSets(final Collection<? extends Collection<T>> listOfSets) {
		if (listOfSets.size() <= 1) {
			return listOfSets;
		}
		List<Set<T>> sets = L.l();
		for (Collection<T> elem : listOfSets) {
			sets.add(elem instanceof Set ? (Set<T>) elem : new HashSet<T>(elem));
		}
		List<Set<T>> setCopy = L.copy(sets);
		Iterator<Set<T>> iterator = sets.iterator();
		while (iterator.hasNext()) {
			Set<T> candidate = iterator.next();
			for (Set<T> elem : setCopy) {
				if (elem != candidate && candidate.containsAll(elem)) {
					iterator.remove();
					break;
				}
			}
		}
		return sets;
	}
}
