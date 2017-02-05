package de.zarncke.lib.coll;

import java.util.ArrayList;
import java.util.Collection;

import de.zarncke.lib.id.Ids.HasGid;

/**
 * A {@link MultipleChoice} using an internal Collection.
 *
 * @author Gunnar Zarncke
 * @param <T> of elements
 */
public class ListChoice<T extends HasGid<T>> extends AbstractChoice<T> {

	private Collection<T> list;

	public ListChoice() {
		this.list = null;
	}

	private ListChoice(final ListChoice<T> l) {
		this.list = l.list == null ? null : new ArrayList<T>(l.list);
	}

	public Collection<T> getAll() {
		return this.list;
	}

	@Override
	public void setAll(final Collection<T> newList) {
		this.list = newList;
	}

	@Override
	public void set(final T value) {
		this.list = L.l(value);
	}

	public void add(final T value) {
		if (this.list == null) {
			set(value);
		} else {
			this.list.add(value);
		}
	}

	public void remove(final T value) {
		if (this.list != null) {
			this.list.remove(value);
		}
	}


	public boolean isDefined() {
		return this.list != null;
	}

	public void clear() {
		this.list = null;
	}

	public MultipleChoice<T> copy() {
		return new ListChoice<T>(this);
	}

}