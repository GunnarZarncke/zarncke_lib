package de.zarncke.lib.io;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

import de.zarncke.lib.err.Warden;
import de.zarncke.lib.io.store.Store;

/**
 * Provides a simple to use and resource-save way to iterate over files line-wise.
 * This example reads all lines of the given Store into a List:
 *
 * <pre>
 * List&lt;String&gt; consume = new LineConsumer&lt;List&lt;String&gt;&gt;() {
 * 	private final List&lt;String&gt; res = new ArrayList&lt;String&gt;();
 *
 * 	&#064;Override
 * 	protected void consume(final String line) {
 * 		this.res.add(line);
 * 	}
 *
 * 	&#064;Override
 * 	protected java.util.List&lt;String&gt; result() {
 * 		return this.res;
 * 	};
 * }.consume(new FileStore(&quot;test.txt&quot;));
 * </pre>
 *
 * Note: No finally block is needed to close any resources.
 * Note: As the consumer has state it is not thread-safe. Use one consumer per Thread.
 *
 * @author Gunnar Zarncke
 * @param <T> return type, may be Void
 */
public abstract class LineConsumer<T> implements StoreConsumer<T> {
	private Store store;
	private LineNumberReader reader;

	/**
	 * Consumes all lines of the content of the Store.
	 *
	 * @param storeToProcess != null
	 * @return result of type T
	 * @throws IOException on IO failure with line indication
	 */
	@Override
	public T consume(final Store storeToProcess) throws IOException {
		try {
			this.store = storeToProcess;
			InputStream ins = this.store.getInputStream();
			String line = null;
			try {
				this.reader = new LineNumberReader(new InputStreamReader(new BufferedInputStream(ins), getEncoding()));
				while ((line = this.reader.readLine()) != null) {
					consume(line);
				}
			} catch (IOException e) { // NOPMD this is no flow control but exception enriching
				throw Warden.spot(new IOException("failed to read " + getStore().getName() + "(" + getLineNumber() + "):\n"
						+ line, e));
			} finally {
				IOTools.forceClose(ins);
				this.reader = null;
			}

			return result();
		} finally {
			this.store = null;
		}
	}

	protected String getEncoding() {
		return "UTF-8";
	}

	protected abstract void consume(final String line);

	protected T result() {
		return null;
	}

	protected long getLineNumber() {
		return this.reader == null ? -1 : this.reader.getLineNumber();
	}

	protected Store getStore() {
		return this.store;
	}

	@Override
	public String toString() {
		if (this.store == null) {
			return "unused LineConsumer " + getEncoding();
		}
		return "consming " + this.store.getName() + "(" + getLineNumber() + ") in " + getEncoding();
	}
}
