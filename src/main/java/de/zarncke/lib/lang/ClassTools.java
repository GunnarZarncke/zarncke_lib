package de.zarncke.lib.lang;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.err.CantHappenException;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.util.Chars;
import de.zarncke.lib.util.Constants;

/**
 * helper class with static methods based on the java reflection api.
 */
public final class ClassTools {
	private ClassTools() { // prevent instances
	}

	/**
	 * This class is a representant for "null".
	 * Its class is used as the type of "null".
	 * It is used by {@link #getBestMethod}.
	 */
	public static final class Any {
		private Any() {
			//
		}
	}

	/**
	 * represents the type of "null".
	 */
	public static final Class<?> ANY_CLASS = Any.class;

	public static List<Class<?>> getAllImplementedInterfaces(final Class<?> clazz) {
		return getAllImplementedInterfaces(clazz, true);
	}

	/**
	 * Determines all interfaces and super classes implemented by the given class.
	 * The results are returned in order of specificity, i.e.
	 * <ol>
	 * <li>the class itself comes first</li>
	 * <li>if superClassesFirst=true then super classes before interfaces, otherwise vice versa.</li>
	 * <li>directly inherited/implemented classes before indirect ones</li>
	 * <li>otherwise the order in the source</li>
	 * <li>Object.class is always added last</li>
	 * </ol>
	 * I.e. the class hierarchy is traversed breadth first (except for Object).
	 *
	 * @param clazz != null
	 * @param superClassesFirst
	 * @return List of {@link Class}
	 */
	public static List<Class<?>> getAllImplementedInterfaces(final Class<?> clazz, final boolean superClassesFirst) {
		List<Class<?>> res;
		if (Object.class.equals(clazz)) {
			res = new LinkedList<Class<?>>();
		} else {
			res = new ArrayList<Class<?>>();
			Set<Class<?>> seenClasses = new HashSet<Class<?>>();
			seenClasses.add(Object.class);
			Deque<Class<?>> candidates = new ArrayDeque<Class<?>>();
			candidates.add(clazz);

			while (!candidates.isEmpty()) {
				Class<?> cand = candidates.removeFirst();
				if (seenClasses.contains(cand)) {
					continue;
				}
				seenClasses.add(cand);
				res.add(cand);

				Class<?> sup = cand.getSuperclass();
				if (superClassesFirst) {
					if (sup != null) {
						candidates.addLast(sup);
					}
				}
				for (Class<?> interf : cand.getInterfaces()) {
					candidates.addLast(interf);
				}
				if (!superClassesFirst) {
					if (sup != null) {
						candidates.addLast(sup);
					}
				}
			}
		}
		res.add(Object.class);
		return res;
	}

	/**
	 * Tries to clone an object by calling its (public) clone() method.
	 * This will work only if the Object made clone() public.
	 *
	 * @param obj any Object, not null
	 * @return the copy of the Object
	 * @exception IllegalArgumentException if the Object has no public clone(),
	 * the clone() threw CloneNotSupportedException
	 */
	public static Object clone(final Object obj) {
		Method m = null;
		try {
			m = obj.getClass().getMethod("clone", Constants.NO_CLASSES);
			return m.invoke(obj, Constants.NO_OBJECTS);
		} catch (NoSuchMethodException nsme) {
			throw new IllegalArgumentException("Object has no public clone()");
		} catch (IllegalAccessException iae) {
			throw new CantHappenException("we querried a public method!" + iae);
		} catch (InvocationTargetException ite) {
			Throwable ex = ite.getTargetException();
			if (ex instanceof CloneNotSupportedException) {
				throw new IllegalArgumentException("Objects clone() threw " + ex);
			}
			if (ex instanceof RuntimeException) {
				throw (RuntimeException) ex;
			}
			if (ex instanceof Error) {
				throw (Error) ex;
			}
			throw new CantHappenException("clone() threw an checked Exception" + " without declaring it!" + ex);
		}
	}

