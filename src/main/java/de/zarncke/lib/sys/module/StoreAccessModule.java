package de.zarncke.lib.sys.module;

import java.net.URI;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.i18n.Translations;
import de.zarncke.lib.io.store.Store;
import de.zarncke.lib.io.store.StoreLocator;
import de.zarncke.lib.io.store.StoreUtil;
import de.zarncke.lib.sys.BinarySupplier;
import de.zarncke.lib.sys.Health;
import de.zarncke.lib.util.Chars;

/**
 * Provides read access to a selected {@link Store}.
 * Access example (with SystemMonitoring):
 * http://localhost:8080/api/monitoring?module=EPG+Client&property=log%2Fepg%2Fepg_latest.log
 *
 * @author Gunnar Zarncke
 */
public class StoreAccessModule implements Module, BinarySupplier {

	private State state = State.UNINITIALIZED;
	private final String storeUri;

	public StoreAccessModule(final String uri) {
		this.storeUri = uri;
	}

	@Override
	public Health getHealth() {
		if (this.getStore().canRead()) {
			return Health.CLEAN;
		}
		return Health.ERRORS;
	}

	@Override
	public double getLoad() {
		return 1 - 1 / (1 + this.getStore().getSize() / 1000000000.0);
	}

	@Override
	public State getState() {
		return this.state;
	}

	@Override
	public Translations getName() {
		return new Translations("Store");
	}

	@Override
	public List<Module> getSubModules() {
		return L.e();
	}

	@Override
	public void kill() {
		//
	}

	@Override
	public void shutdown() {
		// this.state = State.GOING_DOWN;

		this.state = State.DOWN;
	}

	@Override
	public void startOrRestart() {
		// this.state = State.GOING_UP;
		this.state = State.UP;
	}

	@Override
	public void tryRecovery() {
		// this.state = State.RECOVERING;
		this.state = State.UP;
	}

	@Override
	public String toString() {
		return "Store " + this.getStore();
	}

	@Override
	public String getMetaInformation() {
		return this.getStore().getName();
	}

	@Override
	public Store getBinaryInformation(final Object key) {
		String path = key.toString();
		Store element = StoreUtil.resolvePath(this.getStore(), path, "/");
		if (!element.exists()) {
			return null;
		}
		if (element.canRead()) {
			return element;
		}
		if (!element.iterationSupported()) {
			return null;
		}

		return StoreUtil.asStore(
				Chars.join(Collections2.transform(L.copy(element.iterator()), new Function<Store, String>() {
					@Override
					public String apply(final Store from) {
						return from.getName();
					}
				}), "\n"), null);
	}

	protected Store getStore() {
		return StoreLocator.CTX.get().resolve(URI.create(this.storeUri));
	}
}
