package de.zarncke.lib.time;

import de.zarncke.lib.err.GuardedTest;
import de.zarncke.lib.test.TestClock;

public class ClockTest extends GuardedTest
{
	public void testJavaClock()
    {
        Clock jc = new JavaClock();
        long tm = System.currentTimeMillis();
        long jm = jc.getCurrentTimeMillis();
        assertTrue("java clock should read system clock", tm == jm || tm + 1 == jm);
    }

    public void testTestClock() throws InterruptedException
    {
        long tm = System.currentTimeMillis();
		TestClock tc = new TestClock(tm);
		Thread.sleep(2);
		assertEquals(tm, tc.getCurrentTimeMillis());

		long millis = 1000000L;
		tc.setSimulatedMillis(millis);
		assertEquals(millis, tc.getCurrentTimeMillis());
	}

	public void testOffsetClock()
    {
		long refMillis = 1000000L;
		TestClock tc = new TestClock(refMillis);
		// start stopped
		StoppableOffsetClock soc = new StoppableOffsetClock(tc, true);
		assertEquals(refMillis, soc.getCurrentTimeMillis());

		tc.incrementSimulatedMillis(10);
		// test clock now +10
		assertEquals(refMillis, soc.getCurrentTimeMillis());

		soc.setStopped(false);
		assertEquals(refMillis, soc.getCurrentTimeMillis());

		soc.setOffsetMillis(0);
		assertEquals(refMillis + 10, soc.getCurrentTimeMillis());

		tc.incrementSimulatedMillis(10);
		// test clock now +20
		assertEquals(refMillis + 10 + 10, soc.getCurrentTimeMillis());

		soc.setOffsetMillis(-20);
		assertEquals(refMillis, soc.getCurrentTimeMillis());

		soc.setStopped(true);
		tc.incrementSimulatedMillis(10);
		// test clock now +30
		assertEquals(refMillis, soc.getCurrentTimeMillis());

		soc.setReferenceMillis(refMillis + 1000);
		assertEquals(refMillis + 1000, soc.getReferenceMillis());
		assertEquals(refMillis + 1000, soc.getCurrentTimeMillis());
		assertEquals(1000 - 30, soc.getOffsetMillis());

		soc.setStopped(false);
		tc.incrementSimulatedMillis(10);
		// test clock now +40
		assertEquals(refMillis + 1000 + 10, soc.getCurrentTimeMillis());
    }
}
