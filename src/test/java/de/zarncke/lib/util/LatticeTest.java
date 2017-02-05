package de.zarncke.lib.util;

import java.util.Arrays;
import java.util.HashSet;

import junit.framework.TestSuite;

import de.zarncke.lib.coll.HierarchicalMap;
import de.zarncke.lib.err.GuardedTest;
import de.zarncke.lib.struct.Lattice;
import de.zarncke.lib.struct.PartialOrder;

/**
 * Tests lattice related classes of util package.
 */
public class LatticeTest extends GuardedTest
{
	public LatticeTest()
	{
		super();
	}

	private static PartialOrder prefixOrder = new PartialOrder()
	{
		public boolean lessEqual(final Object a, final Object b)
			{
				if (a == Lattice.BOT)
				{
					return true;
				}
				if (b == Lattice.BOT)
				{
					return false;
				}
				return ((String) a).startsWith((String) b);
			}
		};

	private static PartialOrder logicOrder = new PartialOrder()
	{
		public boolean lessEqual(final Object a, final Object b)
			{
				int aa = ((Number) a).intValue();
				int bb = ((Number) b).intValue();
				return (aa | bb) == aa;
			}

		};


	public void testLattice()
	{
		// some basic lattice tests
		{
			Lattice set = new Lattice(Lattice.IMPLICIT_ORDER,
 Integer.valueOf(Integer.MAX_VALUE),
					Integer.valueOf(Integer.MIN_VALUE));
			int[] ia = new int[] { Integer.MIN_VALUE, 10, -5, 0, 4,
								   1000, -50, -20, Integer.MAX_VALUE };
			for (int i = 1; i < ia.length - 1; i++)
			{
				set.add(Integer.valueOf(ia[i]));
			}
			Arrays.sort(ia);

			for (int i = 1; i < ia.length - 1; i++)
			{
				assertEquals(new Object[] { Integer.valueOf(ia[i - 1]) },
 set.getLower(Integer.valueOf(ia[i])));
				assertEquals(new Object[] { Integer.valueOf(ia[i + 1]) }, set.getUpper(Integer.valueOf(ia[i])));
			}
		}


		{
			Lattice set = new Lattice(prefixOrder, "", Lattice.BOT);
			set.add("aa");
			set.add("ab");
			set.add("a");

			assertEquals(new Object[] { "aa", "ab" },
						 set.getLower("a"));
			assertEquals(new Object[] { "" },
						 set.getUpper("a"));
			assertEquals(new Object[] { "a" },
						 set.getUpper("aa"));
			assertEquals(new Object[] { "a" },
						 set.getUpper("ab"));
			assertEquals(new Object[] { Lattice.BOT },
						 set.getLower("ab"));
		}

		{
			Lattice set =
 new Lattice(logicOrder, Integer.valueOf(0), Integer.valueOf(-1));

			set.add(Integer.valueOf(0x001));
			set.add(Integer.valueOf(0x010));
			set.add(Integer.valueOf(0x100));
			set.add(Integer.valueOf(0x011));
			set.add(Integer.valueOf(0x110));
			set.add(Integer.valueOf(0x101));

			assertEquals(new Object[] { Integer.valueOf(0) }, set.getUpper(Integer.valueOf(0x001)));
			assertEquals(new Object[] { Integer.valueOf(-1) }, set.getLower(Integer.valueOf(0x011)));

			assertEquals(new Object[] { Integer.valueOf(0x001), Integer.valueOf(0x100) }, set.getUpper(Integer.valueOf(0x101)));
			assertEquals(new Object[] { Integer.valueOf(0x011), Integer.valueOf(0x110), Integer.valueOf(0x101) },
					set.getUpper(Integer.valueOf(0x111)));

		}
		/*
		{
			Lattice set = new Lattice();
			set.add(TestA.class);
			set.add(TestK.class);
			set.add(TestL.class);
			set.add(TestB.class);
			set.add(TestC.class);

			assertEquals(set.getLower(TestC.class),
				   new Object[] { TestB.class, TestL.class });
		}

		{
			Lattice set = new Lattice();
			set.add(TestA.class);
			set.add(TestK.class);
			set.add(TestB.class);
			set.remove(TestK.class);

			assertEquals(set.getLower(TestB.class),
				   new Object[] { TestA.class });
			assertEquals(set.getUpper(TestA.class),
				   new Object[] { TestB.class });
		}

		{
			Lattice set = new Lattice();
			set.add(a);
			set.add(b);
			set.add(c);
			set.add(ab);
			set.add(ac);
			set.add(abc);
			set.add(abx);
			set.remove(ab);

			assertEquals(set.getUpper(a), new Object[] { ac, abx });
			assertEquals(set.getUpper(b), new Object[] { abc, abx });
			assertEquals(set.getUpper(c), new Object[] { ac });
			assertEquals(set.getUpper(ac), new Object[] { abc });
		}

		{
			Lattice set = new Lattice();
			set.add(a);
			set.add(b);
			set.add(c);
			set.add(ab);
			set.add(ac);
			set.add(bc);

			assertEquals(set.getLower(Lattice.NONECLASS),
				   new Object[] { ab, bc, ac });
			assertEquals(set.getUpper(Object.class),
				   new Object[] { a, b, c });
		}

		{
			Comparator struc = Lattice.STRUCTURAL_ASSIGNABILITY;

			assertEquals(-1, struc.compare(TestA.class, TestA2.class));
			assertEquals(-1, struc.compare(TestA2.class, TestA.class));
			assertEquals(-1, struc.compare(TestB.class, TestB2.class));
			assertEquals(-1, struc.compare(TestB2.class, TestB.class));

			assertTrue(!TestA.class.isAssignableFrom(TestA2.class));
			assertTrue(!TestA2.class.isAssignableFrom(TestA.class));
			assertTrue(!TestB.class.isAssignableFrom(TestB2.class));
			assertTrue(!TestB2.class.isAssignableFrom(TestB.class));

			Lattice set = new Lattice(Lattice.STRUCTURAL_ASSIGNABILITY);
			set.add(TestA.class);
			set.add(TestA2.class);
			set.add(TestB.class);
			set.add(TestB2.class);

			assertEquals(new Object[] { TestA.class, TestA2.class },
						 set.getLower(TestB.class));
			assertEquals(new Object[] { TestB.class, TestB2.class },
						 set.getUpper(TestA.class));
		}
		*/
	}

