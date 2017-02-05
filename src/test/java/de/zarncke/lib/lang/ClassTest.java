package de.zarncke.lib.lang;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import de.zarncke.lib.cache.Cache;
import de.zarncke.lib.err.CantHappenException;
import de.zarncke.lib.io.IOTools;
import de.zarncke.lib.lang.gen.ClassBinaryCreator;
import de.zarncke.lib.lang.gen.ClassFactory;
import de.zarncke.lib.lang.gen.ClassSet;
import de.zarncke.lib.lang.gen.DemandClassLoader;
import de.zarncke.lib.struct.PartialOrder;

/**
 * Tests class related classes of util package.
 */
@SuppressWarnings("unused")
public class ClassTest extends TestCase {
	// as for @SuppressWarnings("unused"): we have lots of methods which are accessed by reflection

	public ClassTest() {
		super("ClassTest");
	}

	/*
	 * Test java class hierarchy
	 * <pre>
	 * Object
	 * /|\ \
	 * K | I L
	 * | A'|/
	 * B' J
	 * `C'
	 * </pre>
	 * as structural hierarchy:
	 * <pre>
	 * Object | I/J/K/L
	 * A/A2
	 * B/B2
	 * C
	 * </pre>
	 * to test ClassTools
	 */
	private interface TestL {
		// test dummy
	}

	private interface TestI {
		// test dummy
	}

	private interface TestJ extends TestI, TestL {
		// test dummy
	}

	private interface TestK {
		// test dummy
	}

	private static class TestA implements TestI {
		public int a() {
			return 0;
		}
	}

	private static class TestA2 implements TestI {
		public int a() {
			return 0;
		}
	}

	private static class TestB extends TestA implements TestK {
		public int b() {
			return 0;
		}
	}

	private static class TestB2 extends TestA implements TestK {
		public int b() {
			return 0;
		}
	}

	private static class TestC extends TestB implements TestJ {
		// method for getBestMethod
		private void t(final TestI a, final TestI b) {
			// test dummy
		}

		public static void t(final TestB a, final TestB b) {
			// test dummy
		}

		public void t(final Object a, final Object b) {
			// test dummy
		}

		public void t(final TestA a, final TestB b) {
			// test dummy
		}

		public void t(final TestK a, final TestI b) {
			// test dummy
		}

		public void t(final TestK a, final TestA b) {
			// test dummy
		}

		public void t(final int a, final String b) {
			// test dummy
		}

		public void t(final java.io.Serializable a, final Number b) {
			// test dummy
		}
	}

	public static void testAllInterfaces() throws Exception {
		List<Class<?>> allImplementedClasses = ClassTools.getAllImplementedInterfaces(TestC.class);
		ArrayList<Class<?>> expected = new ArrayList<Class<?>>();
		expected.add(TestC.class);
		expected.add(TestB.class);
		expected.add(TestJ.class);
		expected.add(TestA.class);
		expected.add(TestK.class);
		expected.add(TestI.class);
		expected.add(TestL.class);
		expected.add(Object.class);
		assertEquals(expected.toString(), allImplementedClasses.toString());
	}

