package de.zarncke.lib.coll;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import com.google.common.collect.Ordering;

import de.zarncke.lib.err.GuardedTest;

public class ElementsTest extends GuardedTest {

	@Test
	public void testMerge() {
		testMerge(false);
		testMerge(true);
	}

	public void testMerge(final boolean unique) {
		List<String> empty = L.<String> l();
		List<String> a = L.l("a");
		List<String> aabb = L.l("a", "a", "b", "b");
		List<String> ab = L.l("a", "b");
		List<String> ac = L.l("a", "c");
		List<String> abc = L.l("a", "b", "c");
		List<String> abcde = L.l("a", "b", "c", "d", "e");
		List<String> b = L.l("b");
		List<String> c = L.l("c");
		List<String> de = L.l("d", "e");

		assertEquals(empty, Elements.merge(empty, empty, Ordering.<String> natural(), unique));
		assertEquals(a, Elements.merge(a, empty, Ordering.<String> natural(), unique));
		assertEquals(a, Elements.merge(empty, a, Ordering.<String> natural(), unique));
		assertEquals(abc, Elements.merge(abc, empty, Ordering.<String> natural(), unique));
		assertEquals(abc, Elements.merge(empty, abc, Ordering.<String> natural(), unique));

		assertEquals(ab, Elements.merge(a, b, Ordering.<String> natural(), unique));
		assertEquals(abc, Elements.merge(ac, b, Ordering.<String> natural(), unique));
		assertEquals(abc, Elements.merge(b, ac, Ordering.<String> natural(), unique));
		assertEquals(abc, Elements.merge(ab, c, Ordering.<String> natural(), unique));
		assertEquals(abcde, Elements.merge(abc, de, Ordering.<String> natural(), unique));
		assertEquals(abcde, Elements.merge(de, abc, Ordering.<String> natural(), unique));

		if (unique) {
			assertEquals(a, Elements.merge(a, a, Ordering.<String> natural(), true));
			assertEquals(ab, Elements.merge(a, ab, Ordering.<String> natural(), true));
			assertEquals(ab, Elements.merge(ab, ab, Ordering.<String> natural(), true));
			assertEquals(aabb, Elements.merge(aabb, ab, Ordering.<String> natural(), true));
		} else {
			assertEquals(aabb, Elements.merge(ab, ab, Ordering.<String> natural(), false));
		}
	}

	@Test
	public void testCompare() {
		List<String> empty = L.<String> l();
		List<String> nul = L.<String> l((String) null);
		List<String> nbc = L.<String> l(null, "b", "c");
		List<String> ab0 = L.l("a", "b", null);
		List<String> abc = L.l("a", "b", "c");
		List<String> abcde = L.l("a", "b", "c", "d", "e");
		List<String> acb = L.l("a", "c", "b");
		List<String> b = L.l("b");

		List<List<String>> all = L.l(empty, nul, nbc, ab0, abc, abcde, acb, b);
		for (int i = 0; i < all.size(); i++) {
			for (int j = 0; j < all.size(); j++) {
				List<String> ie = all.get(i);
				List<String> je = all.get(j);
				if (i == j) {
					Assert.assertTrue(Elements.compare(ie, je) == 0);
				} else if (i < j) {
					Assert.assertTrue(Elements.compare(ie, je) < 0);
					Assert.assertTrue(Elements.compare(je, ie) > 0);
				} else {
					Assert.assertTrue(Elements.compare(ie, je) > 0);
					Assert.assertTrue(Elements.compare(je, ie) < 0);
				}
			}
		}
	}

	@Test
	public void testFlatten() {
		List<String> abc = L.l("a", "b", "c");
		List<? extends Collection<String>> l = L.l(abc, L.<String> l(), L.l("d", "e")); // NOPMD
		List<String> l2 = L.l("a", "b", "c", "d", "e");
		Assert.assertTrue(l2 + "!=" + l, Elements.contentEquals(l2, Elements.flatten(l)));// NOPMD
	}

	@Test
	public void testCheckedIterator() {
		Integer i13 = Integer.valueOf(13);// NOPMD anyway
		List<Object> l = L.l(new Object(), null, "a", i13, "b", Long.valueOf(100)); // NOPMD anyway

		Collection<Boolean> c0 = new ArrayList<Boolean>();
		Elements.dumpIteratorInto(Elements.checkedIterator(l, Boolean.class), c0);
		Elements.contentEquals(L.l(), c0);

		Collection<String> cs = new ArrayList<String>();
		Elements.dumpIteratorInto(Elements.checkedIterator(l, String.class), cs);
		Elements.contentEquals(L.l("a", "b"), cs);

		Collection<Object> c = new ArrayList<Object>();
		Elements.dumpIteratorInto(Elements.checkedIterator(l, Object.class), c);
		Assert.assertEquals(5, c.size()); // NOPMD 5 results

		Collection<Integer> ci = new ArrayList<Integer>();
		Elements.dumpIteratorInto(Elements.checkedIterator(l, Integer.class), ci);
		Elements.contentEquals(L.l(i13), ci);
	}

	@Test
	public void testIntToArray() {
		testIntToArray(0);
		for (int i = 0; i < 32; i++) {
			testIntToArray(1 << i);
		}
		testIntToArray(-1);
		testIntToArray(-127);
		testIntToArray(-128);
		testIntToArray(-256);
		testIntToArray(Integer.MAX_VALUE);
		testIntToArray(Integer.MIN_VALUE);
	}

	private void testIntToArray(final int t) {
		assertEquals(t + "=" + Elements.toString(Elements.toByteArray(t)), t,
				Elements.intFromByteArray(Elements.toByteArray(t)));
	}

	@Test
	public void testSafeSublist() {
		assertEquals(L.l(), Elements.safeSublist(L.e(), 0, 0));
		assertEquals(L.l(), Elements.safeSublist(L.e(), -1, 0));
		assertEquals(L.l(), Elements.safeSublist(L.e(), 0, 1));
		assertEquals(L.l(), Elements.safeSublist(L.e(), -1, 1));
		assertEquals(L.l(), Elements.safeSublist(L.e(), 0, -1));
		assertEquals(L.l(), Elements.safeSublist(L.e(), 1, -1));

		List<String> l123 = L.l("1", "2", "3");
		assertEquals(l123, Elements.safeSublist(l123, 0, 3));
		assertEquals(l123, Elements.safeSublist(l123, -1, 3));
		assertEquals(l123, Elements.safeSublist(l123, -1, 4));
		assertEquals(l123, Elements.safeSublist(l123, 0, 4));

		assertEquals(L.l("1"), Elements.safeSublist(l123, 0, 1));
		assertEquals(L.l("1"), Elements.safeSublist(l123, -1, 1));

		assertEquals(L.l("2"), Elements.safeSublist(l123, 1, 2));

		assertEquals(L.l("3"), Elements.safeSublist(l123, 2, 3));
		assertEquals(L.l("3"), Elements.safeSublist(l123, 2, 4));

	}

}
