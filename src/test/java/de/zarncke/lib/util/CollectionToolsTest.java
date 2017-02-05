package de.zarncke.lib.util;

import java.util.Arrays;
import java.util.List;

import de.zarncke.lib.coll.Elements;
import de.zarncke.lib.err.GuardedTest;

public class CollectionToolsTest extends GuardedTest
{
    public void testSplit()
    {
        List<String> l12 = Arrays.asList("1", "2");
        List<String> l3 = Arrays.asList("3");
        List<String> l123 = Arrays.asList("1", "2", "3");

        assertEquals(Arrays.asList(l123), Elements.split(l123, 3));

        assertEquals(Arrays.asList(l123), Elements.split(l123, 4));

        assertEquals(Arrays.asList(l12, l3), Elements.split(l123, 2));

    }

    public void testArrayCompare()
    {
        byte[] b = new byte[] {};
        byte[] b12 = new byte[] { 1, 2 };
        byte[] b123 = new byte[] { 1, 2, 3 };
        byte[] b13 = new byte[] { 1, 3 };

        assertEquals(0, Elements.arraycompare(b, b));
        assertEquals(0, Elements.arraycompare(b123, b123));

        assertTrue(Elements.arraycompare(b, b123) < 0);
        assertTrue(Elements.arraycompare(b12, b123) < 0);
        assertTrue(Elements.arraycompare(b13, b123) > 0);
        assertTrue(Elements.arraycompare(b12, b13) < 0);
    }
}
