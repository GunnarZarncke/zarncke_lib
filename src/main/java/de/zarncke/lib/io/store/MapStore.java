package de.zarncke.lib.io.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.MapMaker;

import de.zarncke.lib.coll.WrapIterator;
import de.zarncke.lib.err.CantHappenException;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.region.Region;
import de.zarncke.lib.time.Times;

/**
 * A {@link Store} which aggregates other Stores.
 * The child stores are provides as a {@link Map}.
 * Note: The Stores returned by {@link #element(String)} are <em>not</em> identical to the Stores in the Map but differ
 * in the
 * following methods:
 * <ul>
 * <li>{@link #getName()}</li>
 * <li>{@link #getParent()}</li>
 * <li>{@link #delete()}</li>
 * </ul>
 * <br/>
 * The Store is thread-safe.
 * The order of elements {@link #add(String, Store) added} is not preserved.
 *
 * @author Gunnar Zarncke
 */
@ThreadSafe
public class MapStore extends AbstractStore implements ChildRemovableStore {

	public enum CreateMode {
		FINAL, DENY, ALLOW_MAPS, ALLOW_LEAFS, ALLOW_AUTOMATIC, LAZY_AUTOMATIC
	}

	private static final class StoreElement extends DelegateStore implements MutableStore, ChildRemovableStore {
		private final String name;
		private final ChildRemovableStore parent;

		private StoreElement(final Store elem, final String name, final ChildRemovableStore parent) {
			super(elem);
			if (name == null) {
				throw new IllegalArgumentException("name==null");
			}
			this.name = name;
			this.parent = parent;
		}

		@Override
		public void behaveAs(final Store store) {
			Store realParent = getParent();

			realParent = unwrap(realParent);
			if (realParent instanceof MapStore) {
				((MapStore) realParent).add(this.name, store);
			}
		}

		@Override
		public Store element(final String ename) {
			return new StoreElement(getDelegate().element(ename), ename, this);
		}

		@Override
		public Iterator<Store> iterator() {
			return new WrapIterator<Store, Store>(getDelegate().iterator()) {
				@Override
				protected Store wrap(final Store next) {
					return new StoreElement(next, next.getName(), StoreElement.this);
				}
			};
		}

