package de.zarncke.lib.coll;

import java.util.Iterator;

import de.zarncke.lib.err.Warden;

/**
 * An Iterator which {@link #wrap(Object)}s results from another {@link Iterator}.
 * Can be used to get rid of wildcard types (but then the result models don't come from the original source.
 *
 * @author Gunnar Zarncke
 * @param <S> source type (may be a wildcard type)
 * @param <D> target type
 */
public abstract class WrapIterator<S, D> implements Iterator<D>, Remaining {

	private final Iterator<? extends S> iterator;
	private long remainingResults;
	private long maxResults;

	protected WrapIterator(final Iterator<? extends S> iterator) {
		this(iterator, Integer.MAX_VALUE);
	}

	protected WrapIterator(final Iterator<? extends S> iterator, final long maxResults) {
		this.iterator = iterator;
		this.remainingResults = iterator instanceof Remaining ? ((Remaining) iterator).available() : UNKNOWN;
		this.maxResults = maxResults;
	}

	@Override
	public boolean hasNext() {
		if (this.remainingResults == 0 || this.maxResults == 0) {
			return false;
		}
		return this.iterator.hasNext();
	}

	@Override
	public D next() {
		if (this.remainingResults == 0 || this.maxResults == 0) {
			throw Warden.spot(new IllegalStateException("no more elements"));
		}
		if (this.remainingResults != UNKNOWN) {
			this.remainingResults--;
		}
		this.maxResults--;
		return wrap(this.iterator.next());
	}

	@Override
	public void remove() {
		this.iterator.remove();
	}

	protected abstract D wrap(S next);

	@Override
	public String toString() {
		return this.remainingResults + " remaining of " + this.iterator.toString();
	}

	@Override
	public long available() {
		return this.remainingResults;
	}
}
