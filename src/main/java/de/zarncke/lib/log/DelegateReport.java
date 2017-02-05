package de.zarncke.lib.log;

import de.zarncke.lib.sys.Health;

/**
 * A Report which delegates all calls to another Report. Intended for extension.
 *
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
public class DelegateReport implements Report {
	private final Report report;

	public DelegateReport(final Report report) {
		this.report = report;
	}

	@Override
	public long firstOccurenceMillis() {
		return this.report.firstOccurenceMillis();
	}

	@Override
	public long lastOccurenceMillis() {
		return this.report.lastOccurenceMillis();
	}

	@Override
	public int numberOfOccurences() {
		return this.report.numberOfOccurences();
	}

	@Override
	public Caller getCaller() {
		return this.report.getCaller();
	}

	@Override
	public String getSummary() {
		return this.report.getSummary();
	}

	@Override
	public CharSequence getFullReport() {
		return this.report.getFullReport();
	}

	@Override
	public Health getEstimatedSeverity() {
		return this.report.getEstimatedSeverity();
	}

	@Override
	public Type getType() {
		return this.report.getType();
	}
}
