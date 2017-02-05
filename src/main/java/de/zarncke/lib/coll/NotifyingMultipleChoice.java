package de.zarncke.lib.coll;

import java.util.Collection;
import java.util.Iterator;

import com.google.common.base.Function;

/**
 * Decorates a {@link MultipleChoice} with a listener on any modifying change.
 * Note: {@link #copy()} returns an undecorated copy.
 * 
 * @author Gunnar Zarncke
 * @param <T> element type
 */
public class NotifyingMultipleChoice<T> implements MultipleChoice<T> {
	private final MultipleChoice<T> delegate;
	private final Function<MultipleChoice<T>, Void> listener;

	public NotifyingMultipleChoice(final MultipleChoice<T> delegate, final Function<MultipleChoice<T>, Void> listener) {
		this.delegate = delegate;
		this.listener = listener;
	}

	public Iterator<T> iterator() {
		return this.delegate.iterator();
	}

	public boolean match(final Collection<T> values) {
		return this.delegate.match(values);
	}

	public void clear() {
		this.delegate.clear();
		this.listener.apply(this);
	}

	public boolean isDefined() {
		return this.delegate.isDefined();
	}

	public Collection<T> getAll() {
		return this.delegate.getAll();
	}

	public void setAll(final Collection<T> value) {
		this.delegate.setAll(value);
		this.listener.apply(this);
	}

	public void set(final T value) {
		this.delegate.set(value);
		this.listener.apply(this);
	}

	public void add(final T value) {
		this.delegate.add(value);
		this.listener.apply(this);
	}

	public void remove(final T value) {
		this.delegate.remove(value);
		this.listener.apply(this);
	}

	public void toggle(final T value) {
		this.delegate.toggle(value);
		this.listener.apply(this);
	}

	public void select(final T value, final Boolean enabled) {
		this.delegate.select(value, enabled);
		this.listener.apply(this);
	}

	public MultipleChoice<T> copy() {
		return this.delegate.copy();
	}

	@Override
	public String toString() {
		return "delegated " + this.delegate.toString();
	}
}