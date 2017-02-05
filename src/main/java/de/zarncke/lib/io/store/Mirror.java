package de.zarncke.lib.io.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import de.zarncke.lib.coll.WrapIterator;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.io.DelegateOutputStream;
import de.zarncke.lib.region.Region;

/**
 * A Store which reads from a shadow store if it is available.
 *
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
public class Mirror extends DelegateStore {

	public enum WriteStrategy {
		/**
		 * Write to master first, then to shadow.
		 * Comparable to write-thru.
		 */
		WRITE_SHADOW_LAST,
		/**
		 * Write to shadow first, then to master.
		 * Comparable to write-back
		 */
		WRITE_SHADOW_FIRST,
		/**
		 * Writes to both stores simultaneous.
		 */
		WRITE_BOTH
	}

	protected final Store shadow;
	private WriteStrategy strategy = WriteStrategy.WRITE_BOTH;
	private boolean copyOnRead = false;

	public Mirror(final Store master, final Store shadow) {
		super(master);
		this.shadow = shadow;
	}

	@Override
	public Region asRegion() throws IOException {
		throw Warden.spot(new UnsupportedOperationException("cannot shadow asRegion yet"));
		// if (this.shadow.exists()) {
		// return this.shadow.asRegion();
		// }
		// return super.asRegion();
	}

	@Override
	public long getSize() {
		if (this.shadow.exists()) {
			return this.shadow.getSize();
		}
		// capture();
		return super.getSize();
	}

	@Override
	public boolean canRead() {
		if (this.shadow.exists()) {
			return this.shadow.canRead();
		}
		return super.canRead();
	}

	@Override
	public Iterator<Store> iterator() {
		if (super.iterationSupported()) {
			return super.iterator();
		}
		// in this case we iterate over the known entries of the shadow but wrap as if elements of master
		return new WrapIterator<Store, Store>(this.shadow.iterator()) {
			@Override
			protected Store wrap(final Store next) {
				return Mirror.this.wrap(Mirror.this.delegate.element(next.getName()));
			}
		};
	}

	@Override
	public boolean iterationSupported() {
		if (super.iterationSupported()) {
			return true;
		}
		return this.shadow.iterationSupported();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		if (isDirty()) {
			if (this.shadow.exists()) {
				this.shadow.delete();
			}
		} else {
			return this.shadow.getInputStream();
		}
		if (this.copyOnRead) {
			StoreUtil.copy(Mirror.this.delegate, Mirror.this.shadow);

			return this.shadow.getInputStream();
		}
		return super.getInputStream();
	}

	/**
	 * May be overridden to provide better dirty detection.
	 *
	 * @return true if shadow should not be used; by default only dirty if shadow doesn't even exist
	 */
	public boolean isDirty() {
		return !this.delegate.exists();
	}

	@Override
	public boolean canWrite() {
		// imprecise
		return this.shadow.canWrite() || getDelegate().canWrite();
	}

	@Override
	public boolean exists() {
		return this.shadow.exists() || super.exists();
	}

	@Override
	public OutputStream getOutputStream(final boolean append) throws IOException {
		switch (this.strategy) {
		case WRITE_BOTH: {
			final OutputStream masterStream = getDelegate().getOutputStream(append);
			final OutputStream shadowStream = this.shadow.getOutputStream(append);
			return new DelegateOutputStream(masterStream) {
				@Override
				public void write(final byte[] b) throws IOException {
					super.write(b);
					shadowStream.write(b);
				}

				@Override
				public void write(final byte[] b, final int off, final int len) throws IOException {
					super.write(b, off, len);
					shadowStream.write(b, off, len);
				}

				@Override
				public void write(final int b) throws IOException {
					super.write(b);
					shadowStream.write(b);
				}

				@Override
				public void flush() throws IOException {
					super.flush();
					shadowStream.flush();
				}
			};
		}
		case WRITE_SHADOW_FIRST: {
			final OutputStream shadowStream = this.shadow.getOutputStream(append);
			return new DelegateOutputStream(shadowStream) {
				@Override
				public void close() throws IOException {
					super.close();
					StoreUtil.copy(Mirror.this.shadow, Mirror.this.delegate);
				}
			};
		}
		case WRITE_SHADOW_LAST: {
			final OutputStream masterStream = getDelegate().getOutputStream(append);
			return new DelegateOutputStream(masterStream) {
				@Override
				public void close() throws IOException {
					super.close();
					StoreUtil.copy(Mirror.this.delegate, Mirror.this.shadow);
				}
			};
		}
		default:
			throw Warden.spot(new UnsupportedOperationException("unsupported " + this.strategy));
		}
	}

	/**
	 * Shadowing is not extended to parents!
	 */
	@Override
	public Store getParent() {
		return super.getParent();
	}

	@Override
	protected Store wrap(final Store element) {
		return new Mirror(element, this.delegate.element(element.getName())) {
			@Override
			public Store getParent() {
				return Mirror.this;
			}
		};
	}

	@Override
	public boolean setLastModified(final long millis) {
		this.shadow.setLastModified(millis);
		return super.setLastModified(millis);
	}

	@Override
	public boolean delete() {
		this.shadow.delete();
		return super.delete();
	}

	@Override
	public String toString() {
		return super.toString() + " with shadow " + this.shadow;
	}

	public Mirror setStrategy(final WriteStrategy strategy) {
		this.strategy = strategy;
		return this;
	}

	public Mirror setCopyOnRead(final boolean copyOnRead) {
		this.copyOnRead = copyOnRead;
		return this;
	}
}
