package de.zarncke.lib.io.store;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.err.Warden;

/**
 * Provides as its own elements all elements of sub-elements of the decoreated Store.
 *
 * @author Gunnar Zarncke <gunnar@konzentrik.de>
 */
public class UnionStore extends DelegateStore {

	public UnionStore(final Store delegate) {
		super(delegate);
		if (!delegate.iterationSupported()) {
			throw Warden.spot(new IllegalArgumentException(delegate + " must support iteration!"));
		}
	}

	@Override
	public boolean iterationSupported() {
		for (Store child : this.delegate) {
			if (child.iterationSupported()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Iterator<Store> iterator() {
		List<Store> childrenReverse = L.copy(this.delegate.iterator());
		Collections.reverse(childrenReverse);
		// take duplicate elements once, in the order they appeared taking the first occurring of same named
		Set<Store> elems = new LinkedHashSet<Store>();
		for (Store child : childrenReverse) {
			elems.addAll(L.copy(child.iterator()));
		}
		return elems.iterator();
	}

	@Override
	public Store element(final String name) {
		Map<String, Store> matchingElements = new LinkedHashMap<String, Store>();
		for (Store child : this.delegate) {
			Store elem = child.element(name);
			if (elem.exists()) {
				matchingElements.put(child.getName(), elem);
			}
		}
		if (matchingElements.isEmpty()) {
			return new AbsentStore(name);
		}
		if (matchingElements.size() == 1) {
			return new DelegateStore(matchingElements.values().iterator().next()) {
				@Override
				public Store getParent() {
					return UnionStore.this;
				}
			};
		}

		return new UnionStore(new MapStore(matchingElements) {
			@Override
			public Store getParent() {
				return UnionStore.this;
			}
		});

		// return super.element(name);
	}
}
