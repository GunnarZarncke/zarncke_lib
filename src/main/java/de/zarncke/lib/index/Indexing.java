/**
 *
 */
package de.zarncke.lib.index;

import java.util.Collection;
import java.util.Comparator;

import de.zarncke.lib.index.crit.Criteria;

public interface Indexing<T> {
	void add(T entry);

	void clear();

	Index<T> getIndex(final Criteria<?, T> crit);

	double getPredictivity(final Criteria<?, T> crit);

	Collection<? extends Comparator<T>> getOrdering();

	Class<?> getType();
}