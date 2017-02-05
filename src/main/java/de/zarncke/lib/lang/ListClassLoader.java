package de.zarncke.lib.lang;

import java.io.InputStream;
import java.util.List;

public class ListClassLoader extends InfoClassLoader {
	private final List<InfoClassLoader> loaders;

	public ListClassLoader(final List<InfoClassLoader> loaders, final ClassLoader parentClassLoader) {
		super(parentClassLoader);
		this.loaders = loaders;
	}

	@Override
	public InputStream getResourceAsStream(final String name) {
		for (InfoClassLoader cl : this.loaders) {
			InputStream ins = cl.getResourceAsStream(name);
			if (ins != null) {
				return ins;
			}
		}
		return null;
	}

	@Override
	protected byte[] findClassBinary(final String name) {
		byte[] byteArray = null;
		for (InfoClassLoader cl : this.loaders) {
			byteArray = cl.findClassBinary(name);
			if (byteArray != null) {
				break;
			}
		}
		return byteArray;
	}
}
