/**
 *
 */
package de.zarncke.lib.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ListIndex<T> implements Index<T> {

	private final List<T> entries = new ArrayList<T>();

	public ListIndex(final Collection<T> values) {
		this.entries.addAll(values);
	}

	public ListIndex() {
		// initially empty
	}

	public Index<T> add(final T entry) {
		this.entries.add(entry);
		return this;
	}

	public Results<T> getAll() {
		return new ListResults<T>(this.entries);
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

	@Override
	public String toString() {
		return this.entries.toString();
	}
}