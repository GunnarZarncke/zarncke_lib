package de.zarncke.lib.coll;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.annotation.Nonnull;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;

import de.zarncke.lib.err.Warden;
import de.zarncke.lib.lang.ClassTools;
import de.zarncke.lib.log.Log;
import de.zarncke.lib.util.Chars;

/**
 * helper class with static methods for collection handling.
 */
public final class Elements {

	private static final int MAX_ARRAY_TO_STRING_SIZE = 8000;

	public static final class CheckIterator<T> implements Iterator<T>, Remaining {
		private final Class<T> clazz;
		private final Iterable<?> any;
		Iterator<?> it;
		boolean hasNext;
		private Object next = null;

		// FEATURE we might inspect Iterable for HasSize to improve Remaining

		public CheckIterator(final Class<T> clazz, final Iterable<?> any) {
			this.clazz = clazz;
			this.any = any;
			this.it = this.any.iterator();
			next();
		}

		@Override
		public boolean hasNext() {
			return this.hasNext;
		}

		@Override
		public T next() {
			@SuppressWarnings("unchecked" /* this is exactly the job ob this iterator */)
			T n = (T) this.next;
			this.hasNext = false;
			while (this.it.hasNext()) {
				this.next = this.it.next();
				if (this.clazz.isInstance(this.next)) {
					this.hasNext = true;
					break;
				}
			}
			return n;
		}

		@Override
		public void remove() {
			this.it.remove();
		}

		@Override
		public long available() {
			if (this.it instanceof Remaining) {
				return ((Remaining) this.it).available();
			}
			return UNKNOWN;
		}

		@Override
		public String toString() {
			return "ensure " + this.clazz + " of " + this.it;
		}
	}

	// FEATURE might support Remaining interface
	public static final class FlattenIterator<T> implements Iterator<T> {
		private final Collection<T>[] collections;
		int pos = 0;
		Iterator<T> current = advance();

		public FlattenIterator(final Collection<T>[] collections) {
			this.collections = collections;
		}

		private Iterator<T> advance() {
			return this.pos >= this.collections.length ? null : this.collections[this.pos].iterator();
		}

		@Override
		public boolean hasNext() {
			return this.pos < this.collections.length;
		}

		@Override
		public T next() {
			T n = this.current.next();
			if (!this.current.hasNext()) {
				this.pos++;
				this.current = advance();
			}
			return n;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("cannot modify the original collections");
		}
	}

	public static final class NotNull<T> implements Predicate<T> {
		@Override
		public boolean apply(final T input) {
			return input != null;
		}
	}

	private static final int TYPICAL_SCREEN_WIDTH_CHARS = 80;
	public static final byte[] NO_BYTES = new byte[0];
	public static final int BYTES_PER_INT = 4;
	public static final int BYTES_PER_LONG = 8;
	public static final int BYTE_MASK = 0xff;
	public static final int BITS_PER_BYTE = 8;

	private Elements() { // helper
	}

	/**
	 * @param <T> element and return type
	 * @param list of T != null, may be empty
	 * @return first element or null if empty
	 * @throws IllegalArgumentException if the Collections contains more than one element
	 */
	public static <T> T singletonOrNull(final Collection<T> list) {
		if (list.isEmpty()) {
			return null;
		}
		Iterator<T> it = list.iterator();
		T o = it.next();
		if (!it.hasNext()) {
			return o;
		}
		throw Warden.spot(new IllegalArgumentException("more than one element in " + list));
	}

	/**
	 * @param <T> element and return type
	 * @param list of T, may be empty; null is handled like empty
	 * @return first element or null if empty
	 */
	public static <T> T firstOrNull(final Collection<T> list) {
		if (list == null || list.isEmpty()) {
			return null;
		}
		return list.iterator().next();
	}

	/**
	 * Slow if collection doesn't support efficient access of the last element of course.
	 *
	 * @param <T> element and return type
	 * @param list of T != null, may be empty
	 * @return last element or null if empty
	 */
	public static <T> T lastOrNull(final Collection<T> list) {
		if (list.isEmpty()) {
			return null;
		}
		if (list instanceof NavigableSet<?>) {
			return ((NavigableSet<T>) list).last();
		}
		if (list instanceof List<?>) {
			return ((List<T>) list).get(list.size());
		}
		Iterator<T> it = list.iterator();
		while (true) {
			T curr = it.next();
			if (!it.hasNext()) {
				return curr;
			}
		}
	}

	public static boolean contentEquals(final List<?> a, final List<?> b) {
		if (a == null) {
			return b == null;
		}
		if (b == null) {
			return false;
		}
		if (a.size() != b.size()) {
			return false;
		}
		Iterator<?> bit = b.iterator();
		for (Object o : a) {
			if (!o.equals(bit.next())) {
				return false;
			}
		}
		return true;
	}

	public static boolean contentNestedEquals(final List<?> a, final List<?> b) {
		if (a == null) {
			return b == null;
		}
		if (b == null) {
			return false;
		}
		if (a.size() != b.size()) {
			return false;
		}
		Iterator<?> bit = b.iterator();
		for (Object ao : a) {
			Object bo = bit.next();
			if (ao instanceof List<?> && bo instanceof List) {
				if (!contentEquals((List<?>) ao, (List<?>) bo)) {
					return false;
				}
			} else if (!ao.equals(bo)) {
				return false;
			}
		}
		return true;
	}

	public static boolean contentEquals(final Set<?> a, final Set<?> b) {
		if (a == null) {
			return b == null;
		}
		if (b == null) {
			return false;
		}
		if (a.size() != b.size()) {
			return false;
		}
		for (Object o : a) {
			if (!b.contains(o)) {
				return false;
			}
		}
		return true;
	}

	public static boolean contentEquals(final Collection<?> a, final Collection<?> b) {
		if (a == null) {
			return b == null;
		}
		if (b == null) {
			return false;
		}
		if (a.size() != b.size()) {
			return false;
		}
		if (!a.containsAll(b)) {
			return false;
		}
		return b.containsAll(a);
	}

