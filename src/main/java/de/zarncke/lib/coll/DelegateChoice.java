package de.zarncke.lib.coll;

import java.util.Collection;
import java.util.Collections;

import de.zarncke.lib.err.Warden;
import de.zarncke.lib.id.Ids.HasGid;

/**
 * A {@link MultipleChoice} using a backing Collection.
 * If the backing collection is empty this is interpreted to mean "undefined".
 *
 * @author Gunnar Zarncke
 * @param <T> of elements
 */
public class DelegateChoice<T extends HasGid<T>> extends AbstractChoice<T> {

	private final Collection<T> delegate;

	public DelegateChoice(final Collection<T> delegate) {
		this.delegate = delegate;
	}

	public Collection<T> getAll() {
		return Collections.unmodifiableCollection(this.delegate);
	}

	@Override
	public void setAll(final Collection<T> newList) {
		this.delegate.clear();
		this.delegate.addAll(newList);
	}

	@Override
	public void set(final T value) {
		this.delegate.clear();
		this.delegate.add(value);
	}

	public void add(final T value) {
		this.delegate.add(value);
	}

	public void remove(final T value) {
		this.delegate.remove(value);
	}

	public boolean isDefined() {
		return !this.delegate.isEmpty();
	}

	public void clear() {
		this.delegate.clear();
	}

	public MultipleChoice<T> copy() {
		throw Warden.spot(new UnsupportedOperationException("cannot share delegate"));
	}

}