package de.zarncke.lib.util;

import static java.lang.Math.*;

import org.junit.Test;

import de.zarncke.lib.err.GuardedTest4;

public class GaussCircleTest extends GuardedTest4 {
	class Tree {
		int value;
		Tree left;
		Tree right;

		public Tree(final int value) {
			this.value = value;
		}

		public Tree(final int value, final Tree left, final Tree right) {
			this.left = left;
			this.right = right;
			this.value = value;
		}

		private int maxDepth() {
			return max(this.left == null ? 0 : this.left.maxDepth(), this.right == null ? 0 : this.right.maxDepth()) + 1;
		}

		private int minDepth() {
			// if(left == null && right == null)return 0;
			return min(this.left == null ? 0 : this.left.maxDepth(), this.right == null ? 0 : this.right.maxDepth()) + 1;
		}
	}

	// beachten:
	// - 0, leer
	// - negative
	// - overflow (int und double!)
	// - gleiche werte
	// - modulo result
	// - in-place?
	// - order irrelevant?
	// - standard solutions?
	// - replace tree by summary of tree

	// bonuspoints:
	// - concurrent
	// - scala
	// - add references

	@Test
	public void test0() {
		assertEquals(1, solution(0));
	}

	static {
		// for (int j = 0; j < 10; j++) {
			for (int i = 0; i < 10000; i++) {
				solution2(i);
				solution(i);
			}
		// }
	}

	@Test
	public void testN() {
			for (int i = 0; i < 10000; i++) {
				assertEquals(solution2(i), solution(i));
			}
	}

	@Test
	public void testTime1() {
		for (int i = 0; i < 10000; i++) {
			solution(i);
		}
	}

	@Test
	public void testTime2() {
		for (int i = 0; i < 10000; i++) {
			solution2(i);
		}
	}

	@Test
	public void testx() {
		assertEquals(-1, solution(1000000));
	}

	@Test
	public void testx2() {
		assertEquals(3141549, solution(1000));
	}

	@Test
	public void test1() {
		assertEquals(5, solution(1));
	}

	@Test
	public void test2() {
		assertEquals(13, solution(2));
	}

	@Test
	public void test3() {
		assertEquals(29, solution(3));
	}

	@Test
	public void test4() {
		assertEquals(49, solution(4));
	}

	@Test
	public void test5() {
		assertEquals(81, solution(5));
	}

	@Test
	public void test6() {
		assertEquals(113, solution(6));
	}

	public int solution(final int[] A, final int[] B) {
		if (A.length == 0) {
			return 0;
		}

		return 1;
	}

	public static int solution(final int N) {
		// also called Gauss's Circle Problem
		if (N == 0) {
			return 1;
		}
		// adapted from History of the Theory of Number vol. 2 by Dickson et al, page 234
		long A = (long) N * N;
		long q = (long) floor(sqrt(A / 2));
		long rootN = (long) floor(sqrt(A));
		long r = q + rootN;
		long numberOfPoints = 4 * q * q + 1 + 4 * rootN;
		for (long j = q + 1; j <= r; j++) {
			if (A > j * j) {
				numberOfPoints += 8 * floor(sqrt(A - j * j));
			}
		}
		if (numberOfPoints > 1000000000) {
			return -1;
		}
		return (int) numberOfPoints;
	}

	public static int solution2(final int N) {
		// Algorithm of Horn

		if (N == 0) {
			return 1;
		}

		long numberOfPointsInSegment = 1;
		int d = 0;
		int x = N;
		int y = 0;
		while (true) {
			// take all the points in the slice from -y to +y

			d = d + 2 * y + 1;
			y = y + 1;
			if (d > 0) {
				d = d - 2 * x + 1;
				x = x - 1;
				if (x == y) {
					break;
				}
				if (x < y) {
					break;
				}
				numberOfPointsInSegment += 2 * y + 1;
			} else {
				if (x == y) {
					numberOfPointsInSegment++;
					y--;
					x--;
					break;
				}
				numberOfPointsInSegment += 2;
			}
		}

		long numberOfPointsInSquare = (2 * x + 1) * (2 * x + 1);
		long numberOfPoints = 4 * numberOfPointsInSegment + numberOfPointsInSquare;

		// overflow!!!
		if (numberOfPoints > 1000000000) {
			return -1;
		}
		return (int) numberOfPoints;
	}

	public static void main(final String[] args) {
	}

}