	public static void testMediatingClasses() throws Exception {

		// test expected results
		List<Class<?>> caa = ClassTools.getMediatingClasses(TestA.class, TestA.class);
		assertEquals(Collections.EMPTY_LIST, caa);

		List<Class<?>> coa = ClassTools.getMediatingClasses(Object.class, TestA.class);
		assertEquals(Arrays.asList(new Class[] { TestA.class }), coa);

		List<Class<?>> cab = ClassTools.getMediatingClasses(TestA.class, TestB.class);
		assertEquals(Arrays.asList(new Class[] { TestB.class }), cab);

		List<Class<?>> cac = ClassTools.getMediatingClasses(TestA.class, TestC.class);
		assertEquals(Arrays.asList(new Class[] { TestB.class, TestC.class }), cac);

		List<Class<?>> coc = ClassTools.getMediatingClasses(Object.class, TestC.class);
		assertEquals(Arrays.asList(new Class[] { TestA.class, TestB.class, TestC.class }), coc);

		List<Class<?>> ckb = ClassTools.getMediatingClasses(TestK.class, TestB.class);
		assertEquals(Arrays.asList(new Class[] { TestB.class }), ckb);

		List<Class<?>> ckc = ClassTools.getMediatingClasses(TestK.class, TestC.class);
		assertEquals(Arrays.asList(new Class[] { TestB.class, TestC.class }), ckc);

		List<Class<?>> cjc = ClassTools.getMediatingClasses(TestJ.class, TestC.class);
		assertEquals(Arrays.asList(new Class[] { TestC.class }), cjc);

		List<Class<?>> cij = ClassTools.getMediatingClasses(TestI.class, TestJ.class);
		assertEquals(Arrays.asList(new Class[] { TestJ.class }), cij);

		List<Class<?>> cic = ClassTools.getMediatingClasses(TestI.class, TestC.class);
		if (!Arrays.asList(
				new Collection[] { Arrays.asList(new Class[] { TestJ.class, TestC.class }),
						Arrays.asList(new Class[] { TestA.class, TestB.class, TestC.class }), }).contains(coc)) {
			fail("one of the two given inheritance sequences must match!");
		}

		List<Class<?>> coi = ClassTools.getMediatingClasses(Object.class, TestI.class);
		assertEquals(Arrays.asList(new Class[] { TestI.class }), coi);

		List<Class<?>> coj = ClassTools.getMediatingClasses(Object.class, TestJ.class);
		assertEquals(Arrays.asList(new Class[] { TestI.class, TestJ.class }), coj);

		// expected failures
		try {
			ClassTools.getMediatingClasses(TestA.class, TestI.class);
			fail("should have failed: interface inherit from no class.");
		} catch (IllegalArgumentException iae) {
			;
		}

		try {
			ClassTools.getMediatingClasses(TestB.class, String.class);
			fail("should have failed: TestB doesn't inherit from String.");
		} catch (IllegalArgumentException iae) {
			;
		}
	}

	public static void testBestMethods() throws Exception {

		Method mka0 = TestC.class.getMethod("t", new Class[] { TestK.class, TestA.class });
		Method mka = ClassTools.getBestMethod(TestC.class, "t", new Class[] { TestK.class, TestA.class });
		assertEquals(mka0, mka);

		Method mab0 = TestC.class.getMethod("t", new Class[] { TestA.class, TestB.class });
		Method mbb = ClassTools.getBestMethod(TestC.class, "t", new Class[] { TestB.class, TestB.class });
		assertEquals(mab0, mbb);

		Method moo0 = TestC.class.getMethod("t", new Class[] { Object.class, Object.class });
		Method mss = ClassTools.getBestMethod(TestC.class, "t", new Class[] { String.class, String.class });
		assertEquals(moo0, mss);

		Method msn0 = TestC.class.getMethod("t", new Class[] { java.io.Serializable.class, Number.class });
		Method msl = ClassTools.getBestMethod(TestC.class, "t", new Class[] { String.class, Long.TYPE });
		assertEquals(msn0, msl);

	}

	public static void testDemandClassLoader() throws Exception {
		DemandClassLoader dcl = new DemandClassLoader(new File("./tmp"));

		final String simName = "de.zarncke.lib.lang.ClassTest$DemandTest";

		InputStream ins = null;
		ins = ClassTest.class.getResourceAsStream("ClassTest$DemandTest.binary");
		if (ins == null) {
			throw new CantHappenException("ClassTest$DemandTest.binary must be present near ClassTest!");
		}
		final byte[] bytes = IOTools.getAllBytes(ins);

		dcl.registerCreator(new ClassBinaryCreator() {
			@Override
			public byte[] createBinary(final String className) {
				if (className.equals(simName)) {
					return bytes;
				}
				return null;
			}
		});

		Class<?> cl = dcl.loadClass(simName);
		assertEquals(simName, cl.getName());
		assertSame(dcl, cl.getClassLoader());

		IOTools.deleteAll(new File("./tmp"));
	}

