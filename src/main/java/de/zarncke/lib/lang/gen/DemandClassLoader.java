package de.zarncke.lib.lang.gen;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.zarncke.lib.io.IOTools;
import de.zarncke.lib.lang.ClassTools;

/**
 * loads/creates Classes on demand.
 */
public class DemandClassLoader extends ClassLoader {
	private File rootDirectory;

	private final List creators = new ArrayList();

	private final Map classes = new HashMap();

	public DemandClassLoader(final File rootDirectory) {
		init(rootDirectory);
	}

	public DemandClassLoader(final File rootDirectory, final ClassLoader parent) {
		super(parent);
		init(rootDirectory);
	}

	private void init(final File rootDirectory) {
		if (!rootDirectory.exists()) {
			rootDirectory.mkdirs();
		}
		if (!rootDirectory.isDirectory()) {
			throw new IllegalArgumentException("rootDirectory " + rootDirectory + "must be dir!");
		}
		this.rootDirectory = rootDirectory;

		registerCreator(new LoadingCreator(rootDirectory));
	}

	public final void registerCreator(final ClassBinaryCreator creator) {
		this.creators.add(creator);
	}

	/**
	 * @return true if newly stored, false if already present (not stored)
	 */
	private boolean storeBinary(final String name, final byte[] data) throws IOException {
		FileOutputStream fos = null;
		try {
			File file = new File(this.rootDirectory, ClassTools.classNameToFileName(name));
			if (file.exists()) {
				return false;
			}
			file.getParentFile().mkdirs();
			fos = new FileOutputStream(file);
			fos.write(data);
			fos.flush();
		} finally {
			IOTools.forceClose(fos);
		}
		return true;
	}

	@Override
	public Class findClass(final String name) throws ClassNotFoundException {
		Class cl = (Class) this.classes.get(name);
		if (cl != null) {
			return cl;
		}

		for (Iterator it = this.creators.iterator(); it.hasNext();) {
			byte[] bs = ((ClassBinaryCreator) it.next()).createBinary(name);
			if (bs != null) {
				try {
					storeBinary(name, bs);
				} catch (IOException ioe) {
					throw new ClassNotFoundException("cannot store generated class binary " + name + " due to " + ioe);
				}
				try {
					cl = super.defineClass(name, bs, 0, bs.length);
				} catch (Throwable error) { // NOPMD
					throw new ClassNotFoundException("cannot create class " + name + " due to generation error ", error);
				}
				this.classes.put(name, cl);
				return cl;
			}
		}

		throw new ClassNotFoundException("Class " + name + " cannot be created on demand.");
	}

	@Override
	public String toString() {
		return "DemandClassLoader with creators " + this.creators;
	}

}
