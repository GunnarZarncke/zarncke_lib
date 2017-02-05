package de.zarncke.lib.io.store.ext;

import java.util.Iterator;

import de.zarncke.lib.io.store.DelegateStore;
import de.zarncke.lib.io.store.Store;

/**
 * A base class for a generified Store.
 *
 * @author Gunnar Zarncke
 * @param <T> type of actual derived class
 */
public class EnhanceBaseStore<T extends EnhancedStore<T>> extends DelegateStore implements EnhancedStore<T> {

	private final Enhancer<T> enhancer;

	public EnhanceBaseStore(final Store delegate, final Enhancer<T> enhancer) {
		super(delegate);
		this.enhancer = enhancer;
	}

	@Override
	public T element(final String name) {
		return this.enhancer.enhance(super.element(name));
	}

	@Override
	public Iterator<T> enhancedIterator() {
		final Iterator<Store> it = iterator();
		return new Iterator<T>() {

			Store next = null;
			{
				advance();
			}

			@Override
			public boolean hasNext() {
				return this.next != null;
			}

			@Override
			public T next() {
				Store curr = this.next;
				advance();
				return EnhanceBaseStore.this.enhancer.enhance(curr);
			}

			private void advance() {
				if (!it.hasNext()) {
					this.next = null;
					return;
				}
				while (true) {
					this.next = it.next();
					if (EnhanceBaseStore.this.enhancer.isIncluded(this.next)) {
						break;
					}
					if (!it.hasNext()) {
						this.next = null;
						return;
					}
				}
			}

			@Override
			public void remove() {
				it.remove();
			}
		};
	}
}