package de.zarncke.lib.io.store;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.diff.ContentComparer;
import de.zarncke.lib.diff.Diff;
import de.zarncke.lib.err.CantHappenException;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.io.IOTools;
import de.zarncke.lib.io.Path;
import de.zarncke.lib.io.store.MapStore.CreateMode;
import de.zarncke.lib.io.store.ext.EnhanceBaseStore;
import de.zarncke.lib.io.store.ext.EnhancedStore;
import de.zarncke.lib.io.store.ext.Enhancer;
import de.zarncke.lib.io.store.ext.StoreGroup;
import de.zarncke.lib.log.Log;
import de.zarncke.lib.region.Region;
import de.zarncke.lib.region.RegionUtil;
import de.zarncke.lib.util.Chars;
import de.zarncke.lib.util.InterruptionMode;
import de.zarncke.lib.util.Misc;

public final class StoreUtil {

	private static final String TEMP_DIR_SUFFIX = ".dir";
	private static final String DEFAULT_PATH_SEPARATOR_REG_EXP = "/";
	public static final String DEFAULT_PATH_SEPARATOR = DEFAULT_PATH_SEPARATOR_REG_EXP;
	static final int PARENT_LIMIT = 1000;

	public static final Comparator<Store> COMPARE_BY_NAME = new Comparator<Store>() {
		@Override
		public int compare(final Store o1, final Store o2) {
			return o1.getName().compareTo(o2.getName());
		}
	};

	public static final Store STD_ERR = new AbstractStore() {
		@Override
		public String getName() {
			return "stderr";
		}

		@Override
		public OutputStream getOutputStream(final boolean append) throws IOException {
			return System.err;
		}

		@Override
		public boolean canWrite() {
			return true;
		}

		@Override
		public boolean exists() {
			return true;
		}
	};

	public static final Store STD_OUT = new AbstractStore() {
		@Override
		public String getName() {
			return "stdout";
		}

		@Override
		public OutputStream getOutputStream(final boolean append) throws IOException {
			return System.out;
		}

		@Override
		public boolean canWrite() {
			return true;
		}

		@Override
		public boolean exists() {
			return true;
		}
	};

	public static final Store STD_IN = new AbstractStore() {
		@Override
		public String getName() {
			return "stdin";
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return System.in;
		}

		@Override
		public boolean canRead() {
			return true;
		}

		@Override
		public boolean exists() {
			return true;
		}
	};

	/**
	 * Marker for different ways to customize copying.
	 */
	public interface CopyStrategy { // marker
	}

	/**
	 * How to handle overwrites.
	 *
	 * @author gunnar
	 */
	public interface OverwriteStrategy extends CopyStrategy {
		/**
		 * Called when the target already exists.
		 * No copying is done.
		 * Implementations may copy or do any other necessary tasks (delete, move, archive,...).
		 *
		 * @param source to copy
		 * @param target to copy to
		 * @throws IOException may be forwarded, need not be wrapped
		 */
		void handleExisting(Store source, Store target) throws IOException;
	}

	/**
	 * Directs copying.
	 *
	 * @author gunnar
	 */
	public interface DirectorStrategy extends CopyStrategy {
		/**
		 * Called before any single Store is copied.
		 * Can prevent further processing of the element.
		 * May perform monitoring or replacement operations.
		 *
		 * @param source about to be copied
		 * @param target to be copied to
		 * @return true: proceed with copying node recursively; false: do not further process this Store
		 * @throws IOException
		 */
		boolean beforeCopy(Store source, Store target) throws IOException;
	}

	public static final OverwriteStrategy SKIP = new OverwriteStrategy() {
		@Override
		public void handleExisting(final Store source, final Store target) {
			// skip
		}
	};

	/**
	 * This strategy can be useful if it is very likely that same size implies same file, e.g. for non-raw images.
	 */
	public static final OverwriteStrategy SKIP_IF_SAME_SIZE = new OverwriteStrategy() {
		@Override
		public void handleExisting(final Store source, final Store target) throws IOException {
			if (source.getSize() != target.getSize()) {
				copy(source, target);
			}
		}
	};

	/**
	 * Strategy which just overwrites an existing file with the new source.
	 * CAUTION: If source and target are different paths to the same file (e.g. hard-links) then this copy will corrupt
	 * the file!
	 * If you use e.g. {@link #hashCode()} then you should consider {@link #DELETE_AND_COPY} or
	 * {@link #DELETE_AND_HARDLINK}.
	 */
	public static final OverwriteStrategy OVERWRITE = new OverwriteStrategy() {
		@Override
		public void handleExisting(final Store source, final Store target) throws IOException {
			copy(source, target);
		}
	};

	/**
	 * Strategy which just overwrites an existing file with the new source AFTER deleting the target.
	 * This is safe with hard-links.
	 */
	public static final OverwriteStrategy DELETE_AND_COPY = new OverwriteStrategy() {
		@Override
		public void handleExisting(final Store source, final Store target) throws IOException {
			target.delete();
			copy(source, target);
		}
	};

