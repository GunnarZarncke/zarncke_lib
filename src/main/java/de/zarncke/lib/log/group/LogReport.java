package de.zarncke.lib.log.group;

import java.util.ArrayList;
import java.util.List;

import de.zarncke.lib.log.ExcerptableReport;
import de.zarncke.lib.log.Report;
import de.zarncke.lib.log.Report.Caller;
import de.zarncke.lib.sys.Health;
import de.zarncke.lib.util.Chars;

/**
 * A complete report about all issues.
 */
class LogReport {
	private final GroupingLog groupingLog;
	private static final int LOG_ENTRY_OVERHEAD_BYTES_ESTIMATE = 64;
	private int totalCount;
	private int nextBatchSize = 1;
	private long firstOccurence = -1;
	private long lastOccurence;
	private long lastReport = -1;
	private List<Entry> entries = new ArrayList<Entry>();
	private final Caller caller;

	private volatile Health maxEncounteredSeverity = Health.VIRGIN;
	private volatile int currentCount;

	LogReport(final GroupingLog groupingLog, final Caller caller) {
		this.groupingLog = groupingLog;
		this.caller = caller;
	}

	int size() {
		return this.entries.size();
	}

	void report(final LogCaller reporter, final LogCaller throwableCaller) {
		add(new ThrowableEntry(reporter, throwableCaller));
	}

	private void add(final Entry entry) {
		Health s = entry.getEstimatedSeverity();

		synchronized (this) {
			if (s.ordinal() > this.maxEncounteredSeverity.ordinal()) {
				this.maxEncounteredSeverity = s;
			}
			// sampling is always done non-truncated
			if (this.currentCount % GroupingLog.SAMPLING_FRACTION == 0) {
				this.entries.add(entry);
			} else if (this.currentCount < GroupingLog.DISCARD_LIMIT || s.ordinal() >= Health.ERRORS.ordinal()) {
				if (this.currentCount > GroupingLog.TRUNCATE_LIMIT) {
					entry.truncate();
				}
				this.entries.add(entry);
			}
			this.currentCount++;
			this.totalCount++;
		}
		check(entry);
	}

	void report(final LogCaller reporter, final CharSequence issue) {
		add(new MessageEntry(reporter, issue));
	}

	public void reportExplicit(final Caller reporter, final CharSequence issue, final long simulatedTime,
			final Health severity) {
		add(new Entry(reporter, simulatedTime) {
			private static final long serialVersionUID = 1L;

			@Override
			public void addTo(final StringBuilder sb) {
				super.addTo(sb);
				sb.append(issue).append("\n");
			}

			@Override
			public void addToShort(final StringBuilder sb) {
				super.addToShort(sb);
				sb.append(issue).append("\n");
			}

			@Override
			public Health getEstimatedSeverity() {
				return severity;
			}

			@Override
			public CharSequence getShortMessage() {
				return issue;
			}
		});
	}

	public void report(final LogCaller reporter, final Object debugObject) {
		add(new ObjectEntry(reporter, debugObject));
	}

	void check(final Entry entry) {
		this.lastOccurence = System.currentTimeMillis();
		boolean rescale = false;
		if (this.lastOccurence > this.lastReport + this.groupingLog.timeMillisBetweenRescale) {
			this.nextBatchSize = (int) (this.nextBatchSize / this.groupingLog.reportFactor);
			if (this.nextBatchSize <= 0) {
				this.nextBatchSize = 1;
			} else {
				rescale = true;
			}
		}
		if (this.firstOccurence < 0) {
			this.firstOccurence = this.lastOccurence;
		}
		if (this.currentCount >= this.nextBatchSize) {
			sendReport(rescale ? Report.Type.PERIODICAL : Report.Type.REGULAR);
		} else {
			this.groupingLog.reportListener.notifyOfLog(new Report() {
				@Override
				public long firstOccurenceMillis() {
					return LogReport.this.firstOccurence;
				}

				@Override
				public long lastOccurenceMillis() {
					return LogReport.this.lastOccurence;
				}

				@Override
				public int numberOfOccurences() {
					return 1;
				}

				@Override
				public Caller getCaller() {
					return LogReport.this.caller;
				}

				@Override
				public String getSummary() {
					return entry.getShortMessage().toString() + " " + LogReport.this.caller.getCallerKey();
				}

				@Override
				public CharSequence getFullReport() {
					return entry.getShortMessage();
				}

				@Override
				public Health getEstimatedSeverity() {
					return LogReport.this.maxEncounteredSeverity;
				}

				@Override
				public Type getType() {
					return Type.SINGLE;
				}
			});
		}
	}

