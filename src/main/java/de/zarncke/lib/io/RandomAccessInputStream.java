package de.zarncke.lib.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import de.zarncke.lib.err.Warden;

/**
 * Provides an InputStream of a {@link RandomAccessFile}.
 * The position is <em>not</em> abstracted away, i.e.&nbsp;the underlying file pointer is advanced by this stream.
 *
 * @author Gunnar Zarncke
 */
public class RandomAccessInputStream extends InputStream {
	private final RandomAccessFile base;
	private long mark;

	public RandomAccessInputStream(final RandomAccessFile base) {
		this.base = base;
	}

	@Override
	public int read() throws IOException {
		return this.base.read();
	}

	@Override
	public int read(final byte[] b) throws IOException {
		return this.base.read(b);
	}

	@Override
	public int read(final byte[] b, final int off, final int len) throws IOException {
		return this.base.read(b, off, len);
	}

	@Override
	public long skip(final long n) throws IOException {
		long p = this.base.getFilePointer();
		this.base.seek(p + n);
		return this.base.getFilePointer() - p;
	}

	@Override
	public int available() throws IOException {
		long av = this.base.length() - this.base.getFilePointer();
		if (av > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		return (int) av;
	}

	@Override
	public synchronized void mark(final int readlimit) {
		try {
			this.mark = this.base.getFilePointer();
		} catch (IOException e) {
			throw Warden.spot(new IllegalStateException("cannot mark because current position is unavailable", e));
		}
	}

	@Override
	public synchronized void reset() throws IOException {
		this.base.seek(this.mark);
	}

	@Override
	public boolean markSupported() {
		return true;
	}

	@Override
	public String toString() {
		return "InputStream on " + this.base.toString();
	}
}
