package de.zarncke.lib.struct;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import de.zarncke.lib.err.Warden;

/**
 * Implements - if I guess right - the classical union find algorithm with path compression and additionally links the elements.
 * Not thread-safe!
 *
 * @author Gunnar Zarncke
 * @param <T> type of elements
 */
public class UnionFind<T> implements Union<T>, Serializable {
	private static final long serialVersionUID = 1L;

	public static <T> Union<T> create(final T element) {
		return new UnionFind<T>(element);
	}
	public static <T> Union<T> create(final T... elements) {
		// TODO allow empty union?
		Union<T> u = create(elements[0]);
		// TODO optimize construction (flat)?
		for (int i = 1; i < elements.length; i++) {
			u = u.unionWith(create(elements[i]));
		}
		return u;
	}

	/**
	 * <pre>
	 *      .->0
	 *  ,-- R <.
	 * (  / | \ )
	 *  >A->B->C
	 *
	 * union with
	 *      .->0
	 *  ,-- S <.
	 * (  / | \ )
	 *  >D->E->F
	 *
	 *  is
	 *       .--------.,->0
	 *      /  _____.--R <.
	 *     /  /      / | \ )
	 *    /  /.---->A->B->C
	 *   /  |/
	 *  /   S <.
	 * (  / | \ )
	 *  >D->E->F
	 *
	 *
	 *
	 * </pre>
	 */
	private final T element;
	private UnionFind<T> representant;
	private UnionFind<T> next;
	private int size;

	public UnionFind(final T element) {
		this.element = element;
		this.representant = null;
		this.next = this;
		this.size = 1;
	}

	public Union<T> unionWith(final Union<T> union) {
		UnionFind<T> greater = (UnionFind<T>) union.find();
		UnionFind<T> smaller = find();
		if (greater == smaller) {
			return greater;
		}
		if (greater.size < smaller.size) {
			UnionFind<T> t = smaller;
			smaller = greater;
			greater = t;
		}
		smaller.representant = greater;
		UnionFind<T> start = greater.next;
		greater.next = smaller.next;
		smaller.next = start;
		greater.size += smaller.size;

		return greater;
	}

	public UnionFind<T> find() {
		if (this.representant != null) {
			// path compression
			return this.representant = this.representant.find();
		}
		return this;
	}

	public Collection<T> elements() {
		final UnionFind<T> union = find();
		return new AbstractCollection<T>() {

			transient Collection<T> values = null;

			@Override
			public Iterator<T> iterator() {
				if (this.values != null) {
					return this.values.iterator();
				}
				return new Iterator<T>() {

					private UnionFind<T> current = union;
					private int i = union.size;
					public boolean hasNext() {
						return this.i > 0;
					}

					public T next() {
						if (this.i-- <= 0) {
							throw Warden.spot(new NoSuchElementException("already all returned"));
						}
						T t = this.current.element;
						this.current = this.current.next;
						return t;
					}

					public void remove() {
						throw Warden.spot(new UnsupportedOperationException("cannot modify Union."));
					}
				};
			}

			private Collection<T> fill() {
				if (this.values == null) {
					this.values = new ArrayList<T>(this);
				}
				return this.values;
			}

			@Override
			public int size() {
				return union.size;
			}

			@Override
			public int hashCode() {
				return fill().hashCode();
			}

			@Override
			public boolean equals(final Object obj) {
				return fill().equals(obj);
			}
		};
	}


	@Override
	public int hashCode() {
		return find().hashCode();
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
		UnionFind<?> other = (UnionFind<?>) obj;
		return find() == other.find();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		// doesn't do path compression - this is rather debug info
		UnionFind<T> root = this;
		while (root.representant != null) {
			root = root.representant;
		}

		UnionFind<T> curr = root;

		sb.append(curr.element).append("\n");
		for (int i = 1; i < root.size; i++) {
			curr = curr.next;
			sb.append(curr.element).append("->").append(curr.representant.element).append("\n");
		}

		return sb.toString();
	}
}
