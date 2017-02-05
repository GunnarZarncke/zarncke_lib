package de.zarncke.lib.index;

import java.util.Collection;

import de.zarncke.lib.index.crit.Criteria;

/**
 * Provides a context for estimating number of matches for given criteria.
 *
 * @author Gunnar Zarncke
 * @param <T> type of element matched
 */
public interface EstimationContext<T> {
	/**
	 * Get number of elements possible to match.
	 *
	 * @return int>=0
	 */
	int getTotalCandidates();


	/**
	 * Estimates the size of a search with the given filter conditions.
	 *
	 * @param filter != null
	 * @return estimates number of matching Broadcasts, need not be accurate
	 */
	int estimateSize(Collection<? extends Criteria<?, T>> filter);

}
