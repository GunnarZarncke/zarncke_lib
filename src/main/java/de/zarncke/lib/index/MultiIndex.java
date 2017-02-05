package de.zarncke.lib.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.index.Index.Conservative;
import de.zarncke.lib.index.crit.Criteria;
import de.zarncke.lib.index.crit.ResolvedCriteria;

/**
 * Indexes a set of objects T by multiple {@link Criteria} and supports {@link #getMatches(Collection, int) retrieval} by these
 * criteria.
 * ALso supports {@link #estimateSize(Collection) estimating result size} without actually performing the query.
 *
 * @author Gunnar Zarncke
 * @param <T> type of indexed objects.
 */
public class MultiIndex<T> implements EstimationContext<T>, PredictivityContext<T> {
	private final class LimitedResults extends ListResults<T> {
		private final int maxSize;
		private final int maxResults;
		private final Collection<? extends Criteria<?, T>> criteria;

		private LimitedResults(final List<T> entries, final int maxSize, final int maxResults,
				final Collection<? extends Criteria<?, T>> criteria) {
			super(entries);
			this.maxSize = maxSize;
			this.maxResults = maxResults;
			this.criteria = criteria;
		}

		@Override
		public int readTo(final int position) {
			this.entries = getMatches(this.criteria, position + this.maxResults).realize();
			return this.entries.size();
		}

		@Override
		public int size() {
			return this.maxSize;
		}
	}

	private final Map<Class<?>, Indexing<T>> indexByType = new HashMap<Class<?>, Indexing<T>>();
	private Index<T> all = new ListIndex<T>();

	public void add(final T entry) {
		for (Indexing<T> indexing : this.indexByType.values()) {
			indexing.add(entry);
		}
		this.all = this.all.add(entry);
	}

	public void addIndex(final Class<?> indexedType, final Indexing<T> index) {
		this.indexByType.put(indexedType, index);
		for (T entry : this.all.getAll()) {
			index.add(entry);
		}
	}

	/**
	 * Queries for results.
	 *
	 * @param criteria Collection of {@link Criteria}, empty means all
	 * @param maxResults >=0
	 * @return {@link Results} object for lazily and incrementally fetching the results
	 */
	public Results<T> getMatches(final Collection<? extends Criteria<?, T>> criteria, final int maxResults) {
		// TODO handle RuntimExceptions in criteria calls and wrap them
		List<Criteria<?, T>> crits = getCriteriaBySpecificity(criteria);

		// determine index
		Index<T> index = determineEffectiveIndex(crits);

		Results<T> indexResults = index == null ? null : index.getAll();
		if (indexResults != null && crits.isEmpty()) {
			// no further constraints: done
			return indexResults;
		}

		Results<T> results;
		// no suitable index found: search in all
		if (indexResults == null) {
			Collection<T> extra = L.l();
			boolean complete = false;
			for (Criteria<?, T> c : criteria) {
				if (c instanceof ResolvedCriteria<?, ?>) {
					// TODO handle Results.getRemaingCriteria
					ResolvedCriteria.Results<T> matches = ((ResolvedCriteria<?, T>) c).getMatches(crits, maxResults);
					if (matches == null || matches.getMatches() == null) {
						continue;
					}
					extra.addAll(matches.getMatches());
					if (matches.isComplete()) {
						crits.remove(c);
						complete = true;
						break;
					}
				}
			}
			if (!complete) {
				extra.addAll(this.all.getAll().realize());
			}

			results = new ListResults<T>(extra);
		} else {
			results = indexResults;
		}

		List<T> matchingEntries = L.l();
		int realSize = filterAndAccumulate(crits, results, maxResults, matchingEntries);
		if (matchingEntries.size() == realSize) {
			return new ListResults<T>(matchingEntries);
		}

		return new LimitedResults(matchingEntries, realSize, maxResults, criteria);
	}

	/**
	 * @param type for which an index is wanted != null
	 * @return the available Index or null if no index is available
	 */
	public Indexing<T> getIndexByType(final Class<?> type) {
		return this.indexByType.get(type);
	}

