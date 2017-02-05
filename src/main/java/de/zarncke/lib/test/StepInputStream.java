package de.zarncke.lib.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import de.zarncke.lib.err.Warden;

/**
 * An {@link InputStream} class which delivers its contents {@link #step()} by {@link #step()}.
 * Intended for testing.
 * Caution if you are debugging your test: The read times out after 1 minute.
 *
 * @author Gunnar Zarncke
 */
public class StepInputStream extends InputStream {

	private final Semaphore progress = new Semaphore(0);

	private final InputStream[] sources;
	private int curr = 0;

	public StepInputStream(final InputStream[] inputStreams) {
		this.sources = inputStreams;
	}

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
		if (this.curr >= this.sources.length) {
			return -1;
		}
		int r = this.sources[this.curr].read();
		if (r >= 0) {
			return r;
		}
		this.curr++;
		if (this.curr >= this.sources.length) {
			return r;
		}
		waitForStep();
		return read();
	}

	@Override
	public int available() throws IOException {
		if (this.curr >= this.sources.length) {
			return 0;
		}
		return this.sources[this.curr].available();
	}

	private void waitForStep() throws IOException {
		try {
			if (!this.progress.tryAcquire(1, TimeUnit.MINUTES)) {
				throw new IOException("debug failure");
			}
		} catch (InterruptedException e) {
			throw Warden.spot(new IOException("huh?", e));
		}
	}

	/**
	 * Must be called once for each contained {@link InputStream} except the last.
	 */
	public void step() {
		this.progress.release();
	}

}