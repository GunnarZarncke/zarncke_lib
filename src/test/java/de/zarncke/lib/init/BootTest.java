package de.zarncke.lib.init;

import java.io.File;
import java.util.jar.Manifest;

import de.zarncke.lib.err.GuardedTest;
import de.zarncke.lib.io.IOTools;
import de.zarncke.lib.io.store.FileStore;
import de.zarncke.lib.io.store.Store;
import de.zarncke.lib.io.store.StoreUtil;

public class BootTest extends GuardedTest
{
    public void testSimpleBoot() throws Throwable
    {
        File dir = IOTools.createTempDir("boot-test");
		IOTools.deleteOnExit(dir);
        Store testStore = new FileStore(dir);

        Manifest m = new Manifest(BootTest.class.getResourceAsStream("test-MANIFEST.MF"));
		m.write(testStore.element("META-INF").element("MANIFEST.MF").getOutputStream(false));

		Store bootTestClass = StoreUtil.resolvePath(testStore, BootDummy.class.getName().replaceAll("\\.", "/") + ".class", "/");
		IOTools.copy(BootTest.class.getResourceAsStream("BootDummy.class"), bootTestClass.getOutputStream(false));

		IOTools.copy(BootTest.class.getResourceAsStream("test.zip"), testStore.element("test.zip").getOutputStream(false));

        Boot b = new Boot(testStore);
		b.boot(testStore, true, ClassLoader.getSystemClassLoader());
		IOTools.deleteAll(dir);
    }
}
