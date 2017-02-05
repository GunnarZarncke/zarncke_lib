/**
 *
 */
package de.zarncke.lib.index;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import de.zarncke.lib.coll.L;

/**
 * Simple {@link Results}.
 *
 * @author Gunnar Zarncke
 * @param <T> type
 */
public class ListResults<T> implements Results<T> {
	protected List<T> entries;

	public ListResults(final Collection<T> entries) {
		this.entries = L.<T> copy(entries);
	}

	public List<T> realize() {
		return this.entries;
	}

	public int size() {
		return this.entries.size();
	}

	public Iterator<T> iterator() {
		return this.entries.iterator();
	}

	public int available() {
		return this.entries.size();
	}

	public int readTo(final int position) {
		// already there
		return available();
	}

	@Override
	public String toString() {
		return this.entries.toString();
	}
}