	public static String classNameToFileName(final String className) {
		return Chars.replaceAll(className, ".", "/") + ".class";
	}

	public static String toSignature(final Class<?> cl) {
		return cl.isArray() ? "[" + toSignature(cl.getComponentType()) : !cl.isPrimitive() ? toSignature(cl.getName())
				: cl.equals(Boolean.TYPE) ? "Z" : cl.equals(Byte.TYPE) ? "B" : cl.equals(Short.TYPE) ? "S"
						: cl.equals(Character.TYPE) ? "C" : cl.equals(Integer.TYPE) ? "I" : cl.equals(Long.TYPE) ? "J"
								: cl.equals(Float.TYPE) ? "F" : cl.equals(Double.TYPE) ? "D" : "V";
	}

	public static String toSignature(final String clName) {
		return "L" + Chars.replaceAll(clName, ".", "/") + ";";
	}

	/**
	 * tries to find the best matching method for the given method name
	 * and arguments (considers super types also).
	 * primitives are always considered wrapped
	 * (as is done by reflection automatically).
	 *
	 * @param clazz is the Class where to look for the method
	 * @param name is the method name (currently exact matches)
	 * @param args array of argument types the method must be callable with.
	 * use ANY for null-values.
	 * @return a Method
	 * @exception NoSuchMethodException if no match found
	 */
	public static Method getBestMethod(final Class<?> clazz, final String name, final Class<?>[] args)
			throws NoSuchMethodException {
		return getBestMethod(clazz, name, false, args);
	}

	public static Method getBestMethod(final Class<?> clazz, final String name, final boolean includeStatic,
			final Class<?>[] args) throws NoSuchMethodException {
		// handle special case length = 0 first for performance
		if (args.length == 0) {
			return clazz.getMethod(name, args);
		}

		// replace primitives with wrappers
		Class<?>[] args2 = replacePrimitives(args);

		Method[] ms = getMethods(clazz);

		Method opt = null;
		int dist = Integer.MAX_VALUE;

		for (Method m : ms) {
			int mod = m.getModifiers();
			if (m.getName().equals(name) && (includeStatic || !Modifier.isStatic(mod)) && Modifier.isPublic(mod)) {
				Class<?>[] params = replacePrimitives(m.getParameterTypes());
				if (params.length == args2.length) {
					int sd = 0;
					int j;
					for (j = 0; j < params.length; j++) {
						if (!(args2[j] == ANY_CLASS || params[j].isAssignableFrom(args2[j]))) {
							j = -1;
							break;
						}

						// add up the number of needed mediating classes
						sd += getMediatingClasses(params[j], args2[j]).size();
					}

					if (j != -1 && sd < dist) {
						opt = m;
						dist = sd;
					}
				}
			}
		}

		if (opt == null) {
			throw new NoSuchMethodException("no matching method found.");
		}

		return opt;
	}

	/**
	 * tries to find the best matching costructor of the given class
	 * for the given arguments (considers super types also).
	 * primitives are always considered wrapped
	 * (as is done by reflection automatically).
	 *
	 * @param clazz is the Class where to look for the constructor
	 * @param args array of argument types the method must be called with these
	 * @return a Constructor
	 * @exception NoSuchMethodException if no match found
	 */
	public static Constructor<?> getBestConstructor(final Class<?> clazz, final Class<?>[] args) throws NoSuchMethodException {
		// handle special case length = 0 first for performance
		if (args.length == 0) {
			return clazz.getConstructor(args);
		}

		// replace primitives with wrappers
		Class<?>[] args2 = replacePrimitives(args);

		Constructor<?>[] cs = clazz.getConstructors();

		Constructor<?> opt = null;
		int dist = Integer.MAX_VALUE;

		for (Constructor<?> c : cs) {
			int mod = c.getModifiers();
			if (Modifier.isPublic(mod)) {
				Class<?>[] params = replacePrimitives(c.getParameterTypes());
				if (params.length == args2.length) {
					int sd = 0;
					int j;
					for (j = 0; j < params.length; j++) {
						if (!(args2[j] == ANY_CLASS || params[j].isAssignableFrom(args2[j]))) {
							j = -1;
							break;
						}

						// add up the number of needed mediating classes
						sd += getMediatingClasses(params[j], args2[j]).size();
					}

					if (j != -1 && sd < dist) {
						opt = c;
						dist = sd;
					}
				}
			}
		}

		if (opt == null) {
			throw new NoSuchMethodException("no matching constructor found.");
		}

		return opt;
	}

