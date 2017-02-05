package de.zarncke.lib.region;

import java.io.IOException;

import de.zarncke.lib.coll.Elements;
import de.zarncke.lib.err.GuardedTest;
import de.zarncke.lib.io.RegionInputStream;

public class RegionUtilTest extends GuardedTest
{
	public void testGetBigEndianInt()
	{
		assertEquals(0, RegionUtil.getBigEndianInt(makeRegion(0, 0, 0, 0), 0));
		assertEquals(1, RegionUtil.getBigEndianInt(makeRegion(0, 0, 0, 1), 0));
		assertEquals(42, RegionUtil.getBigEndianInt(makeRegion(0, 0, 0, 42), 0));

		assertEquals(-128, makeRegion(0, 0, 0, 128).get(3));
		assertEquals(0, makeRegion(0, 0, 0, 128).get(2));
		assertEquals(0, makeRegion(0, 0, 0, 128).get(1));
		assertEquals(0, makeRegion(0, 0, 0, 128).get(0));

		assertEquals(128, RegionUtil.getBigEndianInt(makeRegion(0, 0, 0, 128), 0));

		assertEquals(256, RegionUtil.getBigEndianInt(makeRegion(0, 0, 1, 0), 0));
		assertEquals(256 + 1, RegionUtil.getBigEndianInt(makeRegion(0, 0, 1, 1), 0));
		assertEquals(256 + 128, RegionUtil.getBigEndianInt(makeRegion(0, 0, 1, 128), 0));
		assertEquals(32768, RegionUtil.getBigEndianInt(makeRegion(0, 0, 128, 0), 0));
		assertEquals(32768 + 1, RegionUtil.getBigEndianInt(makeRegion(0, 0, 128, 1), 0));

		assertEquals(65536, RegionUtil.getBigEndianInt(makeRegion(0, 1, 0, 0), 0));
		assertEquals(65536 + 256, RegionUtil.getBigEndianInt(makeRegion(0, 1, 1, 0), 0));
		assertEquals(65536 + 256 + 1, RegionUtil.getBigEndianInt(makeRegion(0, 1, 1, 1), 0));
		assertEquals((1 << 23), RegionUtil.getBigEndianInt(makeRegion(0, 128, 0, 0), 0));
		assertEquals((1 << 23) + 1, RegionUtil.getBigEndianInt(makeRegion(0, 128, 0, 1), 0));
		assertEquals((1 << 23) + 512 + 1, RegionUtil.getBigEndianInt(makeRegion(0, 128, 2, 1), 0));

		assertEquals(1 << 24, RegionUtil.getBigEndianInt(makeRegion(1, 0, 0, 0), 0));
		assertEquals((1 << 24) + 1, RegionUtil.getBigEndianInt(makeRegion(1, 0, 0, 1), 0));
		assertEquals((1 << 24) + 256 + 42, RegionUtil.getBigEndianInt(makeRegion(1, 0, 1, 42), 0));
		assertEquals((1 << 24) + (2 << 16) + 256 + 42, RegionUtil.getBigEndianInt(makeRegion(1, 2, 1, 42), 0));
		assertEquals((1 << 31), RegionUtil.getBigEndianInt(makeRegion(128, 0, 0, 0), 0));
	}

	private Region makeRegion(final int... ints)
	{
		byte[] bytes = new byte[ints.length];
		for (int i = 0; i < ints.length; i++) {
			bytes[i]=(byte)ints[i];
		}
		return new PrimitiveRegion(bytes);
	}

	public void testRegionInputStream() throws IOException {
		byte[] ba = new byte[256];
		for (int i = 0; i < 256; i++) {
			ba[i]=(byte)i;
		}
		Region r = RegionUtil.asRegion(ba);
		RegionInputStream ris = new RegionInputStream(r);
		byte[] b2 = new byte[8];
		ris.read(b2, 2, 4);
		assertEquals(b2[0], 0);
		assertEquals(b2[1], 0);
		assertEquals(b2[2], 0);
		assertEquals(b2[3], 1);
		assertEquals(b2[4], 2);
		assertEquals(b2[5], 3);
		assertEquals(b2[6], 0);
		assertEquals(b2[7], 0);
		for (int i = 4; i < 256; i++) {
			assertEquals((byte) ris.read(), (byte) i);
		}
		assertEquals(ris.read(), -1);
	}

	public void testReadOnlyRegion() {
		Region r = makeRegion(-1, 0, 2, 100);
		Region rr = RegionUtil.readOnly(r);
		assertEquals(r.get(0), rr.get(0));
		assertEquals(r.get(3), rr.get(3));
		assertTrue(Elements.arrayequals(r.select(1, 2).toByteArray(), rr.select(1, 2).toByteArray()));
		r.select(1, 2).replace(makeRegion(4, 5, 6, 7));
		try {
			rr.select(1, 2).replace(makeRegion(4, 5, 6, 7));
			fail("exception expected");
		} catch (UnsupportedOperationException e) {
			// expected
		}
	}

	public void testCompare() {
		assertEquals(0, RegionUtil.compare(makeRegion(), makeRegion()));
		assertEquals(0, RegionUtil.compare(makeRegion(1, 2, 3), makeRegion(1, 2, 3)));
		assertTrue(RegionUtil.compare(makeRegion(), makeRegion(1)) < 0);
		assertTrue(RegionUtil.compare(makeRegion(), makeRegion(-1, -2)) < 0);
		assertTrue(RegionUtil.compare(makeRegion(1), makeRegion(2)) < 0);
		assertTrue(RegionUtil.compare(makeRegion(1, 2), makeRegion(1, 2, 3)) < 0);
		assertTrue(RegionUtil.compare(makeRegion(1, 2, 3), makeRegion(1, 2, 100)) < 0);
		assertTrue(RegionUtil.compare(makeRegion(1, 2, 3), makeRegion(1, 100, 3)) < 0);
		assertTrue(RegionUtil.compare(makeRegion(1), makeRegion()) > 0);
		assertTrue(RegionUtil.compare(makeRegion(-1, -2), makeRegion()) > 0);
		assertTrue(RegionUtil.compare(makeRegion(-1), makeRegion(-2)) > 0);
		assertTrue(RegionUtil.compare(makeRegion(1, 2, 3), makeRegion(1, 2)) > 0);
		assertTrue(RegionUtil.compare(makeRegion(1, 2, 100), makeRegion(1, 2, 3)) > 0);
		assertTrue(RegionUtil.compare(makeRegion(1, 100, 3), makeRegion(1, 2, 3)) > 0);
	}
}
