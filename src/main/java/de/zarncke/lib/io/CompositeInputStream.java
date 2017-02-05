package de.zarncke.lib.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * An {@link InputStream} class which aggregates/appends content from multiple constituent {@link InputStream}s.
 *
 * @author Gunnar Zarncke
 */
public class CompositeInputStream extends InputStream {

	private final List<InputStream> sources;
	private int curr = 0;

	public CompositeInputStream(final List<InputStream> inputStreams) {
		this.sources = inputStreams;
	}

	/**
	 * Never returns chunks which span multiple input sources.
	 */
	@Override
	public int read(final byte b[], final int off, final int len) throws IOException {
		int maxlen = len;
		int av = available();
		if (av == 0) {
			maxlen = 1;
		} else if (maxlen > av) {
			maxlen = av;
		}
		return super.read(b, off, maxlen);
	}

	@Override
	public int read() throws IOException {
		if (this.curr >= this.sources.size()) {
			return -1;
		}
		int r = this.sources.get(this.curr).read();
		if (r >= 0) {
			return r;
		}
		this.curr++;
		if (this.curr >= this.sources.size()) {
			return r;
		}
		beforeNextStep(this.curr);
		return read();
	}

	/**
	 * Called when the end of an input is reached.
	 * Does nothing.
	 * Derived classes may do something.
	 *
	 * @param position current position in the list of sources
	 */
	protected void beforeNextStep(final int position) {
		// nop
	}

	@Override
	public int available() throws IOException {
		if (this.curr >= this.sources.size()) {
			return 0;
		}
		return this.sources.get(this.curr).available();
	}

	@Override
	public void close() throws IOException {
		// TODO close all even if any single fails
		for (InputStream ins : this.sources) {
			ins.close();
		}
	}

	@Override
	public String toString() {
		return this.sources.toString();
	}
}