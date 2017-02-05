package de.zarncke.lib.value;

import java.util.Map;

public class MapRef<K, T> implements Ref<T> {

	private final Value<Map<K, T>> map;
	private final K key;

	public MapRef(final Value<Map<K, T>> map, final K key) {
		this.map = map;
		this.key = key;
	}

	@Override
	public T get() {
		return this.map.get().get(this.key);
	}

	@Override
	public void set(final T value) {
		this.map.get().put(this.key, value);
	}

	@Override
	public String toString() {
		return this.map + "(" + this.key + ")";
	}

	public Value<Map<K, T>> getMapValue() {
		return this.map;
	}
}
