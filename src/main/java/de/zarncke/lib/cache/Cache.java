package de.zarncke.lib.cache;

import java.io.Externalizable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

import de.zarncke.lib.err.CantHappenException;
import de.zarncke.lib.err.ExceptionUtil;
import de.zarncke.lib.err.NotAvailableException;
import de.zarncke.lib.io.IOTools;
import de.zarncke.lib.log.Log;

/**
 * This class realizes a file cache for large Objects.
 * If memory runs short (the soft references are reclaimed)
 * the content held by this cache is written to file.
 * but can be recovered every time.<br>
 * The only requirement is, that the data is Serializable.
 * Please note, that this will only work, if no other references
 * onto the data exist.<br>
 * Please note, that only the first reference is tracked.
 * That means, that if the user of the Cache has further references
 * into the inner structure of the cached Object (-tree)
 * these structures will duplicated after recovery.
 *
 * @param <T> type of cached value
 */
@Deprecated
public class Cache<T extends Serializable> implements Externalizable {
	private static class Holder<T extends Serializable> {
		private final Cache<T> parent;
		private final T data;

		public Holder(final Cache<T> parent, final T data) {
			this.parent = parent;
			this.data = data;
		}

		public T get() {
			return this.data;
		}

		@Override
		protected void finalize() throws Throwable { // NOPMD super
			this.parent.store(this.data);
			super.finalize();
		}
	}

	/**
	 * this is a soft reference to the cached data
	 */
	private transient Reference<Holder<T>> cache;

	/**
	 * where the data is held persistently
	 */
	private File file = null;

	/**
	 * Creates a file cache for the data
	 *
	 * @param data the data to be cached
	 */
	public Cache(final T data) {
		this.cache = new SoftReference<Holder<T>>(new Holder<T>(this, data));
	}

	private synchronized void store(final T data) {
		while (true) {
			// here I find it not unlikely, that we will run out of
			// memory, so I cache this case and give others a try.
			try {
				ObjectOutputStream oos = null;
				try {
					this.file = File.createTempFile("cache", ".data");
					oos = new ObjectOutputStream(new FileOutputStream(this.file));
					this.file.deleteOnExit();

					oos.writeObject(data);
					oos.flush();
					oos.close();
					oos = null;
					this.cache = null;
				} catch (IOException ioe) {
					// TOD to exception or not to exception?
					Log.LOG.get().report(
							"cannot write cache data to file " + this.file + " due to " + ioe + ". keep it in memory.");
					// we cannot write out, so we recover the link
					this.cache = new SoftReference<Holder<T>>(new Holder<T>(this, data));
					this.file = null;
				} finally {
					IOTools.forceClose(oos);
				}
				break;
			} catch (OutOfMemoryError oome) {
				try {
					wait(10);
				} catch (InterruptedException ie) {
					;
				}
				// retry
			} catch (RuntimeException ex) {
				ExceptionUtil.emergencyAlert("cache is out of luck", ex);
				throw ex;
			}
		}
	}

	/**
	 * get the data back from the case
	 *
	 * @return the Serializable object
	 * @throws NotAvailableException if data got lost
	 */
	public T get() throws NotAvailableException {
		Holder<T> h = null;
		if (this.cache != null) {
			h = this.cache.get();
		}
		if (h != null) {
			// use cache data directly
			Holder<T> hh = h;
			return hh.get();
		}
		// recover data from file

		// ensure finalization has run
		if (this.file == null) {
			Runtime.getRuntime().runFinalization();
			if (this.file == null) {
				throw new CantHappenException("either the data or the file should be there!");
			}
		}

		// recover
		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(new FileInputStream(this.file));
			T readObject = (T) ois.readObject();
			return readObject;
		} catch (IOException ioe) {
			throw new NotAvailableException("cannot recover data from file " + this.file, ioe);
		} catch (ClassNotFoundException cnfe) {
			throw new NotAvailableException("cannot deserialize data from file " + this.file, cnfe);
		} finally {
			IOTools.forceClose(ois);
		}
	}

	@Override
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
		T readObject = (T) in.readObject();
		this.cache = new SoftReference<Holder<T>>(new Holder<T>(this, readObject));
		this.file = null;
	}

	@Override
	public void writeExternal(final ObjectOutput out) throws IOException {
		out.writeObject(get());
	}

	/**
	 * @return String
	 */
	@Override
	public String toString() {
		return "cache for " + get();
	}
}
