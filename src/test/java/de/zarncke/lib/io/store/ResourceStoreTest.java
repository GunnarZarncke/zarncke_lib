package de.zarncke.lib.io.store;

import java.io.IOException;

import de.zarncke.lib.err.GuardedTest;
import de.zarncke.lib.io.IOTools;

public class ResourceStoreTest extends GuardedTest
{
    public void testStore() throws IOException
    {
		Store fs = new ResourceStore(ResourceStoreTest.class);
		Store fc = fs.element("test.txt");

        assertTrue(fc.canRead());
		assertFalse(fc.canWrite());
		assertEquals("test.txt", fc.getName());
		assertEquals("hello world!", new String(IOTools.getAllBytes(fc.getInputStream())));
		assertEquals("hello world!", new String(fc.asRegion().toByteArray()));

		Store ff = fs.element("does not exist");
		assertFalse(ff.canRead());
		assertFalse(ff.canWrite());

    }
}
