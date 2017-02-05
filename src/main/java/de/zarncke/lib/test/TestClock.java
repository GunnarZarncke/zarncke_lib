package de.zarncke.lib.test;

import org.joda.time.ReadableDateTime;

import de.zarncke.lib.time.JavaClock;

public class TestClock extends JavaClock {
	private long simulatedMillis = 0;

	public TestClock(final ReadableDateTime testingTime) {
		this(testingTime.getMillis());
	}

	public TestClock(final long initialMillis) {
		this.simulatedMillis = initialMillis;
	}

	@Override
	public long getCurrentTimeMillis() {
		return this.simulatedMillis;
	}

	public void setSimulatedMillis(final long millis) {
		this.simulatedMillis = millis;
	}

	public void incrementSimulatedMillis(final long deltaMillis) {
		this.simulatedMillis += deltaMillis;
	}

}
