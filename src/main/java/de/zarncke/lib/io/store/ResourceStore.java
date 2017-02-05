package de.zarncke.lib.io.store;

import java.io.IOException;
import java.io.InputStream;

import de.zarncke.lib.io.IOTools;
import de.zarncke.lib.region.Region;
import de.zarncke.lib.region.RegionUtil;

/**
 * Store which reads resources from the classpath.
 * Caution: {@link ResourceStore ResourceStores} are always differnt.
 *
 * @author Gunnar Zarncke
 */
public class ResourceStore extends AbstractStore {

	private static final String RESOURCE_SEPARATOR = "/";
	private final Class<?> base;
	private final Store parent;
	private final String path;

	public ResourceStore(final Class<?> base) {
		this(base, null);
	}

	public ResourceStore(final Class<?> base, final String path) {
		this(base, path, null);
	}

	public ResourceStore(final Class<?> base, final String path, final Store parent) {
		this.base = base;
		this.path = path;
		this.parent = parent;
	}

	@Override
	public boolean exists() {
		return canRead();
	}

	private InputStream getStream() {
		return this.base.getResourceAsStream(this.path);
	}

	@Override
	public Store element(final String name) {
		String childPath;
		if (this.path == null) {
			childPath = name;
		} else if (name == null) {
			childPath = this.path;
		} else if (name.startsWith("/")) {
			childPath = name;
		} else if (this.path.endsWith("/")) {
			childPath = this.path + name;
		} else {
			childPath = this.path + RESOURCE_SEPARATOR + name;
		}
		return new ResourceStore(this.base, childPath, this);
	}

	public String getName() {
		if (this.path == null) {
			return null;
		}
		int p = this.path.lastIndexOf(RESOURCE_SEPARATOR);
		return this.path.substring(p + 1);
	}

	@Override
	public Store getParent() {
		return this.parent;
	}

	@Override
	public boolean canRead() {
		try {
			InputStream ins = getStream();
			if (ins == null) {
				return false;
			}
			ins.close();
			return true;
		} catch (Exception e) {
			// note: JDK throws NPE if stream not present we don't log any details
			return false;
		}
	}

	@Override
	public InputStream getInputStream() throws IOException {
		InputStream is = getStream();
		if (is == null) {
			throw new IOException("cannot read " + this.base + ":" + this.path);
		}
		return is;
	}

	@Override
	public Region asRegion() throws IOException {
		// TODO consider returning a region which fills as the stream is read
		return RegionUtil.asRegion(IOTools.getAllBytes(getStream()));
	}

	@Override
	public String toString() {
		return this.base + ":" + this.path;
	}

}
