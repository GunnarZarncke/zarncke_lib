package de.zarncke.lib.cache;

import java.io.IOException;
import java.io.Serializable;

import de.zarncke.lib.data.HasData;
import de.zarncke.lib.io.store.Store;
import de.zarncke.lib.region.Region;
import de.zarncke.lib.time.Times;

/**
 * A data block which is fetched once from a Store.
 * 
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
public class StoreContent implements HasData, Serializable {
	private static final long serialVersionUID = 1L;

	protected final Store source;
	private transient Region region;

	public StoreContent(final Store source) {
		this.source = source;
	}

	@Override
	public Region asRegion() throws IOException {
		if (this.region == null || hasChanged()) {
			this.region = this.source.asRegion();
			wasUpdated();
		}
		return this.region;
	}

	protected void wasUpdated() {
		// nop
	}

	protected boolean hasChanged() {
		return false;
	}

	protected long getMaxAgeMs() {
		return Times.MILLIS_PER_MINUTE;
	}

	@Override
	public String toString() {
		return this.source.toString();
	}
}
