/**
 *
 */
package de.zarncke.lib.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

public class SortedIndex<T> implements Index<T> {

	private final SortedSet<T> entries;

	public SortedIndex(final Collection<T> values, final Comparator<T> comparator) {
		this(comparator);
		this.entries.addAll(values);
	}

	public SortedIndex(final Collection<T> values) {
		this();
		this.entries.addAll(values);
	}

	protected SortedSet<T> getEntries() {
		return this.entries;
	}

	public SortedIndex() {
		this.entries = new TreeSet<T>();
	}

	public SortedIndex(final Comparator<T> comparator) {
		this.entries = new TreeSet<T>(comparator);
	}

	public Index<T> add(final T entry) {
		this.entries.add(entry);
		return this;
	}

	public Results<T> getAll() {
		return new ListResults<T>(new ArrayList<T>(this.entries));
	}

	public int size() {
		return this.entries.size();
	}

	public Indexing<T> getSubIndexing() {
		return null;
	}

	@Override
	public void clear() {
		this.entries.clear();
	}

}