	public static void testPartialOrder() {
		/*
		 * Test class hierarchy
		 * Object /|\ \ K | I L | A'|/ B' J `C'
		 * to test ClassTools
		 */
		PartialOrder struc = ClassSet.JAVA_ASSIGNABILITY;
		assertRelation(struc, '>', ClassSet.VOIDCLASS, TestI.class);
		assertRelation(struc, '>', TestA.class, ClassSet.NONECLASS);
		assertRelation(struc, '>', TestI.class, TestJ.class);
		assertRelation(struc, '<', TestJ.class, TestL.class);
		assertRelation(struc, '<', TestA.class, TestI.class);
		assertRelation(struc, '>', TestA.class, TestB.class);
		assertRelation(struc, '>', TestA.class, TestB2.class);
		assertRelation(struc, '<', TestB.class, TestA.class);
		assertRelation(struc, '<', TestB2.class, TestA.class);
		assertRelation(struc, '!', TestB.class, TestA2.class);
		assertRelation(struc, '!', TestL.class, TestB.class);
		assertRelation(struc, '!', TestI.class, TestL.class);

		struc = ClassSet.STRUCTURAL_ASSIGNABILITY;
		// assertRelation(struc, '<', Integer.TYPE, Long.TYPE);
		assertRelation(struc, '>', ClassSet.VOIDCLASS, TestI.class);
		assertRelation(struc, '>', TestA.class, ClassSet.NONECLASS);
		assertRelation(struc, '=', TestI.class, TestJ.class);
		assertRelation(struc, '=', TestJ.class, TestL.class);
		assertRelation(struc, '=', TestA.class, TestA.class);
		assertRelation(struc, '=', TestA.class, TestA2.class);
		assertRelation(struc, '>', TestA.class, TestB.class);
		assertRelation(struc, '>', TestA.class, TestB2.class);
		assertRelation(struc, '<', TestB.class, TestA.class);
		assertRelation(struc, '<', TestB2.class, TestA.class);
		assertRelation(struc, '<', TestB.class, TestA2.class);
		assertRelation(struc, '>', TestL.class, TestB.class);
		assertRelation(struc, '=', TestI.class, TestL.class);
	}