	/**
	 * Strategy which hardlinks an existing file with the new source after deleting the target.
	 */
	public static final OverwriteStrategy DELETE_AND_HARDLINK = new OverwriteStrategy() {
		@Override
		public void handleExisting(final Store source, final Store target) throws IOException {
			target.delete();
			hardLink(source, target);
		}
	};

	/**
	 * Strategy which fails when encountering an existing target file.
	 * Note: Already copied files are left as is.
	 */
	public static final OverwriteStrategy FAIL = new OverwriteStrategy() {
		@Override
		public void handleExisting(final Store source, final Store target) throws IOException {
			throw new IOException("overwriting existing file " + target + " is not allowed");
		}
	};

	/**
	 * Calls {@link MutableStore#behaveAs(Store)} instead of {@link #copy(Store, Store)}.
	 */
	public static final OverwriteStrategy TRANSMUTATE = new OverwriteStrategy() {
		@Override
		public void handleExisting(final Store source, final Store target) throws IOException {
			if (!(source instanceof MutableStore)) {
				throw Warden.spot(new IllegalArgumentException(source + " is not mutable"));
			}
			((MutableStore) target).behaveAs(source);
		}
	};

	public static final Collection<String> FILE_EXTENSONS_OF_COMPRESSED_FILES = L.l(".tgz", ".tar.gz", ".gz", ".zip",
			".jpg", ".jpeg", ".png", ".gif");

	static Set<Store> storesToDeleteOnExit = new LinkedHashSet<Store>();
	static {
		Runtime.getRuntime().addShutdownHook(new DeleteOnExitThread());
	}

	private static final class DeleteOnExitThread extends Thread {
		private DeleteOnExitThread() {
			super("deleteStoresOnExit");
		}

		@Override
		public void run() {
			List<Store> copyToDelete;
			synchronized (storesToDeleteOnExit) {
				copyToDelete = L.copy(storesToDeleteOnExit);
				copyToDelete.clear();
			}
			Collections.reverse(copyToDelete);
			for (Store file : copyToDelete) {
				try {
					file.delete();
				} catch (Exception e) {
					Log.LOG.get().report(e);
				}
			}
		}
	}

	private StoreUtil() {
		// helper
	}

	public static <T extends EnhancedStore<T>> T enhance(final Store store, final Enhancer<T> enhancer) {
		return enhancer.enhance(new EnhanceBaseStore<T>(store, enhancer));
	}


	/**
	 * Resolves the path by traversing the Store.
	 * If the path starts with a separator, then the root ancestor (top most parent) of the store is used as a base.
	 * Otherwise the store is the base and the path is relative to it.
	 * If the path is empty or null the store itself is returned.
	 *
	 * @param root store to start at != null
	 * @param path to follow, components separated by
	 * @param separatorRegExp separating path components.
	 * @return Store found, may be {@link AbsentStore} which can be tested by {@link Store#exists()}
	 */
	public static Store resolvePath(final Store root, final String path, final String separatorRegExp) {
		if (Chars.isEmpty(path)) {
			return root;
		}

		String effectivePath = path;
		Store storeCandidate = root;
		// like path.startsWith but with RegExp
		Matcher startsWith = Pattern.compile(separatorRegExp).matcher(path);
		if (startsWith.lookingAt()) {
			storeCandidate = determineRootStore(root);
			effectivePath = path.substring(startsWith.end());
		}
		if (storeCandidate == null) {
			return new AbsentStore(null);
		}
		if (effectivePath.length() == 0) {
			return storeCandidate;
		}

		String[] segs = effectivePath.split(separatorRegExp);
		for (String seg : segs) {
			storeCandidate = storeCandidate.element(seg);
		}
		return storeCandidate;
	}

	/**
	 * Traverse {@link Store#getParent()} up to the root.
	 *
	 * @param store to find root for
	 * @return Store which is the root
	 * @throws IllegalArgumentException if search appears to loop
	 */
	@Nonnull
	public static Store determineRootStore(final Store store) {
		Store rootCandidate = store;
		int i = 0;
		while (rootCandidate.getParent() != null) {
			rootCandidate = rootCandidate.getParent();
			if (i++ > PARENT_LIMIT) {
				throw Warden.spot(new IllegalArgumentException("The Store " + store + " seems to have an infinite (>"
						+ PARENT_LIMIT + ") number of ancestors!"));
			}
		}
		return rootCandidate;
	}

	/**
	 * @param ancestor to build path from (may be null, indicating to root)
	 * @param descendant to build path to
	 * @param pathSeparator to put between path segments
	 * @return String, may be empty if ancestor is descendant
	 */
	public static String buildPathFromTo(final Store ancestor, final Store descendant, final String pathSeparator) {
		if (sameStore(ancestor, descendant)) {
			return "";
		}
		int i = 0;
		StringBuilder path = new StringBuilder();
		Store current = descendant;
		while (!sameStore(current, ancestor)) {
			if (current == null) {
				throw Warden.spot(new IllegalArgumentException("didn't find path from " + ancestor + " to "
						+ descendant + " maybe one is differently wrapped"));
			}
			if (path.length() == 0) {
				if (current.getName() != null) {
					path.append(current.getName());
				}
			} else {
				path.insert(0, pathSeparator);
				if (current.getName() != null) {
					path.insert(0, current.getName());
				}
			}
			current = current.getParent();
			if (i++ > PARENT_LIMIT) {
				throw Warden.spot(new IllegalArgumentException("The Store " + descendant
						+ " seems to have an infinite (>" + PARENT_LIMIT + ") number of ancestors!"));
			}
		}
		return path.toString();
	}

