package de.zarncke.lib.io.store;

import java.io.File;
import java.io.IOException;

import de.zarncke.lib.err.GuardedTest;
import de.zarncke.lib.io.IOTools;

public class FileStoreTest extends GuardedTest
{
    public void testStore() throws IOException
    {
        File f = File.createTempFile("test", ".store");
        File fp = f.getParentFile();
        f.deleteOnExit();
        Store fs = new FileStore(fp);
        Store fc = fs.element(f.getName());
        assertTrue(fc.canRead());
		IOTools.deleteAll(f);
    }
}
