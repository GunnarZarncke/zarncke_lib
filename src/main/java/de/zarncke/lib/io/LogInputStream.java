package de.zarncke.lib.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import de.zarncke.lib.util.Misc;

/**
 * This stream wraps another stream and tracks the complete content fetched from the underlying stream (for log purposes).
 *
 * @author Gunnar Zarncke
 */
public class LogInputStream extends InputStream {

	private final InputStream decorated;
	private int mark = 0;
	private String content = null;
	private final ByteArrayOutputStream contentBytes = new ByteArrayOutputStream();

	public LogInputStream(final InputStream decorated) {
		this.decorated = decorated;
	}

	public byte[] getReadBytes() {
		return this.contentBytes.toByteArray();
	}

	@Override
	public int read() throws IOException {
		int b = this.decorated.read();
		if (b >= 0) {
			this.contentBytes.write(b);
		}
		this.content = null;
		return b;
	}

	@Override
	public int hashCode() {
		return this.decorated.hashCode();
	}

	@Override
	public int read(final byte[] b) throws IOException {
		int r = this.decorated.read(b);
		this.contentBytes.write(b, 0, r);
		this.content = null;
		return r;
	}


	@Override
	public boolean equals(final Object obj) {
		return this.decorated.equals(obj);
	}

	@Override
	public int read(final byte[] b, final int off, final int len) throws IOException {
		int r = this.decorated.read(b, off, len);
		if (r > 0) {
			this.contentBytes.write(b, off, r);
			this.content = null;
		}
		return r;
	}

	@Override
	public long skip(final long n) throws IOException {
		long r = this.decorated.skip(n);
		for (int i = 0; i < r; i++) {
			this.contentBytes.write(0);
		}
		this.content = null;
		return r;
	}

	@Override
	public String toString() {
		if (this.content != null) {
			return this.content;
		}
		this.content = new String(this.contentBytes.toByteArray(), Misc.UTF_8);
		return this.content;
		// return this.decorated.toString();
	}

	@Override
	public int available() throws IOException {
		return this.decorated.available();
	}

	@Override
	public void close() throws IOException {
		this.decorated.close();
	}

	@Override
	public synchronized void mark(final int readlimit) {
		this.decorated.mark(readlimit);
		this.mark = this.contentBytes.size();
	}

	@Override
	public synchronized void reset() throws IOException {
		this.decorated.reset();
		byte[] bytes = this.contentBytes.toByteArray();
		this.contentBytes.reset();
		this.contentBytes.write(bytes, 0, this.mark);
		this.content = null;
	}

	@Override
	public boolean markSupported() {
		return this.decorated.markSupported();
	}

}
