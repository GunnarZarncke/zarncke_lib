package de.zarncke.lib.math;

import de.zarncke.lib.err.CantHappenException;

/**
 * Fast bit operations.
 * See <a href="http://graphics.stanford.edu/~seander/bithacks.html"Bit Twiddling Hacks by Sean Eron Anderson></a>
 * and <a href="http://aggregate.org/MAGIC/">The Aggregate Magic Algorithms</a>.
 *
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
public final class BitOps {
	private static final double LOG2 = Math.log(2);

	// a number which contains each possible 6-bit sequence as sub-sequences
	private static final long DE_BRUIJN_64 = 0x022fdd63cc95386dl;
	private static final int[] DE_BRUIJN_64_INDEX = new int[64];
	static {
		long m = 63 << 58;

		long debruijn = DE_BRUIJN_64;
		for (int i = 0; i < 64; i++) {
			DE_BRUIJN_64_INDEX[(int) ((debruijn & m) >>> 58)] = i;
			debruijn = debruijn << 1;
		}
		for (int i = 0; i < 64; i++) {
			int j;
			for (j = 0; j < 64; j++) {
				if (DE_BRUIJN_64_INDEX[i] == j) {
					break;
				}
			}
			if (j == 64) {
				throw new CantHappenException("de bruijn not properly set");
			}
		}
	}

	private BitOps() {// helper
	}

	public static int countBits(final long l) {
		return countBits((int) (l >>> 32)) + countBits((int) l);
		// int c = 0;
		// long lr = l;
		// while (lr != 0) {
		// lr ^= lr & -lr;
		// c++;
		// }
		// return c;
	}

	public static int countBits(int v) {
		v = v - (v >>> 1 & 0x55555555);
		v = (v & 0x33333333) + (v >>> 2 & 0x33333333);
		return (v + (v >> 4) & 0xF0F0F0F) * 0x1010101 >> 24;
	}

	/**
	 * Example: 74=0b1001100, the highest set bit is bit 6 (2^6=64).
	 * Note: Bit counting starts at 0.
	 *
	 * @param number
	 * @return bit number of highest bit set, -1 if no bits are set (number=0)
	 */
	public static int highestBit(long number) {
		if (number == 0) {
			return -1;
		}
		number |= number >>> 1;
		number |= number >>> 2;
		number |= number >>> 4;
		number |= number >>> 8;
		number |= number >>> 16;
		number |= number >>> 32;
		return DE_BRUIJN_64_INDEX[(int) ((number - (number >>> 1)) * DE_BRUIJN_64 >>> 58)];
		// int c = -1;
		// long lr = number;
		// while (lr != 0) {
		// lr >>>= 1;
		// c++;
		// }
		// return c;
	}

	public static int highestBit(int number) {
		if (number == 0) {
			return -1;
		}
		number |= number >>> 1;
		number |= number >>> 2;
		number |= number >>> 4;
		number |= number >>> 8;
		number |= number >>> 16;
		return DE_BRUIJN_64_INDEX[(int) ((number - (number >>> 1)) * DE_BRUIJN_64 >>> 58)];
	}

	/**
	 * Example: 74=0b1001100, the lowest set bit is bit 2 (2^2=4).
	 * Note: Bit counting starts at 0.
	 * Uses de Bruijn index.
	 *
	 * @param number
	 * @return bit number of lowest bit set, 64 if no bits are set (number=0)
	 */
	public static int lowestBit(final long number) {
		return number == 0 ? 64 : DE_BRUIJN_64_INDEX[(int) ((number & -number) * DE_BRUIJN_64 >>> 58)];
	}

	/**
	 * floor((a+b)/2)
	 * 
	 * @param a
	 * @param b
	 * @return average of a and b (never produces overflow and always rounds to lower value
	 */
	public static int averageOverflowSafeRoundingDown(final int a, final int b) {
		return (a & b) + ((a ^ b) >> 1);
	}

	public static long averageOverflowSafeRoundingDown(final long a, final long b) {
		return (a & b) + ((a ^ b) >> 1);
	}

	public static int reverseBits(int number) {
		number = (number & 0xaaaaaaaa) >>> 1 | (number & 0x55555555) << 1;
		number = (number & 0xcccccccc) >>> 2 | (number & 0x33333333) << 2;
		number = (number & 0xf0f0f0f0) >>> 4 | (number & 0x0f0f0f0f) << 4;
		number = (number & 0xff00ff00) >>> 8 | (number & 0x00ff00ff) << 8;
		return number >>> 16 | number << 16;
	}

	public static long reverseBits(long number) {
		number = (number & 0xaaaaaaaaaaaaaaaal) >>> 1 | (number & 0x5555555555555555l) << 1;
		number = (number & 0xccccccccccccccccl) >>> 2 | (number & 0x3333333333333333l) << 2;
		number = (number & 0xf0f0f0f0f0f0f0f0l) >>> 4 | (number & 0x0f0f0f0f0f0f0f0fl) << 4;
		number = (number & 0xff00ff00ff00ff00l) >>> 8 | (number & 0x00ff00ff00ff00ffl) << 8;
		number = (number & 0xffff0000ffff0000l) >>> 16 | (number & 0x0000ffff0000ffffl) << 16;
		return number >>> 32 | number << 32;
	}

	/**
	 * Example: 8 is 3 times divisible by 2, so is -8 and 24 also.
	 *
	 * @param l
	 * number to test
	 * @return number of times the number is evenly divisible by 2, -1 for zero
	 */
	public static int divisibleByPowerOf2(final long l) {
		if (l == 0) {
			return -1;
		}
		int c = 0;
		long lr = l;
		while ((lr & 1) == 0) {
			lr >>>= 1;
			c++;
		}
		return c;
	}

	public static short bytesToShort(final byte[] twoBytes) {
		assert twoBytes != null && twoBytes.length == 2;
		return (short) (twoBytes[0] << 8 | twoBytes[1]);
	}

	public static int bytesToInteger(final byte[] fourBytes) {
		assert fourBytes != null && fourBytes.length == 4;
		return fourBytes[0] << 24 | fourBytes[1] << 16 | fourBytes[2] << 8 | fourBytes[3];
	}

	public static long bytesToLong(final byte[] eightBytes) {
		assert eightBytes != null && eightBytes.length == 8;
		return (long) eightBytes[0] << 56 | (long) eightBytes[1] << 48 | (long) eightBytes[2] << 40
				| (long) eightBytes[3] << 32 | eightBytes[4] << 24 | eightBytes[5] << 16 | eightBytes[6] << 8
				| eightBytes[7];
	}

	/**
	 * @param number to determine required bits for
	 * @return minimum number of bits required to store this number
	 */
	public static int bitsRequired(final long number) {
		if (number < 0) {
			return highestBit(-number - 1) + 2;
		}
		return highestBit(number) + 1;
		// log2 might be faster:
		// if (number == 0) {
		// return 0;
		// }
		// return -(int) Math.floor(-Math.log(number + 1) / LOG2);
	}

}