		@Override
		public boolean delete() {
			boolean removed = this.parent.deleteChild(this.name);
			// Store realParent = getParent();
			// realParent = unwrap(realParent);
			// if (realParent instanceof MapStore) {
			// removed = ((MapStore) realParent).stores.remove(this.name) != null;
			// } else {
			// throw Warden.spot(new IllegalStateException("encountered detached StoreElement " + this
			// + " without parent MapStore " + realParent));
			// }
			boolean removedSuper = super.delete();
			return removed && removedSuper;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public Store getParent() {
			return this.parent;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + this.parent.hashCode();
			result = prime * result + (this.name == null ? 0 : this.name.hashCode());
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
			StoreElement other = (StoreElement) obj;
			if (!this.parent.equals(other.parent)) {
				return false;
			}
			if (this.name == null) {
				if (other.name != null) {
					return false;
				}
			} else if (!this.name.equals(other.name)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return this.name + "->(" + this.getDelegate().toString() + ")";
		}

		@Override
		public boolean deleteChild(final String aname) {
			Store mapStore = getDelegate();
			if (mapStore instanceof ChildRemovableStore) {
				return ((ChildRemovableStore) mapStore).deleteChild(aname);
			}
			return false;
		}
	}

	private final class AutomaticElementStore extends DelegateStore {
		private final String name;
		boolean leaf = false;
		boolean map = false;

		private AutomaticElementStore(final Store delegate, final String name) {
			super(delegate);
			this.name = name;
		}

		@Override
		public boolean exists() {
			return this.leaf || this.map;
		}

		@Override
		public boolean canRead() {
			return !this.map;
		}

		@Override
		public boolean canWrite() {
			return !this.map;
		}

		@Override
		public Store element(final String ename) {
			setMap();
			return super.element(ename);
		}

		@Override
		public InputStream getInputStream() throws IOException {
			setLeaf();
			return super.getInputStream();
		}

		@Override
		public OutputStream getOutputStream(final boolean append) throws IOException {
			setLeaf();
			return super.getOutputStream(append);
		}

		@Override
		public Region asRegion() throws IOException {
			setLeaf();
			return super.asRegion();
		}

		@Override
		public Iterator<Store> iterator() {
			setMap();
			return super.iterator();
		}

		private void setLeaf() throws IOException {
			if (this.map) {
				throw new IOException("cannot change from map to leaf");
			}
			if (this.leaf) {
				return;
			}
			this.leaf = true;
			this.delegate = createLeaf(MapStore.this, this.name);
			MapStore.this.stores.put(this.name, this.delegate);
		}

		private void setMap() {
			if (this.leaf) {
				return;
			}
			if (this.map) {
				return;
			}
			this.map = true;
			this.delegate = new MapStore() {
				@Override
				public String getName() {
					return AutomaticElementStore.this.name;
				}

				@Override
				public Store getParent() {
					return MapStore.this;
				}

				@Override
				protected Store createLeaf(final Store parent, final String name) {
					return MapStore.this.createLeaf(this, name);
				}
			}.setCreateMode(CreateMode.ALLOW_AUTOMATIC);
			MapStore.this.stores.put(this.name, this.delegate);
		}

		@Override
		protected Store getDelegate() {
			if (!this.map && !this.leaf) {
				try {
					setLeaf();
				} catch (IOException e) {
					throw Warden.spot(new CantHappenException("impossible", e));
				}
			}
			return super.getDelegate();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + (this.name == null ? 0 : this.name.hashCode());
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
			AutomaticElementStore other = (AutomaticElementStore) obj;
			if (!getOuterType().equals(other.getOuterType())) {
				return false;
			}
			if (this.name == null) {
				if (other.name != null) {
					return false;
				}
			} else if (!this.name.equals(other.name)) {
				return false;
			}
			return true;
		}

		private MapStore getOuterType() {
			return MapStore.this;
		}
	}

	private final Map<String, Store> stores = new MapMaker().makeMap();
	private CreateMode createMode = CreateMode.DENY;
	private long lastModified = Times.now().getMillis();

	public static MapStore newAllowAutomatic() {
		return new MapStore().setCreateMode(CreateMode.ALLOW_AUTOMATIC);
	}

	public MapStore() {
		// empty
	}

	public MapStore(final Map<String, Store> stores) {
		this.stores.putAll(stores);
	}

	public MapStore setCreateMode(final CreateMode mode) {
		if (this.createMode == CreateMode.FINAL) {
			throw Warden.spot(new IllegalStateException("may not change mode FINAL"));
		}
		this.createMode = mode;
		return this;
	}

	public MapStore add(final String name, final Store element) {
		this.stores.put(name, element);
		return this;
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public boolean delete() {
		this.stores.clear();
		return true;
	}

	@Override
	public Store element(final String name) {
		Store elem = this.stores.get(name);
		if (elem == null) {
			if (Store.CURRENT.equals(name)) {
				return this;
			}
			if (Store.PARENT.equals(name)) {
				return getParent();
			}
			switch (this.createMode) {
			case DENY:
			case FINAL:
				return new AbsentStore(name);
			case ALLOW_MAPS:
				elem = new MapStore() {
					@Override
					protected Store createLeaf(final Store parent, final String name) {
						return MapStore.this.createLeaf(this, name);
					}
				}.setCreateMode(CreateMode.ALLOW_LEAFS);
				break;
			case ALLOW_LEAFS:
				elem = createLeaf(this, name);
				break;
			case ALLOW_AUTOMATIC:
			case LAZY_AUTOMATIC:
				elem = new AutomaticElementStore(null, name);
				break;
			}
			if (this.createMode != CreateMode.LAZY_AUTOMATIC) {
				this.stores.put(name, elem);
			}
		}
		return new StoreElement(elem, name, this);
	}

	/**
	 * This method is invoked if a specific leaf node needs to be created.
	 * Leafs are created in {@link CreateMode#ALLOW_AUTOMATIC} if
	 *
	 * @param parent of the node to be created
	 * @param name of the new node
	 * @return Store
	 */
	protected Store createLeaf(final Store parent, final String name) {
		return new MemStore();
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public Iterator<Store> iterator() {
		Iterator<Entry<String, Store>> iterator = Iterators.filter(this.stores.entrySet().iterator(),
				new Predicate<Entry<String, Store>>() {
					@Override
					public boolean apply(@Nullable final Entry<String, Store> input) {
						return input.getValue().exists();
					}
				});
		return new WrapIterator<Map.Entry<String, Store>, Store>(iterator, this.stores.size()) {
			@Override
			protected Store wrap(final Map.Entry<String, Store> next) {
				return new StoreElement(next.getValue(), next.getKey(), MapStore.this);
			}
		};
	}

	@Override
	public boolean iterationSupported() {
		return true;
	}

	@Override
	public String toString() {
		return this.stores.toString();
	}

	@Override
	public boolean deleteChild(final String name) {
		return this.stores.remove(name) != null;
	}

	@Override
	public long getLastModified() {
		return this.lastModified;
	}

	@Override
	public boolean setLastModified(final long millis) {
		this.lastModified = millis;
		return true;
	}
}

interface ChildRemovableStore extends Store {
	boolean deleteChild(String name);
}
