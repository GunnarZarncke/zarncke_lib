package de.zarncke.lib.log.group;

import java.io.Serializable;

import de.zarncke.lib.log.Report;
import de.zarncke.lib.sys.Health;
import de.zarncke.lib.time.JavaClock;
import de.zarncke.lib.util.ObjectTool;

/**
 * One truncatable entry in a {@link LogReport} about a log call.
 */
abstract class Entry implements Serializable {
	private static final long serialVersionUID = 1L;

	public static final int SUMMARY_DEFAULT_LENGTH = 300;

	final long realTime;
	private final long simulatedTime;
	protected Report.Caller reporter;

	public Entry(final Report.Caller caller, final long simulatedTime) {
		this.reporter = caller;
		this.realTime = System.currentTimeMillis();
		this.simulatedTime = simulatedTime;
	}

	public Entry(final Report.Caller caller) {
		this(caller, JavaClock.getTheClock().getCurrentTimeMillis());
	}

	public void addTo(final StringBuilder sb) {
		sb.append(this.reporter.getSourceLocation()).append("\n");
		sb.append("occurence at ").append(GroupingLog.FORMATTER.print(this.realTime));
		if (this.realTime != this.simulatedTime) {
			sb.append(" (application time ").append(GroupingLog.FORMATTER.print(this.simulatedTime)).append(")");
		}
		sb.append("\n");
	}

	public void addToShort(final StringBuilder sb) {
		sb.append(this.reporter.getSourceLocation()).append("\n");
		sb.append("occurence at ").append(GroupingLog.FORMATTER.print(this.realTime));
		if (this.realTime != this.simulatedTime) {
			sb.append(" (application time ").append(GroupingLog.FORMATTER.print(this.simulatedTime)).append(")");
		}
		sb.append("\n");
	}

	public int estimateSize() {
		return ObjectTool.estimateSize(this);
	}

	public abstract CharSequence getShortMessage();

	public abstract Health getEstimatedSeverity();

	@Override
	public String toString() {
		return getShortMessage().toString();
	}

	void truncate() {
		if (this.reporter instanceof LogCaller) {
			((LogCaller) this.reporter).truncate();
		}
	}
}