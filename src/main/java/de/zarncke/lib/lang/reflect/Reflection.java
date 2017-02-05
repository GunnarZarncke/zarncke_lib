package de.zarncke.lib.lang.reflect;


/**
 * Misc helper functions using reflection.
 *
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
public class Reflection {
	private Reflection() {
		// hidden constructor of helper class
	}

	// public static <T> Map<Object, T> indexByReflection(final Collection<? extends T> values, final String
	// propertyName) {
	// Map<Object, T> index = new HashMap<Object, T>();
	// for (T object : values) {
	// index.put(new BeanMap(object).get(propertyName), object);
	// }
	// return index;
	// }
	//
	// public static <T> void sortByReflection(final List<T> values, final String propertyName) {
	// Collections.sort(values, new Comparator() {
	// @Override
	// public int compare(final Object o1, final Object o2) {
	// return ((Comparable) new BeanMap(o1).get(propertyName)).compareTo(new BeanMap(o2).get(propertyName));
	// }
	// });
	// }

}
