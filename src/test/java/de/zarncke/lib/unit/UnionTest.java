package de.zarncke.lib.unit;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import junit.framework.Assert;

import org.junit.Test;

import de.zarncke.lib.struct.Union;
import de.zarncke.lib.struct.UnionFind;

public class UnionTest {
	@Test
	public void testUnion() {
		Union<String> a = UnionFind.create("a");
		equalCollection(Collections.singleton("a"), a.elements());
		a.unionWith(a);

		Union<String> b = UnionFind.create("b");
		a.unionWith(b);
		// System.out.println(a);
		Assert.assertEquals(a, b);
		equalCollection(Arrays.asList("a", "b"), a.elements());

		Union<String> cd = UnionFind.create("c", "d");
		a.unionWith(cd);
		// System.out.println(a);
		equalCollection(Arrays.asList("a", "b", "c", "d"), b.elements());
		Assert.assertEquals(a, cd);

		Union<String> xyz = UnionFind.create("x", "y", "z");
		b.unionWith(xyz);
		// System.out.println(a);
		equalCollection(Arrays.asList("a", "b", "c", "d", "x", "y", "z"), cd.elements());
	}

	private <T> void equalCollection(final Collection<T> a, final Collection<T> b) {
		Assert.assertEquals(new HashSet<T>(a), new HashSet<T>(b));
	}
}
