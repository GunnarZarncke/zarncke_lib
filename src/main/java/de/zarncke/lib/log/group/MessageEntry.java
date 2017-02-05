package de.zarncke.lib.log.group;

import de.zarncke.lib.err.ExceptionUtil;
import de.zarncke.lib.sys.Health;
import de.zarncke.lib.util.Chars;

/**
 * An {@link Entry} for normal messages.
 */
class MessageEntry extends Entry {
	private static final long serialVersionUID = 1L;

	private CharSequence issue;

	private final LogCaller caller;

	public MessageEntry(final LogCaller caller, final CharSequence issue) {
		super(caller);
		this.caller = caller;
		this.issue = issue;
	}

	@Override
	public void addTo(final StringBuilder sb) {
		super.addTo(sb);
		sb.append(this.issue).append("\n");
		if (this.caller.throwable != null) {
			sb.append("reported by ");
			sb.append(ExceptionUtil.getStackTrace(this.caller.throwable));
		}
	}

	@Override
	public void addToShort(final StringBuilder sb) {
		super.addTo(sb);
		sb.append(Chars.summarize(this.issue, SUMMARY_DEFAULT_LENGTH)).append("\n");
	}

	@Override
	public CharSequence getShortMessage() {
		return this.issue;
	}

	@Override
	public String toString() {
		return this.issue != null ? Chars.summarize(this.issue, GroupingLog.TO_STRING_SUMMARY_TEXT_LENGTH).toString() : "";
	}

	@Override
	public Health getEstimatedSeverity() {
		return Health.WARNINGS;
	}

	@Override
	void truncate() {
		super.truncate();
		this.issue = Chars.summarize(this.issue, GroupingLog.TRUNCATE_SUMMARY_TEXT_LIMIT);
	}
}