	public static boolean sameStore(final Store a, final Store b) {
		if (a == null) {
			return b == null;
		}
		if (b == null) {
			return false;
		}
		if (a == b || a.equals(b)) {
			return true;
		}
		if (a.exists() != b.exists()) {
			return false;
		}
		return DelegateStore.unwrap(a).equals(DelegateStore.unwrap(b));
	}

	/**
	 * Determine path from the root ancestor of the store to the store.
	 *
	 * @param store != null
	 * @param separator to put between path components
	 * @return path != null, may be empty if the store is the root and has no parents
	 */
	public static CharSequence formPath(final Store store, final String separator) {
		if (store == null) {
			throw Warden.spot(new IllegalArgumentException("store must be given"));
		}
		StringBuilder path = new StringBuilder();
		Store parent = store;
		while (parent != null) {
			if (path.length() > 0) {
				path.insert(0, separator);
			}
			path.insert(0, parent.getName());
			parent = parent.getParent();
		}
		return path;
	}

	/**
	 * Filter the elements of a Store by regular expression.
	 *
	 * @param listableStore != null
	 * @param regExp != null (think of escaping ".")
	 * @return Collection of Store, may be empty.
	 */
	public static Collection<Store> listFilteredByName(final Store listableStore, final String regExp) {
		Pattern compiledRegex = Pattern.compile(regExp);
		Collection<Store> res = L.l();
		for (Store store : listableStore) {
			if (compiledRegex.matcher(store.getName()).matches()) {
				res.add(store);
			}
		}
		return res;
	}

	/**
	 * Tries to convert an Object into a Store for accessing it.
	 *
	 * @param object != null, anything that can be converted into a Store (no general Objects yet)
	 * @param parent Store, may be null
	 * @return Store != null
	 * @throws IllegalArgumentException
	 */
	public static Store asStore(final Object object, final Store parent) {
		if (object == null) {
			throw Warden.spot(new IllegalArgumentException("Object may not be null"));
		}
		if (object instanceof Store) {
			return (Store) object;
		}
		if (object instanceof File) {
			return new FileStore((File) object, parent);
		}
		if (object instanceof CharSequence) {
			return new MemStore(RegionUtil.asRegion(object.toString().getBytes(Misc.UTF_8)), null, parent);
		}
		if (object instanceof byte[]) {
			return new MemStore(RegionUtil.asRegion((byte[]) object), null, parent);
		}
		if (object instanceof Region) {
			return new MemStore((Region) object, null, parent);
		}
		throw Warden.spot(new IllegalArgumentException("cannot convert " + object + " into a Store."));
	}

	/**
	 * Copy a Store into another Store. Note: The source must be readable and the target must be writable (no
	 * directory).
	 * NOTE: If the target exists its contents will be overwritten without deleting the file before. This means that
	 * hard-linked references are updated too. If this is not wanted then delete the target before.
	 * CAUTION: If source and target are different paths to the same file (e.g. hard-links) then this copy will corrupt
	 * the file!
	 *
	 * @param source != null
	 * @param target != null
	 * @throws IOException on failures
	 */
	public static void copy(final Store source, final Store target) throws IOException {
		// CAUTION: This tests only paths not file identity; copying hardlinked files will corrupt the file!
		if (source.equals(target)) {
			return;
		}
		if (!source.exists()) {
			return;
		}
		if (!source.canRead()) {
			throw Warden.spot(new IOException("cannot read " + source));
		}
		if (!target.canWrite()) {
			throw Warden.spot(new IOException("cannot write " + target));
		}
		int bufferSize = (int) (16 * Misc.BYTES_PER_KB);
		long size = source.getSize();
		if (size != Accessible.UNKNOWN_SIZE && size > Misc.BYTES_PER_GB) {
			bufferSize = (int) Misc.BYTES_PER_MB;
		} else if (size != Accessible.UNKNOWN_SIZE && size > 10 * Misc.BYTES_PER_MB) {
			bufferSize = (int) (64 * Misc.BYTES_PER_KB);
		}
		InputStream sourceStream = source.getInputStream();
		if (!(sourceStream instanceof BufferedInputStream)) {
			sourceStream = new BufferedInputStream(sourceStream, bufferSize);
		}
		OutputStream targetStream = target.getOutputStream(false);
		if (!(targetStream instanceof BufferedOutputStream)) {
			targetStream = new BufferedOutputStream(targetStream, bufferSize);
		}
		try {
			IOTools.copy(sourceStream, targetStream);
		} catch (Exception e) {
			throw Warden.spot(new IOException("cannot copy " + source + " into " + target, e));
		}
	}

