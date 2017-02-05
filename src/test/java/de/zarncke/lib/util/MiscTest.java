package de.zarncke.lib.util;

import java.io.IOException;
import java.util.Comparator;

import de.zarncke.lib.coll.Pair;
import de.zarncke.lib.err.GuardedTest;
import de.zarncke.lib.io.StoreConsumer;

public class MiscTest extends GuardedTest {
	@SuppressWarnings({ "unchecked", "rawtypes" }/* intended for all types */)
	public void testComparator() {
		// TEST comparator
		Comparator comp = Misc.COMPARABLE_COMPARATOR;
		assertTrue(comp.compare("", "a") < 0);
		assertTrue(comp.compare("xx", "xx") == 0);
		assertTrue(comp.compare("bcdd", "bcz") < 0);
		assertTrue(comp.compare("bcd", "bczzz") < 0);
		assertTrue(comp.compare(Integer.valueOf(4), Integer.valueOf(-4)) > 0);
		assertTrue(comp.compare(Integer.valueOf(0), Integer.valueOf(1000)) < 0);
		comp = new Misc.PartialOrderComparator() {
			@Override
			public boolean lessEqual(final Object a, final Object b) {
				return ((Number) a).intValue() <= ((Number) b).intValue();
			}
		};
		assertTrue(comp.compare(Integer.valueOf(0), Integer.valueOf(0)) == 0);
		assertTrue(comp.compare(Integer.valueOf(0), Integer.valueOf(3)) < 0);
		assertTrue(comp.compare(Integer.valueOf(2), Integer.valueOf(0)) > 0);
		assertTrue(comp.compare(Integer.valueOf(2), Integer.valueOf(3)) < 0);
		assertTrue(comp.compare(Integer.valueOf(2), Integer.valueOf(-3)) > 0);
		assertTrue(comp.compare(Integer.valueOf(-7), Integer.valueOf(-7)) == 0);

	}

	public void testMime() {
		// TEST mimetypes
		assertEquals("text/html", Misc.getMimeTypeOf("xyz/test.html", null));
		assertEquals("application/pdf", Misc.getMimeTypeOf("hallo.pdf", null));
		assertEquals("application/octet-stream", Misc.getMimeTypeOf("junk", null));
	}

	public void testSleep() {
		// TEST sleep
		long t = System.currentTimeMillis();
		Misc.sleep(10);
		if (System.currentTimeMillis() < t + 10) {
			fail("sleeping too short");
		}
	}
	public void testEquals() {
		assertTrue(Misc.equals(null, null));
		assertTrue(Misc.equals("a", "a"));
		assertFalse(Misc.equals(null, "a"));
		assertFalse(Misc.equals("a", null));

		assertEquals(0, Misc.<String> compare(null, null));
		assertEquals(0, Misc.compare("a", "a"));
		assertTrue(Misc.compare(null, "a") < 0);
		assertTrue(Misc.compare("a", null) > 0);
		assertTrue(Misc.compare("a", "b") < 0);
		assertTrue(Misc.compare("b", "a") > 0);
	}

	public void testJavaRun() throws IOException {
		Pair<String, Integer> res = Misc.processCommand(new ProcessBuilder(Misc.getJavaBinaryPath(), "-help"),
				StoreConsumer.TO_UTF_STRING);
		assertEquals(Q.i(0), res.getSecond());
		if (!res.getFirst().isEmpty() && !res.getFirst().startsWith("Usage:")) {
			fail("java -help should output Usage");
		}
	}

	public void testRamSpace() {
		if (Misc.isWindows() || Misc.isLinux()) {
			if (Misc.isLinux() && failIfAfter(2014, 9, 1)) {
				return;
			}

			Long free = Misc.getFreeSystemRamBytes();
			Long total = Misc.getTotalSystemRamBytes();
			assertNotNull(free);
			assertNotNull(total);
			assertTrue(free.longValue() > 0);
			assertTrue(total.longValue() > free.longValue());
			getUnbufferedLog().report("ram:" + free + "/" + total);
		}
	}
}
