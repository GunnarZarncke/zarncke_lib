package de.zarncke.lib.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * This stream wraps another stream and tracks the number of bytes fetched from the underlying stream.
 *
 * @author Gunnar Zarncke
 */
public class CountInputStream extends InputStream {

	private final InputStream decorated;
	private long count;
	private long closeAt = Long.MAX_VALUE;

	public CountInputStream(final InputStream decorated) {
		this.decorated = decorated;
	}

	@Override
	public int read() throws IOException {
		if (this.count >= this.closeAt) {
			return -1;
		}
		int b = this.decorated.read();
		if (b >= 0) {
			this.count++;
		}
		return b;
	}

	@Override
	public int read(final byte[] b) throws IOException {
		return read(b, 0, b.length);
	}



	@Override
	public int read(final byte[] b, final int off, final int len) throws IOException {
		int minLen = (int) Math.min(this.closeAt - this.count, len);
		if (minLen == 0) {
			return -1;
		}
		int r = this.decorated.read(b, off, minLen);
		if (r > 0) {
			this.count += r;
		}
		return r;
	}

	@Override
	public long skip(final long n) throws IOException {
		long minN = Math.min(n, this.closeAt - this.count);
		long r = this.decorated.skip(minN);
		this.count += r;
		return r;
	}

	@Override
	public String toString() {
		return this.count + " bytes read from " + this.decorated.toString();
	}

	@Override
	public int available() throws IOException {
		return (int) Math.min(this.closeAt - this.count, this.decorated.available());
	}

	@Override
	public void close() throws IOException {
		this.decorated.close();
	}

	@Override
	public synchronized void mark(final int readlimit) {
		this.decorated.mark(readlimit);
	}

	@Override
	public synchronized void reset() throws IOException {
		this.decorated.reset();
	}

	@Override
	public boolean markSupported() {
		return this.decorated.markSupported();
	}

	public long getCount() {
		return this.count;
	}

	public void setCount(final long count) {
		this.count = count;
	}

	public long getCloseAt() {
		return this.closeAt;
	}

	public void setCloseAt(final long closeAt) {
		this.closeAt = closeAt;
	}

	@Override
	public int hashCode() {
		return this.decorated.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		CountInputStream other = (CountInputStream) obj;
		if (this.decorated == null) {
			if (other.decorated != null) {
				return false;
			}
		} else if (!this.decorated.equals(other.decorated)) {
			return false;
		}
		return true;
	}

}