	/**
	 * Copy a Store recursively into another Store using hard-linking where possible.
	 *
	 * @param source != null
	 * @param target != null
	 * @param overwrite strategy to use when (sub) target already exists (e.g. {@link #OVERWRITE}).
	 * @param imode strategy to use for handling interruptions
	 * @throws InterruptedException
	 * @throws IOException on failures
	 */
	public static void hardlinkRecursiveInterruptible(final Store source, final Store target,
			final OverwriteStrategy overwrite, final InterruptionMode imode) throws InterruptedException, IOException {
		if (Thread.interrupted()) {
			if (imode != InterruptionMode.IGNORE) {
				throw Warden.spot(new InterruptedException("interrupted during before copying " + source));
			}
		}
		if (source.canRead()) {
			if (target.canRead()) {
				overwrite.handleExisting(source, target);
			} else {
				hardLink(source, target);
			}
		} else if (source.iterationSupported()) {
			for (Store child : source) {
				hardlinkRecursiveInterruptible(child, target.element(child.getName()), overwrite, imode);
			}
		} else {
			throw Warden.spot(new IllegalArgumentException("cannot copy recursively as the source " + source
					+ " cannot be read and doesn't support iteration."));
		}
	}

	/**
	 * Copy a Store and all of its elements into another Store.
	 * Note: The source must be readable or {@link Store#iterationSupported() iterable}.
	 * If source and target denote files and rsync is in the path, then it will be used.
	 *
	 * @param source != null
	 * @param target != null
	 * @param overwrite strategy to use when (sub) target already exists (e.g. {@link #OVERWRITE}).
	 * @throws IOException on failures
	 * @throws IllegalArgumentException if the source is neither readable nor iterable.
	 */
	public static void copyRecursiveOptimized(final Store source, final Store target, final CopyStrategy overwrite)
			throws IOException {
		try {
			copyRecursiveOptimizedInterruptible(source, target, overwrite, InterruptionMode.IGNORE);
		} catch (InterruptedException e) {
			throw Warden.spot(new CantHappenException("shoul have been ignored", e));
		}
	}

	/**
	 * Copy a Store and all of its elements into another Store.
	 * Note: The source must be readable or {@link Store#iterationSupported() iterable}.
	 * If source and target denote files and rsync is in the path, then it will be used.
	 *
	 * @param source != null
	 * @param target != null
	 * @param overwrite strategy to use when (sub) target already exists (e.g. {@link #OVERWRITE}).
	 * @param imode strategy to use for handling interruptions
	 * @throws IOException on failures
	 * @throws InterruptedException if the copy process is interrupted
	 * @throws IllegalArgumentException if the source is neither readable nor iterable.
	 */
	public static void copyRecursiveOptimizedInterruptible(final Store source, final Store target,
			final CopyStrategy overwrite, final InterruptionMode imode) throws IOException, InterruptedException {
		boolean success = false;

		File srcf = FileStore.getFile(source);
		File dstf = FileStore.getFile(target);
		if (srcf != null && dstf != null && overwrite == OVERWRITE) {
			File binary = Path.CTX.get().locate("rsync");
			if (binary != null) {
				String src = srcf.getAbsolutePath();
				String dst = dstf.getAbsolutePath();
				try {
					Process res = Runtime.getRuntime().exec(binary.getAbsolutePath() + " -r " + src + " " + dst);
					if (res.waitFor() == 0) {
						success = true;
					}
				} catch (IOException e) {
					Warden.disregardAndReport(e);
				} catch (InterruptedException e) {
					if (imode == InterruptionMode.IGNORE) {
						Warden.disregardAndReport(e);
					} else {
						throw Warden.spot((InterruptedException) new InterruptedException("interrupted during " + src
								+ " of " + source).initCause(e));
					}
				}
			}
			// else
			// if(IOTools.isHardlinkSupported())
			// IOTools.hardlink(src, dst);
		}
		if (!success) {
			StoreUtil.copyRecursiveInterruptible(source, target, overwrite, imode);
		}
	}

	public static void copyRecursive(final Store source, final Store target, final CopyStrategy overwrite)
			throws IOException {
		try {
			copyRecursiveInterruptible(source, target, overwrite, InterruptionMode.IGNORE);
		} catch (IOException e) {
			throw Warden.spot(new IOException("failure during copy " + source + " to " + target, e));
		} catch (InterruptedException e) {
			throw Warden.spot(new CantHappenException("should be ignored", e));
		}
	}

	/**
	 * @param source to copy
	 * @param target to copy to
	 * @param copyStrategy how to copy; defaults to {@link #SKIP skip existing}
	 * @param imode see {@link InterruptionMode}
	 * @throws InterruptedException if interrupted; affected by {@link InterruptionMode}
	 * @throws IOException passed on from copy operations
	 */
	public static void copyRecursiveInterruptible(final Store source, final Store target,
			final CopyStrategy copyStrategy, final InterruptionMode imode) throws InterruptedException, IOException {
		copyRecursiveInterruptibleNeverIntoItself(source, target, copyStrategy, imode, target);
	}

