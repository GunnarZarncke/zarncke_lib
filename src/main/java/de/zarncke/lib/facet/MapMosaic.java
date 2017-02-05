package de.zarncke.lib.facet;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.zarncke.lib.value.Default;

public class MapMosaic extends AbstractMap<Default<?>, Object> implements Mosaic, Serializable {
	private static final long serialVersionUID = 1L;
	private final Map<Default<?>, Object> facets = new HashMap<Default<?>, Object>();

	public MapMosaic() {
		//
	}

	public MapMosaic(final Mosaic initialMosaic) {
		putAll(initialMosaic);
	}

	public <T> T get(final Default<T> spec) {
		Object o = this.facets.get(spec);
		if (o == null) {
			o = initializeFor(spec);
		}
		@SuppressWarnings("unchecked")
		T t = (T) o;
		return t;
	}

	@Override
	public Object put(final Default<?> key, final Object value) {
		return this.facets.put(key, value);
	}

	/**
	 * Allows derived classes to replace the default mechanism.
	 * The default behavior is to use the default value of the spec.
	 *
	 * @param <T> type of default
	 * @param spec default to initialize for
	 * @return The initial value to return
	 */
	protected <T> Object initializeFor(final Default<T> spec) {
		Object o = spec.getValue();
		this.facets.put(spec, o);
		return o;
	}

	public <T> void set(final Default<T> spec, final T value) {
		if (value == null) {
			this.facets.remove(spec);
		} else {
			this.facets.put(spec, value);
		}
	}

	@Override
	public Set<java.util.Map.Entry<Default<?>, Object>> entrySet() {
		return this.facets.entrySet();
	}

}