	public static int sumBytes(final byte[] byteArray) {
		int s = 0;
		for (byte b : byteArray) {
			s += (char) b;
		}
		return s;
	}

	/**
	 * Split list into equally sized parts.
	 *
	 * @param <T> type of elements
	 * @param list of T
	 * @param chunkLen
	 * determines element size
	 * @return List of lists of T, all of equal size except the last one
	 */
	public static <T> List<List<T>> split(final List<T> list, final int chunkLen) {
		int size = list.size();
		int fullChunks = size / chunkLen;
		ArrayList<List<T>> chunkList = new ArrayList<List<T>>(fullChunks + 1);
		for (int i = 0; i < fullChunks; i++) {
			chunkList.add(list.subList(i * chunkLen, (i + 1) * chunkLen));
		}
		if (fullChunks * chunkLen < size) {
			chunkList.add(list.subList(fullChunks * chunkLen, size));
		}
		return chunkList;
	}

	/**
	 * Flatten out a "collection", i.e. replacing all sub-Collection with their elements in order. Please note, that
	 * null is considered as the empty Collection and any non-Collection is represented as a one-element Collection
	 *
	 * @param coll
	 * the Collection to flatten
	 * @return a result Collection of the same type (if possible)
	 */
	public static Collection<?> flattenRecursively(final Object coll) {
		return flatten(coll, Integer.MAX_VALUE);
	}

	/**
	 * Flatten out a "collection", i.e. replacing all sub-Collection p to a given depth with their elements in order. <br>
	 * Please note, that null is considered as the empty Collection and any non-Collection is represented as a
	 * one-element Collection. Sub-Collections, that are not flattened are used as is.
	 *
	 * @param coll
	 * the Collection to flatten
	 * @param depth
	 * is the number of nestings to flatten (0 = no flattening)
	 * @return a result Collection of the same type (if possible)
	 */
	public static Collection<?> flatten(final Object coll, final int depth) {
		if (coll == null) {
			return new ArrayList<Object>();
		}
		if (coll instanceof Collection<?>) {
			if (depth == 0) {
				return (Collection<?>) coll;
			}
			@SuppressWarnings("unchecked"/* we did */)
			Collection<Object> res = sameCollection((Collection<Object>) coll);
			flattenAppend((Collection<?>) coll, res, depth);
			return res;
		}
		if (coll instanceof Collection<?>[]) {
			return flattenRecursively(L.l(coll));
		}
		Collection<Object> res = L.l();
		res.add(coll);
		return res;
	}

	/**
	 * Accumulate the given collections in one large virtual collection.
	 * No recursion on elements is done. Use {@link #flattenRecursively(Object)} for that.
	 * The result collection will provide the elements in the same order as yielded by the single collections.
	 *
	 * @param collections []
	 * @param <T> type of element
	 * @return Collection != null
	 */
	public static <T> Collection<T> flattenInPlace(final Collection<T>... collections) {
		int s = 0;
		for (Collection<T> c : collections) {
			s += c.size();
		}
		final int fs = s;
		return new AbstractCollection<T>() {

			@Override
			public Iterator<T> iterator() {
				return new FlattenIterator<T>(collections);
			}

			@Override
			public int size() {
				return fs;
			}
		};
	}

	@SuppressWarnings("unchecked")
	public static <T> Collection<T> flattenInPlace(final Collection<Collection<T>> collections) {
		return flattenInPlace((Collection<T>[]) collections.toArray(new Collection[collections.size()]));
	}

	/**
	 * Accumulate the given collections in one large collection.
	 * No recursion on elements is done. Use {@link #flattenRecursively(Object)} for that.
	 *
	 * @param collections []
	 * @param <T> type of element
	 * @return Collection != null if possible of same type as first given Collection
	 */
	public static <T> Collection<T> concat(final Collection<T>... collections) {
		if (collections.length == 0) {
			return L.e();
		}
		if (collections.length == 1) {
			return collections[0];
		}
		Collection<T> accu = sameCollection(collections[0]);
		for (Collection<T> c : collections) {
			accu.addAll(c);
		}
		return accu;
	}

	/**
	 * Accumulate the given collections in one large collection.
	 * No recursion on elements is done. Use {@link #flattenRecursively(Object)} for that.
	 *
	 * @param collections Collection
	 * @param <T> type of element
	 * @return Collection != null if possible of same type as first given Collection; if only one collection was given
	 * that is
	 * returned as is.
	 */
	public static <T> Collection<T> flatten(final Collection<? extends Collection<T>> collections) {
		if (collections.size() == 0) {
			return L.e();
		}
		if (collections.size() == 1) {
			return collections.iterator().next();
		}
		Collection<T> accu = sameCollection(collections.iterator().next());
		for (Collection<T> c : collections) {
			accu.addAll(c);
		}
		return accu;
	}

	public static <T> void flattenAppend(final Collection<?> coll, final Collection<T> res, final int depth) {
		for (Object el : coll) {
			if (depth > 0 && el instanceof Collection) {
				flattenAppend((Collection<?>) el, res, depth - 1);
			} else if (el != null) {
				res.add((T) el);
			}
		}
	}

	/**
	 * wrap all values of the Map in a Collection
	 *
	 * @param <K> key type
	 * @param <V> value type
	 * @param map != null
	 * @return Map
	 */
	public static <K, V> Map<K, Collection<V>> collectifyMap(final Map<K, V> map) {
		return Mapper.map(new Mapper<V, Collection<V>>() {
			@Override
			public Collection<V> map(final V ob) {
				return new OneElementCollection<V>(ob);
			}
		}, map);
	}

	/**
	 * The mappings of two Maps are joined by merging values of equal keys with mergeGeneral.
	 *
	 * @param a any
	 * @param b any
	 * @return any
	 */
	// TODO add generics
	@SuppressWarnings("unchecked")
	public static Map mergeMaps(final Map a, final Map b) {
		if (b == null) {
			return a;
		}
		if (a == null) {
			return b;
		}

		Map res = new HashMap(b);
		for (Iterator it = a.entrySet().iterator(); it.hasNext();) {
			Map.Entry me = (Map.Entry) it.next();
			Object key = me.getKey();
			Object val = mergeGeneral(me.getValue(), b.get(key));

			if (val != null) {
				res.put(key, val);
			}
		}

		return res;
	}