	/**
	 * replace all primitive classes with their wrapper types in the array.
	 *
	 * @param types to replace with primitives
	 * @return non primitive
	 */
	public static Class<?>[] replacePrimitives(final Class<?>[] types) {
		// replace primitives with wrappers
		Class<?>[] res = new Class<?>[types.length];
		for (int i = 0; i < types.length; i++) {
			if (types[i].isPrimitive()) {
				res[i] = getWrapperType(types[i]);
			} else {
				res[i] = types[i];
			}
		}
		return res;
	}

	/**
	 * Return the corresponding wrapper type for the given primitive type
	 * (for non-primitives, no replacement occurs).
	 *
	 * @param primitive != null
	 * @return non primitive
	 */
	public static Class<?> getWrapperType(final Class<?> primitive) {
		return primitive == Integer.TYPE ? Integer.class : primitive == Boolean.TYPE ? Boolean.class
				: primitive == Character.TYPE ? Character.class : primitive == Byte.TYPE ? Byte.class
						: primitive == Short.TYPE ? Short.class : primitive == Long.TYPE ? Long.class
								: primitive == Double.TYPE ? Double.class : primitive == Float.TYPE ? Float.class : primitive;
	}

	/**
	 * used by getMediatingClasses
	 */
	private static class LinkUp {
		public Class<?> mediator;
		public LinkUp parent;

		public LinkUp(final Class<?> mediator, final LinkUp parent) {
			this.mediator = mediator;
			this.parent = parent;
		}
	}

	/**
	 * Get a List of classes forming a chain of mediating classes between
	 * a given ancestor in the class hierachy and its given descendant.
	 * This method does a potential exponential reverse tree search and thus
	 * may be slow on your class tree.
	 * The first match is returned, not the shortest.
	 * For the descendant ANY the empty list is returned always.
	 *
	 * @param ancestor
	 * @param descendant
	 * @return List of Class starting at the ancestor (exclusive)
	 * and ending at the descendant (inclusive).
	 * The length of the list corresponds to the distance.
	 * @exception IllegalArgumentException is thrown if the descendant is not
	 * an instance of the ancestor or rather
	 * ancestor.isAssignable(descendant) is false
	 * this is implicitly detected and not fail-fast.
	 */
	public static List<Class<?>> getMediatingClasses(final Class<?> ancestor, final Class<?> descendant) {
		// special cases identity and null:
		if (ancestor.equals(descendant) || descendant == ANY_CLASS) {
			return L.e();
		}

		LinkUp chain;
		// special cases: descendant is interface and ancestor is Object
		if (descendant.isInterface() && ancestor.equals(Object.class)) {
			chain = getMediatorsInt(descendant, null);
		} else {
			chain = getMediators(ancestor, descendant, null);
		}
		if (chain == null) {
			throw new IllegalArgumentException("Class " + descendant + " must be a descendant of " + ancestor + "!");
		}

		List<Class<?>> res = new ArrayList<Class<?>>();
		while (chain != null) {
			res.add(chain.mediator);
			chain = chain.parent;
		}
		return res;
	}

	private static LinkUp getMediators(final Class<?> ancestor, final Class<?> mediator, final LinkUp chain) {
		if (ancestor.equals(mediator)) {
			return chain;
		}

		LinkUp up = new LinkUp(mediator, chain);

		Class<?> sup = mediator.getSuperclass();

		if (sup != null) {
			LinkUp res = getMediators(ancestor, sup, up);
			if (res != null) {
				return res;
			}
		}

		Class<?>[] is = mediator.getInterfaces();
		for (Class<?> element : is) {
			LinkUp res = getMediators(ancestor, element, up);
			if (res != null) {
				return res;
			}
		}

		return null;
	}

