package de.zarncke.lib.util;

import java.util.List;
import java.util.Random;

import org.junit.Test;

import de.zarncke.lib.err.GuardedTest4;

public class TestTest extends GuardedTest4 {
	class Tree {
		char value;
		List<Tree> suffixes;

		public Tree(final char value) {
			this.value = value;
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

	public static final String XXX = "";

	static {
	}

	@Test
	public void testN() {
		for (String s : new String[] { "a", "aa", "aaa", "abca", "ababababa", "aba", "aaabaa", "ab", "abcabcabc" }) {
			assertEquals(s, solution(s), solution2(s));
		}
	}

	@Test
	public void testR() {
		Random rand = new Random();
		for (int i = 0; i < 100; i++) {
			String s = "";
			for (int j = 0; j < 10; j++) {
				s = s + (char) ('a' + rand.nextInt(3));
			}
			assertEquals(s, solution(s), solution2(s));
		}
	}

	@Test
	public void testX() {
		assertEquals(0, solution2("ab"));
	}

	@Test
	public void testX1() {
		assertEquals(0, solution2("abc"));
	}

	@Test
	public void testX3() {
		assertEquals(1, solution2("abcabcda"));
	}

	@Test
	public void testX2() {
		assertEquals(6, solution2("abcabcabc"));
	}

	@Test
	public void test3() {
		assertEquals(3, solution("abahhbaba"));
	}

	@Test
	public void test0() {
		assertEquals(0, solution("codility"));
	}

	@Test
	public void test1() {
		assertEquals(1, solution("hgggghh"));
	}
	@Test
	public void test2() {
		assertEquals(2, solution("aahhahaaa"));
	}


	public int solution(final String A) {
		int l = A.length();
		for (int i = l - 1; i >= 0; i--) {
			if (A.substring(0, i).equals(A.substring(l - i, l))) {
				return i;
			}
		}
		return 0;
	}

	public int solution2(final String A) {
		char[] w = A.toCharArray();
		int n = A.length();
		int[] N = new int[n + 1];
		int i = 0; // Variable i zeigt immer auf die aktuelle Position
		int j = -1; // im Muster, Variable j gibt die Länge des gefun-
		// denen Präfixes an.

		N[i] = j; // Der erste Wert ist immer -1

		while (i < n) { // solange das Ende des Musters nicht erreicht ist

			while (j >= 0 && w[j] != w[i]) { // Falls sich ein gefundenes
				// Präfix nicht verlängern lässt,
				j = N[j]; // nach einem kürzeren suchen.

			}

			// an dieser Stelle ist j=-1 oder w[i]=w[j]

			i++; // Unter dem nächsten Zeichen im Muster
			j++; // den gefundenen Wert (mindestens 0)
			N[i] = j; // in die Präfix-Tabelle eintragen.

		}

		return N[n];
	}

	public int dynamicSolution(final int n) {
		Integer[] map = new Integer[n];
		return dynamicSolution(n, map);
	}

	public int dynamicSolution(final int n, final Integer[] map) {
		if (n < 0) {
			return 0;
		}
		if (n == 0) {
			return 1;
		}
		if (map[n] != null) {
			return map[n].intValue();
		}

		map[n] = new Integer(dynamicSolution(n - 1, map) + dynamicSolution(n - 2, map) + dynamicSolution(n - 3, map));
		return map[n].intValue();
	}

	public static int solution(final int N) {
		// also called Gauss's Circle Problem
		if (N == 0) {
			return 1;
		}

		long numberOfPoints = 1;
		if (numberOfPoints > 1000000000) {
			return -1;
		}
		return (int) numberOfPoints;
	}

	public static void main(final String[] args) {
	}

}
