package de.zarncke.lib.index;

import java.util.Collection;
import java.util.Comparator;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.index.crit.Criteria;

public abstract class DefaultSubIndexing<T> implements Indexing<T> {

	private final Class<?> type;

	protected DefaultSubIndexing(final Class<?> type) {
		this.type = type;
	}

	public void add(final T entry) {
		throw Warden.spot(new UnsupportedOperationException("unexpected"));
	}

	public Collection<Comparator<T>> getOrdering() {
		return L.e();
	}

	public double getPredictivity(final Criteria<?, T> crit) {
		return 1.0;
	}

	public Class<?> getType() {
		return this.type;
	}
}
