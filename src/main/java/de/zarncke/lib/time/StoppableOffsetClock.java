/**
 *
 */
package de.zarncke.lib.time;


/**
 * A {@link Clock} which is based on another {@link Clock} (i.e. runs at the same speed as the other clock) but can be stopped,
 * continued and
 * set as needed to simulate time.
 *
 * @author Gunnar Zarncke
 */
public final class StoppableOffsetClock extends JavaClock {

	private final Clock reference;
	// if !stopped: offset to real time
	private long offsetMillis = 0;
	// if stopped: current simulated time
	private long referenceMillis;
	private boolean stopped;

	public StoppableOffsetClock(final Clock reference, final boolean stopped) {
		this.reference = reference;
		this.stopped = stopped;
		this.referenceMillis = reference.getCurrentTimeMillis();
	}

	public StoppableOffsetClock(final Clock reference, final long initialTimeMillis) {
		this.reference = reference;
		this.stopped = true;
		this.referenceMillis = initialTimeMillis;
	}

	public boolean isStopped() {
		return this.stopped;
	}

	/**
	 * Only defined if not stopped.
	 *
	 * @return ms
	 */
	public long getOffsetMillis() {
		return this.offsetMillis;
	}

	/**
	 * Only defined if stopped.
	 *
	 * @return ms
	 */
	public long getReferenceMillis() {
		return this.referenceMillis;
	}

	/**
	 * Allows to stop the clock (and stay at the current time) and continue it (which changes the offset).
	 *
	 * @param stopped true: the clock is halted
	 */
	public void setStopped(final boolean stopped) {
		if (this.stopped && !stopped) {
			// continue at last reference
			setOffsetMillis(this.referenceMillis - this.reference.getCurrentTimeMillis());
		}
		if (!this.stopped && stopped) {
			// stop at currently simulated time
			setReferenceMillis(getCurrentTimeMillis());
		}
		this.stopped = stopped;
	}

	@Override
	public long getCurrentTimeMillis() {
		if (this.stopped) {
			return this.referenceMillis;
		}
		return this.reference.getCurrentTimeMillis() + this.offsetMillis;
	}

	public void setOffsetMillis(final long millis) {
		this.offsetMillis = millis;
		this.referenceMillis = this.reference.getCurrentTimeMillis() + millis;
	}

	public void setReferenceMillis(final long newRefMillis) {
		this.offsetMillis = newRefMillis - this.reference.getCurrentTimeMillis();
		this.referenceMillis = newRefMillis;
	}

}