	/**
	 * The mappings of two Maps are joined by appending the values of keys in the second list to the Collection fould in
	 * the first one. if no list or an element is found it is wrapped up. This is an asymetrical variant of mergeMaps.
	 * <p>
	 * Important Note: The Collections of the left hand Map are modified and/or reused! do not use them any longer.
	 *
	 * @param a any
	 * @param b any
	 * @return any
	 */
	// TODO add generics
	@SuppressWarnings("unchecked")
	public static Map appendMaps(final Map a, final Map b) {
		if (b == null) {
			return a;
		}
		if (a == null) {
			return collectifyMap(b);
		}
		Map res = new HashMap(a);
		for (Iterator it = b.entrySet().iterator(); it.hasNext();) {
			Map.Entry me = (Map.Entry) it.next();
			Object key = me.getKey();
			Object val = appendGeneral(a.get(key), me.getValue());
			if (val != null) {
				res.put(key, val);
			}
		}

		return res;
	}

	/**
	 * merge two "lists", where null is considered an empty list and an Object is considered a one element-Collection.
	 * the result is returned in the same form (really empty or one-element lists are NOT unwrapped).
	 *
	 * @param a any
	 * @param b any
	 * @return any
	 */
	// TODO add generics
	@SuppressWarnings("unchecked")
	public static Object mergeGeneral(final Object a, final Object b) {
		if (a == null) {
			return b;
		}
		if (b == null) {
			return a;
		}

		if (a instanceof Collection) {
			// append to a collectin
			Collection c = copyCollection((Collection) a);
			if (b instanceof Collection) {
				c.addAll((Collection) b);
				return c;
			}

			c.add(b);
			return c;
		}

		if (b instanceof Collection) {
			// reuse same collection type as b
			Collection c = sameCollection((Collection) b);
			c.add(a);
			c.addAll((Collection) b);
			return c;
		}

		// for two elements create strait Collection
		Collection c = new ArrayList();
		c.add(a);
		c.add(b);
		return c;
	}

	/**
	 * Append an object to a list. if b is empty it is not added. if a is empty or no Collection a new one is created
	 * and b added. otherwise b is added to a copy of a.
	 *
	 * @param a
	 * is the "List" to append to
	 * @param b
	 * is the Object to append
	 * @return the resultant Collection or null
	 */
	// TODO add generics
	@SuppressWarnings("unchecked")
	private static Object appendGeneral(final Object a, final Object b) {
		if (b == null) {
			return a;
		}
		if (a == null) {
			Collection c = new ArrayList();
			c.add(b);
			return b;
		}
		if (a instanceof Collection) {
			Collection c = copyCollection((Collection) a);
			c.add(b);
			return c;
		}

		Collection c = new ArrayList();
		c.add(a);
		c.add(b);
		return c;
	}

	/**
	 * Create an empty Collection of the same type (if possible) of the given Collection.
	 *
	 * @param coll != null
	 * @param <T> type of elements
	 * @return Collection of same type (basically)
	 */
	@SuppressWarnings("unchecked")
	public static <T> Collection<T> sameCollection(final Collection<T> coll) {
		if (coll == null) {
			throw new IllegalArgumentException("collection not null!");
		}

		// try the default constructor of same class
		try {
			Class<?> cl = coll.getClass();
			Object[] arg = new Object[] { coll };
			Constructor<?> cons = cl.getConstructor(new Class[] { cl });
			return (Collection<T>) cons.newInstance(arg);
		} catch (Exception ex) {
			// ignore, try jdk defaults
		}

		// try next best matches
		if (coll instanceof SortedSet<?>) {
			return new TreeSet<T>();
		}
		if (coll instanceof Set<?>) {
			return new HashSet<T>();
		}

		// when in doubt, use list
		return new ArrayList<T>();
	}

	/**
	 * Create a copy of a given Collection. Try hard to make it the same Class. And do so fast.
	 *
	 * @param coll to clone != null
	 * @param <T> type of elements
	 * @return new collection of most equal type
	 */
	public static <T> Collection<T> copyCollection(final Collection<T> coll) {
		if (coll == null) {
			throw new IllegalArgumentException("collection not null!");
		}

		// first try to clone it
		if (coll instanceof Cloneable) {
			try {
				@SuppressWarnings("unchecked")
				Collection<T> ct = (Collection<T>) ClassTools.clone(coll);
				return ct;
			} catch (Exception ex) {
				Log.LOG.get().report("\n\nhopplahopp: clone????" + coll + coll.getClass());
				Log.LOG.get().report(ex);
			}
		}

		// then try the copy constructor
		@SuppressWarnings("unchecked")
		Class<? extends Collection<T>> cl = (Class<? extends Collection<T>>) coll.getClass();
		Object[] arg = new Object[] { coll };
		try {
			Constructor<? extends Collection<T>> cons = cl.getConstructor(new Class[] { cl });
			return cons.newInstance(arg);
		} catch (Exception ex) { // NOPMD genral
		}

		// then try the default constructor
		try {
			Constructor<? extends Collection<T>> cons = cl.getConstructor(new Class[] { Collection.class });
			return cons.newInstance(arg);
		} catch (Exception ex) { // NOPMD general
		}

		// try next best matches
		if (coll instanceof SortedSet<?>) {
			return new TreeSet<T>(coll);
		}
		if (coll instanceof Set<?>) {
			return new HashSet<T>(coll);
		}

		// when in doubt, use list
		return new ArrayList<T>(coll);
	}

