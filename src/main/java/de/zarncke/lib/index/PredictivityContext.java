package de.zarncke.lib.index;

import de.zarncke.lib.index.crit.Criteria;

/**
 * Provides a context for estimating the predictivity of given {@link Criteria}.
 *
 * @author Gunnar Zarncke
 * @param <T> type of element matched
 */
public interface PredictivityContext<T> {

	/**
	 * Returns conservatively estimated fraction matching the given criteria.
	 *
	 * @param criteria != null
	 * @return 0<=x<=1
	 */
	double getPredictivityOf(Criteria<?, T> criteria);

	/**
	 * Returns conservatively estimated fraction matching a criteria of the given class.
	 *
	 * @param criteriaClass != null
	 * @return 0<=x<=1
	 */
	double getPredictivityOf(Class<?> criteriaClass);

}
