package de.zarncke.lib.region;

import de.zarncke.lib.coll.Elements;
import de.zarncke.lib.err.GuardedTest;

public class RegionTest extends GuardedTest
{
    public void testConstRegion()
    {
        assertTrue(Elements
                .arrayequals(new byte[] { 3, 3, 3, 3, 3 }, new ConstRegion(5, (byte) 3).toByteArray()));
        assertTrue(Elements.arrayequals(new byte[] { 3, 3, 8, 9, 3, 3 }, new ConstRegion(5, (byte) 3).select(2,
                1).replace(RegionUtil.asRegion(new byte[] { 8, 9 })).toByteArray()));
    }
}
