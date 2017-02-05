package de.zarncke.lib.io.store;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import de.zarncke.lib.err.GuardedTest;
import de.zarncke.lib.io.IOTools;
import de.zarncke.lib.io.store.AbstractReadOnlyJar.Directory;
import de.zarncke.lib.log.Log;

public class RegionJarTest extends GuardedTest
{
    public static void testJar() throws IOException
    {
        File file = File.createTempFile("test", ".jar");
        file.deleteOnExit();
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(file));
        ZipEntry ze = new ZipEntry("test.txt");
        jos.putNextEntry(ze);
        jos.write("hello world".getBytes("ASCII"));
        jos.close();

        // read
        RegionJar myJar = new RegionJar();
        myJar.init(file);
        Directory directory = myJar.getCentralDirectory();

		Log.LOG.get().report(directory.getEntries());
		for (AbstractReadOnlyJar.Entry e : directory.getEntries())
        {
            if ( !e.getName().endsWith("/") )
            {
				Log.LOG.get().report(e.getName() + "\n" + new String(IOTools.getAllBytes(e.getInputStream())));
            }
        }

    }
}
