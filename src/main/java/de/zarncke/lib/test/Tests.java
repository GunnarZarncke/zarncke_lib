package de.zarncke.lib.test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.joda.time.DateTime;
import org.junit.Assume;
import org.junit.experimental.categories.Category;

public class Tests {

	public static void assumeTimeIsAfter(final int year, final int month, final int day) {
		Assume.assumeTrue(new DateTime().isAfter(new DateTime(year, month, day, 0, 0, 0, 0)));
	}

	public static void assertContentEquals(final List<?> a, final List<?> b) {
		Assert.assertEquals(new ArrayList<Object>(a), new ArrayList<Object>(b));
	}

	public static void assertContentEquals(final Set<?> a, final Set<?> b) {
		Assert.assertEquals(new HashSet<Object>(a), new HashSet<Object>(b));
	}

	public static void assertContentEquals(final Collection<?> a, final Collection<?> b) {
		Assert.assertTrue("a containsAll b", a.containsAll(b));
		Assert.assertTrue("b containsAll a", b.containsAll(a));
	}

	public static void addTestsFromTestCase(final TestSuite suite, final Class<?> theClass,
			final Class<?>... categories) {
		addTestsFromTestCase(suite, true, theClass, categories);

	}

	public static void addTestsFromTestCase(final TestSuite suite, final boolean onlyClassesImplementingTest,
			final Class<?> theClass, final Class<?>... categories) {
		try {
			TestSuite.getTestConstructor(theClass); // Avoid generating multiple error messages
		} catch (NoSuchMethodException e) {
			suite.addTest(TestSuite.warning("Class " + theClass.getName()
					+ " has no public constructor TestCase(String name) or TestCase()"));
			return;
		}

		if (!Modifier.isPublic(theClass.getModifiers())) {
			suite.addTest(TestSuite.warning("Class " + theClass.getName() + " is not public"));
			return;
		}

		Class<?> superClass = theClass;
		List<String> names = new ArrayList<String>();
		while (onlyClassesImplementingTest ? Test.class.isAssignableFrom(superClass) : superClass != null) {
			for (Method each : superClass.getDeclaredMethods()) {
				if (hasMethodMatchingCategory(each, categories) || hasClassMatchingCategory(superClass, categories)) {
					addTestMethod(suite, each, names, theClass);
				}
			}
			superClass = superClass.getSuperclass();
		}
	}

	public static boolean hasMethodMatchingCategory(final Method testMethod, final Class<?>[] categories) {
		Category category = testMethod.getAnnotation(Category.class);
		return isCategoryMatching(category, categories);
	}

	public static boolean hasClassMatchingCategory(final Class<?> testClass, final Class<?>[] categories) {
		Category category = testClass.getAnnotation(Category.class);
		return isCategoryMatching(category, categories);
	}

	private static boolean isCategoryMatching(final Category category, final Class<?>[] categories) {
		boolean ok;
		if (categories.length == 0) {
			ok = category == null;
		} else {
			ok = false;
			if (category != null) {
				findCategory: for (Class<?> requiredCategory : categories) {
					for (Class<?> annotatedCategory : category.value()) {
						if (requiredCategory.isAssignableFrom(annotatedCategory)) {
							ok = true;
							break findCategory;
						}
					}
				}
			}
		}
		return ok;
	}

	private static void addTestMethod(final TestSuite suite, final Method m, final List<String> names,
			final Class<?> theClass) {
		String name = m.getName();
		if (names.contains(name)) {
			return;
		}
		if (!isPublicTestMethod(m)) {
			if (isTestMethod(m)) {
				suite.addTest(TestSuite.warning("Test method isn't public: " + m.getName() + "("
						+ theClass.getCanonicalName() + ")"));
			}
			return;
		}
		names.add(name);
		suite.addTest(TestSuite.createTest(theClass, name));
	}

	private static boolean isPublicTestMethod(final Method m) {
		return isTestMethod(m) && Modifier.isPublic(m.getModifiers());
	}

	private static boolean isTestMethod(final Method m) {
		return m.getParameterTypes().length == 0 && m.getName().startsWith("test")
				&& m.getReturnType().equals(Void.TYPE);
	}

}
