package de.zarncke.lib.coll;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Generic implementation of a Map which {@link #getDelegate() delegates} calls to another Map.
 *
 * @author Gunnar Zarncke
 * @param <K> key type
 * @param <V> value type
 */
public abstract class DelegateMap<K, V> implements Map<K, V> {

	@Override
	public int size() {
		return this.getDelegate().size();
	}

	@Override
	public boolean isEmpty() {
		return this.getDelegate().isEmpty();
	}

	@Override
	public boolean containsKey(final Object key) {
		return this.getDelegate().containsKey(key);
	}

	@Override
	public boolean containsValue(final Object value) {
		return this.getDelegate().containsValue(value);
	}

	@Override
	public V get(final Object key) {
		return this.getDelegate().get(key);
	}

	@Override
	public V put(final K key, final V value) {
		return this.getDelegate().put(key, value);
	}

	@Override
	public V remove(final Object key) {
		return this.getDelegate().remove(key);
	}

	@Override
	public void putAll(final Map<? extends K, ? extends V> m) {
		this.getDelegate().putAll(m);
	}


	@Override
	public void clear() {
		this.getDelegate().clear();
	}

	@Override
	public Set<K> keySet() {
		return this.getDelegate().keySet();
	}

	@Override
	public Collection<V> values() {
		return this.getDelegate().values();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return this.getDelegate().entrySet();
	}

	@Override
	public boolean equals(final Object o) {
		return this.getDelegate().equals(o);
	}

	@Override
	public int hashCode() {
		return this.getDelegate().hashCode();
	}

	protected abstract Map<K, V> getDelegate();
}
