package de.zarncke.lib.err;

import java.util.LinkedList;
import java.util.List;

public class ErrorCollector extends Exception {

	private static final long serialVersionUID = 1L;
	private final List<Throwable> errors = new LinkedList<Throwable>();

	public ErrorCollector() {
		//
	}

	public void addAndDisregardForNow(final Throwable throwable) {
		Warden.disregard(throwable);
		add(throwable);
	}

	public void add(final Throwable throwable) {
		this.errors.add(throwable);
	}

	public void throwIfAny() throws ErrorCollector {
		if (!this.errors.isEmpty()) {
			initCause(this.errors.get(0));
			// TODO add the other stack traces
			throw Warden.spot(this);
		}
	}


	@Override
	public String toString() {
		return this.errors.toString();
	}
}