	public static void testHierarchicalMap()
	{
		HierarchicalMap map =
			new HierarchicalMap(
new Lattice(logicOrder, Integer.valueOf(0), Integer.valueOf(-1)), true);

		map.put(Integer.valueOf(0x001), "001");
		map.put(Integer.valueOf(0x010), "010");
		map.put(Integer.valueOf(0x100), "100");
		map.put(Integer.valueOf(0x011), "011");
		map.put(Integer.valueOf(0x110), "110");
		map.put(Integer.valueOf(0x101), "101");
		map.put(Integer.valueOf(0x111), "111");

		assertTrue("001".equals(map.get(Integer.valueOf(0x001))));
		assertTrue("010".equals(map.get(Integer.valueOf(0x010))));
		assertTrue("100".equals(map.get(Integer.valueOf(0x100))));
		assertTrue("011".equals(map.get(Integer.valueOf(0x011))));
		assertTrue("110".equals(map.get(Integer.valueOf(0x110))));
		assertTrue("101".equals(map.get(Integer.valueOf(0x101))));
		assertTrue("111".equals(map.get(Integer.valueOf(0x111))));

		assertTrue("010".equals(map.get(Integer.valueOf(0x10010))));
		assertTrue("101".equals(map.get(Integer.valueOf(0x1101))));
		assertTrue("111".equals(map.get(Integer.valueOf(0x11111))));

		// test tie breaking
		//map.put(F.class, cmi);
		//assertSame(cmi, map.get(F.class));
	}



	public static junit.framework.Test suite()
	{
		return new TestSuite(LatticeTest.class);
	}

	private static void assertEquals(final Object[] a, final Object[] b)
	{
		if (!new HashSet(Arrays.asList(a))
			.equals(new HashSet(Arrays.asList(b))))
		{
			throw new de.zarncke.lib.err.CantHappenException
				("expected: " + Arrays.asList(a) +
				 " but was: " + Arrays.asList(b));
		}
	}
}





