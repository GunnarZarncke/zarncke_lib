package de.zarncke.lib.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.Pipe.SinkChannel;
import java.nio.channels.Pipe.SourceChannel;
import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import de.zarncke.lib.err.NotAvailableException;
import de.zarncke.lib.err.Warden;

/**
 * Queue using a {@link Pipe} for keeping the elements which are serialized via Kryo.
 * 
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 * @param <E> type of elements
 * @deprecated unsatisfactory performance, use {@link FileQueue} for large data
 * or e.g. {@link com.sun.jmx.remote.internal.ArrayQueue} for small
 */
@Deprecated
public class PipeQueue<E> extends AbstractQueue<E> {

	class PipeInst {

		private Output output;
		private Input input;
		private SourceChannel source;
		private SinkChannel sink;
		private final Object readLock = new Object();
		private final Object writeLock = new Object();
		private final int id = PipeQueue.this.idCnt++;

		PipeInst() throws IOException {
			Pipe pipe = Pipe.open();
			this.source = pipe.source();
			this.sink = pipe.sink();

			this.output = new Output(new OutputStream() {
				@Override
				public void write(final int b) throws IOException {
					PipeInst.this.sink.write(ByteBuffer.wrap(new byte[] { (byte) b }));
				}

				@Override
				public void write(final byte[] b) throws IOException {
					PipeInst.this.sink.write(ByteBuffer.wrap(b));
				}

				@Override
				public void write(final byte[] b, final int off, final int len) throws IOException {
					PipeInst.this.sink.write(ByteBuffer.wrap(b, off, len));
				}

				@Override
				public String toString() {
					return "output to " + PipeInst.this.sink;
				}
			});
			this.input = new Input(new InputStream() {
				@Override
				public int read() throws IOException {
					byte[] b = new byte[1];
					int n = PipeInst.this.source.read(ByteBuffer.wrap(b));
					return n <= 0 ? n : (int) b[0];
				}

				@Override
				public int read(final byte[] b) throws IOException {
					return PipeInst.this.source.read(ByteBuffer.wrap(b));
				}

				@Override
				public int read(final byte[] b, final int off, final int len) throws IOException {
					ByteBuffer bb = ByteBuffer.allocate(len);
					int r = PipeInst.this.source.read(bb);
					if (r > 0) {
						bb.flip();
						bb.get(b, off, r);
					}
					return r;
				}

				@Override
				public String toString() {
					return "input from " + PipeInst.this.source;
				}
			});
		}

		void finishInput() throws IOException {
			this.output.flush();
			this.output.close();
			this.sink.close();
			this.output = null;
		}

		public void close() throws IOException {
			finishInput();
			IOTools.forceClose(this.source);
			this.source = null;
			this.sink = null;
			this.input = null;
		}

		boolean write(final E e) {
			synchronized (this.writeLock) {
				if (this.output == null) {
					return false;
				}
				PipeQueue.this.kryo.writeObject(this.output, e);
				return true;
			}
		}

		public E read() {
			synchronized (this.readLock) {
				try {
					return PipeQueue.this.kryo.readObject(this.input, PipeQueue.this.type);
				} catch (KryoException e) {
					// only way to detect EOF in Kryo
					if ("Buffer underflow.".equals(e.getMessage())) {
						// on EOF the Pipe is dead and removed
						// PipeQueue.this.pipes.removeLastOccurrence(this);
						return null;
					}

					// all other cases are dangerous and it is unclear what to do
					throw Warden.spot(new NotAvailableException("Kryo failed on Pipe data, "
							+ "we don't know what to do but leave the Queue in the current state", e));
				}
			}
		}

		@Override
		public String toString() {
			return "Pipe " + this.id + (this.output==null?" closed":"");
		}

		public void flush() {
			this.output.flush();
		}
	}

	private int idCnt=0;
	private final Kryo kryo;
	private final Class<E> type;

	private final AtomicInteger size = new AtomicInteger();

	PipeInst pipeInst;

	public PipeQueue(final Class<E> type) throws IOException {
		this.type = type;
		// this.pipes.add(new PipeInst());
		this.pipeInst = new PipeInst();
		this.kryo = new Kryo();
		this.kryo.register(type);
	}

	@Override
	public boolean offer(final E e) {
		if (this.pipeInst.write(e)) {
			this.size.incrementAndGet();
			return true;
		}
		return false;
	}

	@Override
	public E poll() {
		E val = this.pipeInst.read();
		if (val != null) {
			this.size.decrementAndGet();
			return val;
		}
		return null;
	}

	@Override
	public E peek() {
		throw Warden.spot(new UnsupportedOperationException("cannot peek on Pipes"));
	}

	@Override
	public Iterator<E> iterator() {
		throw Warden.spot(new UnsupportedOperationException("cannot iterate over pipeInst"));
	}

	@Override
	public int size() {
		return this.size.get();
	}

	@Override
	public String toString() {
		return "Queue via Pipe and Kryo " + size();
	}

	public void flush() {
		this.pipeInst.flush();
	}

	public void close() throws IOException {
		this.pipeInst.close();
	}

	@Override
	protected void finalize() throws Throwable {
		close();
	}
}
