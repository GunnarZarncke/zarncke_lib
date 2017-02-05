package de.zarncke.lib.cache;

import de.zarncke.lib.io.store.Store;

/**
 * A data block which is backed by a Store and refetched if the modification date changes.
 *
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
public class CheckModfiedContent extends StoreContent {
	private static final long serialVersionUID = 1L;

	public CheckModfiedContent(final Store source) {
		super(source);
	}

	private long lastModified = Long.MIN_VALUE;

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
		return super.toString();
	}
}
