package de.zarncke.lib.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Writer;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.CRC32;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.annotation.Nonnull;

import de.zarncke.lib.coll.Elements;
import de.zarncke.lib.coll.L;
import de.zarncke.lib.coll.Pair;
import de.zarncke.lib.err.NotAvailableException;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.io.store.Store;
import de.zarncke.lib.io.store.StreamStore;
import de.zarncke.lib.jna.LinuxFunctions;
import de.zarncke.lib.jna.WindowsFunctions;
import de.zarncke.lib.log.Log;
import de.zarncke.lib.util.Chars;
import de.zarncke.lib.util.Misc;

/**
 * helper class with miscellaneous static methods.
 */
public final class IOTools {
	private static final int MAX_CREATE_TEMP_TRIES = 100;
	private static final int CRC_BUF_SIZE = 4096;
	private static final int FAST_NIO_COPY_BUFFER_SIZE = 16 * 1024;
	private static final int RANGE_OF_TEMP_FILE_IDS = 10000;

	private IOTools() {
		// helper
	}

	public static final OutputStream DEV_NULL = new TrashStream();

	private static final class DeleteOnExitThread extends Thread {
		private DeleteOnExitThread() {
			super("deleteOnExit");
		}

		@Override
		public void run() {
			List<File> copyToDelete;
			synchronized (toDelete) {
				copyToDelete = L.copy(toDelete);
				toDelete.clear();
			}
			Collections.reverse(copyToDelete);
			for (File file : copyToDelete) {
				try {
					deleteAll(file);
				} catch (Exception e) {
					Log.LOG.get().report(e);
				}
			}
		}
	}

	/**
	 * Copies data from {@link InputStream} to {@link OutputStream}.
	 */
	public static class StreamPump implements Runnable {
		private final InputStream in;
		private final OutputStream out;
		private boolean closeOutput = true;
		private boolean closeInput = true;

		private final int block;

		private IOException problem = null;

		private boolean done = false;
		private final Runnable completionHandler;

		private long volume;

		public StreamPump(final InputStream in, final OutputStream out, final Runnable completionHandler) {
			this(in, out, completionHandler, 1);
		}

		public StreamPump(final InputStream in, final OutputStream out, final Runnable completionHandler, final int block) {
			if (in == null) {
				throw Warden.spot(new IllegalArgumentException("in may not be null"));
			}
			if (out == null) {
				throw Warden.spot(new IllegalArgumentException("out may not be null"));
			}
			this.in = in;
			this.out = out;
			this.completionHandler = completionHandler;
			this.block = block;
		}

		/**
		 * Wait for completion and throw any problems encountered during copying.
		 *
		 * @throws IOException from copying
		 */
		public void problems() throws IOException {
			if (!this.done) {
				synchronized (this) {
					while (!this.done) {
						try {
							wait();
						} catch (InterruptedException ie) {
							// ignore
						}
					}
				}
			}
			if (this.problem != null) {
				throw Warden.spot(this.problem);
			}
		}

		@Override
		public void run() {
			if (this.done) {
				throw Warden.spot(new IllegalStateException("run() may only be called once."));
			}
			try {
				try {
					copyInternal();
					this.out.flush();
				} finally {
					try {
						if (this.closeInput) {
							this.in.close();
						}
					} finally {
						if (this.closeOutput) {
							this.out.close();
						}
					}
				}

				if (this.completionHandler != null) {
					this.completionHandler.run();
				}
			} catch (IOException ioe) {
				this.problem = ioe;
			} finally {
				synchronized (this) {
					this.done = true;
					notifyAll();
				}
			}
		}

		private void copyInternal() throws IOException {
			ReadableByteChannel inputChannel = Channels.newChannel(this.in);
			WritableByteChannel outputChannel = Channels.newChannel(this.out);
			fastChannelCopy(inputChannel, outputChannel);
			//
			// if (this.block <= 1) {
			// // this shorter version seems also to be faster
			// int b;
			// while ((b = this.in.read()) != -1) {
			// this.out.write(b);
			// this.volume++;
			// }
			// } else {
			// byte[] bs = new byte[this.block];
			//
			// while (true) {
			// int l = this.in.available();
			// if (l <= 0) {
			// int b = this.in.read();
			// if (b == -1) {
			// break;
			// }
			// this.out.write(b);
			// this.volume++;
			// } else {
			// if (l > this.block) {
			// l = this.block;
			// }
			// l = this.in.read(bs, 0, l);
			// if (l > 0) {
			// this.out.write(bs, 0, l);
			// this.volume += l;
			// }
			// }
			// }
			// }
		}

