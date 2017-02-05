package de.zarncke.lib.lang.init;

import de.zarncke.lib.err.GuardedTest;
import de.zarncke.lib.init.Initializer;
import de.zarncke.lib.init.Initializer.Interest;

public class InitializerTest extends GuardedTest
{
    interface T
    {
		//
    }

    static class S implements T
    {
		//
    }

    public void testInitSimpleFail()
    {
        Initializer init = new Initializer();
        Interest<T> ti = init.signalInterestInImplementation(T.class);
        assertFalse(ti.isResolved());
        try
        {
            init.completeInitialization();
            fail("may not resolve");
        }
        catch ( IllegalStateException e )
        {
            // OK
        }
    }

    public void testInitSimpleOk()
    {
        Initializer init = new Initializer();
        Interest<T> ti = init.signalInterestInImplementation(T.class);
        init.registerImplementation(S.class);
        assertFalse(ti.isResolved());
        init.completeInitialization();
        assertTrue(ti.isResolved());
        assertSame(S.class, ti.getImplementation());
    }

    public void testInitAsync() throws InterruptedException
    {
        Initializer init = new Initializer();
        final Interest<T> ti = init.signalInterestInImplementation(T.class);
        init.registerImplementation(S.class);
        assertFalse(ti.isResolved());
        new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    ti.waitForResolution();
                }
                catch ( InterruptedException e )
                {
					throw new RuntimeException("interupted", e); // NOPMD
                }
                assertTrue(ti.isResolved());
                assertSame(S.class, ti.getImplementation());
            }
        }).start();
        Thread.sleep(10);
        init.completeInitialization();
        assertTrue(ti.isResolved());
        assertSame(S.class, ti.getImplementation());
    }
}
