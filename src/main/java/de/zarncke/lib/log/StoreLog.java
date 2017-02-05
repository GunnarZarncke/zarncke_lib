package de.zarncke.lib.log;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import de.zarncke.lib.coll.Elements;
import de.zarncke.lib.err.ExceptionUtil;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.io.IOTools;
import de.zarncke.lib.io.store.Store;

/**
 * Log into a store.
 *
 * @author Gunnar Zarncke
 */
public class StoreLog implements Log {

	private final Store store;
	private final Charset encoding;

	public StoreLog(final Store store, final Charset encoding) {
		this.store = store;
		this.encoding = encoding;
	}

	@Override
	public void report(final Throwable throwableToReport) {
		report(ExceptionUtil.getStackTrace(throwableToReport));
	}

	@Override
	public void report(final CharSequence issue) {
		OutputStream os = null;
		try {
			os = this.store.getOutputStream(true);
			os.write((issue + "\n").getBytes(this.encoding));
		} catch (IOException e) {
			throw Warden.spot(new RuntimeException("cannot write to log file", e));
		} finally {
			IOTools.forceClose(os);
		}
	}

	@Override
	public void report(final Object... debugObject) {
		report(Elements.toString(debugObject));
	}

	@Override
	public String toString() {
		return "log into " + this.store;
	}
}
