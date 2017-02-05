package de.zarncke.lib.io.store;

import java.io.File;
import java.io.IOException;

import de.zarncke.lib.ctx.Context;
import de.zarncke.lib.data.HasName;
import de.zarncke.lib.value.Default;

/**
 * A storage abstract which behaves like {@link File} but not limited to files but to any data.
 */
public interface Store extends Iterable<Store>, Accessible, HasName
{
	Store defaultRoot = new DelegateStore(new FileStore(new File("."))) {
		@Override
		public Store element(final String name) {
			if (name.equals(DEFAULT_TEMP_PATH)) {
				return new CleanupStore(new FileStore(new File(System.getProperty("java.io.tmpdir"))), true);
			}
			return super.element(name);
		}
	};

	/**
	 * A context which may provide a root Store. Defaults to a root at the current directory.
	 */
	Context<Store> ROOT = Context.of(Default.of(defaultRoot, Store.class));

	/**
	 * The default path to a temporary sub Store within the {@link #ROOT root store}. {@value #DEFAULT_TEMP_PATH}.
	 */
	String DEFAULT_TEMP_PATH = "temp";

	/**
	 * Returned by {@link #getLastModified()} when the modification is unknown.
	 */
	long UNKNOWN_MODIFICATION = Long.MIN_VALUE;

	/**
	 * Name of the artificial element which is the store itself.
	 * Conforming Stores should return themselves when queried for this element.
	 */
	String CURRENT = ".";

	/**
	 * Name of the artificial element which is the parent of the store.
	 * Conforming Stores should return their parent when queried for this element.
	 */
	String PARENT = "..";

	/**
	 * This is independent of {@link #canRead()} and {@link #canWrite()} e.g. for directories or files in not yet realized
	 * folders. Also false for invalid path names (which may or may not be checked early because of network connectivity).
	 *
	 * @return false: is not physically present or not valid
	 */
	boolean exists();

	/**
	 * @return parent store of this store (where {@link #element(String) element}({@link #getName()}) can be called
	 */
    Store getParent();

	/**
	 * @return long or {@link #UNKNOWN_MODIFICATION}
	 */
    long getLastModified();

	/**
	 * @param millis
	 * @return true if the timestamp could be modified
	 */
	boolean setLastModified(long millis);

	/**
	 * Returns a store for a child element of this Store.
	 * The child need not (yet) exist.
	 * The reference might be used to check its {{@link #exists() existence}.
	 * Accessing it may actually create it.
	 *
	 * @param name of the element, may be null or empty but may cause {@link IOException}
	 * @return Store != null, may be the store itself (in case of null or empty name)
	 */
	Store element(String name);

    /**
     * Should delete the contents of this store. Child elements are expected to also get deleted.
     *
     * @return true if the deletion was successful
     */
    boolean delete();

	/**
	 * Test if this Store supports listing elements.
	 * Some Stores (e.g. resource or web base stores) don't support listing.
	 *
	 * @return true if elements can be listed with {@link Iterable#iterator()}, false if it always return empty results.
	 */
	boolean iterationSupported();
}
