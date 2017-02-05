package de.zarncke.lib.util;

import java.math.BigDecimal;
import java.math.BigInteger;

import de.zarncke.lib.coll.Elements;
import de.zarncke.lib.coll.Pair;

/**
 * Very short short-cuts for often needed functions.
 * These are handy to avoid or simplify (un)boxing.
 *
 * @author Gunnar Zarncke
 * @clean 28.02.2012
 */
public final class Q {
	private Q() {
		// helper
	}

	/**
	 * Same as {@link Boolean#valueOf(boolean)}.
	 *
	 * @param b to wrap
	 * @return Boolean.TRUE or Boolean.FALSE
	 */
	public static Boolean b(final boolean b) {
		return b ? Boolean.TRUE : Boolean.FALSE;
	}

	public static Integer i(final int i) {
		return Integer.valueOf(i);
	}

	public static Long l(final long l) {
		return Long.valueOf(l);
	}

	public static Double d(final double d) {
		return Double.valueOf(d);
	}

	public static BigInteger bi(final long l) {
		return new BigInteger(Elements.toByteArray(l));
	}

	public static BigInteger bi(final String bd) {
		return new BigInteger(bd);
	}

	public static BigDecimal bd(final long l) {
		return new BigDecimal(l);
	}

	public static BigDecimal bd(final double d) {
		return new BigDecimal(d);
	}

	public static BigDecimal bd(final String bd) {
		return new BigDecimal(bd);
	}

	public static String s(final byte[] bytes) {
		return bytes == null ? "" : new String(bytes, Misc.UTF_8);
	}

	public static String s(final Object o) {
		return String.valueOf(o);
	}

	public static String s(final long l) {
		return String.valueOf(l);
	}

	public static String s(final double d) {
		return String.valueOf(d);
	}

	public static <A, B> Pair<A, B> p(final A a, final B b) {
		return Pair.pair(a, b);
	}
}