		public long getCopiedVolume() {
			return this.volume;
		}

		public void setCloseOutput(final boolean closeOutput) {
			this.closeOutput = closeOutput;
		}

		public void setCloseInput(final boolean closeInput) {
			this.closeInput = closeInput;
		}

		@Override
		public String toString() {
			return "copying " + this.in + " to " + this.out + (this.done ? " done" : "")
					+ (this.problem != null ? " with problems " + this.problem.getMessage() : "");
		}
	}

	/**
	 * Based on code from Coder Mr. Hitchen.
	 *
	 * @param src != null
	 * @param dest != null
	 * @throws IOException from NIO
	 */
	public static void fastChannelCopy(final ReadableByteChannel src, final WritableByteChannel dest) throws IOException {
		if (src instanceof FileChannel) {
			FileChannel fileChannel = (FileChannel) src;
			long size = fileChannel.size();
			long done = 0;
			while (done < size) {
				done += fileChannel.transferTo(done, size - done, dest);
			}
			return;
		}
		// if (dest instanceof FileChannel) {
		// final ByteBuffer buffer = ByteBuffer.allocateDirect(FAST_NIO_COPY_BUFFER_SIZE);
		// FileChannel fileChannel = (FileChannel) dest;
		// while (src.read(buffer) != -1) {
		// buffer.flip();
		// MappedByteBuffer dbb = null;
		// dbb =fileChannel. map(MapMode.READ_ONLY, 0, size);
		// int n = dbb.put(buffer);
		// // note: FileChannel copy uses some unmapping on the MappedByteBuffer
		// // fileChannel.transferFrom(buffer., 0, buffer.position());
		// buffer.clear();
		// }
		// return;
		// }
		final ByteBuffer buffer = ByteBuffer.allocateDirect(FAST_NIO_COPY_BUFFER_SIZE);
		while (src.read(buffer) != -1) {
			// prepare the buffer to be drained
			buffer.flip();
			// write to the channel, may block
			dest.write(buffer);
			// If partial transfer, shift remainder down
			// If buffer is empty, same as doing clear()
			buffer.compact();
		}
		// EOF will leave buffer in fill state
		buffer.flip();
		// make sure the buffer is fully drained.
		while (buffer.hasRemaining()) {
			dest.write(buffer);
		}
	}

	/**
	 * Copies all bytes from the input into the output stream.
	 * The output is flushed. Both streams are guaranteed to be closed.
	 * Returns when the transfer is complete (or an exception is thrown).
	 *
	 * @param in != null
	 * @param out != null
	 * @throws IOException on failure
	 */
	public static void copy(final InputStream in, final OutputStream out) throws IOException {
		copy(in, out, true, true);
	}

	public static void copy(final InputStream source, final OutputStream target, final boolean closeOutput) throws IOException {
		copy(source, target, true, closeOutput);
	}

	/**
	 * Copies all bytes from the input into the output stream.
	 * The output is flushed. The input stream is guaranteed to be closed.
	 * Returns when the transfer is complete (or an exception is thrown).
	 *
	 * @param in != null
	 * @param out != null
	 * @param closeOutput true: close output after copy, false: leave open, e.g. for writing more
	 * @param closeInput true: close input after copy, false: leave open, e.g. for reseting to a mark
	 * @throws IOException on failure
	 */
	public static void copy(final InputStream in, final OutputStream out, final boolean closeInput, final boolean closeOutput)
			throws IOException {
		StreamPump sp = new StreamPump(in, out, null);
		sp.setCloseOutput(closeOutput);
		sp.setCloseInput(closeInput);
		sp.run();
		sp.problems();
	}

	/**
	 * Copies all bytes from the input into the output stream.
	 * The operations is performed asynchronously.
	 * The output is flushed. Both streams are guaranteed to be closed eventually.
	 *
	 * @param in != null
	 * @param out != null
	 * @return The copy process is returned (can be used to wait for the result
	 */
	public static StreamPump copyAsync(final InputStream in, final OutputStream out) {
		return copyAsync(in, out, null);
	}

	/**
	 * Copies all bytes from the input into the output stream.
	 * The operations is performed asynchronously.
	 * The output is flushed. Both streams are guaranteed to be closed eventually.
	 *
	 * @param in != null
	 * @param out != null
	 * @param completionHandler called when processing is complete. May be null.
	 * @return The copy process is returned (can be used to wait for the result
	 */
	public static StreamPump copyAsync(final InputStream in, final OutputStream out, final Runnable completionHandler) {
		StreamPump sp = new StreamPump(in, out, completionHandler);
		new Thread(sp).start();
		return sp;
	}

