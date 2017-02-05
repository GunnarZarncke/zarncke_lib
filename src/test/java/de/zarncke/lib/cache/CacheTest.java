package de.zarncke.lib.cache;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Test;

import de.zarncke.lib.err.GuardedTest;
import de.zarncke.lib.log.Log;
import de.zarncke.lib.util.Misc;

/**
 * Tests Cache.
 */
public class CacheTest extends GuardedTest {
	private byte[] fill;

	@Test
	public void testCacheReminder() {
		System.out.println("don't forget to make the CacheTest work again"); // NOPMD teaser
	}

	// TODO this test fails with the 1.6 GC. better simulate GC
	public void testCache() throws Exception {
		if (failIfAfter(2016, 12, 1)) {
			return;
		}

		int N = 300000; // *large* data to cache
		int SLACK = 20000; // allowed slack in measurement
		Runtime r = Runtime.getRuntime();

		long m00 = r.totalMemory() - r.freeMemory();
		Log.LOG.get().report("mem00 = " + m00);

		// prepare: fill up memory such that free < 10000 bytes
		this.fill = new byte[(int) r.freeMemory() - 10000];

		// initial state
		gcExtreme();
		long m0 = r.totalMemory() - r.freeMemory();
		Log.LOG.get().report("mem0 = " + m0);

		// alloc (should take up 4N bytes)
		Serializable o = new int[N];
		long m1 = r.totalMemory() - r.freeMemory();
		Log.LOG.get().report("mem1 = " + m1);

		if (m1 < m0 + 4L * N - SLACK) {
			Assert.fail("this test does not apply, " + N + " ints take up lessen then 4*" + N
					+ " bytes! does some Thread run async?");
		}

		// cache (should change nothing yet)
		Cache<Serializable> c = new Cache<Serializable>(o);
		gcExtreme();
		long m2 = r.totalMemory() - r.freeMemory();
		Log.LOG.get().report("mem1' = " + m2);

		if (m2 < m1 - SLACK || m2 > m1 + SLACK) {
			Assert.fail("the cache should use up more or less no memory " + "and may not yet discard the data (we still have "
					+ "a reference). does some Thread run async?");
		}

		// now forget the reference and shortly increase memory load
		// (should result in a memory footprint as initial)
		o = null;
		System.gc(); // NOPMD this is a GC test
		for (int i = 0; i < 10; i++) {
			o = new int[N / 2];
			Misc.sleep(10);
			System.gc();// NOPMD this is a GC test
			o = null;
		}
		System.gc();// NOPMD this is a GC test
		long m3 = r.totalMemory() - r.freeMemory();
		Log.LOG.get().report("mem0' = " + m3);

		if (m3 < m0 - SLACK || m3 > m0 + SLACK) {
			Assert.fail("the garbage collection should have cleaned up and "
					+ "invalidated our cache. now the initial memory footprint "
					+ "should occur. does some Thread run async or the gc work " + "not as expected?");
		}

		Assert.assertNotNull(this.fill);
		this.fill = null;

		// recover data
		o = c.get();
		Assert.assertEquals(o.getClass(), int[].class);
	}

	private static void gcExtreme() {
		for (int i = 0; i < 10; i++) {
			Misc.sleep(10);
			System.gc();// NOPMD this is a GC test
			System.runFinalization();// NOPMD this is a GC test
		}
	}

	private static void assertEquals(final Object[] a, final Object[] b) {
		if (!new HashSet<Object>(Arrays.asList(a)).equals(new HashSet<Object>(Arrays.asList(b)))) {
			throw new de.zarncke.lib.err.CantHappenException("expected: " + Arrays.asList(a) + " but was: " + Arrays.asList(b));
		}
	}
}
