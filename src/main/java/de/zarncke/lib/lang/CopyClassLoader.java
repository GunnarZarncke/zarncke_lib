/**
 *
 */
package de.zarncke.lib.lang;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

import de.zarncke.lib.io.IOTools;

// loads all classes from the parent but sells them as its own (beta)
public class CopyClassLoader extends ClassLoader {
	private final Pattern pattern;

	public CopyClassLoader(final ClassLoader parent, final String pattern) {
		super(parent);
		this.pattern = Pattern.compile(pattern);
	}

	@Override
	protected Class<?> findClass(final String name) throws ClassNotFoundException {
		if (this.pattern.matcher(name).matches()) {
			InputStream ins = getParent().getResourceAsStream(name + ".class");
			if (ins == null) {
				throw new ClassNotFoundException("cannot load " + name + ".class from parent " + getParent());
			}

			byte[] bytes;
			try {
				bytes = IOTools.getAllBytes(ins);
			} catch (IOException e) {
				throw new ClassNotFoundException("cannot load " + name + ".class from parent " + getParent(), e);
			}
			// TODO set protection domain
			defineClass(name, bytes, 0, bytes.length);
		}
		return super.findClass(name);
	}
}