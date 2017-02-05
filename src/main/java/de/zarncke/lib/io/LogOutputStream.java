package de.zarncke.lib.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import de.zarncke.lib.util.Misc;

/**
 * This stream wraps another stream and tracks the complete content written to the underlying stream (for log purposes).
 *
 * @author Gunnar Zarncke
 */
public class LogOutputStream extends DelegateOutputStream {

	private String content = null;
	private final ByteArrayOutputStream contentBytes = new ByteArrayOutputStream();

	public LogOutputStream(final OutputStream decorated) {
		super(decorated);
	}

	public byte[] getContent() {
		return this.contentBytes.toByteArray();
	}

	@Override
	public void write(final int b) throws IOException {
		super.write(b);
			this.contentBytes.write(b);
		this.content = null;
	}

	@Override
	public void write(final byte[] b) throws IOException {
		super.write(b);
		this.contentBytes.write(b);
		this.content = null;
	}

	@Override
	public void write(final byte[] b, final int off, final int len) throws IOException {
		super.write(b, off, len);
		this.contentBytes.write(b, off, len);
		this.content = null;
	}

	@Override
	public String toString() {
		if (this.content != null) {
			return this.content;
		}
		this.content = new String(this.contentBytes.toByteArray(), Misc.UTF_8);
		return this.content;
	}

	public void clear() {
		this.contentBytes.reset();
		this.content = null;
	}
}
