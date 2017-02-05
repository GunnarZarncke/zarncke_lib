/**
 *
 */
package de.zarncke.lib.value;


import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;

import de.zarncke.lib.err.Warden;

/**
 * A {@link TypeNameMapping} which maintains a bijection.
 * 
 * @author Gunnar Zarncke
 */
public final class BiTypeMapping implements TypeNameMapping {
	private final BiMap<String, Class<?>> classByName = Maps.synchronizedBiMap(HashBiMap.<String, Class<?>> create());

	public BiTypeMapping assign(final Class<?> type, final String name) {
		if (this.classByName.containsKey(name)) {
			Class<?> prevClass = this.classByName.get(name);
			if (!prevClass.equals(type)) {
				throw Warden.spot(new IllegalArgumentException("name " + name + " already bound to different type " + prevClass));
			}
		}
		if (this.classByName.inverse().containsValue(name)) {
			String prevName = this.classByName.inverse().get(type);
			if (!prevName.equals(name)) {
				throw Warden.spot(new IllegalArgumentException("type " + type + " already bound by different name " + prevName));
			}
		}
		this.classByName.put(name, type);
		return this;
	}

	public String getNameForType(final Class<?> type) {
		return this.classByName.inverse().get(type);
	}

	public Class<?> getTypeForName(final String name) {
		return this.classByName.get(name);
	}

	@Override
	public String toString() {
		return this.classByName.toString();
	}
}