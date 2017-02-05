/**
 *
 */
package de.zarncke.lib.index;


public interface Index<T> {
	/**
	 * Indicates that an Index result might be conservative.
	 */
	interface Conservative {
		/**
		 * @return true: the index result is only a conservative estimate, false: the index results is exact
		 */
		boolean isConservativeEstimate();
	}

	class DelegateIndex<T> implements Index<T>, Conservative {

		private final Index<T> delegate;

		public DelegateIndex(final Index<T> delegate) {
			this.delegate = delegate;
		}

		@Override
		public boolean isConservativeEstimate() {
			return delegate instanceof Conservative ? ((Conservative) delegate).isConservativeEstimate() : false;
		}

		@Override
		public Results<T> getAll() {
			return delegate.getAll();
		}

		@Override
		public Index<T> add(final T entry) {
			return delegate.add(entry);
		}

		@Override
		public int size() {
			return delegate.size();
		}

		@Override
		public Indexing<T> getSubIndexing() {
			return delegate.getSubIndexing();
		}

		@Override
		public void clear() {
			delegate.clear();
		}

		@Override
		public String toString() {
			return "delegating to " + delegate;
		}
	}

	/**
	 * Provide all values in this index (for further filtering).
	 * If this Index doesn't actually know the results (e.g. too many), it may return null to indicate so.
	 *
	 * @return all Results or null if this index doesn't know the results
	 */
	Results<T> getAll();

	Index<T> add(final T entry);

	/**
	 * Actual number of results.
	 *
	 * @return >=0
	 */
	int size();

	/**
	 * Indexing for the sub set of data available by getAll.
	 *
	 * @return null if the Index doesn't support sub indexing.
	 */
	Indexing<T> getSubIndexing();

	void clear();

}