	public static void testClassSet() {
		/*
		 * Test class hierarchy
		 * Object /|\ \ K | I L | A'|/ B' J `C'
		 * to test ClassTools
		 */
		// some set tests
		{
			ClassSet set = new ClassSet();

			assertEquals(new Class[] { ClassSet.VOIDCLASS }, set.getSuperClasses(ClassSet.NONECLASS));
			assertEquals(new Class[] { ClassSet.NONECLASS }, set.getSubClasses(ClassSet.VOIDCLASS));

			set.add(TestA.class);

			assertEquals(new Class[] { ClassSet.VOIDCLASS }, set.getSuperClasses(TestA.class));
			assertEquals(new Class[] { ClassSet.NONECLASS }, set.getSubClasses(TestA.class));

			set.add(TestK.class);
			set.add(TestB.class);

			assertEquals(new Class[] { TestA.class, TestK.class }, set.getSuperClasses(TestB.class));
			assertEquals(new Class[] { ClassSet.NONECLASS }, set.getSubClasses(TestB.class));
			assertEquals(new Class[] { TestB.class }, set.getSubClasses(TestA.class));
			assertEquals(new Class[] { TestB.class }, set.getSubClasses(TestK.class));
		}

		{
			ClassSet set = new ClassSet();
			set.add(TestI.class);
			set.add(TestL.class);
			set.add(TestJ.class);

			assertEquals(new Class[] { TestI.class, TestL.class }, set.getSuperClasses(TestJ.class));
		}

		{
			ClassSet set = new ClassSet();
			set.add(TestL.class);
			set.add(TestB.class);
			set.add(TestC.class);

			assertEquals(new Class[] { ClassSet.VOIDCLASS }, set.getSuperClasses(TestB.class));
			assertEquals(new Class[] { ClassSet.VOIDCLASS }, set.getSuperClasses(TestL.class));
			assertEquals(new Class[] { TestB.class, TestL.class }, set.getSuperClasses(TestC.class));
			assertEquals(new Class[] { TestC.class }, set.getSubClasses(TestL.class));
			assertEquals(new Class[] { TestL.class, TestB.class }, set.getSubClasses(ClassSet.VOIDCLASS));
		}

		{
			ClassSet set = new ClassSet();
			set.add(TestL.class);
			set.add(TestB.class);

			assertEquals(new Class[] { TestB.class, TestL.class }, set.getSuperClasses(ClassSet.NONECLASS));
			assertEquals(new Class[] { TestB.class, TestL.class }, set.getSubClasses(ClassSet.VOIDCLASS));

			assertEquals(new Class[] { ClassSet.VOIDCLASS }, set.getSuperClasses(TestL.class));
			assertEquals(new Class[] { ClassSet.NONECLASS }, set.getSubClasses(TestL.class));
			assertEquals(new Class[] { ClassSet.VOIDCLASS }, set.getSuperClasses(TestB.class));
			assertEquals(new Class[] { ClassSet.NONECLASS }, set.getSubClasses(TestB.class));
		}

		{
			ClassSet set = new ClassSet();
			set.add(TestL.class);
			set.add(TestB.class);
			set.add(TestK.class);

			assertEquals(new Class[] { TestB.class, TestL.class }, set.getSuperClasses(ClassSet.NONECLASS));
			assertEquals(new Class[] { TestK.class, TestL.class }, set.getSubClasses(ClassSet.VOIDCLASS));

			assertEquals(new Class[] { TestB.class }, set.getSubClasses(TestK.class));
		}

		{
			ClassSet set = new ClassSet();
			set.add(TestA.class);
			set.add(TestK.class);
			set.add(TestL.class);
			set.add(TestB.class);
			set.add(TestC.class);

			assertEquals(new Class[] { TestC.class }, set.getSuperClasses(ClassSet.NONECLASS));
			assertEquals(new Class[] { TestA.class, TestK.class, TestL.class }, set.getSubClasses(ClassSet.VOIDCLASS));

			assertEquals(new Class[] { TestC.class }, set.getSubClasses(TestL.class));

			assertEquals(new Class[] { TestB.class, TestL.class }, set.getSuperClasses(TestC.class));
		}

		{
			ClassSet set = new ClassSet();
			set.add(TestA.class);
			set.add(TestK.class);
			set.add(TestB.class);
			set.remove(TestK.class);

			assertEquals(new Class[] { TestA.class }, set.getSuperClasses(TestB.class));
			assertEquals(new Class[] { TestB.class }, set.getSubClasses(TestA.class));
		}

		/*
		 * { ClassSet set = new ClassSet(); set.add(a); set.add(b); set.add(c); set.add(ab); set.add(ac); set.add(abc);
		 * set.add(abx); set.remove(ab);
		 * assertEquals(set.getSubClasses(a), new Class[] { ac, abx }); assertEquals(set.getSubClasses(b), new Class[] {
		 * abc, abx }); assertEquals(set.getSubClasses(c), new Class[] { ac }); assertEquals(set.getSubClasses(ac), new
		 * Class[] { abc }); }
		 * { ClassSet set = new ClassSet(); set.add(a); set.add(b); set.add(c); set.add(ab); set.add(ac); set.add(bc);
		 * assertEquals(set.getSuperClasses(ClassSet.NONECLASS), new Class[] { ab, bc, ac });
		 * assertEquals(set.getSubClasses(Object.class), new Class[] { a, b, c }); }
		 */

		{
			PartialOrder struc = ClassSet.STRUCTURAL_ASSIGNABILITY;

			assertTrue(struc.lessEqual(TestA.class, TestA2.class));
			assertTrue(struc.lessEqual(TestA2.class, TestA.class));
			assertTrue(struc.lessEqual(TestB.class, TestB2.class));
			assertTrue(struc.lessEqual(TestB2.class, TestB.class));

			assertTrue(!TestA.class.isAssignableFrom(TestA2.class));
			assertTrue(!TestA2.class.isAssignableFrom(TestA.class));
			assertTrue(!TestB.class.isAssignableFrom(TestB2.class));
			assertTrue(!TestB2.class.isAssignableFrom(TestB.class));

			ClassSet set = new ClassSet(ClassSet.STRUCTURAL_ASSIGNABILITY);
			set.add(TestA.class);
			set.add(TestA2.class);
			set.add(TestB.class);
			set.add(TestB2.class);

			assertEquals(new Class[] { TestA.class, TestA2.class }, set.getSuperClasses(TestB.class));
			assertEquals(new Class[] { TestB.class, TestB2.class }, set.getSubClasses(TestA.class));
		}
	}

