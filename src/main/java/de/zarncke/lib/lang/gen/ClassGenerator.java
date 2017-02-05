package de.zarncke.lib.lang.gen;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.zarncke.lib.log.Log;
import de.zarncke.lib.util.Chars;

/**
 * a command line tool to generate classes with th GenericityFactory.
 */
public class ClassGenerator
{
	public static void main(final String[] args)
	{
		String path = ".";
		List targets = new ArrayList();
		for (int i = 0; i < args.length; i++)
		{
			String arg = args[i];
			if (arg.equals("-d"))
			{
				path = args[++i];
			}
			else
			{
				targets.add(Chars.replaceAll(arg, "/", "."));
			}
		}

		Log.LOG.get().report("string generated classes into " + path);

		DemandClassLoader loader = new DemandClassLoader(new File(path));
		GenericityFactory gf = new GenericityFactory(loader);
		loader.registerCreator(gf);

		for (Iterator it = targets.iterator(); it.hasNext();)
		{
			String name = (String) it.next();
			Log.LOG.get().report("loading " + name);
			Class c;
			try
			{
				c = Class.forName(name);
			}
			catch (Exception ex)
			{
				Log.LOG.get().report(ex);
				Log.LOG.get().report("cannot find " + name + " due to exception (skipped).");
				continue;
			}

			Log.LOG.get().report("generating processor for " + name);
			Class pc;
			try
			{
				pc = gf.createProcessor(c);
			}
			catch (Exception ex)
			{
				Log.LOG.get().report(ex);
				Log.LOG.get().report("cannot generate processor for " + name + " due to exception (skipped)");
				continue;
			}
			Log.LOG.get().report("successfully generated " + pc.getName());
		}

	}
}