	void sendReport(final Report.Type type) {
		if (this.currentCount == 0) {
			return;
		}

		// TODO should this be done async?
		// TODO small race condition
		// capture state
		this.lastReport = this.lastOccurence;
		final String summary;
		final List<Entry> copyEntries;
		final Health maxSeverity;
		final int reportedCount;
		final long lastOcc;
		synchronized (this) {
			summary = summary();
			copyEntries = this.entries;
			maxSeverity = this.maxEncounteredSeverity;
			reportedCount = this.currentCount;

			// clear state
			this.currentCount = 0;
			this.entries = new ArrayList<Entry>();
			this.maxEncounteredSeverity = Health.VIRGIN;
			lastOcc = this.lastOccurence;
			this.nextBatchSize = (int) (this.nextBatchSize * this.groupingLog.reportFactor);
			if (this.nextBatchSize > this.groupingLog.reportLimit) {
				this.nextBatchSize = this.groupingLog.reportLimit;
			}
		}

		// send on
		this.groupingLog.reportListener.notifyOfReport(new ExcerptableReport() {
			@Override
			public long firstOccurenceMillis() {
				return LogReport.this.firstOccurence;
			}

			@Override
			public Caller getCaller() {
				return LogReport.this.caller;
			}

			@Override
			public CharSequence getFullReport() {
				StringBuilder sb = new StringBuilder();
				sb.append(maxSeverity.name()).append(": ").append(LogReport.this.caller.getCallerKey()).append("\n");
				long realTimeOfFirstReportedEntry = 0; // 0 value will never be used
				int reportedDetails = copyEntries.size();
				if (reportedDetails > 0) {
					realTimeOfFirstReportedEntry = copyEntries.get(0).realTime;
				}

				// limit reported incidents per time
				if (type == Type.REGULAR && reportedDetails > 0) {
					long reportedTimeSpanMs = System.currentTimeMillis() - realTimeOfFirstReportedEntry;
					if (reportedTimeSpanMs < GroupingLog.MAX_MILLIS_PER_REPORTED_ENTRY * reportedDetails) {
						reportedDetails = Math.max(1, Math.min(reportedCount,
								(int) (reportedTimeSpanMs / GroupingLog.MAX_MILLIS_PER_REPORTED_ENTRY)));
					}
					if (LogReport.this.totalCount > GroupingLog.TOTAL_COUNT_TO_TRUNCATE_ALL_RESULTs
							&& reportedDetails > GroupingLog.TRUNCATED_SIZE) {
						reportedDetails = GroupingLog.TRUNCATED_SIZE;
					}
				}

				sb.append(String.valueOf(LogReport.this.totalCount)).append(" occurences (").append(reportedCount)
						.append(" reported in this report, ").append(reportedDetails).append(" with details)\n");
				sb.append("first occurence at ").append(GroupingLog.FORMATTER.print(LogReport.this.firstOccurence))
						.append("\n");
				if (reportedDetails > 0) {
					sb.append("first reported occurence at ")
							.append(GroupingLog.FORMATTER.print(realTimeOfFirstReportedEntry)).append("\n");
				}
				sb.append("last occurence at ").append(GroupingLog.FORMATTER.print(LogReport.this.lastOccurence))
						.append("\n");

				int n = 0;
				for (Entry entry : copyEntries) {
					if (n >= reportedDetails) {
						sb.append(copyEntries.size() - n).append(" further entries truncated\n");
						break;
					}
					entry.addTo(sb);
					n++;
				}
				return sb;
			}

			@Override
			public CharSequence getExcerpt() {
				StringBuilder sb = new StringBuilder();
				sb.append(maxSeverity.name()).append(": ").append(LogReport.this.caller.getCallerKey()).append("\n");
				sb.append(LogReport.this.totalCount + " occurences (last at ")
						.append(GroupingLog.FORMATTER.print(LogReport.this.lastOccurence)).append(")\n");
				int size = copyEntries.size();
				if (size > 0) {
					Entry entry = copyEntries.get(size - 1);
					entry.addToShort(sb);
				}
				return sb;
			}

			@Override
			public String getSummary() {
				return summary;
			}

			@Override
			public long lastOccurenceMillis() {
				return lastOcc;
			}

			@Override
			public int numberOfOccurences() {
				return copyEntries.size();
			}

			@Override
			public String toString() {
				return summary;
			}

			@Override
			public Health getEstimatedSeverity() {
				return maxSeverity;
			}

			@Override
			public Type getType() {
				return type;
			}
		});
	}

	String summary() {
		if (this.entries.isEmpty()) {
			return this.currentCount + "*" + " (all entries discarded)."; // NOPMD text
		}
		CharSequence msg = this.entries.get(0).getShortMessage();
		CharSequence smsg = Chars.summarize(msg, GroupingLog.SUMMARY_TEXT_LENGTH);
		return this.currentCount != this.entries.size() ? this.currentCount + "* " + smsg : this.currentCount + "/"
				+ this.entries.size() + "* " + smsg;
	}

	@Override
	public String toString() {
		return summary() + " at " + this.caller.getCallerKey();
	}

	public int estimateSize() {
		if (this.entries.size() == 0) {
			return LOG_ENTRY_OVERHEAD_BYTES_ESTIMATE;
		}
		return this.entries.get(0).estimateSize() * this.entries.size();
	}

}