	public static void testClassFactory() throws Exception {
		ClassFactory cf = new ClassFactory();

		// try an interface first
		Map<String, String> map = new HashMap<String, String>();
		map.put("A", ClassFactory.toSignature(String.class));
		map.put("B", "B");
		map.put("C", "C");
		map.put("D", "D");
		map.put("F", "F");
		map.put("I", "I");
		map.put("J", "J");
		map.put("K", ClassFactory.toSignature(Cache[].class));
		ClassFactory.Spec s = new ClassFactory.Spec("de.zarncke.lib.util.Temp", true, map, null, new String[0]);

		Class<?> c = cf.createClass(s);
		assertEquals("de.zarncke.lib.util.Temp", c.getName());
		assertTrue(c.isInterface());
		assertNull(c.getSuperclass());
		assertEquals(String.class, c.getMethod("getA", new Class[0]).getReturnType());
		assertEquals(Void.TYPE, c.getMethod("setA", new Class[] { String.class }).getReturnType());

		// now try a real class
		s = new ClassFactory.Spec("de.zarncke.lib.util.Temp2", false, map, "java.lang.Object",
				new String[] { "de.zarncke.lib.util.Temp" });

		Class<?> d = cf.createClass(s);
		assertEquals("de.zarncke.lib.util.Temp2", d.getName());
		assertTrue(!d.isInterface());
		assertEquals(Object.class, d.getSuperclass());
		assertEquals(Arrays.asList(new Class[] { c }), Arrays.asList(d.getInterfaces()));
		Method mget = c.getMethod("getD", new Class[0]);
		Method mset = c.getMethod("setD", new Class[] { Double.TYPE });
		assertEquals(Double.TYPE, mget.getReturnType());
		assertEquals(Void.TYPE, mset.getReturnType());
		Object o = d.newInstance();
		mset.invoke(o, new Object[] { new Double(10.1) });
		assertEquals(new Double(10.1), mget.invoke(o, new Object[0]));

		// add tests for arrays!

		// further additions:
		// - store "line numbers"
	}

	public static junit.framework.Test suite() {
		return new TestSuite(ClassTest.class);
	}

	private static void assertRelation(final PartialOrder ord, final int kind, final Object a, final Object b) {
		switch (kind) {
		case '<':
			assertTrue("failed " + a + "<=" + b, ord.lessEqual(a, b));
			assertTrue("failed !" + b + "<=" + a, !ord.lessEqual(b, a));
			break;
		case '=':
			assertTrue("failed " + a + "<=" + b, ord.lessEqual(a, b));
			assertTrue("failed " + b + "<=" + a, ord.lessEqual(b, a));
			break;
		case '>':
			assertTrue("failed !" + a + "<=" + b, !ord.lessEqual(a, b));
			assertTrue("failed " + b + "<=" + a, ord.lessEqual(b, a));
			break;
		case '!':
			assertTrue("failed !" + a + "<=" + b, !ord.lessEqual(a, b));
			assertTrue("failed !" + b + "<=" + a, !ord.lessEqual(b, a));
			break;
		}
	}

	private static void assertEquals(final Object[] a, final Object[] b) {
		if (!new HashSet<Object>(Arrays.asList(a)).equals(new HashSet<Object>(Arrays.asList(b)))) {
			throw new de.zarncke.lib.err.CantHappenException("expected: " + Arrays.asList(a) + " but was: " + Arrays.asList(b));
		}
	}

	static class TS1 {
		int x;
	}

	static class TS2 {
		TS1 r;
	}

	static class TS3 {
		double d;
		boolean b;
	}

	public void testClassDescription() {
		assertEquals(ClassTools.CLASS_BASE_SIZE, ClassTools.estimateSize(Object.class));
		assertEquals(ClassTools.CLASS_BASE_SIZE + 4, ClassTools.estimateSize(TS1.class));
		assertEquals(ClassTools.CLASS_BASE_SIZE + 4, ClassTools.estimateSize(TS2.class));
		assertEquals(ClassTools.CLASS_BASE_SIZE + 9, ClassTools.estimateSize(TS3.class));
	}
}
