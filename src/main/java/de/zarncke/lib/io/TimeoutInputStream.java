package de.zarncke.lib.io;

import java.io.IOException;
import java.io.InputStream;

import org.joda.time.DateTime;

import de.zarncke.lib.err.Warden;
import de.zarncke.lib.time.Clock;
import de.zarncke.lib.time.JavaClock;

/**
 * This stream wraps another stream and tracks the time since creation and throws TimeoutException when a specified timeout
 * occurs before the stream is closed.
 * Note: Time is checked relative to the {@link Clock}.
 *
 * @author Gunnar Zarncke
 */
public class TimeoutInputStream extends InputStream {

	public static class TimeoutException extends IOException {
		private static final long serialVersionUID = 1L;

		public TimeoutException(final String msg) {
			super(msg);
		}
	}

	private final InputStream decorated;
	private final long timeoutTimeMs;

	/**
	 * @param decorated to delegate to
	 * @param timeoutTimeMs the absolute time in ms after which timeout exceptions occur
	 */
	public TimeoutInputStream(final InputStream decorated, final long timeoutTimeMs) {
		this.decorated = decorated;
		this.timeoutTimeMs = timeoutTimeMs;
	}


	@Override
	public int read() throws IOException {
		int b = this.decorated.read();
		checkTimeout();
		return b;
	}

	@Override
	public int hashCode() {
		return this.decorated.hashCode();
	}

	@Override
	public int read(final byte[] b) throws IOException {
		int r = this.decorated.read(b);
		checkTimeout();
		return r;
	}


	@Override
	public boolean equals(final Object obj) {
		return this.decorated.equals(obj);
	}

	@Override
	public int read(final byte[] b, final int off, final int len) throws IOException {
		int r = this.decorated.read(b, off, len);
		checkTimeout();
		return r;
	}

	@Override
	public long skip(final long n) throws IOException {
		long r = this.decorated.skip(n);
		checkTimeout();
		return r;
	}

	private void checkTimeout() throws IOException {
		long now = JavaClock.getTheClock().getCurrentTimeMillis();
		if (now > this.timeoutTimeMs) {
			throw Warden.spot(new TimeoutException("timeout at " + new DateTime(now) + " > "
					+ new DateTime(this.timeoutTimeMs) + " for " + this.decorated));
		}
	}

	@Override
	public String toString() {
		return this.decorated.toString();
	}

	@Override
	public int available() throws IOException {
		checkTimeout();
		return this.decorated.available();
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

}
