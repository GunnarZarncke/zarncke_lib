/**
 *
 */
package de.zarncke.lib.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.index.crit.Criteria;

public abstract class KeyValueIndexing<K, T> implements Indexing<T> {
	protected final Map<K, Index<T>> indexByKey = createMap();

	private int totalKeys = 0;
	private int maxSize = 0;

	private final Class<K> type;

	public KeyValueIndexing(final Class<K> type) {
		this.type = type;
	}

	public abstract void add(final T entry);

	protected HashMap<K, Index<T>> createMap() {
		return new HashMap<K, Index<T>>();
	}

	protected boolean add(final K key, final T entry) {
		Index<T> index = this.indexByKey.get(key);
		if (index == null) {
			index = createNewIndex(key);
			this.indexByKey.put(key, index);
		}
		Index<T> newIndex = index.add(entry);
		if (newIndex != index) {
			this.indexByKey.put(key, newIndex);
		}
		this.maxSize = Math.max(this.maxSize, newIndex.size());
		// TODO we count keys, but we might also count values for better predictivity
		this.totalKeys++;

		// TODO return value
		return true;
	}

	protected abstract Index<T> createNewIndex(K key);

	public Index<T> getIndex(final Criteria<?, T> crit) {
		Collection<?> keys = crit.getKeys();
		if (keys == null) {
			return null;
		}
		if (keys.size() == 1) {
			return this.indexByKey.get(keys.iterator().next());
		}
		Collection<Index<T>> indizes = new ArrayList<Index<T>>();
		for (Object key : keys) {
			Index<T> idx = this.indexByKey.get(key);
			if (idx != null) {
				indizes.add(idx);
			}
		}
		return new CombinedIndex<T>(indizes, false);
	}

	public double getPredictivity(final Criteria<?, T> crit) {
		if (crit == null) {
			return (double) this.maxSize / this.totalKeys;
		}
		Collection<?> keys = crit.getKeys();
		int hits = 0;
		for (Object key : keys) {
			Index<T> idx = this.indexByKey.get(key);
			if (idx != null) {
				hits += idx.size();
			}
		}
		return (double) hits / this.totalKeys;
	}

	public Collection<? extends Comparator<T>> getOrdering() {
		return L.e();
	}

	public Class<?> getType() {
		return this.type;
	}

	@Override
	public void clear() {
		for (Index<?> idx : this.indexByKey.values()) {
			idx.clear();
		}
	}
}