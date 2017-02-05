package de.zarncke.lib.index.crit;

import java.util.Collection;

import javax.annotation.Nonnull;

import org.joda.time.DateTime;

import de.zarncke.lib.coll.L;

/**
 * Represents constraints on elements of a specific type <strong>which can be queried for results directly</strong>.
 * Specialized for a specific key type.
 *
 * @author Gunnar Zarncke
 * @param <K> key type
 * @param <T> object type
 */
public interface ResolvedCriteria<K, T> extends Criteria<K, T> {

	class Results<T> {
		private final boolean complete;
		private final Collection<T> matches;
		private final Collection<Throwable> problems;
		private final DateTime validUntil;

		public Results(final Collection<T> matches, final boolean complete) {
			this(matches, complete, L.<Throwable> e());
		}

		public Results(final Collection<T> matches, final boolean complete, final Collection<Throwable> problems) {
			this(matches, complete, problems, null);
		}

		public Results(final Collection<T> matches, final boolean complete, final Collection<Throwable> problems,
				final DateTime validUntil) {
			this.matches = matches;
			this.complete = complete;
			this.problems = problems;
			this.validUntil = validUntil;
		}

		/**
		 * @return Ts, may be empty; != null
		 */
		@Nonnull
		public Collection<T> getMatches() {
			return matches;
		}

		/**
		 * This allows a criteria processor (e.g. MultiIndex) to avoid querying indexes or filtering further if the
		 * results are already fully determined by the {@link ResolvedCriteria}.
		 *
		 * @return true: the returned matches are all there are; false: there may be more matches not explicitly given
		 * (not all criteria are "consumed")
		 */
		public boolean isComplete() {
			return complete;
		}

		public Collection<Throwable> getProblems() {
			return problems;
		}

		public DateTime validUntil() {
			return this.validUntil;
		}
	}

	/**
	 * Get matching candidates.
	 * This list of matches need not be complete if {@link Results#isComplete} is false.
	 *
	 * @param remainingCriteria additional sub-filter criteria which may or may not narrow the results
	 * @param maxResultsForAllCriteria optional constraint on the total expected results provided all remainingCriteria
	 * are tested (otherwise the limit is enforced by caller)
	 * @return {@link Results}
	 */
	@Nonnull
	Results<T> getMatches(Collection<? extends Criteria<?, T>> remainingCriteria, int maxResultsForAllCriteria);

}
