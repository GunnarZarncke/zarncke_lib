/**
 *
 */
package de.zarncke.lib.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Composite index merging multiple child indexes.
 *
 * @author Gunnar Zarncke
 * @param <T> element type
 */
public class CombinedIndex<T> implements Index<T>, Index.Conservative {

	private final Collection<? extends Index<T>> indizes;
	private final boolean removeDuplicates;

	public CombinedIndex(final Collection<? extends Index<T>> indizes) {
		this(indizes, false);
	}

	public CombinedIndex(final Collection<? extends Index<T>> indizes, final boolean removeDuplicates) {
		this.indizes = indizes;
		this.removeDuplicates = removeDuplicates;
	}

	public Index<T> add(final T entry) {
		for (Index<T> idx : this.indizes) {
			idx.add(entry);
		}
		return this;
	}

	public Results<T> getAll() {
		final Collection<Results<T>> results = new ArrayList<Results<T>>(this.indizes.size());
		for (Index<T> idx : this.indizes) {
			results.add(idx.getAll());
		}
		return new Results<T>() {

			public int available() {
				int a = 0;
				for (Results<T> res : results) {
					a += res.available();
				}
				return a;
			}

			public int readTo(final int position) {
				int av = 0;
				int p = 0;
				for (Results<T> res : results) {
					av += res.available();
					if (position < av) {
						return av;
					}
					if (av != res.size()) {
						// TODO we might request less
						res.readTo(res.size());
					}
					p += res.size();
					av = p;
				}

				return p;
			}

			public List<T> realize() {
				if (!CombinedIndex.this.removeDuplicates) {
					List<T> all = new ArrayList<T>(available());
					for (Results<T> res : results) {
						all.addAll(res.realize());
					}
					return all;
				}
				Collection<T> all = new HashSet<T>(available() * 2);
				for (Results<T> res : results) {
					all.addAll(res.realize());
				}
				return new ArrayList<T>(all);
			}

			public int size() {
				int s = 0;
				for (Results<T> res : results) {
					s += res.size();
				}
				return s;
			}

			public Iterator<T> iterator() {
				// TODO use individual iterators
				return realize().iterator();
			}

			@Override
			public String toString() {
				return results.toString();
			}
		};
	}

	public Indexing<T> getSubIndexing() {
		return null;
	}

	public int size() {
		int s = 0;
		for (Index<T> idx : this.indizes) {
			s += idx.size();
		}
		return s;
	}

	@Override
	public void clear() {
		for (Index<T> idx : this.indizes) {
			idx.clear();
		}
	}

	@Override
	public String toString() {
		return size() + " in " + this.indizes.toString();
	}

	@Override
	public boolean isConservativeEstimate() {
		for (Index<?> idx : this.indizes) {
			if (idx instanceof Conservative && ((Conservative) idx).isConservativeEstimate()) {
				return true;
			}
		}
		return false;
	}

}