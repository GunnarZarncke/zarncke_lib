package de.zarncke.lib.coll;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import javax.annotation.Nonnull;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

/**
 * Very short notation for creating Lists of elements.
 * The returned are always {@link ArrayList} except where otherwise noted.
 *
 * @author Gunnar Zarncke
 */
public final class L {

	private L() {
		// helper
	}

	private static final class ListSupplier<T> implements Supplier<List<T>>, Serializable {
		private static final long serialVersionUID = 1L;

		@Override
		public List<T> get() {
			return new ArrayList<T>();
		}
	}

	@Nonnull
	public static <T> Supplier<List<T>> supplier() {
		return new ListSupplier<T>();
	}

	/**
	 * @param <T> element
	 * @param initialCapacity >=0 for the new {@link ArrayList}
	 * @return a new empty {@link ArrayList}
	 */
	@Nonnull
	public static <T> List<T> n(final int initialCapacity) {
		return new ArrayList<T>(initialCapacity);
	}

	/**
	 * @param <T> element
	 * @return a empty unmodifiable list.
	 */
	@Nonnull
	public static <T> List<T> e() {
		return Collections.emptyList();
	}

	/**
	 * @param <T> element
	 * @return a new empty {@link ArrayList}
	 */
	@Nonnull
	public static <T> List<T> l() {
		return new ArrayList<T>();
	}

	/**
	 * @param <T> element
	 * @param o objects
	 * @return {@link ArrayList}
	 */
	@SuppressWarnings("unchecked")
	@Nonnull
	public static <T> List<T> l(final T o) {
		return new ArrayList<T>(Arrays.asList(o));
	}

	/**
	 * @param <T> element
	 * @param o1 object
	 * @param o2 object
	 * @return {@link ArrayList}
	 */
	@SuppressWarnings("unchecked")
	@Nonnull
	public static <T> List<T> l(final T o1, final T o2) {
		return new ArrayList<T>(Arrays.asList(o1, o2));
	}

	/**
	 * @param <T> element
	 * @param o1 object
	 * @param o2 object
	 * @param o3 object
	 * @return {@link ArrayList}
	 */
	@SuppressWarnings("unchecked")
	@Nonnull
	public static <T> List<T> l(final T o1, final T o2, final T o3) {
		return new ArrayList<T>(Arrays.asList(o1, o2, o3));
	}

	/**
	 * @param <T> element
	 * @param o objects
	 * @return {@link ArrayList}
	 */
	@Nonnull
	public static <T> List<T> l(final T... o) {
		return new ArrayList<T>(Arrays.asList(o));
	}

	/**
	 * @param ints to put into list
	 * @return List<Integer>
	 */
	@Nonnull
	public static List<Integer> li(final int... ints) {
		// might use fastutil
		return new ArrayList<Integer>(Ints.asList(ints));
	}

	/**
	 * @param longs to put into list
	 * @return List<Long>
	 */
	@Nonnull
	public static List<Long> ll(final long... longs) {
		// might use fastutil
		return new ArrayList<Long>(Longs.asList(longs));
	}

	/**
	 * @param <T> element
	 * @param o object
	 * @return an unmodifiable singleton list.
	 */
	@Nonnull
	public static <T> List<T> s(final T o) {
		return Collections.singletonList(o);
	}

	/**
	 * @param <T> element
	 * @param e element
	 * @return empty (if e is null) or singleton List (unmodifiable)
	 */
	@Nonnull
	public static <T> List<T> nullAsEmpty(final T e) {
		return e == null ? L.<T> e() : s(e);
	}

	/**
	 * @param <T> element
	 * @param c objects
	 * @return {@link ArrayList}
	 */
	@Nonnull
	public static <T> List<T> copy(final Iterator<? extends T> c) {
		int n = 10;
		if (c instanceof Remaining) {
			long a = ((Remaining) c).available();
			if (a >= 0 && a < Integer.MAX_VALUE) {
				n = (int) a;
			}
		}
		return Elements.dumpIteratorInto(c, L.<T> n(n));
	}

	/**
	 * @param <T> element
	 * @param c objects
	 * @return copied {@link ArrayList}
	 */
	@Nonnull
	public static <T> List<T> copy(@Nonnull final Collection<? extends T> c) {
		return new ArrayList<T>(c);
	}

	/**
	 * The resulting List has exactly the needed size.
	 *
	 * @param parts to concat (all are copied)
	 * @param <T> element type
	 * @return List of all partial lists concatenated in order
	 */
	@Nonnull
	public static <T> List<T> add(@Nonnull final Collection<? extends T>... parts) {
		if (parts.length == 0) {
			return L.e();
		}
		if (parts.length == 1) {
			return L.copy(parts[0]);
		}
		int n = 0;
		for (Collection<? extends T> c : parts) {
			n += c.size();
		}
		List<T> accu = L.n(n);
		for (Collection<? extends T> c : parts) {
			accu.addAll(c);
		}
		return accu;
	}

	/**
	 * @param <T> element
	 * @param elements initial elements
	 * @return a new empty {@link HashSet}
	 */
	@Nonnull
	public static <T> Set<T> set(final T... elements) {
		return new HashSet<T>(Arrays.asList(elements));
	}

	/**
	 * @param <T> element
	 * @return a new empty {@link SortedSet}
	 */
	@Nonnull
	public static <T> SortedSet<T> eset() {
		return ImmutableSortedSet.of();
	}

	/**
	 * @param <K> key type
	 * @param <V> value type
	 * @return a new empty {@link HashMap}
	 */
	@Nonnull
	public static <K, V> Map<K, V> map() {
		return new HashMap<K, V>();
	}

	/**
	 * @param keysAndValues alternating keys and values, length must be even
	 * @return a new {@link HashMap}
	 */
	@Nonnull
	public static Map<String, String> stringMap(final String... keysAndValues) {
		return tupleMap(keysAndValues);
	}

	/**
	 * @param keysAndValues alternating keys and values, length must be even
	 * @param <T> type of keys and values
	 * @return a new {@link HashMap}
	 */
	@Nonnull
	public static <T> Map<T, T> tupleMap(final T... keysAndValues) {
		assert keysAndValues.length % 2 == 0;
		Map<T, T> map = map();
		for (int i = 0; i < keysAndValues.length; i += 2) {
			map.put(keysAndValues[i], keysAndValues[i + 1]);
		}
		return map;
	}

}