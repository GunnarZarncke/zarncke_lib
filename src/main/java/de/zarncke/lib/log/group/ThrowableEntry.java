package de.zarncke.lib.log.group;

import de.zarncke.lib.err.ExceptionUtil;
import de.zarncke.lib.sys.Health;

/**
 * An entry for {@link Exception}s.
 */
class ThrowableEntry extends Entry {
	private static final long serialVersionUID = 1L;
	private final LogCaller caller;
	private final LogCaller cause;

	public ThrowableEntry(final LogCaller caller, final LogCaller cause) {
		super(caller);
		this.caller = caller;
		this.cause = cause;
	}

	@Override
	public void addTo(final StringBuilder sb) {
		super.addTo(sb);
		if (this.cause != null) {
			sb.append("encountered ");
			if (this.cause.throwable != null) {
				sb.append(ExceptionUtil.getStackTrace(this.cause.throwable));
			} else {
				if (this.cause.stackTraceElement != null) {
					sb.append(this.cause.toString());
				}
				if (this.cause.qualifier != null) {
					sb.append("qualifier=").append(this.cause.qualifier);
				}
			}
			if (this.caller.throwable != null && this.caller.throwable != this.cause.throwable) {
				sb.append("reported by ");
				sb.append(ExceptionUtil.getStackTrace(this.caller.throwable));
			}
		}
	}

	@Override
	public void addToShort(final StringBuilder sb) {
		super.addToShort(sb);
		if (this.cause != null) {
			if (this.cause.throwable != null) {
				sb.append(ExceptionUtil.extractReasons(this.cause.throwable));
			} else {
				if (this.cause.stackTraceElement != null) {
					sb.append(this.cause.toString());
				}
				if (this.cause.qualifier != null) {
					sb.append("qualifier=").append(this.cause.qualifier);
				}
			}
		}
	}

	@Override
	public CharSequence getShortMessage() {
		if (this.cause == null) {
			return "unknown Throwable";
		}
		if (this.cause.throwable == null) {
			return "truncated " + this.cause.getCallerKey();
		}
		String msg = this.cause.throwable.getMessage();
		if (msg != null) {
			return msg;
		}
		return this.cause.throwable.getClass().getName();
	}

	@Override
	public Health getEstimatedSeverity() {
		return Health.ERRORS;
	}

	@Override
	void truncate() {
		super.truncate();
		if (this.cause != null) {
			this.cause.truncate();
		}
	}

	@Override
	public int estimateSize() {
		int l = super.estimateSize();
		return l;
	}
}