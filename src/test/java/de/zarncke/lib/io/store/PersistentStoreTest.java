package de.zarncke.lib.io.store;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import de.zarncke.lib.err.GuardedTest;
import de.zarncke.lib.io.IOTools;

public class PersistentStoreTest extends GuardedTest
{
    public void testSimple() throws IOException
    {
        File f = File.createTempFile("test", ".ps");
        f.deleteOnExit();
        PersistentStore ps = PersistentStore.init(f);

        ps.put("test");
        assertEquals("test", ps.get());
		ps.close();
		IOTools.deleteAll(f);
    }

    public void testEmpty() throws IOException
    {
        File f = File.createTempFile("test", ".ps");
        f.deleteOnExit();
        PersistentStore ps = PersistentStore.init(f);

        assertEquals(null, ps.get());
		ps.close();
		IOTools.deleteAll(f);
    }

    public void testDouble() throws IOException
    {
        File f = File.createTempFile("test", ".ps");
        f.deleteOnExit();
        PersistentStore ps = PersistentStore.init(f);

        ps.put("hallo");
        ps.put("test");
        assertEquals("test", ps.get());
		ps.close();
		IOTools.deleteAll(f);
    }

    public void testCleanRecovery() throws IOException
    {
        File f = File.createTempFile("test", ".ps");
        f.deleteOnExit();
        PersistentStore ps = PersistentStore.init(f);

        ps.put("test");
		ps.close();

        ps = PersistentStore.init(f);
        assertEquals("test", ps.get());
		ps.close();
		IOTools.deleteAll(f);
    }

    public void testDirtyRecovery() throws IOException
    {
        File f = File.createTempFile("test", ".ps");
        f.deleteOnExit();
        {
            PersistentStore ps = PersistentStore.init(f);

            ps.put("test");
			ps.close();
        }

        {
            FileOutputStream fos = new FileOutputStream(f, true);
            fos.write("garbage-$%&$".getBytes());
            fos.close();

            PersistentStore ps = PersistentStore.init(f);
            assertEquals("test", ps.get());
			ps.close();
        }
		IOTools.deleteAll(f);
    }
}
