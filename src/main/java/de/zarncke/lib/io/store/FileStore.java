package de.zarncke.lib.io.store;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Iterator;

import de.zarncke.lib.coll.EmptyIterator;
import de.zarncke.lib.data.HasName;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.io.IOTools;
import de.zarncke.lib.region.ByteBufferRegion;
import de.zarncke.lib.region.Region;

/**
 * A Store which is based on {@link File}.
 * Creates actual files and/or directories on demand.
 *
 * @author Gunnar Zarncke
 */
public class FileStore implements Store, MovableStore {
	private final File file;

	private final Store parent;
	private final boolean allowParent;

	public FileStore(final String file) {
		this(new File(file), false);
	}

	public FileStore(final String file, final boolean allowParent) {
		this(new File(file), allowParent);
	}

	public FileStore(final File baseDirOrFile) {
		this(baseDirOrFile, null, false);
	}

	public FileStore(final File baseDirOrFile, final boolean allowParent) {
		this(baseDirOrFile, null, allowParent);
	}

	public FileStore(final File baseDirOrFile, final Store parent) {
		this(baseDirOrFile, parent, false);
	}

	protected FileStore(final File baseDirOrFile, final Store parent, final boolean allowParent) {
		this.file = baseDirOrFile;
		this.parent = parent;
		this.allowParent = allowParent;
	}

	@Override
	public void moveTo(final MovableStore target) throws IOException {
		if (target instanceof FileStore) {
			((FileStore) target).ensureParents();
			if (!getFile().renameTo(((FileStore) target).getFile())) {
				throw Warden.spot(new IOException("renaming " + this + " to " + target + " failed"));
			}
		} else {
			throw Warden.spot(new UnsupportedOperationException("cannot rename to " + target + " (not a FileStore)."));
		}
	}

	public FileStore(final FileStore parent, final String name) {
		this(new File(parent.file, name), parent);
	}

	@Override
	public boolean exists() {
		return this.file.exists();
	}

	@Override
	public Store element(final String name) {
		return wrap(name);
	}

	@Override
	public long getLastModified() {
		long lm = this.file.lastModified();
		return lm == 0 ? UNKNOWN_MODIFICATION : lm;
	}

	@Override
	public boolean setLastModified(final long millis) {
		return this.file.setLastModified(millis);
	}

	@Override
	public String getName() {
		return this.file.getName();
		// TODO: better this way: return parent==null?file.getAbsolutePath(): this.file.getName();
	}

	@Override
	public Iterator<Store> iterator() {
		if (!this.file.isDirectory()) {
			return EmptyIterator.getInstance();
		}
		String[] list = listFiles();
		if (list == null) {
			return EmptyIterator.getInstance();
		}
		final Iterator<String> fi = Arrays.asList(list).iterator();
		return new Iterator<Store>() {
			FileStore last;

			@Override
			public boolean hasNext() {
				return fi.hasNext();
			}

			@Override
			public Store next() {
				this.last = wrap(fi.next());
				return this.last;
			}

			@Override
			public void remove() {
				this.last.delete();
			}
		};
	}

	protected String[] listFiles() {
		return this.file.list();
	}

	@Override
	public boolean canRead() {
		return this.file.isFile() && this.file.canRead();
	}

	@Override
	public boolean canWrite() {
		return !this.file.isDirectory() && (!this.file.exists() || this.file.canWrite());
	}

	@Override
	public InputStream getInputStream() {
		try {
			return new FileInputStream(this.file);
		} catch (FileNotFoundException e) {
			return null;
		}
	}

	/**
	 * Uses new {@link FileOutputStream}.
	 * Note: Does not delete the file before open (as usual). Consider the effect of hard-links.
	 */
	@Override
	public OutputStream getOutputStream(final boolean append) throws IOException {
		ensureParents();
		try {
			return new FileOutputStream(this.file, append);
		} catch (FileNotFoundException e) {
			throw (IOException) new IOException("cannot write").initCause(e);
		}
	}

	private void ensureParents() {
		File par = this.file.getParentFile();
		if (par != null && !par.exists()) {
			par.mkdirs();
		}
	}

	@Override
	public long getSize() {
		if (this.file.isFile()) {
			return this.file.length();
		}
		if (this.file.isDirectory()) {
			long total = 0;
			for (Store s : this) {
				total += s.getSize();
			}
			return total;
		}
		return 0;
	}

	@Override
	public Store getParent() {
		if (this.parent == null && this.allowParent) {
			return new FileStore(this.getFile().getParentFile(), true);
		}
		return this.parent;
	}

	@Override
	public String toString() {
		return this.file.toString();
	}

	@Override
	public boolean delete() {
		return IOTools.deleteAll(this.file);
	}

	/**
	 * The region cannot be used to change the contents of the file yet.
	 */
	@Override
	public Region asRegion() throws IOException {
		// TODO Open the file and then get a channel from the stream.
		RandomAccessFile raf = new RandomAccessFile(this.file, "rw");
		try {
			FileChannel channel = raf.getChannel();
			long size = channel.size();

			ByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, size);

			return new ByteBufferRegion(buffer);
		} finally {
			raf.close();
		}
	}

	@Override
	public boolean iterationSupported() {
		if (!this.file.isDirectory() && this.file.exists()) {
			return false;
		}
		return true;
	}

	@Override
	public int compareTo(final HasName named) {
		return getName().compareTo(named.getName());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.file == null ? 0 : this.file.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		FileStore other = (FileStore) obj;
		if (this.file == null) {
			if (other.file != null) {
				return false;
			}
		} else if (!this.file.equals(other.file)) {
			return false;
		}
		return true;
	}

	/**
	 * For better compatibility use {@link #getFile(Store)}.
	 *
	 * @return the File wrapped by this Store
	 */
	public File getFile() {
		return this.file;
	}

	/**
	 * Allows derived classes to supply their wrapper for child elements.
	 *
	 * @param name of child
	 * @return FileStore
	 */
	protected FileStore wrap(final String name) {
		return new FileStore(this, name);
	}

	/**
	 * Tries to extract a File object denoting the contents of the given Store.
	 *
	 * @param store any, may or may not directly or indirectly denote a File, may be null
	 * @return File or null if store is null or no equivalent File can be determined
	 */
	public static File getFile(final Store store) {
		if (store instanceof FileStore) {
			return ((FileStore) store).getFile();
		}
		if (store instanceof DelegateStore) {
			return getFile(((DelegateStore) store).getDelegate());
		}
		return null;
	}

}
