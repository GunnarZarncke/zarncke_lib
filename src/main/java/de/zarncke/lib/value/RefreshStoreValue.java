package de.zarncke.lib.value;

import com.google.common.base.Function;

import de.zarncke.lib.io.store.Store;

/**
 * A {@link Value} which is refetched from a Store when last modified changes.
 *
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 * @param <T> result type
 */
public class RefreshStoreValue<T> extends StoreValue<T> {
	private static final long serialVersionUID = 1L;
	private long lastModified;

	public RefreshStoreValue(final Store source, final Function<Store, T> converter) {
		super(source, converter);
	}

	@Override
	protected void wasUpdated() {
		this.lastModified = this.source.getLastModified();
	}

	@Override
	protected boolean hasChanged() {
		return this.source.getLastModified() != this.lastModified;
	}

	@Override
	public String toString() {
		return this.source.toString();
	}
}
