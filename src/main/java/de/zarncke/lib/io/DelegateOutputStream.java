package de.zarncke.lib.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * This stream delegates all alls to another stream and thus allows for simple customization.
 *
 * @author Gunnar Zarncke
 */
public class DelegateOutputStream extends OutputStream {

	protected final OutputStream decorated;

	public DelegateOutputStream(final OutputStream decorated) {
		this.decorated = decorated;
	}

	@Override
	public void write(final int b) throws IOException {
		this.decorated.write(b);
	}

	@Override
	public void write(final byte[] b) throws IOException {
		this.decorated.write(b);
	}

	@Override
	public void write(final byte[] b, final int off, final int len) throws IOException {
		this.decorated.write(b, off, len);
	}

	@Override
	public int hashCode() {
		return this.decorated.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		return this.decorated.equals(obj);
	}

	@Override
	public void flush() throws IOException {
		this.decorated.flush();
	}

	@Override
	public String toString() {
		return this.decorated.toString();
	}

	@Override
	public void close() throws IOException {
		this.decorated.close();
	}
}
