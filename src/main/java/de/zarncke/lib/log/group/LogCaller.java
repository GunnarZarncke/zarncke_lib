package de.zarncke.lib.log.group;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import de.zarncke.lib.log.Report;
import de.zarncke.lib.log.Report.Caller;

/**
 * Captures a log call location and cause.
 */
final class LogCaller implements Caller, Serializable {

	private static final long serialVersionUID = 1L;
	final StackTraceElement stackTraceElement;
	Throwable throwable;
	final String qualifier;
	Object context;

	public LogCaller(final StackTraceElement stackTraceElement, final Throwable throwable, final String qualifier,
			final Object context) {
		this.stackTraceElement = stackTraceElement;
		this.throwable = throwable;
		this.qualifier = qualifier;
		this.context = context;
	}

	@Override
	public Report.Caller getCause() {
		if (this.throwable == null || this.throwable.getCause() == this.throwable) {
			return null;
		}
		return GroupingLog.determineCaller(this.throwable.getCause(), null, this.context);
	}

	@Override
	public String getCallerKey() {
		return this.context == null ? getSeverityKey() : getSeverityKey() + ":" + this.context.toString();
	}

	@Override
	public String getSeverityKey() {
		// TODO consider removing anonymous inner class references (as they are not really unique)
		return this.qualifier == null ? getMethodKey() : getMethodKey() + ":" + this.qualifier;
	}

	private String getMethodKey() {
		return this.stackTraceElement == null ? "unknown key" : this.stackTraceElement.getClassName() + "."
				+ this.stackTraceElement.getMethodName();
	}

	@Override
	public String toString() {
		return getMethodKey() + getSourceLocation();
	}

	@Override
	public String getClassName() {
		return this.stackTraceElement.getClassName();
	}

	@Override
	public String getMethodName() {
		return this.stackTraceElement.getMethodName();
	}

	@SuppressWarnings("null" /* imprecise null check */)
	@Override
	public List<Caller> relatedCallers() {
		List<Caller> callers = new LinkedList<Caller>();
		Caller c = getCause();
		int n = 0;
		while (c != null & n < GroupingLog.MAX_STACKTRACE_NESTING_DEPTH) {
			callers.add(c);
			c = c.getCause();
			n++;
		}
		return callers;
	}

	void truncate() {
		this.throwable = null;
		this.context = null;
	}

	@Override
	public String getSourceLocation() {
		return this.stackTraceElement == null ? "unknown location" : this.stackTraceElement.toString();
	}

	@Override
	public Object getContext() {
		return this.context;
	}
}