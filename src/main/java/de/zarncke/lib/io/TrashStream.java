package de.zarncke.lib.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * {@link OutputStream} which ignores all data (but counts it).
 *
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
public final class TrashStream extends OutputStream {
	private int receivedBytes;

	@Override
	public void write(final int b) throws IOException {
		this.receivedBytes++;
	}

	@Override
	public void write(final byte[] b, final int off, final int len) throws IOException {
		this.receivedBytes += len;
	}

	public int getReceivedBytes() {
		return this.receivedBytes;
	}

	public void setReceivedBytes(final int receivedBytes) {
		this.receivedBytes = receivedBytes;
	}

	@Override
	public String toString() {
		return "ignored " + this.receivedBytes + " bytes";
	}
}