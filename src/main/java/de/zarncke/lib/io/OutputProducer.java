package de.zarncke.lib.io;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import de.zarncke.lib.err.Warden;
import de.zarncke.lib.io.store.Store;

/**
 * Provides a simple to use and resource-save way to write to files block-wise.
 * This example writes one line into the given Store:
 *
 * <pre>
 * new OutputProducer() {
 * 	&#064;Override
 * 	protected Object produce() {
 * 		return &quot;Hello World&quot;;
 * 	}
 * }.produce(new FileStore(&quot;test.txt&quot;));
 * </pre>
 *
 * Note: No finally block is needed to close any resources.
 * Note: As the producer has state it is not thread-safe. Use one producer per Thread.
 *
 * @author Gunnar Zarncke
 */
public abstract class OutputProducer {
	private Store store;
	private int blocks = 0;
	private final boolean append;

	protected OutputProducer() {
		this(false);
	}

	protected OutputProducer(final boolean append) {
		this.append = append;
	}

	/**
	 * Produces the content of the Store.
	 *
	 * @param storeToProcess != null
	 * @throws IOException on IO failure
	 */
	public void produce(final Store storeToProcess) throws IOException {
		try {
			this.store = storeToProcess;
			OutputStream os = this.store.getOutputStream(this.append);
			try {
				OutputStreamWriter writer = new OutputStreamWriter(new BufferedOutputStream(os), getEncoding());
				while (proceed()) {
					Object o = produce();
					write(o, writer);
					this.blocks++;
				}
				writer.flush();
				done();
			} catch (IOException e) { // NOPMD this is no flow control but exception enriching
				throw Warden.spot(new IOException("failed to write " + getStore().getName() + " at block " + this.blocks, e));
			} finally {
				IOTools.forceClose(os);
			}
		} finally {
			this.store = null;
			this.blocks = 0;
		}
	}

	protected void done() {
		// derive may override
	}

	protected void write(final Object o, final Writer writer) throws IOException {
		if (o != null) {
			writer.write(o.toString());
		}
		writer.write("\n");
	}

	protected boolean proceed() {
		return this.blocks == 0;
	}

	protected String getEncoding() {
		return "UTF-8";
	}

	protected abstract Object produce();

	protected Store getStore() {
		return this.store;
	}

	@Override
	public String toString() {
		if (this.store == null) {
			return "unused OutputProducer " + getEncoding();
		}
		return "producing block " + this.blocks + " to " + this.store.getName() + " in " + getEncoding();
	}
}
