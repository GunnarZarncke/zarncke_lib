package de.zarncke.lib.lang.gen;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import de.zarncke.lib.io.IOTools;
import de.zarncke.lib.lang.ClassTools;

/**
 * loades/creates Classes on demand.
 */
public class LoadingCreator implements ClassBinaryCreator
{
	private File rootDirectory;
	public LoadingCreator(File rootDirectory)
	{
		if (!rootDirectory.isDirectory())
		{
			throw new IllegalArgumentException
				("rootDirectory must be dir!");
		}
		this.rootDirectory = rootDirectory;
	}

	public byte[] createBinary(String className)
		throws ClassNotFoundException
	{
		FileInputStream fis = null;
		try
		{
			fis = new FileInputStream(new File(this.rootDirectory, ClassTools.classNameToFileName(className)));
			return IOTools.getAllBytes(fis);
		}
		catch (java.io.FileNotFoundException fnfe)
		{
			return null;
		}
		catch (IOException ioe)
		{
			throw new ClassNotFoundException
				("Class " + className + " not loaded due to ", ioe);
		}
		finally
		{
			IOTools.forceClose(fis);
		}
	}
		
}

