package de.zarncke.lib.test;

import java.io.IOException;
import java.io.InputStream;

/**
 * provides the bytes of another input stream and fails at all accesses
 * later than a given position.
 */
public class FailingInputStream extends InputStream
{
	private InputStream wrapped;
	private int pos;

	public FailingInputStream(int pos)
	{
		this(pos, null);
	}
	public FailingInputStream(int pos, InputStream ins)
	{
		this.pos = pos;
		this.wrapped = ins;
	}

	public int read()
		throws IOException
	{
		if (this.pos > 0)
		{
			pos--;
			return this.wrapped == null ? 0 : this.wrapped.read();
		}
		throw new IOException("failing as expected.");
	}

	public void close()
		throws IOException
	{
		super.close();
		if (this.wrapped != null)
		{
			this.wrapped.close();
		}
	}

	public String toString()
	{
		return (this.wrapped == null ? "ZeroStream " : wrapped.toString()) + 
			" failing after "+ pos + " bytes";
	}
}

