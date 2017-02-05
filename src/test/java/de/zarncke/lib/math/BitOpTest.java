package de.zarncke.lib.math;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BitOpTest {

	@Test
	public void testBitRequired() {
		assertEquals(0, BitOps.bitsRequired(0));
		assertEquals(1, BitOps.bitsRequired(1));
		assertEquals(2, BitOps.bitsRequired(2));
		assertEquals(2, BitOps.bitsRequired(3));
		assertEquals(3, BitOps.bitsRequired(4));
		assertEquals(3, BitOps.bitsRequired(5));
		assertEquals(3, BitOps.bitsRequired(6));
		assertEquals(3, BitOps.bitsRequired(7));
		assertEquals(4, BitOps.bitsRequired(8));
		assertEquals(1, BitOps.bitsRequired(-1));
		assertEquals(2, BitOps.bitsRequired(-2));
		assertEquals(3, BitOps.bitsRequired(-3));
		assertEquals(3, BitOps.bitsRequired(-4));
		assertEquals(4, BitOps.bitsRequired(-5));
		assertEquals(4, BitOps.bitsRequired(-6));
		assertEquals(4, BitOps.bitsRequired(-7));
		assertEquals(4, BitOps.bitsRequired(-8));
		assertEquals(31, BitOps.bitsRequired(Integer.MAX_VALUE));
		assertEquals(32, BitOps.bitsRequired(Integer.MIN_VALUE));
	}

	@Test
	public void testdivisibleByPowerOf2() {
		assertEquals(-1, BitOps.divisibleByPowerOf2(0));
		assertEquals(0, BitOps.divisibleByPowerOf2(1));
		assertEquals(0, BitOps.divisibleByPowerOf2(3));
		assertEquals(1, BitOps.divisibleByPowerOf2(2));
		assertEquals(1, BitOps.divisibleByPowerOf2(102));
		assertEquals(2, BitOps.divisibleByPowerOf2(4));
		assertEquals(2, BitOps.divisibleByPowerOf2(20));
	}

	@Test
	public void testReverse() {
		assertEquals(0, BitOps.reverseBits(0));
		assertEquals(1, BitOps.reverseBits(0x80000000));
		assertEquals(0x80000000, BitOps.reverseBits(1));
		assertEquals(0x123400, BitOps.reverseBits(0x2c4800));
		assertEquals(0x12345678, BitOps.reverseBits(0x1e6a2c48));
		assertEquals(0xffffffff, BitOps.reverseBits(0xffffffff));

		assertEquals(0l, BitOps.reverseBits(0l));
		assertEquals(0x123456789abcdef0l, BitOps.reverseBits(0x0f7b3d591e6a2c48l));
		assertEquals(0xffffffffffffffffl, BitOps.reverseBits(0xffffffffffffffffl));
	}

	@Test
	public void testAverage() {
		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < 10; j++) {
				assertAverage(i, j);
			}
		}
		for (int i = -10; i < 10; i++) {
			for(int j = -10; j < 10;j++) {
				assertAverage(i, j);
			}
		}
	}

	private void assertAverage(final int a, final int b) {
		assertEquals("(" + a + " + " + b + ")/2", a + b >> 1, BitOps.averageOverflowSafeRoundingDown(a, b));
	}

	@Test
	public void testHighestBit() {
		assertEquals(-1, BitOps.highestBit(0));
		assertEquals(0, BitOps.highestBit(1));
		assertEquals(1, BitOps.highestBit(2));
		assertEquals(1, BitOps.highestBit(3));
		assertEquals(2, BitOps.highestBit(4));
		assertEquals(2, BitOps.highestBit(7));
		assertEquals(3, BitOps.highestBit(8));
		assertEquals(3, BitOps.highestBit(0xf));
		assertEquals(6, BitOps.highestBit(0x64));
		assertEquals(15, BitOps.highestBit(0x8000));
		assertEquals(62, BitOps.highestBit(0x49ab012301230123L));
		assertEquals(63, BitOps.highestBit(0x89ab012301230123L));
	}

	@Test
	public void testLowestBit() {
		assertEquals(64, BitOps.lowestBit(0));
		assertEquals(0, BitOps.lowestBit(1));
		assertEquals(1, BitOps.lowestBit(2));
		assertEquals(0, BitOps.lowestBit(3));
		assertEquals(2, BitOps.lowestBit(4));
		assertEquals(0, BitOps.lowestBit(5));
		assertEquals(1, BitOps.lowestBit(6));
		assertEquals(0, BitOps.lowestBit(7));
		assertEquals(3, BitOps.lowestBit(8));
		assertEquals(0, BitOps.lowestBit(0xf));
		assertEquals(4, BitOps.lowestBit(16));
		assertEquals(2, BitOps.lowestBit(0x64));
		assertEquals(15, BitOps.lowestBit(0x8000));
		assertEquals(0, BitOps.lowestBit(0x49ab012301230123L));
		assertEquals(63, BitOps.lowestBit(0x8000000000000000L));
	}

	@Test
	public void testcountBits() {
		assertEquals(0, BitOps.countBits(0));
		assertEquals(1, BitOps.countBits(1));
		assertEquals(1, BitOps.countBits(0x400));
		assertEquals(1, BitOps.countBits(0x80000000L));
		assertEquals(2, BitOps.countBits(3));
		assertEquals(2, BitOps.countBits(0x240));
		assertEquals(3, BitOps.countBits(7));
		assertEquals(3, BitOps.countBits(0xe00));
		assertEquals(8, BitOps.countBits(0xff));
		assertEquals(16, BitOps.countBits(0xffff));
		assertEquals(32, BitOps.countBits(0xffffffffL));
		assertEquals(64, BitOps.countBits(0xffffffffffffffffL));
	}
}
