package de.zarncke.lib.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.util.Arrays;

import de.zarncke.lib.err.Warden;

/**
 * This stream wraps another stream and automatically {@link #mark(int) marks it} when certain byte sequences are encountered.
 * This can be used to continue using a further decorated stream, e.g. to read a header with a {@link LineNumberReader} and pass
 * the reset InputStream to another method.
 * The mark is positioned on the first byte after each matching pattern.
 * Notes:
 * <ul>
 * <li>No self-overlapping patterns are supported.</li>
 * <li>The mark will be advanced when it would become invalid anyway. This is because the mark is used when a new mark is set
 * within an already read pattern.</li>
 * <li>The mark limit should be set to match the marking limit of the underlying buffer.</li>
 * </ul>
 *
 * @author Gunnar Zarncke
 */
public class AutoMarkInputStream extends InputStream {

	private final InputStream decorated;
	private final byte[][] patterns;
	private final int[] matchPos;
	private int markReadLimit = 4096;
	private int sinceLastMark = 0;
	private boolean autoMarkValid;

	public AutoMarkInputStream(final InputStream decorated, final byte[][] patterns) {
		this.decorated = decorated;
		this.patterns = patterns;
		this.matchPos = new int[patterns.length];
		mark(this.markReadLimit);
		this.autoMarkValid = false;
	}

	public int getMarkReadLimit() {
		return this.markReadLimit;
	}

	public void setMarkReadLimit(final int markReadLimit) {
		this.markReadLimit = markReadLimit;
	}

	@Override
	public int read() throws IOException {
		ensureMark(1);
		int b = this.decorated.read();
		if (b >= 0) {
			inspect((byte) b, 0);
		}
		return b;
	}

	@Override
	public int hashCode() {
		return this.decorated.hashCode();
	}

	@Override
	public int read(final byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	public void ensureMark(final int len) {
		if (this.sinceLastMark + len > this.markReadLimit) {
			mark(this.markReadLimit);
			this.autoMarkValid = false;
		}
	}

	private void inspect(final byte b, final int alreadyReadAhead) throws IOException {
		for (int i = 0; i < this.matchPos.length; i++) {
			if (b == this.patterns[i][this.matchPos[i]]) {
				this.matchPos[i]++;
				if (this.matchPos[i] == this.patterns[i].length) {
					this.autoMarkValid = true;
					if (alreadyReadAhead == 0) {
						this.decorated.mark(this.markReadLimit);
					} else {
						try {
							this.decorated.reset();
						} catch (IOException e) {
							throw Warden.spot(new IllegalStateException(
									"ersetting didn't work even though we maintain a mark at all times " + this.sinceLastMark
											+ " " + this.markReadLimit + " " + alreadyReadAhead, e));
						}
						this.decorated.skip(this.sinceLastMark + 1);
						this.decorated.mark(this.markReadLimit);
						if (alreadyReadAhead > this.markReadLimit) {
							this.autoMarkValid = false;
						}
						this.decorated.skip(alreadyReadAhead);
					}
					this.sinceLastMark = -1; // anticipating ++ below
					this.matchPos[i] = 0;
				}
			} else {
				this.matchPos[i] = 0;
			}

		}
		this.sinceLastMark++;
	}

	private void clearPattern() {
		Arrays.fill(this.matchPos, 0);
	}

	@Override
	public boolean equals(final Object obj) {
		return this.decorated.equals(obj);
	}

	@Override
	public int read(final byte[] b, final int off, final int len) throws IOException {
		if (len > this.markReadLimit) {
			int pos = 0;
			int block = this.markReadLimit;
			while (true) {
				int r = read(b, pos, block);
				if (r < block) {
					return r < 0 ? pos : pos + r;
				}
				pos += block;
				if (pos >= len) {
					return pos;
				}
				if (pos + block > len) {
					block = len - pos;
				}
			}
		}
		ensureMark(len);
		int r = this.decorated.read(b, off, len);
		for (int i = 0; i < r; i++) {
			inspect(b[i + off], r - i - 1);
		}
		return r;
	}

	@Override
	public long skip(final long n) throws IOException {
		long r = this.decorated.skip(n);
		clearPattern();
		this.sinceLastMark += r;
		if (this.sinceLastMark > this.markReadLimit) {
			mark(this.markReadLimit);
			this.autoMarkValid = false;
		}
		return r;
	}

	@Override
	public String toString() {
		return this.decorated.toString();
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
		this.sinceLastMark = 0;
		this.autoMarkValid = true;
	}

	@Override
	public synchronized void reset() throws IOException {
		if (!this.autoMarkValid) {
			throw Warden.spot(new IOException("no valid auto mark set (maybe read ahead the limit)"));
		}
		this.decorated.reset();
		clearPattern();
		this.sinceLastMark = 0;
		this.autoMarkValid = false;
	}

	public int getSinceLastMark() {
		return this.sinceLastMark;
	}

	@Override
	public boolean markSupported() {
		return this.decorated.markSupported();
	}

}
