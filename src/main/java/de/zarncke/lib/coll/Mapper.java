package de.zarncke.lib.coll;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import de.zarncke.lib.err.CantHappenException;

/**
 * Map elements of a Collection or Map. <br>
 * example of use:
 *
 * <pre>
 * Collection coll = Arrays.asList(new String[] { &quot;hallo&quot;, &quot;test&quot; });
 * coll = Converter.map(new Converter() {
 * 	public Object map(Object ob) {
 * 		return ob.substring(1);
 * 	}
 * }, coll);
 * System.out.println(coll);
 * </pre>
 *
 * @param <K> source type
 * @param <V> result type
 * @deprecated Use {@link com.google.common.collect.Maps#transformValues(Map, com.google.common.base.Function)}
 */
@Deprecated
public abstract class Mapper<K, V> {
	/**
	 * create Mapper
	 */
	protected Mapper() {
		// only for deriving
	}

	protected void checkCollectionClass(final Class<?> collClass) {
		// must be a Collection
		if (!Collection.class.isAssignableFrom(collClass)) {
			throw new IllegalArgumentException("the class " + collClass + " is not a Collection.");
		}

		// and instantiable
		try {
			collClass.newInstance();
		} catch (Exception ex) {
			throw new IllegalArgumentException("the class " + collClass + " cannot be instantiated.");
		}
	}

	/**
	 * the conversion method defined by implementing classes.
	 *
	 * @param elem is the Object to test for inclusion
	 * @return the converted/new Object to be added to the destination
	 */
	public abstract V map(K elem);

	/**
	 * convert all elements of the source collection
	 * returning them in the default collection
	 * should not be used, if uncertain if Collection can be instantiated,
	 * use filterConvert then.
	 *
	 * @param <K> key type
	 * @param <V> value type
	 * @param mapper is the Mapper to apply to the elements
	 * @param src Collection to convert
	 * @return a new Collection of the same class as the given Collection
	 * @exception IllegalArgumentException is thrown if no new instance
	 * of the source Collection class can be created.
	 */
	@SuppressWarnings("unchecked")
	public static <K, V> Collection<V> map(final Mapper<K, V> mapper, final Collection<K> src) {
		Collection<V> dest;
		try {
			dest = src.getClass().newInstance();
		} catch (Exception ie) {
			throw new IllegalArgumentException("we cannot create a new instance of the collection " + src.getClass());
		}
		return mapTo(mapper, src, dest);
	}

	/**
	 * convert all elements of the source collection.
	 *
	 * @param <K> key type
	 * @param <V> value type
	 * @param mapper is the Mapper to apply to the elements
	 * @param src Collection to filter
	 * @param dest Class of result
	 * @return a new Collection of the given Class
	 */
	@SuppressWarnings("unchecked")
	public static <K, V> Collection<V> mapConvert(final Mapper<K, V> mapper, final Collection<K> src, final Class<?> dest) {
		Collection<V> d;
		try {
			d = (Collection<V>) dest.newInstance();
		} catch (Exception ie) {
			throw new CantHappenException("may not occur, we tested against it!", ie);
		}
		return mapTo(mapper, src, d);
	}

	/**
	 * convert all elements of the source collection.
	 * copy (add) them to the given destination collection.
	 *
	 * @param <K> key type
	 * @param <V> value type
	 * @param mapper is the Mapper to apply to the elements
	 * @param src Collection to filter
	 * @param dest Collection to add to
	 * @return the given destination Collection
	 */
	public static <K, V> Collection<V> mapTo(final Mapper<K, V> mapper, final Collection<K> src, final Collection<V> dest) {
		for (K ob : src) {
			dest.add(mapper.map(ob));
		}
		return dest;
	}

	/**
	 * map all elements of the source collection
	 * to their converted value.
	 *
	 * @param <K> key type
	 * @param <V> value type
	 * @param mapper is the Mapper to apply to the elements
	 * @param src Collection to map
	 * @return the resulting Map
	 */
	public static <K, V> Map<K, V> toMap(final Mapper<K, V> mapper, final Collection<K> src) {
		Map<K, V> dest = new HashMap<K, V>();
		for (K ob : src) {
			dest.put(ob, mapper.map(ob));
		}
		return dest;
	}

	/**
	 * convert all values of the source Map
	 * returning them in the default Map
	 * should not be used, if uncertain if Collection can be instantiated,
	 * use filterConvert then.
	 *
	 * @param <K> key type
	 * @param <V> value type
	 * @param <D> target value type
	 * @param mapper is the Mapper to apply to the elements
	 * @param src Collection to convert
	 * @return a new Collection of the same class as the given Collection
	 * @exception IllegalArgumentException is thrown if no new instance
	 * of the source Collection class can be created.
	 */
	@SuppressWarnings("unchecked")
	public static <K, V, D> Map<K, D> map(final Mapper<V, D> mapper, final Map<K, V> src) {
		Map<K, D> dest;
		try {
			dest = src.getClass().newInstance();
		} catch (Exception ie) {
			throw new IllegalArgumentException("we cannot create a new instance of the map " + src.getClass());
		}
		return mapTo(mapper, src, dest);
	}

	/**
	 * convert all values of the source map.
	 *
	 * @param <K> key type
	 * @param <V> value type
	 * @param <D> target value type
	 * @param mapper is the Mapper to apply to the elements
	 * @param src Collection to filter
	 * @param dest Class of result
	 * @return a new Collection of the given Class
	 */
	@SuppressWarnings("unchecked")
	public static <K, V, D> Map<K, D> mapConvert(final Mapper<V, D> mapper, final Map<K, V> src, final Class<?> dest) {
		Map<K, D> d;
		try {
			d = (Map<K, D>) dest.newInstance();
		} catch (Exception ie) {
			throw new IllegalArgumentException("we cannot create a new instance of the map " + dest);
		}
		return mapTo(mapper, src, d);
	}

	/**
	 * convert all elements of the source collection.
	 * copy (add) them to the given destination collection.
	 *
	 * @param <K> key type
	 * @param <V> value type
	 * @param <D> target value type
	 * @param mapper is the Mapper to apply to the elements
	 * @param src Collection to filter
	 * @param dest Collection to add to
	 * @return the given destination Collection
	 */
	public static <K, V, D> Map<K, D> mapTo(final Mapper<V, D> mapper, final Map<K, V> src, final Map<K, D> dest) {
		for (Map.Entry<K, V> me : src.entrySet()) {
			dest.put(me.getKey(), mapper.map(me.getValue()));
		}
		return dest;
	}

	/**
	 * Returns a String that represents the value of this object.
	 *
	 * @return a string representation of the receiver
	 */
	@Override
	public String toString() {
		return getClass().getName();
	}

}
