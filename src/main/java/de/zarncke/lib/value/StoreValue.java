package de.zarncke.lib.value;

import java.io.Serializable;

import com.google.common.base.Function;

import de.zarncke.lib.io.store.Store;

/**
 * A {@link Value} which is fetched once from a Store.
 *
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 * @param <T> result type
 */
public class StoreValue<T> implements Value<T>, Serializable {
	private static final long serialVersionUID = 1L;

	protected final Store source;
	private transient T value;
	private final Function<Store, T> converter;

	public StoreValue(final Store source, final Function<Store, T> converter) {
		this.source = source;
		this.converter = converter;
	}

	@Override
	public T get() {
		if (this.value == null || hasChanged()) {
			this.value = this.converter.apply(this.source);
			wasUpdated();
		}
		return this.value;
	}

	protected void wasUpdated() {
		// nop
	}

	protected boolean hasChanged() {
		return false;
	}

	@Override
	public String toString() {
		return this.source.toString();
	}
}
