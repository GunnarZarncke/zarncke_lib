package de.zarncke.lib.io;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import de.zarncke.lib.coll.Elements;
import de.zarncke.lib.err.CantHappenException;
import de.zarncke.lib.err.NotAvailableException;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.lang.reflect.Reflect;
import de.zarncke.lib.util.Misc;

/**
 * Queue intended for <em>large</em> numbers/sizes of elements.
 * Uses a File for keeping the elements which are serialized via Kryo.
 * Note that the file will grow with each entry added, so
 * do not use for unbounded queues.
 * With {@link ClearMode#SPARSE} this queue can be used without risk for total transferred data volume of Exabytes
 * (the file size will be very irritating then and all access will happen at the very end).
 *
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 * @param <E> type of elements
 */
public class FileQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {
	public static enum ClearMode {
		/**
		 * Doesn't clear the file after read.
		 * Fastest. Significantly so esp. when the queue is read and written concurrently.
		 */
		NONE,
		/**
		 * Overwrites the data in the queue with zeros after being read.
		 * Slowest.
		 */
		ZERO,
		/**
		 * Zeroes the data in the queue after being read by 'poking holes' into the file.
		 * This avoids the loss of disk space over time.
		 * A bit faster than zeroing.
		 * Requires native sparse file support.
		 * See {@link IOTools#setSparseZeros(File, long, long)}.
		 */
		SPARSE
	}

	private Kryo kryo;
	private final Class<E> type;

	private final AtomicInteger size = new AtomicInteger();

	private FileChannel channel;
	private volatile long clearPos = 0;
	private volatile long readPos = 0;
	private volatile long writePos = 0;
	private File queueFile;
	private ClearMode clearMode = ClearMode.ZERO;
	private int bufferSize = 64 * (int) Misc.BYTES_PER_KB;
	private boolean useNioIfPossible;

	public FileQueue(final Class<E> type) throws IOException {
		this(type, true);
	}

	public FileQueue(final Class<E> type, final boolean useNioIfPossible) throws IOException {
		this.type = type;
		this.kryo = new Kryo();
		this.kryo.register(type);
		this.useNioIfPossible = useNioIfPossible;
		initQueueFile();
	}

	private void initQueueFile() throws IOException {
		if (this.useNioIfPossible && Misc.isJava7()) {
			Reflect path = Reflect.call("java.nio.file.Files", "createTempFile", "queue", ".kryo",
					Reflect.array("java.nio.file.attribute.FileAttribute"));
			this.channel = (FileChannel) Reflect.call(
					"java.nio.file.Files",
					"newByteChannel",
					path,
					Reflect.array("java.nio.file.OpenOption",
							Reflect.get("java.nio.file.StandardOpenOption", "CREATE"),
							Reflect.get("java.nio.file.StandardOpenOption", "READ"),
							Reflect.get("java.nio.file.StandardOpenOption", "WRITE"),
							Reflect.get("java.nio.file.StandardOpenOption", "SPARSE"))).get();
			this.queueFile = (File) path.call("toFile").get();
			// the above reflection magic corresponds to the following 1.7 code - but allows to compile under 1.6
			// Path file = Files.createTempFile("queue", ".kryo");
			// this.channel = (FileChannel) Files.newByteChannel(file, StandardOpenOption.CREATE,
			// StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.SPARSE);
			// this.queueFile = file.toFile();
		} else {
			this.queueFile = File.createTempFile("queue", ".kryo");
			@SuppressWarnings("resource" /* the RandomAccessFile is closed when the channel is closed */)
			RandomAccessFile backingFile = new RandomAccessFile(this.queueFile, "rw");
			long preallocateQueueFileSize = Misc.BYTES_PER_MB;
			backingFile.setLength(preallocateQueueFileSize);
			this.channel = backingFile.getChannel();
		}
		this.queueFile.deleteOnExit();
	}

	@Override
	public boolean offer(final E e) {
		if (write(e)) {
			this.size.incrementAndGet();
			return true;
		}
		return false;
	}

	@Override
	public boolean offer(final E e, final long timeout, final TimeUnit unit) throws InterruptedException {
		return offer(e);
	}

	synchronized boolean write(final E e) {
		Output output = new Output(this.bufferSize, Integer.MAX_VALUE);
		this.kryo.writeClassAndObject(output, e);
		this.bufferSize = Math.max(this.bufferSize, output.getBuffer().length + 4);
		try {
			this.channel.position(FileQueue.this.writePos);
			int bytesWrittenByKryo = output.position();
			if (bytesWrittenByKryo <= 0) {
				throw Warden.spot(new CantHappenException("kryo wrote no bytes " + bytesWrittenByKryo));
			}
			writeAll(ByteBuffer.wrap(Elements.toByteArray(bytesWrittenByKryo + 4)));
			writeAll(ByteBuffer.wrap(output.getBuffer(), 0, bytesWrittenByKryo));
			FileQueue.this.writePos += bytesWrittenByKryo + 4;
		} catch (IOException e1) {
			Warden.disregardAndReport(e1);
			return false;
		}
		return true;
	}

	private void writeAll(final ByteBuffer buffer) throws IOException {
		while (buffer.hasRemaining()) {
			FileQueue.this.channel.write(buffer);
		}
	}

