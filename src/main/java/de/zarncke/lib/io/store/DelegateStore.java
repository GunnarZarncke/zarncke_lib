package de.zarncke.lib.io.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import de.zarncke.lib.coll.WrapIterator;
import de.zarncke.lib.data.HasName;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.region.Region;

/**
 * This implementation of {@link Store} delegates all calls to a delegate.
 * It is useful to override e.g. {@link Store#getParent} in specialized classes like {@link MapStore}.
 * It doesn't implement {@link Object#equals} and {@link Object#hashCode} though!
 *
 * @author Gunnar Zarncke
 */
public abstract class DelegateStore implements Store {

	protected Store delegate;

	public DelegateStore(final Store delegate) {
		this.delegate = delegate;
	}

	@Override
	public Region asRegion() throws IOException {
		return this.getDelegate().asRegion();
	}

	@Override
	public boolean exists() {
		return this.getDelegate().exists();
	}

	@Override
	public boolean canRead() {
		return this.getDelegate().canRead();
	}

	@Override
	public boolean canWrite() {
		return this.getDelegate().canWrite();
	}

	@Override
	public int compareTo(final HasName o) {
		return this.getDelegate().compareTo(o);
	}

	@Override
	public boolean delete() {
		return this.getDelegate().delete();
	}

	@Override
	public Store element(final String name) {
		return wrap(this.getDelegate().element(name));
	}

	/**
	 * Allows derived classes to wrap elements.
	 *
	 * @param element != null
	 * @return Store
	 */
	protected Store wrap(final Store element) {
		return element;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return this.getDelegate().getInputStream();
	}

	@Override
	public long getLastModified() {
		return this.getDelegate().getLastModified();
	}

	@Override
	public boolean setLastModified(final long millis) {
		return getDelegate().setLastModified(millis);
	}

	@Override
	public String getName() {
		return this.getDelegate().getName();
	}

	@Override
	public OutputStream getOutputStream(final boolean append) throws IOException {
		return this.getDelegate().getOutputStream(append);
	}

	@Override
	public Store getParent() {
		return this.getDelegate().getParent();
	}

	@Override
	public long getSize() {
		return this.getDelegate().getSize();
	}

	@Override
	public boolean iterationSupported() {
		return this.getDelegate().iterationSupported();
	}

	@Override
	public Iterator<Store> iterator() {
		return new WrapIterator<Store, Store>(this.getDelegate().iterator()) {
			@Override
			protected Store wrap(final Store next) {
				return DelegateStore.this.wrap(next);
			}
		};
	}

	protected Store getDelegate() {
		if (this.delegate == null) {
			throw Warden.spot(new IllegalStateException("delegate not yet set"));
		}
		return this.delegate;
	}

	@Override
	public String toString() {
		if (this.delegate == null) {
			return "unset";
		}
		return this.delegate.toString();
	}

	/**
	 * May be overridden w.g. to implement {@link MutableStore#behaveAs(Store)}.
	 *
	 * @param delegate != null
	 */
	protected void setDelegate(final Store delegate) {
		this.delegate = delegate;
	}

	public static Store unwrap(final Store store) {
		Store real = store;
		while (real instanceof DelegateStore) {
			real = ((DelegateStore) real).getDelegate();
		}
		return real;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.delegate == null ? 0 : this.delegate.hashCode());
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
		DelegateStore other = (DelegateStore) obj;
		if (this.delegate == null) {
			if (other.delegate != null) {
				return false;
			}
		} else if (!this.delegate.equals(other.delegate)) {
			return false;
		}
		return true;
	}

}