	/**
	 * climb up interfaces until none left
	 */
	private static LinkUp getMediatorsInt(final Class<?> mediator, final LinkUp chain) {
		LinkUp up = new LinkUp(mediator, chain);

		Class<?>[] is = mediator.getInterfaces();
		if (is.length > 0) {
			return getMediatorsInt(is[0], up);
		}

		// will always succeed if no super interfaces left
		return up;
	}

	/**
	 * get the array containing all fields of the given Class
	 *
	 * @param clazz is the Class
	 * @return array of found Fields (may be empty)
	 */
	public static Field[] getFields(final Class<?> clazz) {
		ArrayList<Field> al = new ArrayList<Field>(Arrays.asList(clazz.getDeclaredFields()));
		Class<?> superClass = clazz.getSuperclass();
		while (superClass != null) {
			al.addAll(Arrays.asList(superClass.getDeclaredFields()));
			superClass = superClass.getSuperclass();
		}
		return al.toArray(new Field[al.size()]);
	}

	/**
	 * get the array containing all methods of the given Class including super classes and interfaces.
	 *
	 * @param clazz is the Class
	 * @return array of found Method (may be empty)
	 */
	public static Method[] getMethods(final Class<?> clazz) {
		Collection<Method> c = new HashSet<Method>();
		Class<?> superClass = clazz;
		do {
			c.addAll(Arrays.asList(superClass.getDeclaredMethods()));

			Class<?>[] is = superClass.getInterfaces();
			for (Class<?> element : is) {
				c.addAll(Arrays.asList(element.getDeclaredMethods()));
			}

			superClass = superClass.getSuperclass();
		} while (superClass != null);

		ArrayList<Method> al = new ArrayList<Method>(c);
		return al.toArray(new Method[al.size()]);
	}

	static class ClassDescription {
		private final Class<?> clazz;
		private final int size;

		public ClassDescription(final Class<?> from) {
			this.clazz = from;
			if (from.isPrimitive()) {
				this.size = sizeOf(from);
				return;
			}
			if (from.isArray()) {
				this.size = 12 + estimateSize(from.getComponentType());
				return;
			}
			int bytes = 0;
			int fields = 0;
			for (Field f : getFields(from)) {
				Class<?> dc = f.getType();
				if (dc.isPrimitive()) {
					bytes += sizeOf(dc);
				} else {
					fields++;
				}
			}
			this.size = bytes + fields * 4 + CLASS_BASE_SIZE;
		}

		public int getEstimatedSize() {
			return this.size;
		}
	}

	private static int sizeOf(final Class<?> from) {
		if (from == Boolean.TYPE) {
			return 1;
		}
		if (from == Integer.TYPE) {
			return 4;
		}
		if (from == Byte.TYPE) {
			return 1;
		}
		if (from == Short.TYPE) {
			return 2;
		}
		if (from == Character.TYPE) {
			return 2;
		}
		if (from == Long.TYPE) {
			return 8;
		}
		if (from == Float.TYPE) {
			return 4;
		}
		if (from == Double.TYPE) {
			return 8;
		}
		return 0;
	}

	private static final LoadingCache<Class<?>, ClassDescription> clazzToDescription = CacheBuilder.newBuilder().weakKeys().softValues().build(
			new CacheLoader<Class<?>, ClassDescription>() {
				@Override
				public ClassDescription load(final Class<?> key) throws Exception {
					return new ClassDescription(key);
				}
			});
	public static final int CLASS_BASE_SIZE = 8;

	public static int estimateSize(final Class<?> clazz) {
		try {
			return clazzToDescription.get(clazz).getEstimatedSize();
		} catch (ExecutionException e) {
			throw Warden.spot(new IllegalArgumentException("cannot estimate class size " + clazz, e));
		}
	}
}