	/**
	 * Determines the index to use by
	 * <ul>
	 * <li>Searching an index for first criteria</li>
	 * <li>Search in its sub indexes for further criteria</li>
	 * <li>Stopping early if the criteria list gets empty.</li>
	 * </ul>
	 *
	 * @param criteria List as determined by {@link #getCriteriaBySpecificity(Collection)}g
	 * @return Index to use != null (may be {@link #all}.
	 */
	private Index<T> determineEffectiveIndex(final List<Criteria<?, T>> criteria) {
		Indexing<T> indexing = null;
		Criteria<?, T> firstCriteria = null;
		for (Criteria<?, T> crit : criteria) {
			indexing = this.indexByType.get(crit.getType());
			if (indexing != null) {
				firstCriteria = crit;
				break;
			}
		}
		Index<T> index = null;
		if (indexing != null) {
			index = indexing.getIndex(firstCriteria);
			if (index != null) {
				// we no longer need to check for this (except if conservative)
				if (!(index instanceof Conservative && ((Conservative) index).isConservativeEstimate())) {
					criteria.remove(firstCriteria);
				}

				if (criteria.isEmpty()) {
					// no further constraints: done
					Results<T> res = index.getAll();
					if (res != null) {
						return index;
					}
					// in case we hit an index which doesn't know specific results (i.e. too many), search in all
					criteria.add(firstCriteria);
					index = this.all;
				}

				subIndexLoop: while (true) {
					// try for sub indexing
					indexing = index.getSubIndexing();

					if (indexing == null) {
						break;
					}

					for (Criteria<?, T> crit : criteria) {
						if (indexing.getType().equals(crit.getType())) {
							Index<T> subIndex = indexing.getIndex(crit);
							if (subIndex != null) {
								// we no longer need to check for this (Except if conservative)
								if (!(subIndex instanceof Conservative && ((Conservative) subIndex).isConservativeEstimate())) {
								criteria.remove(crit);
								}

								if (criteria.isEmpty()) {
									// no further constraints: done
									Results<T> res = subIndex.getAll();
									if (res != null) {
										return subIndex;
									}
									// in case we hit an index which doesn't know specific results, search in last index
									criteria.add(crit);
									break subIndexLoop;
								}
								index = subIndex;
								continue subIndexLoop;
							}
						}
					}
					// not found
					break subIndexLoop;
				}
			}
		}
		return index;
	}

	private List<Criteria<?, T>> getCriteriaBySpecificity(final Collection<? extends Criteria<?, T>> criteria) {
		List<Criteria<?, T>> crits = new ArrayList<Criteria<?, T>>(criteria);
		Collections.sort(crits, new Comparator<Criteria<?, T>>() {
			@Override
			public int compare(final Criteria<?, T> c1, final Criteria<?, T> c2) {
				double pred1 = getPredictivityOf(c1.getType());
				double pred2 = getPredictivityOf(c2.getType());
				return pred1 < pred2 ? -1 : pred1 > pred2 ? 1 : 0;
			}
		});
		return crits;
	}

	/**
	 * Accumulate elements which match all the criteria in a List.
	 *
	 * @param <T> type of elements
	 * @param criteria to check; List != null; may be empty
	 * @param allEntries != null, may be empty
	 * @param maxResults maximum elements taken (in given order
	 * @param matchingEntriesAccu
	 * @return number actually accumulated
	 */
	public static <T> int filterAndAccumulate(final Collection<Criteria<?, T>> criteria, final Results<T> allEntries,
			final int maxResults, final Collection<T> matchingEntriesAccu) {
		int realSize = 0;
		candidateTest: for (T entry : allEntries) {
			for (Criteria<?, T> crit : criteria) {
				if (!crit.matches(entry)) {
					continue candidateTest;
				}
			}
			if (realSize < maxResults) {
				matchingEntriesAccu.add(entry);
			}
			realSize++;
		}
		return realSize;
	}

	@Override
	public int estimateSize(final Collection<? extends Criteria<?, T>> criteria) {
		double frac = 1.0;
		for (Criteria<?, T> c : criteria) {
			frac *= getPredictivityOf(c);
		}
		return (int) (frac * getTotalCandidates());
	}

	@Override
	public double getPredictivityOf(final Criteria<?, T> criteria) {
		Indexing<T> indexing = this.indexByType.get(criteria.getType());
		if (indexing == null) {
			return 0.0;
		}
		return indexing.getPredictivity(criteria);
	}

	@Override
	public double getPredictivityOf(final Class<?> criteriaClass) {
		Indexing<T> indexing = this.indexByType.get(criteriaClass);
		if (indexing == null) {
			return 1.0;
		}
		return indexing.getPredictivity(null);
	}

	@Override
	public int getTotalCandidates() {
		return this.all.size();
	}

	public void clear() {
		for (Indexing<T> indexing : this.indexByType.values()) {
			indexing.clear();
		}
		this.all.clear();
	}
}