	/**
	 * compare two arrays for element-wise equality. note: the arrays must not be of same type, only the elements must
	 * be equals.
	 *
	 * @param a
	 * is any java array (also primitiv)
	 * @param b
	 * is any java array (also primitiv)
	 * @return true if the arrays contain equal elements at equals indizes, the arrays must have the same length, but
	 * not the same type.
	 * @exception IllegalArgumentException
	 * if Objects are not arrays or if elements have differing primitive type
	 */
	public static boolean arrayequals(final Object a, final Object b) {
		// shortcut same arrays
		if (a == b) {
			return true;
		}

		// test arrays
		Class<?> ac = a.getClass();
		Class<?> bc = b.getClass();
		if (!ac.isArray() || !bc.isArray()) {
			throw new IllegalArgumentException("Object is not an array");
		}

		// it is illegal, if only one is of primitiv type or
		// that both are of primitive type, but the primitiv types differ
		ac = ac.getComponentType();
		bc = bc.getComponentType();
		if (ac.isPrimitive() && bc.isPrimitive() && ac != bc || ac.isPrimitive() != bc.isPrimitive()) {
			throw new IllegalArgumentException("Object is not an array");
		}

		// must have equals length
		int l = Array.getLength(a);
		if (l != Array.getLength(b)) {
			return false;
		}

		if (ac.isPrimitive()) {
			if (ac == Byte.TYPE) {
				return arrayequals((byte[]) a, (byte[]) b);
			}
			if (ac == Integer.TYPE) {
				return arrayequals((int[]) a, (int[]) b);
			}
			if (ac == Long.TYPE) {
				return arrayequals((long[]) a, (long[]) b);
			}
		}

		// default general case
		for (int i = 0; i < l; i++) {
			if (!Array.get(a, i).equals(Array.get(b, i))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * compare two arrays for element-wise equality.
	 *
	 * @param a
	 * is a byte array
	 * @param b
	 * is a byte array
	 * @return true if the arrays contain equal elements at equals indizes, the arrays must have the same length
	 */
	public static boolean arrayequals(final byte[] a, final byte[] b) {
		if (a.length != b.length) {
			return false;
		}

		for (int i = 0; i < a.length; i++) {
			if (a[i] != b[i]) {
				return false;
			}
		}

		return true;
	}

	/**
	 * compare two arrays lexicographically byte-wise.
	 *
	 * @param a
	 * is a byte array
	 * @param b
	 * is a byte array
	 * @return same semantics as {@link Byte#compareTo(Byte)}.
	 */
	public static int arraycompare(final byte[] a, final byte[] b) {
		for (int i = 0; i < a.length && i < b.length; i++) {
			if (a[i] != b[i]) {
				return a[i] - b[i];
			}
		}
		return a.length - b.length;
	}

	/**
	 * @param arraya
	 * @param arrayb
	 * @return length of common prefix, at least 0, at most smaller of both array lengths
	 */
	public static int findCommonSuffix(@Nonnull final byte[] arraya, @Nonnull final byte[] arrayb) {
		int l = Math.min(arraya.length, arrayb.length);
		int commonSuffix;
		for (commonSuffix = 0; commonSuffix < l; commonSuffix++) {
			if (arraya[arraya.length - commonSuffix - 1] != arrayb[arrayb.length - commonSuffix - 1]) {
				break;
			}
		}
		return commonSuffix;
	}

	/**
	 * @param arraya
	 * @param arrayb
	 * @return length of common suffix, at least 0, at most smaller of both array lengths
	 */
	public static int findCommonPrefix(final byte[] arraya, final byte[] arrayb) {
		int l = Math.min(arraya.length, arrayb.length);
		int commonPrefix;
		for (commonPrefix = 0; commonPrefix < l; commonPrefix++) {
			if (arraya[commonPrefix] != arrayb[commonPrefix]) {
				break;
			}
		}
		return commonPrefix;
	}

	/**
	 * compare two arrays for element-wise equality.
	 *
	 * @param a
	 * is an int array
	 * @param b
	 * is an int array
	 * @return true if the arrays contain equal elements at equals indizes, the arrays must have the same length
	 */
	public static boolean arrayequals(final int[] a, final int[] b) {
		if (a.length != b.length) {
			return false;
		}

		for (int i = 0; i < a.length; i++) {
			if (a[i] != b[i]) {
				return false;
			}
		}

		return true;
	}

	/**
	 * compare two arrays for element-wise equality.
	 *
	 * @param a
	 * is a long array
	 * @param b
	 * is a long array
	 * @return true if the arrays contain equal elements at equals indizes, the arrays must have the same length
	 */
	public static boolean arrayequals(final long[] a, final long[] b) {
		if (a.length != b.length) {
			return false;
		}

		for (int i = 0; i < a.length; i++) {
			if (a[i] != b[i]) {
				return false;
			}
		}

		return true;
	}

	/**
	 * compare parts of two int-arrays for element-wise equality. The parts must exist.
	 *
	 * @param a
	 * is an int array
	 * @param p
	 * is starting-position in a
	 * @param b
	 * is an int array
	 * @param q
	 * is starting-position in b
	 * @param l
	 * is length to compare
	 * @return true if the arrays contain equal elements at equals indizes, the arrays must have the same length
	 * @exception ArrayIndexOutOfBoundsException
	 * if parts invalid
	 */
	public static boolean arrayequals(final int[] a, final int p, final int[] b, final int q, final int l) {
		int i = p, j = q;
		int c = l;
		while (c-- > 0) {
			if (a[i++] != b[j++]) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Create List of integers
	 *
	 * @param start
	 * starting value (inclusive)
	 * @param end
	 * ending value (exclusive!)
	 * @param step
	 * stepping (>=1)
	 * @return a List containing the values in sequence
	 */
	public static List<Integer> intList(final int start, final int end, final int step) {
		if (end < start || step < 1) {
			throw new IllegalArgumentException("end >= start && step >= 1!");
		}

		List<Integer> l = new ArrayList<Integer>((end - start) / step + 5);
		for (int i = start; i < end; i += step) {
			l.add(Integer.valueOf(i));
		}
		return l;
	}

	/**
	 * Create List of longs.
	 *
	 * @param start
	 * starting value (inclusive)
	 * @param end
	 * ending value (exclusive!)
	 * @param step
	 * stepping (>=1)
	 * @return a List containing the values in sequence
	 */
	public static List<Long> longList(final long start, final long end, final long step) {
		if (end < start || step < 1) {
			throw new IllegalArgumentException("end >= start && step >= 1!");
		}
		if (end < start || step < 1) {
			throw new IllegalArgumentException("end >= start && step >= 1!");
		}

		long s = (end - start) / step;
		if (s > Integer.MAX_VALUE - 5) {
			throw new IllegalArgumentException("list will become too large!");
		}

		List<Long> l = new ArrayList<Long>((int) s + 5);
		for (long i = start; i < end; i += step) {
			l.add(Long.valueOf(i));
		}
		return l;
	}

	public static List<?> asList(final Object array) {
		if (array instanceof Object[]) {
			return Arrays.asList((Object[]) array);
		} else if (array instanceof int[]) {
			return asList((int[]) array);
		} else if (array instanceof byte[]) {
			return asList((byte[]) array);
		} else {
			throw new IllegalArgumentException(array.getClass() + " not yet supported!");
		}
	}

	/**
	 * wrap List of integers
	 *
	 * @param ints
	 * int[]
	 * @return List
	 */
	public static List<Integer> asList(final int[] ints) {
		int l = ints.length;
		Integer[] il = new Integer[l];
		for (int i = 0; i < l; i++) {
			il[i] = Integer.valueOf(ints[i]);
		}
		return Arrays.asList(il);
	}

	/**
	 * wrap List of integers
	 *
	 * @param ints
	 * int[]
	 * @return List
	 */
	public static List<Byte> asList(final byte[] ints) {
		int l = ints.length;
		Byte[] il = new Byte[l];
		for (int i = 0; i < l; i++) {
			il[i] = Byte.valueOf(ints[i]);
		}
		return Arrays.asList(il);
	}

	/**
	 * converts a Map of List to Object into a mu = Map of ( Object to (mu|Object) ). e.g. a -> x, a.b -> y, a.c -> z, b
	 * -> p becomes a -> (null -> x, b -> y, c -> z), b -> p
	 *
	 * @param listToObject != null
	 * @param <K> key type
	 * @param <T> values type
	 * @return x: Map of key to x or Object
	 */
	public static <K, T> Map<K, Object> hierarchify(final Map<List<K>, T> listToObject) {
		Map<K, Object> heads = new HashMap<K, Object>(listToObject.size() / 3 + 2);
		// strip outer layer
		for (Map.Entry<List<K>, T> me : listToObject.entrySet()) {
			List<K> l = me.getKey();
			if (l == null || l.isEmpty()) {
				heads.put(null, me.getValue());
			} else {
				K head = l.get(0);
				List<K> tail = l.subList(1, l.size());
				@SuppressWarnings("unchecked")
				Map<List<K>, Object> elems = (Map<List<K>, Object>) heads.get(head);
				if (elems == null) {
					elems = new HashMap<List<K>, Object>(4);
					heads.put(head, elems);
				}
				elems.put(tail, me.getValue());
			}
		}

		// compactify and process inner
		for (Map.Entry<K, Object> me : heads.entrySet()) {
			if (me.getKey() == null) {
				continue;
			}

			@SuppressWarnings("unchecked")
			Map<List<K>, T> elems = (Map<List<K>, T>) me.getValue();
			if (elems.size() == 1) {
				Map.Entry<List<K>, ?> single = elems.entrySet().iterator().next();
				me.setValue(nestMap(single.getKey(), single.getValue()));
			} else {
				me.setValue(hierarchify(elems));
			}
		}

		return heads;
	}

	private static Object nestMap(final List<?> list, final Object elem) {
		if (list == null || list.size() == 0) {
			return elem;
		}
		int n = list.size();
		if (n == 1) {
			return Collections.singletonMap(list.get(0), elem);
		}
		return nestMap(list.subList(1, n), elem);
	}

	/**
	 * Undoes {@link #hierarchify(Map)}.
	 *
	 * @param hierarchy as above
	 * @param <K> key type
	 * @param <T> value type
	 * @return Map of List of key to value
	 */
	public static <K, T> Map<List<K>, T> flattenHierarchy(final Map<K, Object> hierarchy) {
		Map<List<K>, T> heads = new HashMap<List<K>, T>(hierarchy.size() * 2);
		LinkedList<K> ll = new LinkedList<K>();
		flattenHierarchy(hierarchy, ll, heads);
		return heads;
	}

	@SuppressWarnings("unchecked" /* to avoid endless casts */)
	private static void flattenHierarchy(final Map hierarchy, final List prefix, final Map accu) {
		for (Iterator iter = hierarchy.entrySet().iterator(); iter.hasNext();) {
			Map.Entry me = (Map.Entry) iter.next();
			Object o = me.getKey();
			Object v = me.getValue();

			if (o == null) {
				accu.put(prefix, v);
			} else {
				List prefix2 = new LinkedList(prefix);
				prefix2.add(o);
				if (v instanceof Map) {
					flattenHierarchy((Map) v, prefix2, accu);
				} else {
					accu.put(prefix2, v);
				}
			}
		}
	}

	public static <S, D> List<D> transformList(final Collection<S> list, final Transform<S, D> trans) {
		List<D> res = new ArrayList<D>(list.size());
		for (S s : list) {
			res.add(trans.transform(s));
		}
		return res;
	}

	public static <K, S, D> Map<K, D> transformValues(final Map<K, S> list, final Transform<S, D> trans) {
		Map<K, D> res = new HashMap<K, D>(list.size() * 4 / 3);
		for (Map.Entry<K, S> me : list.entrySet()) {
			res.put(me.getKey(), trans.transform(me.getValue()));
		}
		return res;
	}

	public static <T> Transform<T, T> identity() {
		return new Transform<T, T>() {
			@Override
			public T transform(final T src) {
				return src;
			}
		};
	}

	/**
	 * Creates a Transform which applies itself on the values of maps.
	 *
	 * @param transform
	 * to apply on non-Map values
	 * @return Transform
	 */
	// TODO add generics
	@SuppressWarnings("unchecked")
	public static Transform<Object, Object> makeRecursiveMapTransformer(final Transform<Object, Object> transform) {
		Transform<Map, Map> identity = identity();
		return makeRecursiveMapTransformer(transform, identity);
	}

	/**
	 * Creates a Transform which applies itself on the values of maps and applies the given map transform on the so
	 * transformed Maps. I.e. it converts a mu = Map of ( Object to (mu|Object) ) into a Object|?.
	 *
	 * @param valueTransform
	 * to apply on non-Map values
	 * @param mapTransform
	 * to apply on transformed Map values
	 * @return Transform
	 */
	// TODO add generics
	@SuppressWarnings("unchecked")
	public static Transform<Object, Object> makeRecursiveMapTransformer(final Transform<Object, Object> valueTransform,
			final Transform<Map, ?> mapTransform) {
		return new Transform<Object, Object>() {
			@Override
			public Object transform(final Object src) {
				if (src instanceof Map) {
					return mapTransform.transform(transformValues((Map) src, this));
				}
				return transform(src);
			}
		};
	}



	// inconsistent uses whitespace as separator whereas formatCsv uses ","
	public static List<String> parseCsv(final String string) {
		StringTokenizer st = new StringTokenizer(string);
		List<String> l = new ArrayList<String>(st.countTokens());
		while (st.hasMoreTokens()) {
			l.add(st.nextToken());
		}
		return l;
	}

	public static String formatCsv(final List<String> list) {
		StringBuffer sb = new StringBuffer();
		for (Iterator<String> iter = list.iterator(); iter.hasNext();) {
			sb.append(iter.next());
			if (iter.hasNext()) {
				sb.append(",");
			}
		}
		return sb.toString();
	}

	/**
	 * Creates a sensible debug output for any Object including null and arrays (also primitive and nested).
	 * Do not use for mass output, end user output or business logic.
	 * This is neither optimized nor tested well and more of a programmer convenience.
	 *
	 * @param object anything
	 * @return human readable String != null
	 */
	public static String toString(final Object object) {
		// TODO handle Exceptions
		// TODO handle Objects commonly without sensible output
		if (object == null) {
			return "null";
		}
		if (object.getClass().isArray()) {
			return Chars.lineWrap(arrayToString(object, true), TYPICAL_SCREEN_WIDTH_CHARS);
		}
		return object.toString();
	}

	/**
	 * Same as {@link #toString(Object)} but handles collection classes and maps explicitly to allow printing of usually
	 * non-printable elements like arrays.
	 *
	 * @param object
	 * @return human readable String
	 */
	public static String toStringHandlingArrayElements(final Object object, final int depth) {
		Set<Object> seen = L.set();
		StringBuilder sb = new StringBuilder();
		appendToString(object, sb, seen, depth);
		return sb.toString();
	}

	private static void appendToString(final Object object, final StringBuilder appender, final Set<Object> seen,
			final int depth) {
		if (seen == null) {
			appender.append("null");
			return;
		}
		if (seen.contains(object)) {
			appender.append("recurse");
			return;
		}
		seen.add(object);
		if (depth < 0) {
			appender.append("...");
			return;
		}
		if (object instanceof Collection) {
			appender.append(object.getClass().getSimpleName()).append("(");
			for (Object o : (Collection<?>) object) {
				appendToString(o, appender, seen, depth - 1);
				appender.append(",");
			}
			appender.setLength(appender.length() - 1); // drop last ","
			appender.append(")");
		} else if (object instanceof Map) {
			appender.append(object.getClass().getSimpleName()).append("{");
			for (Map.Entry<?, ?> me : ((Map<?, ?>) object).entrySet()) {
				appendToString(me.getKey(), appender, seen, depth - 2);
				appender.append("->");
				appendToString(me.getValue(), appender, seen, depth - 2);
				appender.append(",");
			}
			appender.setLength(appender.length() - 1); // drop last ","
			appender.append("}");
		} else if (object.getClass().isArray()) {
			// TODO respect depth
			// if (object.getClass().getComponentType().isArray()) {
			// return nonPrimitiveArrayToString((Object[]) object, allowLineWrap);
			// }
			appender.append(Chars.lineWrap(arrayToString(object, true), TYPICAL_SCREEN_WIDTH_CHARS));
		} else {
			appender.append(object.toString());
		}
	}

	private static String arrayToString(final Object object, final boolean allowLineWrap) {
		if (object instanceof byte[]) {
			return Arrays.toString((byte[]) object);
		}
		if (object instanceof short[]) {
			return Arrays.toString((short[]) object);
		}
		if (object instanceof int[]) {
			return Arrays.toString((int[]) object);
		}
		if (object instanceof long[]) {
			return Arrays.toString((long[]) object);
		}
		if (object instanceof float[]) {
			return Arrays.toString((float[]) object);
		}
		if (object instanceof double[]) {
			return Arrays.toString((double[]) object);
		}
		if (object instanceof char[]) {
			return Arrays.toString((char[]) object);
		}
		if (object instanceof boolean[]) {
			return Arrays.toString((boolean[]) object);
		}
		if (object.getClass().getComponentType().isArray()) {
			return nonPrimitiveArrayToString((Object[]) object, allowLineWrap);
		}
		return Arrays.toString((Object[]) object);
	}

	private static String nonPrimitiveArrayToString(final Object[] array, final boolean allowLineWrap) {
		StringBuilder sb = new StringBuilder("[");
		for (Object o : array) {
			sb.append(Chars.indentEveryLine(toString(o), "  "));
			sb.append(',');
		}
		sb.setLength(sb.length() - 1);
		sb.append(']');
		return sb.toString();
	}

	/**
	 * Creates a new array with the contents of the two given arrays.
	 *
	 * @param <T> type of elements
	 * @param a array, null is treated as empty
	 * @param b array, null is treated as empty
	 * @return new array with the actual element type of the first array; if any array is null, the other is returned
	 * unchanged!
	 */
	public static <T> T[] concat(final T[] a, final T... b) {
		if (a == null) {
			return b;
		}
		if (b == null) {
			return a;
		}
		@SuppressWarnings("unchecked"/* Array does its job */)
		T[] res = (T[]) Array.newInstance(a.getClass().getComponentType(), a.length + b.length);
		System.arraycopy(a, 0, res, 0, a.length);
		System.arraycopy(b, 0, res, a.length, b.length);
		return res;
	}

	public static <T> Iterable<T> checkedIterable(final Iterable<?> any, final Class<T> clazz) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return checkedIterator(any, clazz);
			}
		};
	}

	/**
	 * Returns an Iterator which only returns elements of the given class from the given collection.
	 *
	 * @param <T> fetched type
	 * @param any Iterable
	 * @param clazz to find
	 * @return Iterator, doesn't return any null values
	 */
	public static <T> Iterator<T> checkedIterator(final Iterable<?> any, final Class<T> clazz) {
		return new CheckIterator<T>(clazz, any);
	}

	public static <T, S extends Collection<T>> S dumpIteratorInto(final Iterator<? extends T> iterator,
			final S collection) {
		while (iterator.hasNext()) {
			collection.add(iterator.next());
		}
		return collection;
	}

	public static <T, S extends Collection<T>> S dumpIntoCollection(final S toAddTo, final T... values) {
		for (T e : values) {
			toAddTo.add(e);
		}
		return toAddTo;
	}

	public static Iterator<Integer> count(final int start, final int end) {
		return count(start, end, 1);
	}

	public static Iterator<Integer> count(final int start, final int end, final int step) {
		return new Iterator<Integer>() {
			int pos = start;

			@Override
			public boolean hasNext() {
				return this.pos < end;
			}

			@Override
			public Integer next() {
				Integer r = Integer.valueOf(this.pos);
				this.pos += step;
				return r;
			}

			@Override
			public void remove() {
				throw Warden.spot(new UnsupportedOperationException("cannot remove ints!"));
			}

		};
	}

	/**
	 * Renders a byte array in a human readable form.
	 * If the first byte looks like a printable ASCII the whole string is
	 * converted to String using ASCCI encoding.
	 * If the first byte is 0 and the array is less then 9 bytes long a number is calculated (big endian). The number is
	 * shown
	 * in decimal if less than 2^16, as hexadecimal otherwise.
	 * If neither case mathes the array is shown as an an array of bytes.
	 *
	 * @param bytes != null
	 * @return String as above
	 */
	public static String byteArrayToHumanReadable(final byte[] bytes) {
		if (bytes.length == 0) {
			return "''";
		}
		if (bytes[0] < (byte) Chars.LEAST_PRINTABLE_ASCII) {
			if (bytes[0] == 0 && bytes.length <= BYTES_PER_LONG) {
				long v = 0;
				for (byte element : bytes) {
					v = (v << BITS_PER_BYTE) + (element & BYTE_MASK);
				}
				return v > Short.MAX_VALUE ? "0x" + Long.toHexString(v) : String.valueOf(v);
			}
			return Chars.summarize(Elements.toString(bytes), MAX_ARRAY_TO_STRING_SIZE, false).toString();
		}
		return "'" + new String(bytes) + "'"; // NOPMD debug
	}

	/**
	 * @param <T> type of elements
	 * @param element any
	 * @param listOfLists Collection of Collections (neither null)
	 */
	public static <T> void appendToAllSubCollections(final T element,
			final Collection<? extends Collection<T>> listOfLists) {
		for (Collection<T> subList : listOfLists) {
			subList.add(element);
		}
	}

	/**
	 * Replaces all values by the first element or toString in case of complex objects.
	 *
	 * @param map any Map
	 * @return Map of String to String
	 */
	public static Map<String, String> toSimpleMap(final Map<String, ?> map) {
		return Maps.transformValues(map, new Function<Object, String>() {
			@Override
			public String apply(final Object from) {
				if (from instanceof String) {
					return (String) from;
				}
				if (from instanceof Collection) {
					return apply(Elements.firstOrNull((Collection<?>) from));
				}
				if (from instanceof Object[]) {
					Object[] oa = (Object[]) from;
					return oa.length == 0 ? "" : apply(oa[0]);
				}
				return from == null ? "" : from.toString();
			}
		});
	}

	/**
	 * Compare lexicograpically. <code>null</code> is considered to be less than any Object.
	 * If a list is a prefix of the other list it is considered less.
	 *
	 * @param <T> comparable type
	 * @param a Collection of T != null, elements may be null
	 * @param b Collection of T != null, elements may be null
	 * @return see {@link Comparator#compare(Object, Object)}.
	 */
	public static <T extends Comparable<T>> int compare(final Collection<? extends T> a, final Collection<? extends T> b) {
		return compare(a, b, Ordering.<T> natural());
	}

	public static <T extends Comparable<T>> int compare(final Collection<? extends T> a,
			final Collection<? extends T> b, final Comparator<T> comparator) {
		int as = a.size();
		int bs = b.size();
		int ms = Math.min(as, bs);
		Iterator<? extends T> ai = a.iterator();
		Iterator<? extends T> bi = b.iterator();
		for (int i = 0; i < ms; i++) {
			T an = ai.next();
			T bn = bi.next();
			if (an == null) {
				if (bn != null) {
					return -1;
				}
			} else if (bn == null) {
				return 1;
			} else {
				int c = comparator.compare(an, bn);
				if (c != 0) {
					return c;
				}
			}
		}
		if (as < bs) {
			return -1;
		}
		if (as > bs) {
			return 1;
		}
		return 0;
	}

	/**
	 * @param <T> element type
	 * @param a List sorted by comp
	 * @param b List sorted by comp
	 * @param comp ordering
	 * @param removeDuplicates
	 * @return List of T sorted by comp
	 */
	public static <T> List<T> merge(final Collection<? extends T> a, final Collection<? extends T> b,
			final Comparator<T> comp, final boolean removeDuplicates) {
		List<T> r = L.n(a.size() + b.size());

		Iterator<? extends T> ait = a.iterator();
		Iterator<? extends T> bit = b.iterator();
		if (!ait.hasNext()) {
			return append(r, bit);
		}
		if (!bit.hasNext()) {
			return append(r, ait);
		}
		T ae = ait.next();
		T be = bit.next();
		while (true) {
			int cmp = comp.compare(ae, be);
			if (cmp < 0) {
				r.add(ae);
				if (ait.hasNext()) {
					ae = ait.next();
				} else {
					r.add(be);
					return append(r, bit);
				}
			} else {
				if (cmp != 0 || !removeDuplicates) {
					r.add(be);
				}

				if (bit.hasNext()) {
					be = bit.next();
				} else {
					r.add(ae);
					return append(r, ait);
				}
			}
		}
	}

	private static <T> List<T> append(final List<T> r, final Iterator<? extends T> it) {
		while (it.hasNext()) {
			r.add(it.next());
		}
		return r;
	}

	public static <T> List<T> withoutNullsCopy(final Collection<T> l) {
		return L.copy(Iterators.filter(l.iterator(), new NotNull<T>()));
	}

	public static <T> Collection<T> withoutNullsLive(final Collection<T> l) {
		return Collections2.filter(l, new NotNull<T>());
	}

	// CHECKSTYLE:OFF best this way
	/**
	 * @param intNumb
	 * @return i bytes most significant first
	 */
	public static byte[] toByteArray(final int intNumb) {
		return new byte[] { (byte) (intNumb >>> 3 * BITS_PER_BYTE), (byte) (intNumb >>> 2 * BITS_PER_BYTE),
				(byte) (intNumb >>> BITS_PER_BYTE), (byte) intNumb };
	}

	/**
	 * @param longNum
	 * @return 8 bytes most significant first
	 */
	public static byte[] toByteArray(final long longNum) {
		return new byte[] { (byte) (longNum >>> 7 * BITS_PER_BYTE), (byte) (longNum >>> 6 * BITS_PER_BYTE),
				(byte) (longNum >>> 5 * BITS_PER_BYTE), (byte) (longNum >>> 4 * BITS_PER_BYTE),
				(byte) (longNum >>> 3 * BITS_PER_BYTE), (byte) (longNum >>> 2 * BITS_PER_BYTE),
				(byte) (longNum >>> BITS_PER_BYTE), (byte) longNum };
	}

	/**
	 * @param bytes 4 bytes most significant first
	 * @return int
	 */
	public static int intFromByteArray(final byte[] bytes) {
		return (bytes[0] & Elements.BYTE_MASK) << 3 * BITS_PER_BYTE
				| (bytes[1] & Elements.BYTE_MASK) << 2 * BITS_PER_BYTE
				| (bytes[2] & Elements.BYTE_MASK) << BITS_PER_BYTE | bytes[3] & Elements.BYTE_MASK;
	}

	// CHECKSTYLE:ON

	public static <K, V> Map<K, V> toMap(final Collection<K> domain, final Function<K, V> relation) {
		return new AbstractMap<K, V>() {
			private HashSet<Entry<K, V>> es = null;

			@Override
			public boolean containsKey(final Object key) {
				return domain.contains(key);
			}

			@SuppressWarnings("unchecked" /* catch CCE */)
			@Override
			public V get(final Object key) {
				try {
					return relation.apply((K) key);
				} catch (ClassCastException e) {
					return null;
				}
			}

			@Override
			public Set<Map.Entry<K, V>> entrySet() {
				if (this.es == null) {
					this.es = new HashSet<Map.Entry<K, V>>();
					for (final K k : domain) {
						this.es.add(new Map.Entry<K, V>() {
							@Override
							public K getKey() {
								return k;
							}

							@Override
							public V getValue() {
								return relation.apply(k);
							}

							@Override
							public V setValue(final V value) {
								throw Warden.spot(new UnsupportedOperationException("cannot change a function"));
							}
						});
					}
				}
				return this.es;
			}
		};
	}

	/**
	 * @param list to modify
	 * @param max elements to retain
	 * @return the modified List
	 */
	public static <T> List<T> truncate(final List<T> list, final int max) {
		while (list.size() > max) {
			list.remove(list.size() - 1);
		}
		return list;
	}

	/**
	 * Any index will be reduced to theavilable range
	 *
	 * @param list any List
	 * @param startPos within that list (first element included), any values allowed, even negative
	 * @param endPos within that list (first element no included), any values allowed, even negative
	 * @return an unmodifiable sub list of that range, empty if start>=size or start>=end or end < 0
	 */
	@Nonnull
	public static <T> List<? extends T> safeSublist(@Nonnull final List<T> list, final int startPos, final int endPos) {
		int l = list.size();
		int end = Math.min(endPos, l);
		int start = Math.min(startPos, l);
		if (end < 0) {
			end = 0;
		}
		if (start < 0) {
			start=0;
		}
		if (start >= end) {
			return L.e();
		}
		if (start == 0 && end == l) {
			return list;
		}
		return list.subList(start, end);
	}

	public static <T, S extends T> List<T> interleave(final List<? extends Collection<S>> lists) {
		@SuppressWarnings("unchecked"/* only read */)
		Iterator<S>[] iterators = new Iterator[lists.size()];
		int s = 0;
		int i = 0;
		for (Collection<S> c : lists) {
			s += c.size();
			iterators[i++] = c.iterator();
		}
		List<T> results = L.n(s);
		boolean any;
		do {
			any = false;
			for (Iterator<S> it : iterators) {
				if (it.hasNext()) {
					results.add(it.next());
					any = true;
				}
			}
		} while (any);
		return results;
	}

	public static boolean contains(final int value, final int[] values) {
		for (int avalue : values) {
			if (value == avalue) {
				return true;
			}
		}
		return false;
	}

	public static boolean contains(final long value, final long[] values) {
		for (long avalue : values) {
			if (value == avalue) {
				return true;
			}
		}
		return false;
	}

	public static boolean contains(final byte value, final byte[] values) {
		for (byte avalue : values) {
			if (value == avalue) {
				return true;
			}
		}
		return false;
	}

	public static <T> T nth(final int index, final Collection<T> collection) {
		if (collection instanceof List) {
			return ((List<T>) collection).get(index);
		}
		Iterator<T> it = collection.iterator();
		T last = null;
		for (int i = 0; i <= index; i++) {
			last = it.next();
		}
		return last;
	}

}