	private static void copyRecursiveInterruptibleNeverIntoItself(final Store source, final Store target,
			final CopyStrategy overwrite, final InterruptionMode imode, final Store targetRoot)
					throws InterruptedException, IOException {
		if (Thread.interrupted()) {
			if (imode != InterruptionMode.IGNORE) {
				throw Warden.spot(new InterruptedException("interrupted during before copying " + source));
			}
		}
		if (overwrite instanceof DirectorStrategy) {
			boolean mayProceed = ((DirectorStrategy) overwrite).beforeCopy(source, target);
			if (!mayProceed) {
				return;
			}
		}
		if (source.canRead()) {
			if (target.exists()) {
				if (overwrite instanceof OverwriteStrategy) {
					((OverwriteStrategy) overwrite).handleExisting(source, target);
				}
			} else {
				copy(source, target);
			}
		} else if (source.iterationSupported()) {
			for (Store child : source) {
				if (sameStore(child, targetRoot)) {
					// never directly copy the target into itself
					continue;
				}
				copyRecursiveInterruptibleNeverIntoItself(child, target.element(child.getName()), overwrite, imode,
						targetRoot);
			}
		} else {
			throw Warden.spot(new IllegalArgumentException("cannot copy recursively as the source " + source
					+ " cannot be read and doesn't support iteration."));
		}
	}

	/**
	 * Move a Store into another Store. Note: The target must be writable (no directory).
	 * Equal to copy(a,b) followed by a.delete(). {@link MovableStore} moves are optimized if possible.
	 * 
	 * @param source != null
	 * @param target != null (this is the actual target i.e. afterwards target == source
	 * @throws IOException on failures
	 */
	public static void move(final Store source, final Store target) throws IOException {
		if (source.equals(target)) {
			return;
		}
		if (source instanceof MovableStore && target instanceof MovableStore) {
			try {
				((MovableStore) source).moveTo((MovableStore) target);
				return;
			} catch (UnsupportedOperationException e) {
				Warden.disregard(e);
				// fall thru to siple copy;
			}
		}
		copyRecursive(source, target, FAIL);
		source.delete();

	}

	/**
	 * Hard-link a Store to another Store.
	 * Afterwards changes to one store are reflected in the other.
	 * Note: May only work on certain stores, e.g. FileStore.
	 * Note: Possibly existing target files are replaced by the hardlink.
	 *
	 * @param source != null
	 * @param target != null
	 * @throws IOException on failures
	 * @throws UnsupportedOperationException on Stores that don't support this
	 */
	public static void hardLink(final Store source, final Store target) throws IOException {
		if (source.equals(target)) {
			return;
		}
		if (source instanceof FileStore && target instanceof FileStore) {
			File sourceFile = ((FileStore) source).getFile();
			if (!(sourceFile.exists() && sourceFile.canRead())) {
				throw Warden.spot(new IllegalArgumentException("source file " + sourceFile + " is no file"));
			}
			File targetFile = ((FileStore) target).getFile();
			if (targetFile.exists()) {
				IOTools.deleteAll(targetFile);
			} else if (!targetFile.getParentFile().exists()) {
				targetFile.getParentFile().mkdirs();
			}

			IOTools.hardlink(sourceFile, targetFile);
		} else if (target.getParent() instanceof MapStore) {
			((MutableStore) target).behaveAs(source);
		} else {
			throw Warden.spot(new UnsupportedOperationException("can only hard-link FileStores"));
		}
	}

	/**
	 * Copy the given given parts of the source Store into a new, iterable store with the same elements nested in the
	 * same way.
	 * Example given a Store <em>st</em> <code><pre>
	 * a
	 * +-b
	 * +-c
	 * | +-d
	 * | +-e
	 * +-f
	 * </pre></code> then the query
	 * <code>{@link #copyIntoIterable(Store, String...) copyIntoIterable}(st, "a/b", "a/c/e")</code> returns <code><pre>
	 * a
	 * +-b
	 * +-c
	 * | +-e
	 * </pre></code>
	 *
	 * @param source Store != null, probably not {@link Store#iterationSupported() iterable}
	 * @param origPaths list of paths to the elements desired to be copied; path separator is "/"; no path may be empty
	 * @return MapStore with the elements available. This Store is iterable.
	 * @throws IOException if any source element cannot be accessed
	 */
	public static Store copyIntoIterable(final Store source, final String... origPaths) throws IOException {
		MapStore targetCopy = new MapStore();
		targetCopy.setCreateMode(CreateMode.ALLOW_AUTOMATIC);

		for (String path : origPaths) {
			copyPath(source, targetCopy, path);
		}
		return targetCopy;
	}

