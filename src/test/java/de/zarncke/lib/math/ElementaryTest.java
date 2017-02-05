package de.zarncke.lib.math;

import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.junit.Test;

import de.zarncke.lib.coll.Elements;

public class ElementaryTest extends TestCase {

	@Test
	public void testFactorize() {
		factorsOf(1, 1);
		factorsOf(2, 2);
		// CHECKSTYLE:OFF
		factorsOf(3, 3);
		factorsOf(4, 2, 2);
		factorsOf(5, 5);
		factorsOf(6, 2, 3);
		factorsOf(9, 3, 3);
		factorsOf(10, 2, 5);
		factorsOf(12, 2, 2, 3);
		factorsOf(42, 2, 3, 7);
		factorsOf(36, 2, 2, 3, 3);
		factorsOf(60, 2, 2, 3, 5);
		// CHECKSTYLE:ON
	}

	private void factorsOf(final int n, final int... vals) {
		int i = 0;
		List<Integer> factors = Elementary.factorize(n);
		if (factors.size() != vals.length) {
			Assert.fail(factors + "!=" + Elements.asList(vals) + " in length");
		}
		for (Integer f : factors) {
			if (f.intValue() != vals[i++]) {
				Assert.fail(f + "!=" + vals[i - 1] + " for " + n + "=" + Elements.asList(vals));
			}
		}
	}

}