	/**
	 * @param inputStream read as {@link Misc#UTF_8}
	 * @return String
	 * @throws IOException on failure
	 */
	public static CharSequence getAsString(final InputStream inputStream) throws IOException {
		// PERFORMANCE may use CharBuffer
		return new String(getAllBytes(inputStream), Misc.UTF_8);
	}

	/**
	 * Get all lines from the given InputStream.
	 * Uses UTF-8.
	 *
	 * @param ins to read
	 * @return List of all lines
	 * @throws IOException passed on
	 */
	public static List<String> getAllLines(final InputStream ins) throws IOException {
		final List<String> lines = L.l();
		new LineConsumer<List<String>>() {
			@Override
			protected void consume(final String line) {
				lines.add(line);
			}
		}.consume(new StreamStore(ins));
		return lines;
	}

	/**
	 * get all bytes from the given InputStream
	 *
	 * @param ins to purge
	 * @return byte[]
	 * @throws IOException passed on
	 */
	public static byte[] getAllBytes(final InputStream ins) throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		copy(ins, bout);
		return bout.toByteArray();
	}

	/**
	 * get all bytes from the given InputStream
	 *
	 * @param file read fully
	 * @return byte[]
	 * @throws IOException passed on
	 */
	public static byte[] getAllBytes(final File file) throws IOException {
		FileInputStream fis = new FileInputStream(file);
		byte[] d;
		try {
			d = getAllBytes(fis);
		} finally {
			forceClose(fis);
		}
		return d;
	}

	/**
	 * convenience close: works for null and catches IOex
	 *
	 * @param ins may be null
	 */
	public static void forceClose(final InputStream ins) {
		if (ins != null) {
			try {
				ins.close();
			} catch (IOException ioe) {
				Log.LOG.get().report(ioe);
			} catch (Exception e) {
				Log.LOG.get().report(new Exception("very unusual exception during close of " + ins, e));
			}
		}
	}

	/**
	 * convenience close.
	 *
	 * @param cls may be null
	 */
	public static void forceClose(final Closeable cls) {
		if (cls != null) {
			try {
				cls.close();
			} catch (IOException e) {
				Log.LOG.get().report(e);
			} catch (Exception e) {
				Log.LOG.get().report(new Exception("very unusual exception during close of " + cls, e));
			}
		}
	}

	/**
	 * convenience close.
	 *
	 * @param socket may be null
	 */
	public static void forceClose(final Socket socket) {
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
				Log.LOG.get().report(e);
			}
		}
	}

	/**
	 * convenience close.
	 *
	 * @param ch may be null
	 */
	public static void forceClose(final Channel ch) {
		if (ch != null) {
			try {
				ch.close();
			} catch (IOException e) {
				Log.LOG.get().report(e);
			}
		}
	}

	/**
	 * convenience close: works for null and catches IOex
	 *
	 * @param outs may be null
	 */
	public static void forceClose(final OutputStream outs) {
		if (outs != null) {
			try {
				outs.close();
			} catch (IOException ioe) {
				Log.LOG.get().report(ioe);
			}
		}
	}

	/**
	 * Try to delete file/directory. Recurses into directories.
	 *
	 * @param f != null, may not exist
	 * @return true if delete successful
	 */
	public static boolean deleteAll(final File f) {
		if (f == null) {
			return false;
		}
		if (!f.exists()) {
			return true;
		}

		if (f.isDirectory()) {
			File[] fs = f.listFiles();
			if (fs != null) {
				boolean anyFail = false;
				for (File element : fs) {
					anyFail = !deleteAll(element) || anyFail;
				}
				if (anyFail) {
					// we don't try to delete our parent because it doesn't make sense
					return false;
				}
			}
		}

		return f.delete();
	}

	private static List<File> toDelete = Collections.synchronizedList(L.<File> l());

	static {
		Runtime.getRuntime().addShutdownHook(new DeleteOnExitThread());
	}

	/**
	 * Calls {@link #deleteAll(File)} for each registered File <em>on exit</em>.
	 *
	 * @param file to delete
	 */
	public static void deleteOnExit(@Nonnull final File file) {
		toDelete.add(file);
	}

	/**
	 * create a temporary directory.
	 *
	 * @param prefix of the temp dir
	 * @return File != null
	 */
	public static File createTempDir(final String prefix) {
		int idx = new java.util.Random().nextInt(RANGE_OF_TEMP_FILE_IDS);

		int count = 0;
		File f = null;
		do {
			f = new File(getTempDir(), prefix + (idx + count));
			if (count++ > MAX_CREATE_TEMP_TRIES) {
				throw Warden.spot(new NotAvailableException("cannot create temp file " + new File(getTempDir(), prefix)
						+ "* after " + MAX_CREATE_TEMP_TRIES + " tries"));
			}
		} while (!f.mkdir());

		return f;
	}

	public static File getTempDir() {
		String tmp = System.getProperty("java.io.tmpdir", null);
		if (tmp == null) {
			if (File.pathSeparatorChar == '\\') {
				tmp = "C:\\TEMP";
			} else {
				tmp = "/tmp";
			}
		}
		return new File(tmp);
	}

	/**
	 * converts a String into a valid filename.
	 *
	 * @param name != null
	 * @return String without any of "\\/: "
	 */
	public static String cleanName(final String name) {
		return Chars.replace(
				Chars.replace(Chars.replace(Chars.replace(Chars.replace(name, "_", "_-"), "\\", "_1"), "/", "_2"), ":", "_3"),
				" ", "__");
	}

	/**
	 * Uses UTF-8 for dumping the data.
	 *
	 * @param data != null
	 * @param os to write into
	 * @throws IOException
	 */
	public static void dump(final CharSequence data, final OutputStream os) throws IOException {
		OutputStreamWriter osw = new OutputStreamWriter(os, Misc.UTF_8);
		try {
			dump(data, osw);
		} finally {
			osw.close();
		}
	}

	public static void dump(final CharSequence data, final Writer writer) throws IOException {
		writer.append(data);
	}

	public static void dump(final CharSequence data, final Store store) throws IOException {
		dump(data, store.getOutputStream(false));
	}

	public static void dump(final CharSequence data, final File f) throws IOException {
		FileOutputStream fos = new FileOutputStream(f);
		try {
			dump(data, fos);
		} finally {
			fos.close();
		}
	}

	public static void dump(final byte[] data, final File f) throws IOException {
		FileOutputStream fos = new FileOutputStream(f);
		try {
			dump(data, fos);
		} finally {
			fos.close();
		}
	}

	public static void dump(final byte[] data, final OutputStream os) throws IOException {
		os.write(data);
	}

	public static long crc32(final InputStream in) throws IOException {
		CRC32 crc = new CRC32();
		byte[] ba = new byte[CRC_BUF_SIZE];
		while (true) {
			int n = in.read(ba);
			if (n < 0) {
				break;
			}
			if (n > 0) {
				crc.update(ba, 0, n);
			}
		}
		return crc.getValue();
	}

	public static String getNormalizedPath(final String path) {
		if (File.separatorChar == '\\') {
			return path.replaceAll("\\\\", "/");
		}
		return path;
	}

	/**
	 * Wraps the given input stream such that received data in gzip-compressed on the fly.
	 * Note: This is different from {@link GZIPInputStream} which *decompresses* data on the fly.
	 * Uses pips and a temporary thread to do the decompression.
	 *
	 * @param input {@link InputStream} (uncompressed)
	 * @param buffer size in bytes to use
	 * @return wrapped {@link InputStream} (compressed)
	 * @throws IOException
	 */
	@Nonnull
	public static InputStream gzipOnTheFly(@Nonnull final InputStream input, final int buffer) throws IOException {
		PipedOutputStream pipedOutput = new PipedOutputStream();
		PipedInputStream pipedInput = new PipedInputStream(pipedOutput, buffer);
		GZIPOutputStream gzippingOutput = new GZIPOutputStream(pipedOutput, buffer);
		IOTools.copy(input, gzippingOutput, true, true);
		return pipedInput;
	}

	public static boolean isHardlinkSupported() {
		try {
			if (Misc.isLinux()) {
				LinuxFunctions.init();
				return true;
			} else if (Misc.isWindows()) {
				WindowsFunctions.init();
				return true;
			}
		} catch (Exception e) {
			return false;
		}
		return false;
	}

	/**
	 * Create a hard link.
	 *
	 * @param src existing file
	 * @param dest target location of the hard link
	 * @throws IOException if the operation fails
	 * @throws UnsupportedOperationException if OS doesn't support
	 */
	public static void hardlink(final File src, final File dest) throws IOException {
		if (Misc.isLinux()) {
			LinuxFunctions.hardlink(src, dest);
			return;
		} else if (Misc.isWindows()) {
			WindowsFunctions.hardlink(src, dest);
			return;
		}
		throw Warden.spot(new UnsupportedOperationException("cannot do hard links on " + System.getProperty("os.name")));
	}

	/**
	 * @param fileOnFileSystem a file which may be used to determine the filesystem, may be null
	 * @return free size and total size of disk in bytes (of the filesystem where the file resides), null if not
	 * available
	 */
	public static Pair<Long, Long> getAvailableSystemDiskBytes(final File fileOnFileSystem) {
		if (Misc.isWindows()) {
			return WindowsFunctions.getAvailableSystemDiskBytesWindows(fileOnFileSystem);
		}
		// else just try Linux
		return LinuxFunctions.getAvailableSystemDiskBytes(fileOnFileSystem);
	}

	/**
	 * Creates an InputStream which makes the contents of the OutputStream available immediatly.
	 *
	 * @return a Pair of OutputStream, InputStream
	 */
	public static Pair<OutputStream, InputStream> createPipe() {
		final Queue<byte[]> buffers = new ConcurrentLinkedQueue<byte[]>();
		OutputStream outs = new OutputStream() {
			@Override
			public void write(final int b) throws IOException {
				buffers.add(new byte[] { (byte) b });
			}

			@Override
			public void write(final byte[] b, final int off, final int len) throws IOException {
				if (len == 0) {
					return;
				}
				byte[] ba;
				if (len != b.length) {
					ba = new byte[len];
					System.arraycopy(b, off, ba, 0, len);
				} else {
					ba = b;
				}
				buffers.add(ba);
			}

			@Override
			public void close() throws IOException {
				buffers.add(Elements.NO_BYTES);
			}

		};
		InputStream ins = new InputStream() {
			byte[] buf = Elements.NO_BYTES;
			int off = 0;

			@Override
			public int read() throws IOException {
				while (!fill()) {
					; // wait for data
				}
				if (this.buf == null) {
					return -1;
				}
				return this.buf[this.off++];
			}

			@Override
			public int read(final byte[] b, final int offset, final int len) throws IOException {
				fill();
				if (this.buf == null) {
					return -1;
				}
				int remain = this.buf.length - this.off;
				if (len > remain) {
					System.arraycopy(this.buf, this.off, b, offset, remain);
					this.off += remain;
					return remain;
				}
				System.arraycopy(this.buf, this.off, b, offset, len);
				this.off += len;
				return len;
			}

			@Override
			public int available() throws IOException {
				fill();
				if (this.buf == null) {
					return 0;
				}
				return this.buf.length - this.off;
			}

			/**
			 * @return true: at least one byte is available or EOF has been reached
			 */
			private boolean fill() {
				if (this.buf == null || this.off < this.buf.length) {
					return true;
				}
				while (true) {
					this.buf = buffers.poll();
					if (this.buf != null) {
						this.off = 0;
						if (this.buf.length == 0) {
							this.buf = null;
						}
						return true;
					}
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						return false;
					}
				}
			}
		};
		return Pair.pair(outs, ins);
	}

	/**
	 * Tries to zero a section of a sparse file.
	 * This is also called 'punching a hole'.
	 * Notes:
	 * <ul>
	 * <li>Only supported on some file systems, e.g. ntfs, btrfs, tmpfs. Fuse can support it. XFS can depending on version.</li>
	 * <li>Implementation is OS specific, uses JNA and is slow! Do not use for holes &lt; 1 MB.</li>
	 * <li>Implementation is sensitive to file permissions because Java accsss and and OS/JNA access appear to see different
	 * file permissions</li>
	 * <li></li>
	 * </ul>
	 *
	 * @param file to punch hole into
	 * @param position offset
	 * @param length length of hole
	 * @return true: successfully punched hole; false: operation not supported by file system
	 * @throws IOException operation supported but failed
	 */
	public static boolean setSparseZeros(final File file, final long position, final long length) throws IOException {
		if (Misc.isWindows()) {
			return WindowsFunctions.setSparseZeros(file, position, length);
		}
		// best effort
		return LinuxFunctions.setSparseZeros(file, position, length);
	}

	public static InputStream streamBytes(final byte[] bytes) {
		return new ByteArrayInputStream(bytes);
	}

	public static InputStream streamBytes(final CharSequence chars, final Charset encoding) {
		return streamBytes(chars.toString().getBytes(encoding));
	}
}
