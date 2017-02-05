/**
 *
 */
package de.zarncke.lib.sys;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.google.common.collect.MapMaker;

import de.zarncke.lib.err.NotAvailableException;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.io.IOTools;
import de.zarncke.lib.io.store.Store;
import de.zarncke.lib.log.Log;
import de.zarncke.lib.sys.Installation.SeverityStore;

/**
 * Stores severity assignments in a properties file.
 * No precaution done to avoid loss of the file except synchronizing the access in this class.
 * The recommended use is to simply backup the store and recover it if a failure is reported.
 * Any locking/transactional access is considered insufficiently robust.
 *
 * @author Gunnar Zarncke
 */
public class PropertySeverities implements SeverityStore {

	private static final String MINIMUM_OPERATION = ">";

	private static final String PREFIX_IMMEDIATE = "!";

	private final Store propertyStore;

	private final Map<String, Health> severitiesByCallerKey = new MapMaker().makeMap();
	private final Set<String> immediateCallerKeys = Collections.synchronizedSet(new HashSet<String>());
	private boolean loaded = false;

	private final Map<String, Integer> minimumCounts = new MapMaker().makeMap();

	public PropertySeverities(final Store propertyStore) {
		this.propertyStore = propertyStore;
		if (propertyStore.canRead()) {
			load();
		} else {
			this.loaded = true;
		}
	}

	@Override
	public Map<String, Health> getSeveritiesByCallerKey() {
		load();
		return this.severitiesByCallerKey;
	}

	@Override
	public synchronized void remember(final String callerKey, final Health severity, final boolean immediate,
			final int minimumCount) {
		load();
		if (severity == null) {
			this.severitiesByCallerKey.remove(callerKey);
		} else {
			this.severitiesByCallerKey.put(callerKey, severity);
		}
		if (immediate) {
			this.immediateCallerKeys.add(callerKey);
		} else {
			this.immediateCallerKeys.remove(callerKey);
		}
		if (minimumCount > 1) {
			this.minimumCounts.put(callerKey, Integer.valueOf(minimumCount));
		} else {
			this.minimumCounts.remove(callerKey);
		}
		store();
	}

	private synchronized void load() {
		if (!this.loaded) {
			Properties props = new Properties();
			InputStream ins = null;
			try {
				ins = this.propertyStore.getInputStream();
				props.load(ins);
				this.loaded = true;
			} catch (IOException e) {
				throw Warden.spot(new NotAvailableException("cannot load severities from "
						+ this.propertyStore.getName(), e));
			} finally {
				IOTools.forceClose(ins);
			}
			for (Map.Entry<Object, Object> me : props.entrySet()) {
				if (!this.severitiesByCallerKey.containsKey(me.getKey().toString())) {
					try {
						String key = me.getKey().toString();
						String val = me.getValue().toString();
						if (val.startsWith(PREFIX_IMMEDIATE)) {
							val = val.substring(1);
							this.immediateCallerKeys.add(key);
						}
						int p = val.indexOf(MINIMUM_OPERATION);
						if (p >= 0) {
							this.minimumCounts.put(key, Integer.valueOf(val.substring(p + 1)));
							val = val.substring(0, p);
						}
						Health state = Enum.valueOf(Health.class, val);
						this.severitiesByCallerKey.put(key, state);
					} catch (Exception e) {
						Warden.disregard(e);
						Log.LOG.get().report("invalid health key " + me + " in " + this.propertyStore);
					}
				}
			}
		}
	}

	private synchronized void store() {
		Properties props = new Properties();
		for (Map.Entry<String, Health> me : this.severitiesByCallerKey.entrySet()) {
			String key = me.getKey();
			String value = me.getValue().name();
			String literal = value;
			if (this.immediateCallerKeys.contains(key)) {
				value = PREFIX_IMMEDIATE + value;
			}
			Integer count = this.minimumCounts.get(key);
			if (count != null) {
				value = value + MINIMUM_OPERATION + count;
			}
			props.put(key, literal);
		}
		try {
			props.store(this.propertyStore.getOutputStream(false), "severity assignments");
		} catch (IOException e) {
			throw Warden.spot(new NotAvailableException("cannot store severities in " + this.propertyStore.getName()
					+ " (will try again on next change)", e));
		}
	}

	@Override
	public Set<String> getImmediateCallerKeys() {
		return this.immediateCallerKeys;
	}

	@Override
	public Map<String, Integer> getMinimumCounts() {
		return this.minimumCounts;
	}
}