package de.zarncke.lib.util;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.zarncke.lib.coll.Transform;

/**
 * This is a prototype for a Java DSL for adapting one class to another interface by declaratively and type-safely specifiying
 * delegation and conversion.
 *
 * @author Gunnar Zarncke
 */
@SuppressWarnings("all")
public class Adapter {
	static class MapAdapter<K, V, NV> {

		Map<K, Transform<V, ? extends NV>> adapter = new HashMap<K, Transform<V, ? extends NV>>();

		public Convert<K, V, NV> convert(final K key) {
			return new Convert<K, V, NV>(this, key);
		}

		void convertTo(final K key, final Class<? extends NV> clazz) {

		}

		void convertBy(final K key, final Transform<V, ? extends NV> trans) {
			this.adapter.put(key, trans);
		}

		Map<K, NV> adapt(final Map<K, V> map) {
			return new AbstractMap<K, NV>() {

				@Override
				public NV get(final Object key) {
					Transform<V, ? extends NV> transform = MapAdapter.this.adapter.get(key);
					V nv = map.get(key);
					// TODO
					return null;
				}

				@Override
				public Set<java.util.Map.Entry<K, NV>> entrySet() {
					return null;
				}
			};
		}
	}

	static class Convert<K, V, NV> {
		MapAdapter<K, V, NV> adapter;
		K key;

		public Convert(final MapAdapter<K, V, NV> mapAdapter, final K key2) {
			this.adapter = mapAdapter;
			this.key = key2;
		}

		MapAdapter<K, V, NV> to(final Class<? extends NV> clazz) {
			this.adapter.convertTo(this.key, clazz);
			return this.adapter;
		}

		MapAdapter<K, V, NV> by(final Transform<V, ? extends NV> transformer) {
			this.adapter.convertBy(this.key, transformer);
			return this.adapter;
		}
	}
	static class When {
		Adapter then(final Transform transformer) {
			return null;
		}
	}

	public static <T> T adapt(final Class<T> interfaceClass) {
		return null;
	}

	public static <K, V> MapAdapter<K, V, V> adaptMap(final Class<K> key, final Class<V> value) {
		return new MapAdapter<K, V, V>();
	}

	static When when(final Object method) {
		return new When();
	}

	private <T> Transform<T, Object> build(final T adapted) {
		return null;
	}

	static interface Test{
		String getS();
		List<String> getSl();
		Test getT();
		Set<Test> getTs();
	}

	public void testObjectAdapter() {
		Test adapted = Adapter.adapt(Test.class);
		// when(adapted.getS()).then("Hello");
		// take(adapted.getS()).from(List.class);
		// convert(adapted.getS()).to(List.class);
		// convert(adapted.getS()).by(Transform.TO_STRING);
		Transform<Test, Object> transform = build(adapted);
	}

	public void testMapAdapter() {
		// MapAdapter<String, String, Object> adapter = Adapter.adaptMap(String.class, String.class);
		// // adapter.when("key").then("value");
		// adapter.convert("s").to(List.class);
		// convert(adapted.getS()).by(Transform.TO_STRING);
		// Transform<Test, Object> transform = build(adapted);
	}
}
