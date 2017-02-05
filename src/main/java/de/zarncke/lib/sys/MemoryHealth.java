/**
 *
 */
package de.zarncke.lib.sys;

import java.util.Collections;
import java.util.Map;

import com.google.common.collect.MapMaker;

import de.zarncke.lib.sys.Installation.HealthStore;
import de.zarncke.lib.sys.module.Module;

/**
 * Remembers the lastest health information in memory.
 *
 * @author Gunnar Zarncke
 */
public class MemoryHealth implements HealthStore {

	private final Map<Module, Health> healthByModule = new MapMaker().makeMap();

	@Override
	public Map<Module, Health> getHealthByModule() {
		return Collections.unmodifiableMap(this.healthByModule);
	}

	@Override
	public void remember(final Module module, final Health health) {
		this.healthByModule.put(module, health);
	}

}