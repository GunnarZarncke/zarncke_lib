package de.zarncke.lib.io.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import de.zarncke.lib.coll.EmptyIterator;
import de.zarncke.lib.data.HasName;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.region.Region;

/**
 * This default implementation of {@link Store} returns values for unknown/failure or throws the defined exceptions where
 * applicable.
 * It supports nothing out of the box except for the empty {@link #element(String)}.
 *
 * @author Gunnar Zarncke
 */
public abstract class AbstractStore implements Store {

	public AbstractStore() {
		super();
	}

	@Override
	public Store element(final String name) {
		if (name == null || name.length() == 0) {
			return this;
		}
		return new AbsentStore(name);
	}

	@Override
	public boolean delete() {
		return false;
	}

	@Override
	public long getLastModified() {
		return UNKNOWN_MODIFICATION;
	}

	@Override
	public boolean setLastModified(final long millis) {
		return false;
	}

	@Override
	public long getSize() {
		return UNKNOWN_SIZE;
	}

	@Override
	public Store getParent() {
		return null;
	}

	@Override
	public Iterator<Store> iterator() {
		return EmptyIterator.<Store> getInstance();
	}

	@Override
	public Region asRegion() throws IOException {
		throw Warden.spot(new IOException("cannot read"));
	}

	@Override
	public boolean canRead() {
	    return false;
	}

	@Override
	public boolean canWrite() {
	    return false;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		throw Warden.spot(new IOException("cannot read"));
	}

	@Override
	public OutputStream getOutputStream(final boolean append) throws IOException {
		throw Warden.spot(new IOException(this + " does not exist"));
	}

	@Override
	public boolean iterationSupported() {
		return false;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ":" + getName();
	}

	@Override
	public int compareTo(final HasName named) {
		return getName().compareTo(named.getName());
	}

}