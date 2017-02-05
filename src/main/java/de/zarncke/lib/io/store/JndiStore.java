package de.zarncke.lib.io.store;

import java.util.Iterator;

import javax.naming.Context;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import de.zarncke.lib.err.Warden;

public class JndiStore extends AbstractStore {
	static class JndiStoreException extends RuntimeException {
		public JndiStoreException(final String msg, final Throwable e) {
			super(msg, e);
		}

		private static final long serialVersionUID = 1L;

	}

	private final Context jndiCtx;

	private final Store parent;

	public JndiStore(final Context rootContext) {
		this(null, rootContext);
	}

	JndiStore(final JndiStore parent, final Context ctx) {
		this.jndiCtx = ctx;
		this.parent = parent;
	}

	public JndiStore(final JndiStore parent, final String name) {
		this(parent, lookupStore(parent, name));
	}

	@Override
	public boolean exists() {
		return false;
	}

	private static Context lookupStore(final JndiStore parent, final String name) {
		Object element;
		try {
			element = parent.jndiCtx.lookup(name);
		} catch (NamingException e) {
			throw Warden.spot(new IllegalArgumentException("element " + name + " of " + parent + " doesn't exist."));
		}
		if (!(element instanceof Context)) {
			throw Warden.spot(new IllegalArgumentException("element " + name + " of " + parent + " is no context."));
		}
		return (Context) element;
	}

	@Override
	public Store element(final String name) {
		Object element;
		try {
			element = this.jndiCtx.lookup(name);
		} catch (NamingException e) {
			// TODO return absent elements
			throw Warden.spot(new IllegalArgumentException("element " + name + " of " + this.parent + " doesn't exist."));
		}
		if (element instanceof Context) {
			return new JndiStore(this, (Context) element);
		}

		try {
			return StoreUtil.asStore(element, this);
		} catch (IllegalArgumentException e) {
			return new AbstractStore() {

				@Override
				public boolean delete() {
					try {
						JndiStore.this.jndiCtx.unbind(name);
						return true;
					} catch (NamingException e2) {
						Warden.disregard(e2);
						return false;
					}
				}

				@Override
				public boolean exists() {
					return true;
				}

				@Override
				public Store element(final String n) {
					// Resolved JNDI Objects do not have elements.
					return new AbsentStore(n);
				}

				@Override
				public Store getParent() {
					return JndiStore.this;
				}

				public String getName() {
					return name;
				}
			};
		}
	}

	public String getName() {
		return null;
	}

	@Override
	public Iterator<Store> iterator() {
		try {
			final NamingEnumeration<NameClassPair> fi = this.jndiCtx.list("");
			return new Iterator<Store>() {
				Store last;

				public boolean hasNext() {
					try {
						return fi.hasMore();
					} catch (NamingException e) {
						throw Warden.spot(new JndiStoreException("JNDI iteration failed", e));
					}
				}

				public Store next() {
					NameClassPair elem;
					try {
						elem = fi.next();
					} catch (NamingException e) {
						throw Warden.spot(new JndiStoreException("JNDI iteration failed", e));
					}
						this.last = element(elem.getName());
					return this.last;
				}

				public void remove() {
					JndiStore.this.delete();
				}
			};
		} catch (NamingException e1) {
			throw Warden.spot(new JndiStoreException("JNDI listing failed", e1));
		}
	}

	@Override
	public Store getParent() {
		return this.parent;
	}

	@Override
	public String toString() {
		return this.jndiCtx.toString();
	}

	@Override
	public boolean delete() {
		try {
			this.jndiCtx.unbind("");
			return true;
		} catch (NamingException e) {
			return false;
		}
	}

	@Override
	public boolean iterationSupported() {
		return true;
	}
}
