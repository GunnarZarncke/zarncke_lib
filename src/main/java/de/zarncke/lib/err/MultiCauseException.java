package de.zarncke.lib.err;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This Exception serves to keep track of multiple Exceptions which are reported by and/or cause some ultimate exception.
 * Intended usage:
 *
 * <pre>
 * Exceptions causes = new Exceptions();
 * for (;;) {
 * 	try {
 * 		doSomethingDangerous();
 * 	} catch (SomeException e) {
 * 		causes.add(e);
 * 	}
 * }
 * causes.throwIfNeeded();
 * // OR
 * causes.throwIfNeeded(&quot;failed&quot;); // throws MultiCauseException directly
 * // OR
 * if (!causes.isEmpty()) {
 * 	throw new MyException(&quot;summary&quot;, causes.exception());
 * 	// OR
 * 	throw new MyMultiException(&quot;summary&quot;, causes);
 * }
 *
 * static class MyMultiException extends MultiCauseException {
 * 	public MyException(final String msg, final List&lt;Throwable&gt; causes) {
 * 		super(msg, causes);
 * 	}
 * }
 * </pre>
 *
 * @author Gunnar Zarncke
 */
public class MultiCauseException extends Exception {
	/**
	 * Accumulates Exceptions to be thrown e.g. by {@link MultiCauseException}.
	 */
	public static class Exceptions {
		private final List<Throwable> causes = new CopyOnWriteArrayList<Throwable>();

		public List<Throwable> getCauses() {
			return this.causes;
		}

		public boolean isEmpty() {
			return this.causes.isEmpty();
		}

		public void throwIfNeeded() throws MultiCauseException {
			throwIfNeeded("multiple Exceptions");
		}

		public void throwIfNeeded(final String msg) throws MultiCauseException {
			if (!isEmpty()) {
				throw exception(msg);
			}
		}

		public MultiCauseException exception(final String msg) {
			return new MultiCauseException(msg, this.causes);
		}

		public void add(final Throwable t) {
			this.causes.add(t);
		}

		@Override
		public String toString() {
			return this.causes.toString();
		}
	}

	private static final long serialVersionUID = 1L;

	private final Collection<? extends Throwable> causes;

	/**
	 * @param msg to use
	 * @param causes != null, may not be empty
	 */
	public MultiCauseException(final String msg, final Collection<? extends Throwable> causes) {
		super(msg, causes.iterator().next());
		this.causes = causes;
	}

	public Collection<? extends Throwable> getCauses() {
		return this.causes;
	}

	@Override
	public String toString() {
		return super.toString() + " with " + this.causes.size() + " causes";
	}
}
