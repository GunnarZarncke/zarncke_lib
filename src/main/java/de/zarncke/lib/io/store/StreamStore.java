package de.zarncke.lib.io.store;

import java.io.IOException;
import java.io.InputStream;

/**
 * Wraps an {@link InputStream}.
 * No marking or resetting is done. Not Thread save. Might be used only once.
 * 
 * @author Gunnar Zarncke
 */
public class StreamStore extends AbstractStore {

	private final InputStream stream;

	public StreamStore(final InputStream stream) {
		this.stream = stream;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return this.stream;
	}

	@Override
	public boolean exists() {
		return this.stream != null;
	}

	@Override
	public boolean canRead() {
		return this.stream != null;
	}

	@Override
	public String getName() {
		return null;
	}

}