	/**
	 * Copy a selected part of a Store into another Store.
	 * The target element of the target Store must be be a MutableStore.
	 *
	 * @param source Store
	 * @param target Store
	 * @param path separated by "/"
	 */
	public static void copyPath(final Store source, final Store target, final String path) {
		Store srcElem = resolvePath(source, path, DEFAULT_PATH_SEPARATOR_REG_EXP);
		Store dstElem = resolvePath(target, path, DEFAULT_PATH_SEPARATOR_REG_EXP);
		((MutableStore) dstElem).behaveAs(srcElem);
	}

	/**
	 * Compress a Store into another Store. The source store must be iterable, use
	 * {@link #copyIntoIterable(Store, String...)} if needed.
	 * Note: Default compression is used. Compressed files ( {@link #FILE_EXTENSONS_OF_COMPRESSED_FILES}) are stored.
	 * Note: If the toBeCompressedStore is readable the contents of the ZIP will be a single entry with the name of that
	 * Store; otherwise the relative directory structure will be preserved.
	 *
	 * @param toBeCompressedStore != null
	 * @param compressedStore != null
	 * @return the compressed Store
	 * @throws IOException on failure to read, iterate or write
	 */
	public static Store zip(final Store toBeCompressedStore, final Store compressedStore) throws IOException {
		assert toBeCompressedStore != null;
		assert compressedStore != null;
		OutputStream rawStream = compressedStore.getOutputStream(false);
		try {
			ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(rawStream));
			try {
				zipAppend(toBeCompressedStore, out, "");
			} finally {
				out.close();
			}
		} finally {
			IOTools.forceClose(rawStream);
		}
		return compressedStore;
	}

	private static void zipAppend(final Store toBeCompressedStore, final ZipOutputStream out, final String prefix)
			throws IOException {
		if (toBeCompressedStore.canRead()) {
			String entryName = prefix.isEmpty() ? toBeCompressedStore.getName() : prefix;
			ZipEntry entry = new ZipEntry(entryName);
			long lm = toBeCompressedStore.getLastModified();
			if (lm != Accessible.UNKNOWN_SIZE) {
				entry.setTime(lm);
			}
			long s = toBeCompressedStore.getSize();
			if (s != Accessible.UNKNOWN_SIZE && isCompressedFile(toBeCompressedStore.getName())) {
				entry.setMethod(ZipEntry.STORED);
				entry.setSize(s);
				InputStream crcIn = toBeCompressedStore.getInputStream();
				try {
					entry.setCrc(IOTools.crc32(crcIn));
				} finally {
					IOTools.forceClose(crcIn);
				}
			}
			try {
				out.putNextEntry(entry);
			} catch (IOException e) {
				// retry with default entry
				entry = new ZipEntry(entryName);
				out.putNextEntry(entry);
			}
			InputStream rawIn = toBeCompressedStore.getInputStream();
			try {
				InputStream bufIn = new BufferedInputStream(rawIn);
				IOTools.copy(bufIn, out, true, false);
			} finally {
				IOTools.forceClose(rawIn);
			}
			out.flush();
		} else if (toBeCompressedStore.iterationSupported()) {
			for (Store elem : toBeCompressedStore) {
				zipAppend(elem, out,
						prefix.isEmpty() ? elem.getName() : prefix + DEFAULT_PATH_SEPARATOR + elem.getName());
			}
		} else {
			throw Warden.spot(new IOException("cannot iterate contents of " + toBeCompressedStore));
		}
	}

	public static boolean isCompressedFile(final String fileName) {
		if (fileName == null) {
			return false;
		}
		for (String comExt : FILE_EXTENSONS_OF_COMPRESSED_FILES) {
			if (fileName.endsWith(comExt)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Tests ancestorship and not only direct parentship.
	 * Traverses getParent() relation. Uses {@link #sameStore(Store, Store)} to test reachability.
	 *
	 * @param potentialParent any Store
	 * @param potentialDescendant any Store
	 * @return true: the potential parent is actually an ancestor of the potential child (includes child=parent)
	 */
	public static boolean isParentOf(final Store potentialParent, final Store potentialDescendant) {
		Store childAncestor = potentialDescendant;
		while (childAncestor != null) {
			if (sameStore(childAncestor, potentialParent)) {
				return true;
			}
			childAncestor = childAncestor.getParent();
		}
		return false;
	}

	/**
	 * Uncompress a Store into another Store.
	 *
	 * @param compressedStore != null
	 * @param targetStore != null
	 * @return Store the targetStore
	 * @throws IOException
	 */
	public static Store unzip(final Store compressedStore, final Store targetStore) throws IOException {
		InputStream rawIn = compressedStore.getInputStream();
		try {
			ZipInputStream in = new ZipInputStream(new BufferedInputStream(rawIn));
			while (true) {
				ZipEntry entry = in.getNextEntry();
				if (entry == null) {
					break;
				}
				if (entry.isDirectory()) {
					continue;
				}
				String name = entry.getName();
				Store dest = name == null || name.length() == 0 ? targetStore : resolvePath(targetStore, name,
						DEFAULT_PATH_SEPARATOR);
				OutputStream rawOut = dest.getOutputStream(false);
				OutputStream bufOut = new BufferedOutputStream(rawOut);
				IOTools.copy(in, bufOut, false, true);
			}
		} finally {
			IOTools.forceClose(rawIn);
		}
		return targetStore;
	}

	/**
	 * Compares two stores recursively and determine sub stores where they differ.
	 * The reading of the differences should be as if a had changed to b in time, e.g. "x was added" (to a, now being
	 * present in b).
	 *
	 * @param a != null
	 * @param b != null
	 * @return List of paths where the stores differ
	 * @throws IOException if neither Store can be iterated or some elements cannot be read
	 */
	public static @Nonnull
	Collection<String> compareStores(@Nonnull final Store a, @Nonnull final Store b) throws IOException {
		return compareStoresDetailed(a, b).asStrings();
	}

	public static Diff compareStoresDetailed(final Store a, final Store b) throws IOException {
		return compareStoresDetailed(a, b, ContentComparer.BINARY);
	}

	public static Diff compareStoresDetailed(final Store a, final Store b, final ContentComparer comparer)
			throws IOException {
		if (!a.canRead()) {
			if (!a.iterationSupported()) {
				if (!b.iterationSupported()) {
					throw Warden.spot(new IOException("neither readable nor iteration supported"));
				}
				// try reversed comparison
				return compareStoresDetailed(b, a, new ContentComparer() {
					@Override
					public void compareContent(final Store aa, final Store bb, final String path, final Diff differences)
							throws IOException {
						comparer.compareContent(bb, aa, path, differences);
					}

					@Override
					public void removed(final Store aa, final String path, final Diff differences) throws IOException {
						comparer.add(aa, path, differences);
					}

					@Override
					public void add(final Store bb, final String path, final Diff differences) throws IOException {
						comparer.removed(bb, path, differences);
					}
				});
			}
		}
		Diff differences = new Diff();
		compareRecursive(a, b, "", differences, comparer);
		return differences;
	}

	public static void compareRecursive(final Store a, final Store b, final String path, final Diff differences,
			final ContentComparer comparer) throws IOException {
		if (a.canRead()) {
			if (b.canRead()) {
				comparer.compareContent(a, b, path, differences);
			} else {
				comparer.removed(a, path, differences);
			}
		} else if (a.iterationSupported()) {
			Set<String> aseen = L.set();
			for (Store child : a) {
				compareRecursive(child, b.element(child.getName()), path + a.getName() + DEFAULT_PATH_SEPARATOR,
						differences, comparer);
				aseen.add(child.getName());
			}
			for (final Store child : b) {
				if (!aseen.contains(child.getName())) {
					comparer.add(child, path + a.getName() + DEFAULT_PATH_SEPARATOR, differences);
				}
			}
		} else {
			throw Warden.spot(new IllegalArgumentException("cannot compare recursively as " + a
					+ " doesn't support iteration."));
		}
	}

	/**
	 * Creates a Store in the default temporary store location. {@link File#createTempFile} is used to ensure locally
	 * unique stores.
	 *
	 * @param prefix != null
	 * @return Store != null
	 * @throws IOException on failure to create or access the location
	 */
	public static Store getTempStore(final String prefix) throws IOException {
		Store temp = Store.ROOT.get().element(Store.DEFAULT_TEMP_PATH);
		File tf = File.createTempFile(prefix, "");
		tf.deleteOnExit();
		return temp.element(tf.getName() + TEMP_DIR_SUFFIX);
	}

	public static FileStore toFileStore(final Store store, final boolean deleteTemporaryOnExit) throws IOException {
		Store realStore = DelegateStore.unwrap(store);
		if (realStore instanceof FileStore) {
			return (FileStore) realStore;
		}
		File tempFile;
		if (store.canRead()) {
			String suffix = store.getName();
			if (suffix == null) {
				suffix = "";
			}
			tempFile = File.createTempFile("local_copy_", suffix);
		} else {
			tempFile = IOTools.createTempDir("local_dir_");
		}
		if (deleteTemporaryOnExit) {
			IOTools.deleteOnExit(tempFile);
		}
		FileStore temp = new FileStore(tempFile);
		copy(store, temp);
		return temp;
	}

	/**
	 * @param store to count
	 * @return number of direct sub stores, 0 if empty OR not {@link Store#iterationSupported()}
	 */
	public static int getSubStoreCount(final Store store) {
		if (store.iterationSupported()) {
			return Misc.sizeOfIterator(store.iterator());
		}
		return 0;
	}

	/**
	 * @param store to count
	 * @param countOnlyLeafs true: intermediate store do not count, false: the count as 1
	 * @return number of all recursive sub stores, not {@link Store#iterationSupported() iterable} store are counted as
	 * 0
	 */
	public static int getSubStoreCountRecursive(final Store store, final boolean countOnlyLeafs) {
		if (!store.iterationSupported()) {
			return 0;
		}

		int count = 0;
		for (Store child : store) {
			if (!countOnlyLeafs) {
				count++;
			}
			count = count + getSubStoreCountRecursive(child, countOnlyLeafs);
		}
		return count;
	}

	/**
	 * @param store whose children are put into nestings based on common prefixes with respect to pathSeparator.
	 * The intermediate nested elements are MapStores for which no {@link DelegateStore#unwrap(Store)} is possible.
	 * The reverse operation is {@link #flatten(Store, String, Class...)}. <br/>
	 * The path elements must all be non-empty
	 * @param pathSeparator some part of the element names which is used as separation, e.g. "."
	 * @return Store with nested sub stores
	 */
	public static Store hierarchify(final Store store, final String pathSeparator) {
		MapStore hierarchy = new MapStore() {
			@Override
			public InputStream getInputStream() throws IOException {
				return store.getInputStream();
			}

			@Override
			public OutputStream getOutputStream(final boolean append) throws IOException {
				return store.getOutputStream(append);
			}

			@Override
			protected Store createLeaf(final Store parent, final String name) {
				String path = buildPathFromTo(this, parent, pathSeparator);
				return store.element(path.isEmpty() ? name : path + pathSeparator + name);
				// throw Warden.spot(new UnsupportedOperationException("cannot change created hierarchy dynamically at "
				// + name));
			}
		}.setCreateMode(CreateMode.ALLOW_AUTOMATIC);
		Pattern sep = Pattern.compile(Pattern.quote(pathSeparator));
		for (Store child : store) {
			Matcher match = sep.matcher(child.getName());
			int end = 0;
			while (match.find()) {
				end = match.end();
			}
			final String lastElement = child.getName().substring(end);
			if (lastElement.isEmpty()) {
				throw Warden.spot(new IllegalArgumentException("all path elements must be empty; violeted in "
						+ child.getName() + " at " + pathSeparator));
			}
			((MutableStore) StoreUtil.resolvePath(hierarchy, child.getName(), Pattern.quote(pathSeparator)))
			.behaveAs(new DelegateStore(child) {
				@Override
				public String getName() {
					return lastElement;
				}
			});
		}
		hierarchy.setCreateMode(CreateMode.ALLOW_AUTOMATIC);
		return hierarchy;
	}

	/**
	 * Converts part of the hierarchy of nested sub Stores into a a flat structure.
	 * The result is a {@link MapStore} whose elements are the wrapped leaves which can be
	 * {@link DelegateStore#unwrap(Store) unwrapped} as needed.
	 * The reverse operation is {@link #hierarchify(Store, String)}. <br/>
	 * See also {@link StoreGroup}.
	 *
	 * @param store to flatten
	 * @param pathSeparator between name parts in the flat elements name built from the nested names
	 * @param flattenedClasses Store classes to flatten (flattening stops at Store types not in this list
	 * @return Store where nested Stores are flattened
	 */
	public static Store flatten(final Store store, final String pathSeparator, @Nonnull final Class<?>... flattenedClasses) {
		List<Class<?>> classes = flattenedClasses == null | flattenedClasses.length == 0 ? L.<Class<?>> s(Store.class)
				: L.l(flattenedClasses);
		List<Store> flatStores = L.l();
		for (Store child : store) {
			flatten(child, pathSeparator, null, flatStores, classes);
		}
		MapStore flat = new MapStore() {
			@Override
			protected Store createLeaf(final Store parent, final String name) {
				throw Warden.spot(new UnsupportedOperationException("cannot change flattend structure dynamically at "
						+ name));
			}
		}.setCreateMode(CreateMode.ALLOW_LEAFS);
		for (Store flatStore : flatStores) {
			flat.add(flatStore.getName(), flatStore);
		}
		flat.setCreateMode(CreateMode.DENY);
		return flat;
	}

	private static void flatten(final Store store, final String pathSeparator, final String prefix,
			final List<Store> flatStores, final List<Class<?>> flattenedClasses) {
		final String name = prefix == null ? store.getName() : prefix + pathSeparator + store.getName();

		if (!store.iterationSupported() || !shouldBeFlattened(store, flattenedClasses)) {
			flatStores.add(new DelegateStore(store) {
				@Override
				public String getName() {
					return name;
				}
			});
			return;
		}
		for (Store child : store) { // if(prefix ==null)
			flatten(child, pathSeparator, name, flatStores, flattenedClasses);
		}
	}

	private static boolean shouldBeFlattened(final Store store, final List<Class<?>> flattenedClasses) {
		Class<? extends Store> storeClass = DelegateStore.unwrap(store).getClass();
		for (Class<?> flattenedClass : flattenedClasses) {
			if (flattenedClass.isAssignableFrom(storeClass)) {
				return true;
			}
		}
		return false;
	}

	public static void deleteOnExit(final Store store) {
		File file = FileStore.getFile(store);
		if (file != null) {
			IOTools.deleteOnExit(file);
			return;
		}

		synchronized (storesToDeleteOnExit) {
			storesToDeleteOnExit.add(store);
		}
	}

	public static Collection<String> getElementNames(final Store store) {
		return Collections2.transform(L.copy(store.iterator()), new Function<Store, String>() {
			@Override
			public String apply(@Nullable final Store input) {
				return input.getName();
			}
		});
	}
}
