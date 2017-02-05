package de.zarncke.lib.cache;

import de.zarncke.lib.io.store.Store;
import de.zarncke.lib.time.JavaClock;
import de.zarncke.lib.time.Times;

/**
 * A data block which is backed by a Store and refetched every {@link #getMaxAgeMs()}.
 *
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
public class RefreshContent extends StoreContent {
	private static final long serialVersionUID = 1L;

	private long lastCaptured = Long.MIN_VALUE;

	public RefreshContent(final Store source) {
		super(source);
	}
	@Override
	protected void wasUpdated() {
		this.lastCaptured = JavaClock.getTheClock().getCurrentTimeMillis();
	}

	@Override
	protected boolean hasChanged() {
		return JavaClock.getTheClock().getCurrentTimeMillis() > this.lastCaptured + getMaxAgeMs();
	}

	@Override
	protected long getMaxAgeMs() {
		return Times.MILLIS_PER_MINUTE;
	}

	@Override
	public String toString() {
		return super.toString() + " refresh after " + getMaxAgeMs();
	}
}
