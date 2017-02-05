package de.zarncke.lib.lang;

import java.net.URL;

import de.zarncke.lib.err.Warden;

public abstract class InfoClassLoader extends ClassLoader
{
	public InfoClassLoader(final ClassLoader parentClassLoader) {
		super(parentClassLoader);
	}

	abstract byte[] findClassBinary(String name);

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException
    {
        byte[] ba = findClassBinary(name);
        if ( ba == null )
        {
            throw new ClassNotFoundException("cannot find " + name);
        }
        return defineClass(name, ba, 0, ba.length);
    }

	@Override
	public URL getResource(final String name) {
		throw Warden.spot(new UnsupportedOperationException("only getResourceAsStream supported yet."));
	}
}