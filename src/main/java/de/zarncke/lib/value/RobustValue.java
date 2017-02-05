package de.zarncke.lib.value;

import org.joda.time.Duration;

import de.zarncke.lib.err.Warden;
import de.zarncke.lib.log.Log;

/**
 * Robust wrapper for a {@link Value}.
 *
 * @author Gunnar Zarncke
 * @param <T> proxy type
 */
public class RobustValue<T> implements Value<T> {

	private final Value<T> delegate;
	private final T defaultValue;
	private long nextTry = Long.MIN_VALUE;
	private final Duration minimumRetryDuration;
	private final boolean useLastValue;
	private T lastValue;
	private final boolean logExceptions;

	/**
	 * @param delegate to query
	 * @param defaultValue to use if query fails
	 * @param minimumRetryDuration time to wait after a failing request (until then the replacement value is returned
	 * @param useLastValue true: in error cases use the last successfully fetched value (default if none yet);
	 * false: use the default value in case of errors
	 * @param logExceptions true: exceptions are logged; false: exceptions are ignored
	 */
	public RobustValue(final Value<T> delegate, final T defaultValue, final Duration minimumRetryDuration,
			final boolean useLastValue,
			final boolean logExceptions) {
		this.delegate = delegate;
		this.defaultValue = defaultValue;
		this.minimumRetryDuration = minimumRetryDuration;
		this.useLastValue = useLastValue;
		this.lastValue = defaultValue;
		this.logExceptions = logExceptions;
	}

	public T get() {
		long now = System.currentTimeMillis();
		if (this.nextTry > now) {
			return chooseReplacement(true);
		}
		try {
			this.lastValue = this.delegate.get();
			return this.lastValue;
		} catch (Exception e) {
			this.nextTry = now + this.minimumRetryDuration.getMillis();
			Warden.disregard(e);
			if (this.logExceptions) {
				Log.LOG.get().report(e);
			}
			return chooseReplacement(false);
		}
	}

	protected T chooseReplacement(final boolean isTimeout) {
		if (this.useLastValue) {
			return this.lastValue;
		}
		return this.defaultValue;
	}

}
