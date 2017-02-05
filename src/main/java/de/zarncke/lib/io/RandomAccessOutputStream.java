package de.zarncke.lib.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

/**
 * Provides an OutputStream of a {@link RandomAccessFile}.
 * The position is <em>not</em> abstracted away, i.e.&nbsp;the underlying file pointer is advanced by this stream.
 *
 * @author Gunnar Zarncke
 */
public class RandomAccessOutputStream extends OutputStream {
	private final RandomAccessFile base;

	public RandomAccessOutputStream(final RandomAccessFile base) {
		this.base = base;
	}

	@Override
	public void write(final byte[] b) throws IOException {
		this.base.write(b);
	}

	@Override
	public void write(final byte[] b, final int off, final int len) throws IOException {
		this.base.write(b, off, len);
	}

	@Override
	public void write(final int b) throws IOException {
		this.base.write(b);
	}

	@Override
	public void flush() throws IOException {
		this.base.getChannel().force(true);
	}

	@Override
	public String toString() {
		return "OutputStream on " + this.base.toString();
	}
}