	public synchronized E read() {
		ByteBuffer buffer;
		int blocklen;
		while (true) {
			try {
				FileQueue.this.channel.position(this.readPos);
			} catch (IOException e1) {
				throw Warden.spot(new NotAvailableException("positioning stream failed", e1));
			}
			buffer = ByteBuffer.allocate(this.bufferSize);
			int r;
			try {
				r = FileQueue.this.channel.read(buffer);
			} catch (IOException e1) {
				Warden.disregardAndReport(e1);
				return null; // retry later
			}
			if (r < 4) {
				return null; // not even length header? try later
			}
			blocklen = Elements.intFromByteArray(buffer.array());
			if (blocklen == 0) {
				return null; // this is assumed to be empty space after the last block (if file is preallocated)
			}
			if (blocklen < 4) {
				throw Warden.spot(new IllegalStateException("inconsistent blocklen " + blocklen + " enountered in "
						+ this.queueFile + " at " + this.readPos));
			}
			if (blocklen <= this.bufferSize) {
				break;
			}
			this.bufferSize = blocklen; // we have to retry with larger buffer
		}
		while (true) {
			try {
				FileQueue.this.channel.read(buffer);
			} catch (IOException e) {
				Warden.disregardAndReport(e);
				return null;
			}
			if (buffer.position() >= blocklen) {
				break;
			}
		}

		try {
			byte[] blockBytes = Arrays.copyOfRange(buffer.array(), 4, blocklen);
			Object obj = this.kryo.readClassAndObject(new Input(blockBytes));
			if (obj != null && !this.type.isAssignableFrom(this.type)) {
				throw Warden.spot(new IllegalStateException("Kryo returned object of unexpected type " + obj.getClass()
						+ " we don't know what to do but leave the Queue in the current state"));
			}
			@SuppressWarnings("unchecked" /* we just did */)
			E e = (E) obj;
			FileQueue.this.readPos += blocklen;
			clearAsNeeded();
			return e;
		} catch (KryoException e) {
			throw Warden.spot(new NotAvailableException("Kryo failed on Pipe data "
					+ Elements.byteArrayToHumanReadable(buffer.array())
					+ " we don't know what to do but leave the Queue in the current state", e));
		}
	}

	private void clearAsNeeded() {
		try {
			long uncleared = this.readPos - this.clearPos;
			switch (FileQueue.this.clearMode) {
			case ZERO:
				if (uncleared >= 64 * Misc.BYTES_PER_KB) {
					ByteBuffer clearBuf = ByteBuffer.allocate(10 * (int) Misc.BYTES_PER_KB);
					this.channel.position(this.clearPos);
					while (this.readPos - this.clearPos > clearBuf.remaining()) {
						int w = this.channel.write(clearBuf);
						FileQueue.this.clearPos += w;
						clearBuf.clear();
					}
				}
				break;
			case SPARSE:
				if (uncleared >= 10 * Misc.BYTES_PER_MB) {
					if (IOTools.setSparseZeros(this.queueFile, this.clearPos, uncleared)) {
						this.clearPos = FileQueue.this.readPos;
					} else {
						this.clearMode = ClearMode.ZERO;
						clearAsNeeded();
					}
				}
				break;
			case NONE:
			default:
				// no clearing
			}
		} catch (IOException e) {
			// if clearing fails it will be retried
			Warden.disregardAndReport(e);
		}
	}

	@Override
	public E poll() {
		E val = read();
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

	public void close() throws IOException {
		this.channel.close();
		this.queueFile.delete();
		this.queueFile = null;
	}

	@Override
	protected void finalize() throws Throwable {
		close();
	}

	public ClearMode getClearMode() {
		return this.clearMode;
	}

	public void setClearMode(final ClearMode clearMode) {
		this.clearMode = clearMode;
	}

	@Override
	public void put(final E e) throws InterruptedException {
		while (!offer(e)) {
			if (Thread.interrupted()) {
				throw Warden.spot(new InterruptedException("interruped"));
			}
		}
	}

	@Override
	public E take() throws InterruptedException {
		while (true) {
			E r = poll();
			if (r != null) {
				return r;
			}
			if (Thread.interrupted()) {
				throw Warden.spot(new InterruptedException("interrupted"));
			}
		}
	}

	@Override
	public E poll(final long timeout, final TimeUnit unit) throws InterruptedException {
		long deadline = System.nanoTime() + unit.toNanos(timeout);

		while (true) {
			E r = poll();
			if (r != null) {
				return r;
			}
			if (System.nanoTime() >= deadline) {
				return null;
			}
			if (Thread.interrupted()) {
				throw Warden.spot(new InterruptedException("interruped"));
			}
		}
	}

	@Override
	public int remainingCapacity() {
		return Integer.MAX_VALUE;
	}

	@Override
	public int drainTo(final Collection<? super E> c) {
		return drainTo(c, Integer.MAX_VALUE);
	}

	@Override
	public int drainTo(final Collection<? super E> c, final int maxElements) {
		int n = 0;
		while (true) {
			E r = poll();
			if (r == null) {
				break;
			}
			c.add(r);
			n++;
			if (n >= maxElements) {
				break;
			}
		}
		return n;
	}

	@Override
	public void clear() {
		super.clear();
		this.clearPos = 0;
		this.readPos = 0;
		this.writePos = 0;
		this.queueFile.delete();
		try {
			initQueueFile();
		} catch (IOException e) {
			throw Warden.spot(new NotAvailableException("failed to clear queue file (leaving inconsistent state)", e));
		}
	}

	public Kryo getKryo() {
		return this.kryo;
	}

	public void setKryo(final Kryo kryo) {
		this.kryo = kryo;
	}
}
