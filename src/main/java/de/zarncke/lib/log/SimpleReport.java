package de.zarncke.lib.log;

import de.zarncke.lib.sys.Health;

/**
 * A {@link Report} which holds a fixed String message.
 *
 * @author Gunnar Zarncke
 */
public class SimpleReport implements Report {
	private final String summary;
	long time = System.currentTimeMillis();

	public SimpleReport(final String summary) {
		this.summary = summary;
	}

	@Override
	public int numberOfOccurences() {
		return 1;
	}

	@Override
	public long lastOccurenceMillis() {
		return this.time;
	}

	@Override
	public String getSummary() {
			return this.summary;
	}

	@Override
	public CharSequence getFullReport() {
		return this.summary;
	}

	@Override
	public Health getEstimatedSeverity() {
			return Health.DISCARDABLE;
	}

	@Override
	public Caller getCaller() {
		return null;
	}

	@Override
	public long firstOccurenceMillis() {
		return 0;
	}

	@Override
	public String toString() {
		return getSummary();
	}

	@Override
	public Type getType() {
		return Type.IMMEDIATE;
	}
}