package de.zarncke.lib.value;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.DiscardPolicy;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import de.zarncke.lib.block.ABlock;
import de.zarncke.lib.data.HasValidity;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.time.Times;

/**
 * Caches a Value for some time and refreshes it in the background.
 * Notes:
 * <ul>
 * <li>If the initial fetching of the value fails with a {@link RuntimeException}, then he exception is passed on.</li>
 * <li>If a later asynchroneous fetching fails, then {@link #handleAsyncFailure the exception is logged and the value is
 * left as-is}.</li>
 * </ul>
 *
 * @author Gunnar Zarncke
 * @param <T> of value
 */
public class Preloaded<T> implements Value<T>, HasValidity {

	private final ThreadPoolExecutor executor;
	{
		this.executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>());
		this.executor.setRejectedExecutionHandler(new DiscardPolicy());
	}

	private final Value<T> delegate;
	private DateTime validUntil;
	private T lastResults;
	private final Duration duration;

	public Preloaded(final Value<T> delegate, final Duration duration) {
		this(delegate, duration, true);
	}

	public Preloaded(final Value<T> delegate, final Duration duration, final boolean loadImmediate) {
		this.delegate = delegate;
		this.duration = duration;
		if (loadImmediate) {
			fetch();
		}
	}

	private void checkFetch() {
		if (this.validUntil == null || Times.now().isAfter(this.validUntil)) {
			triggerReload();
		}
	}

	public void triggerReload() {
		this.executor.execute(ABlock.toRunnable(Warden.guarded(new ABlock<Void>() {
			@Override
			public Void execute() {
				try {
					fetch();
				} catch (RuntimeException e) {
					handleAsyncFailure(e);
				}
				return null;
			}
		}.logAndIgnoreExceptions(null))));
	}

	protected void handleFailure(final RuntimeException e) {
		throw e;
	}

	protected void handleAsyncFailure(final RuntimeException e) {
		Warden.disregardAndReport(e);
	}

	private void fetch() {
		try {
			this.lastResults = this.delegate.get();
			this.validUntil = Times.now().plus(this.duration);
		} catch (RuntimeException e) {
			handleFailure(e);
		}
	}

	@Override
	public T get() {
		if (this.lastResults == null) {
			fetch();
		} else {
			checkFetch();
		}
		return this.lastResults;
	}

	@Override
	public DateTime validUntil() {
		return this.validUntil;
	}

	@Override
	public String toString() {
		return "Cache " + this.delegate + " for " + this.duration;
	}

	@Override
	public String getTag() {
		return this.lastResults == null ? "NULL" : "T" + this.lastResults.hashCode();
	}
}
