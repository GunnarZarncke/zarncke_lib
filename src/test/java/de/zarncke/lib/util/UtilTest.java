package de.zarncke.lib.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import junit.framework.TestSuite;

import de.zarncke.lib.coll.Elements;
import de.zarncke.lib.coll.L;
import de.zarncke.lib.coll.OneElementCollection;
import de.zarncke.lib.err.GuardedTest;
import de.zarncke.lib.region.Region;
import de.zarncke.lib.region.RegionUtil;
import de.zarncke.lib.region.URLData;
import de.zarncke.lib.test.Tests;

/**
 * Tests classes of util package.
 */
public class UtilTest extends GuardedTest {
	public void testAvg() {
		int s3w6 = 0;
		int s3w6ab = 0;
		for (int i = 1; i <= 6; i++) {
			for (int j = 1; j <= 6; j++) {
				for (int k = 1; k <= 6; k++) {
					int p3w6 = i + j + k;
					int p3w6ab = p3w6 / 2;

					s3w6 += p3w6;
					s3w6ab += p3w6ab;
				}
			}
		}

		assertEquals((int) (10.5 * 216), s3w6);
		assertEquals(5 * 216, s3w6ab);
	}

	public void testElements() {

		// setup some elements and lists
		Object o = new Object();
		Object i0 = Integer.valueOf(0);
		Object i1 = Integer.valueOf(1);
		Object i2 = Integer.valueOf(2);
		Collection<Object> c1 = new OneElementCollection<Object>(o);
		Collection<Object> c3 = Arrays.asList(new Object[] { i0, i1, i2 });
		Collection<Object> c4 = Arrays.asList(new Object[] { Integer.valueOf(-3), Integer.valueOf(2),
				Integer.valueOf(7),
				Integer.valueOf(12) });

		// TEST arrayequals
		assertTrue("empty arrays must be equal", Elements.arrayequals(new Object[0], new Object[0]));
		assertTrue("empty arrays must be equal", Elements.arrayequals(new Number[0], new String[0]));
		assertTrue("empty arrays must be equal", Elements.arrayequals(new byte[0], new byte[0]));
		assertTrue("zero arrays must be equal", Elements.arrayequals(new int[1], new int[1]));
		assertTrue("zero arrays must be equal", Elements.arrayequals(new short[100], new short[100]));
		Object[] a3 = new Object[] { i0, i1, i2 };
		Object[] a3b = new Number[] { Integer.valueOf(0), Integer.valueOf(1), Integer.valueOf(2) };
		assertTrue("same Object arrays must be equal", Elements.arrayequals(a3, a3));
		assertTrue("arrays with same Objects must be equal", Elements.arrayequals(a3, a3b));

		// TEST OneElementCollection
		assertEquals(1, c1.size());
		assertTrue("OneElementCollection must contain its element", new OneElementCollection<Object>(o).contains(o));
		Iterator<Object> c1i = c1.iterator();
		assertEquals(o, c1i.next());
		assertTrue("OneElementCollection must contain one element", !c1i.hasNext());

		// TEST intList
		assertEquals(Collections.EMPTY_LIST, Elements.intList(9, 9, 1));
		assertEquals(Collections.EMPTY_LIST, Elements.intList(9, 9, 3));
		assertEquals(L.copy(new OneElementCollection<Object>(i1)), L.copy(Elements.intList(1, 2, 1)));
		assertEquals(c3, Elements.intList(0, 3, 1));
		assertEquals(c4, Elements.intList(-3, 14, 5));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void testElementsHierarchy() {
		{
			Map m = new HashMap();
			m.put(L.l("c", "d", "e"), "r");
			m.put(L.l("c", "d", "f"), "s");
			Map hef = new HashMap();
			hef.put("e", "r");
			hef.put("f", "s");
			Map h = new HashMap();
			h.put("c", Collections.singletonMap("d", hef));
			assertEquals(h, Elements.hierarchify(m));

			assertEquals(m, Elements.flattenHierarchy(h));
		}

		{
			Map m = new HashMap();
			m.put(L.l("a"), "x");
			m.put(L.l("a", "b"), "y");
			m.put(L.l("a", "c"), "z");
			m.put(L.l("b", "c"), "p");
			m.put(L.l("c", "d", "e"), "r");
			m.put(L.l("c", "d", "f"), "s");

			Map ha = new HashMap();
			ha.put(null, "x");
			ha.put("b", "y");
			ha.put("c", "z");
			Map hef = new HashMap();
			hef.put("e", "r");
			hef.put("f", "s");
			Map h = new HashMap();
			h.put("a", ha);
			h.put("b", Collections.singletonMap("c", "p"));
			h.put("c", Collections.singletonMap("d", hef));
			assertEquals(h, Elements.hierarchify(m));

			assertEquals(m, Elements.flattenHierarchy(h));
		}
	}

	public void testElementsCollections() {
		Object o = new Object();
		Object i0 = Integer.valueOf(0);
		Object i1 = Integer.valueOf(1);
		Object i2 = Integer.valueOf(2);
		new OneElementCollection<Object>(o);
		Collection<Object> c3 = Arrays.asList(new Object[] { i0, i1, i2 });
		Collection<Object> c4 = Arrays.asList(new Object[] { Integer.valueOf(-3), Integer.valueOf(2), Integer.valueOf(7),
				Integer.valueOf(12) });
		// TEST flatten
		assertEquals(Collections.EMPTY_LIST, Elements.flattenRecursively(null));
		Tests.assertContentEquals(new OneElementCollection<Object>(o), Elements.flattenRecursively(o));
		Tests.assertContentEquals(new OneElementCollection<Object>(o),
				Elements.flattenRecursively(new OneElementCollection<Object>(o)));
		Tests.assertContentEquals(new OneElementCollection<Object>(o),
				Elements.flattenRecursively(new OneElementCollection<Object>(new OneElementCollection<Object>(o))));
		assertEquals(
				Arrays.asList(new Object[] { "Hallo", i0, i1, i2, o, Integer.valueOf(-3), Integer.valueOf(2),
						Integer.valueOf(7), Integer.valueOf(12) }),
				Elements.flattenRecursively(Arrays.asList(new Object[] { "Hallo", c3, o, c4 })));

		// TEST mergeGeneral
		assertEquals(null, Elements.mergeGeneral(null, null));
		assertEquals(o, Elements.mergeGeneral(null, o));
		assertEquals(o, Elements.mergeGeneral(o, null));
		assertEquals(c3, Elements.mergeGeneral(null, c3));
		assertEquals(c3, Elements.mergeGeneral(c3, null));
		Collection<Object> c12 = Arrays.asList(new Object[] { i1, i2 });
		assertEquals(c12, Elements.mergeGeneral(i1, i2));
		assertEquals(c3, Elements.mergeGeneral(i0, c12));

		// TEST mergeMaps
		Map<Object, Object> m1 = new HashMap<Object, Object>();
		m1.put("i1", i1);
		Map<Object, Object> m2 = new HashMap<Object, Object>();
		m2.put("i2", i2);
		Map<Object, Object> m3 = new HashMap<Object, Object>(m1);
		m3.putAll(m2);
		Map<Object, Object> m4 = new HashMap<Object, Object>(m1);
		m4.put("i2", Arrays.asList(new Object[] { i2, i2 }));

		assertEquals(m2, Elements.mergeMaps(m2, null));
		assertEquals(m3, Elements.mergeMaps(null, m3));

		assertEquals(m3, Elements.mergeMaps(m1, m2));
		assertEquals(m4, Elements.mergeMaps(m2, m3));

		// todo: add more test cases
	}

	public void testHasData() throws Exception {
		byte[] ba = new byte[] { 2, 3, 5, 7, 11, 13, 17 };

		Region d = RegionUtil.asRegion(ba);
		assertTrue("data read from " + d + " must be as prepared", Elements.arrayequals(ba, d.toByteArray()));

		File f = File.createTempFile("test", ".dat");
		f.deleteOnExit();
		OutputStream os = new FileOutputStream(f);
		os.write(ba);
		os.close();

		d = new URLData(new URL("file://" + f.getAbsolutePath())).asRegion();
		assertTrue("data read from " + d + " must be as prepared", Elements.arrayequals(ba, d.toByteArray()));

	}

	public static junit.framework.Test suite() {
		return new TestSuite(UtilTest.class);
	}

	private static void assertEquals(final Object[] a, final Object[] b) {
		if (!new HashSet<Object>(Arrays.asList(a)).equals(new HashSet<Object>(Arrays.asList(b)))) {
			throw new de.zarncke.lib.err.CantHappenException("expected: " + Arrays.asList(a) + " but was: " + Arrays.asList(b));
		}
	}

	public void testReplacer() {
		Replacer r = Replacer.build().replaceAll("ab", "--").replaceAll("cd", "xx").done();

		assertEquals("", r.apply(""));
		assertEquals("ax", r.apply("ax"));
		assertEquals("--", r.apply("ab"));
		assertEquals("xx", r.apply("cd"));
		assertEquals("--xx", r.apply("abcd"));
		assertEquals("axxc", r.apply("acdc"));
	}

}
