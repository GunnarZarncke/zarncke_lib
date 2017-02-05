package de.zarncke.lib.err;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.ComparisonFailure;

import de.zarncke.lib.coll.Elements;
import de.zarncke.lib.coll.L;
import de.zarncke.lib.lang.ClassTools;
import de.zarncke.lib.util.Chars;

/**
 * Extends Assert with list and other methods.
 * 
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
public class Asserts extends Assert {
	private static final int ASSERT_MSG_MAX_LINE = 200;
	private static final Set<String> EXCLUDED_METHOD_NAMES = L.set("getClass", "wait", "notify", "notifyAll");

	/**
	 * State the requirement that a test should fail after the given date.
	 * Used like this:
	 * 
	 * <pre>
	 * if (failIfAfter(2010, 1, 1))
	 * 	return;
	 * </pre>
	 * 
	 * @param year usual
	 * @param month usual
	 * @param day usual
	 * @return true
	 */
	public static boolean failIfAfter(final int year, final int month, final int day) {
		if (ifAfter(year, month, day)) {
			Assert.fail("The calling test fails because time progressed until it should run " + year + "-" + month + "-" + day);
		}
		return true;
	}

	/**
	 * @param year usual
	 * @param month usual
	 * @param day usual
	 * @return true if currently after the given date
	 */
	public static boolean ifAfter(final int year, final int month, final int day) {
		return new DateTime().isAfter(new DateTime(year, month, day, 0, 0, 0, 0));
	}

	public static void assertContentEquals(final List<?> a, final List<?> b) {
		if (!Elements.contentEquals(a, b)) {
			failContent(a, b);
		}
	}

	public static void assertContentNestedEquals(final List<?> a, final List<?> b) {
		if (!Elements.contentNestedEquals(a, b)) {
			failContent(a, b);
		}
	}

	public static void assertContentEquals(final Set<?> a, final Set<?> b) {
		if (!Elements.contentEquals(a, b)) {
			failContent(a, b);
		}
	}

	public static void assertContentEquals(final Collection<?> a, final Collection<?> b) {
		if (!Elements.contentEquals(a, b)) {
			failContent(a, b);
		}
	}

	private static void failContent(final Collection<?> a, final Collection<?> b) {
		assertEquals("not equal content", linewise(a, ""), linewise(b, ""));
		fail("not equal content:\n" + a + "\n" + b);
	}

	private static String linewise(final Collection<?> c, final String prefix) {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (Object o : c) {
			if (o instanceof Collection<?>) {
				sb.append(linewise((Collection<?>) o, prefix + i + " "));
			} else {
				sb.append(prefix).append(Chars.summarize(o.toString(), ASSERT_MSG_MAX_LINE)).append("\n");
			}
			i++;
		}
		return sb.toString();
	}

	public static void assertDistinct(final List<?> l) {
		for (int i = 0; i < l.size(); i++) {
			Object ie = l.get(i);
			for (int j = i + 1; j < l.size(); j++) {
				if (i != j) {
					if (ie.equals(l.get(j))) {
						fail("not all elements are distinct! equal are elements " + i + " and " + j + "(" + ie + "=="
								+ l.get(j) + ")");
					}
				}
			}

		}
	}

	public static void assertEmpty(final Collection<?> list) {
		if (!list.isEmpty()) {
			fail("list should be empty: " + list);
		}
	}

	public static void assertStartsWith(final String expectedPrefix, final String actualResult) {
		if (!actualResult.startsWith(expectedPrefix)) {
			throw new ComparisonFailure("the prefix is not as expected", expectedPrefix, actualResult);
		}
	}

	public static void assertContains(final String expectedPart, final String actualResult) {
		assertContains("the part is not contained", expectedPart, actualResult);
	}

	public static void assertContains(final String msg, final String expectedPart, final String actualResult) {
		if (!actualResult.contains(expectedPart)) {
			throw new ComparisonFailure(msg, expectedPart, actualResult);
		}
	}

	/**
	 * Tool to allow 100% coverage of helpers where the private constructor cannot be called any other way.
	 * 
	 * @param clazz to "test" private constructor of
	 */
	public static void coverPrivateConstructorOf(final Class<?> clazz) {
		Constructor<?>[] cons = clazz.getDeclaredConstructors();
		cons[0].setAccessible(true);
		try {
			cons[0].newInstance((Object[]) null);
		} catch (Exception e) {
			throw new CantHappenException("covering private constructor by reflection failed (permissions?)", e);
		}
	}

	/**
	 * Tool to fake coverage of method which must be present but don't need to be tested fully.
	 * Methods are called with null arguments
	 * 
	 * @param object to call all public methods of
	 * @param exceptionAreOk true: if any public method throws an exception it's ok, false: it's an error
	 */
	public static void coverPublicMethods(final Object object, final boolean exceptionAreOk) {
		assertNotNull(object);
		coverPublicMethods(object, object.getClass(), exceptionAreOk);
	}

	/**
	 * Tool to fake coverage of method which must be present but don't need to be tested fully.
	 * Methods are called with null arguments
	 * 
	 * @param object to call all public methods of
	 * @param testedInterface only methods of this interface or super class are called
	 * @param exceptionAreOk true: if any public method throws an exception it's ok, false: it's an error
	 */
	public static void coverPublicMethods(final Object object, final Class<?> testedInterface, final boolean exceptionAreOk) {
		Method[] methods = ClassTools.getMethods(testedInterface);
		for (Method m : methods) {
			if (Modifier.isPublic(m.getModifiers()) && !Modifier.isStatic(m.getModifiers()) && !m.isSynthetic()
					&& !EXCLUDED_METHOD_NAMES.contains(m.getName())) {
				Object[] args = new Object[m.getParameterTypes().length];
				int i = 0;
				for (Class<?> type : m.getParameterTypes()) {
					if (!type.isPrimitive()) {
						args[i++] = null;
					} else {
						if (type == Boolean.TYPE) {
							args[i++] = Boolean.FALSE;
						} else if (type == Byte.TYPE) {
							args[i++] = Byte.valueOf((byte) 0);
						} else if (type == Short.TYPE) {
							args[i++] = Short.valueOf((byte) 0);
						} else if (type == Integer.TYPE) {
							args[i++] = Integer.valueOf((byte) 0);
						} else if (type == Long.TYPE) {
							args[i++] = Long.valueOf((byte) 0);
						} else if (type == Float.TYPE) {
							args[i++] = Float.valueOf((byte) 0);
						} else if (type == Double.TYPE) {
							args[i++] = Double.valueOf((byte) 0);
						} else if (type == Character.TYPE) {
							args[i++] = Character.valueOf((char) 0);
						} else {
							args[i++] = null;
						}
					}
				}
				try {
					m.invoke(object, args);
				} catch (IllegalArgumentException e) {
					throw Warden.spot(new CantHappenException("we built the object thus", e));
				} catch (IllegalAccessException e) {
					throw Warden.spot(new IllegalArgumentException("object is not accessible for this test " + m, e));
				} catch (InvocationTargetException e) {
					if (exceptionAreOk) {
						continue;
					}
					fail("failure during call to " + m + " of " + object + ":" + e.getMessage());
				}
			}
		}
	}

	public static void assertInstanceOf(final Class<?> type, final Object obj) {
		assertInstanceOf(obj + " must be instanceof " + type.getName(), type, obj);
	}

	/**
	 * @param msg
	 * @param type
	 * @param obj
	 */
	public static void assertInstanceOf(final String msg, final Class<?> type, final Object obj) {
		if (obj == null) {
			return;
		}
		if (!type.isAssignableFrom(obj.getClass())) {
			fail(msg);
		}
	}

}
