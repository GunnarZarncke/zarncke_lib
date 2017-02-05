package de.zarncke.lib.coll;

import java.io.Serializable;
import java.util.Collection;

import de.zarncke.lib.err.Warden;
import de.zarncke.lib.id.Gid;
import de.zarncke.lib.id.Ids.HasGid;
import de.zarncke.lib.id.Resolving;

/**
 * A {@link MultipleChoice} using a backing Collection of Ids.
 *
 * @author Gunnar Zarncke
 * @param <T> of elements
 */
public class ResolvedDelegateChoice<T extends HasGid<T>> extends AbstractChoice<T> implements Serializable {
	private static final long serialVersionUID = 1L;
	private final Collection<Gid<T>> delegate;
	private transient Collection<T> results;

	public ResolvedDelegateChoice(final Collection<Gid<T>> delegate) {
		this.delegate = delegate;
	}

	public Collection<T> getAll() {
		if (this.results == null) {
			this.results = Resolving.resolve(this.delegate, true);
		}
		return this.results;
	}

	@Override
	public void setAll(final Collection<T> newList) {
		this.results = newList;
		this.delegate.clear();
		this.delegate.addAll(Resolving.toPreciseIds(newList));
	}

	@Override
	public void set(final T value) {
		this.results = null;
		this.delegate.clear();
		this.delegate.add(value.getId());
	}

	public void add(final T value) {
		this.results = null;
		this.delegate.add(value.getId());
	}

	public void remove(final T value) {
		this.results = null;
		this.delegate.remove(value.getId());
	}

	@Override
	public void toggle(final T value) {
		this.results = null;
		Gid<T> id = value.getId();
		if (this.delegate.contains(id)) {
			this.delegate.remove(id);
		} else {
			this.delegate.add(id);
		}
	}


	public boolean isDefined() {
		return !this.delegate.isEmpty();
	}

	public void clear() {
		this.delegate.clear();
		this.results = null;
	}

	public MultipleChoice<T> copy() {
		throw Warden.spot(new UnsupportedOperationException("cannot share delegate"));
	}

}