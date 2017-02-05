package de.zarncke.lib.lang;

import java.io.IOException;
import java.io.InputStream;

import de.zarncke.lib.err.Warden;
import de.zarncke.lib.io.store.Store;
import de.zarncke.lib.io.store.StoreUtil;

public class StoreClassLoader extends InfoClassLoader {
	private final Store[] stores;

	public StoreClassLoader(final ClassLoader parentClassLoader, final Store... stores) {
		super(parentClassLoader);
		this.stores = stores;
	}

	@Override
	public InputStream getResourceAsStream(final String name) {
		Store res = resolveToStore(name);
		if (res == null) {
			return null;
		}
		try {
			return res.getInputStream();
		} catch (IOException e) {
			Warden.disregard(e);
			return null;
		}
	}

	@Override
	byte[] findClassBinary(final String name) {
		Store res = resolveToStore(name.replaceAll("\\.", "/") + ".class");
		if (res == null) {
			return null;
		}

		byte[] byteArray;
		try {
			byteArray = res.asRegion().toByteArray();
		} catch (IOException e) {
			Warden.disregard(e);
			return null;
		}

		return byteArray;
	}

	private Store resolveToStore(final String name) {
		Store res = null;
		for (Store store : this.stores) {
			res = StoreUtil.resolvePath(store, name, "/");
			if (res.canRead()) {
				return res;
			}
		}
		return null;
	}
}
