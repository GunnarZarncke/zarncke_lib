package de.zarncke.lib.io.store;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;

import de.zarncke.lib.err.Warden;
import de.zarncke.lib.io.IOTools;
import de.zarncke.lib.region.Region;
import de.zarncke.lib.region.RegionUtil;

/**
 * Store which reads resources from an {@link URLConnection}.
 *
 * @author Gunnar Zarncke
 */
public class UrlStore extends AbstractStore {

	// TODO add test

	private static final String RESOURCE_SEPARATOR = "/";
	private final Store parent;
	private final URI uri;

	public UrlStore(final URI uri) {
		this(uri, null);
	}

	public UrlStore(final URI uri, final Store parent) {
		this.uri = uri;
		this.parent = parent;
	}

	@Override
	public boolean exists() {
		// TODO perform HEAD request
		return canRead();
	}

	private InputStream getStream() throws IOException {
		return this.uri.toURL().openStream();
	}

	@Override
	public Store element(final String name) {
		return new UrlStore(this.uri.resolve(name), this);
	}

	public String getName() {
		if (this.uri == null) {
			return null;
		}
		String path = this.uri.getPath();
		int p = path.lastIndexOf(RESOURCE_SEPARATOR);
		return path.substring(p + 1);
	}

	@Override
	public Store getParent() {
		return this.parent;
	}

	@Override
	public boolean canRead() {
		try {
			return getStream() != null;
		} catch (IOException e) {
			Warden.disregard(e);
			return false;
		}
	}

	@Override
	public InputStream getInputStream() throws IOException {
		InputStream is = getStream();
		if (is == null) {
			throw new IOException("cannot read " + this.uri);
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
		return this.uri.toASCIIString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.uri == null ? 0 : this.uri.hashCode());
		return result;
	}

}
