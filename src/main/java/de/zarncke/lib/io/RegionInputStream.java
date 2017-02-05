package de.zarncke.lib.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import de.zarncke.lib.region.Region;

public class RegionInputStream extends InputStream
{
	private Region buffer;

	private long mark;

	private long pos;

	/**
	 * @param buffer
	 *            is {@link ByteBuffer#duplicate() duplicated}
	 */
	public RegionInputStream(final Region buffer)
	{
		this.buffer = buffer;
	}

	@Override
	public boolean markSupported()
	{
		return true;
	}

	@Override
	public synchronized void mark(final int readlimit)
	{
		this.mark = this.pos;
	}

	@Override
	public synchronized void reset() throws IOException
	{
		checkNull();
		this.pos = this.mark;
	}

	@Override
	public int read(final byte[] b, final int off, final int len) throws IOException
	{
		if ( remaining() <= 0 )
		{
			return -1;
		}

		int selLen = (int) Math.min(len, remaining());
		byte[] sel = this.buffer.select(this.pos, selLen).toByteArray();
		System.arraycopy(sel, 0, b, off, selLen);
		this.pos += sel.length;
		return sel.length;
	}

	@Override
	public long skip(final long n) throws IOException
	{
		if ( n <= 0 )
		{
			return 0;
		}
		long m = remaining();
		if (n <= m)
		{
			this.pos += n;
			return n;
		}
		this.pos = this.buffer.length();
		return m;
	}

	@Override
	public int read() throws IOException
	{
		if (remaining() <= 0) {
			return -1;
		}
		try
		{
			return this.buffer.get(this.pos++) & 0xff;
		}
		catch ( NullPointerException e )
		{
			checkNull();
			throw e;
		}
	}

	@Override
	public int available() throws IOException
	{
		long l = remaining();
		return l > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) l;
	}

	private long remaining() throws IOException
	{
		checkNull();
		return this.buffer.length() - this.pos;
	}

	private void checkNull() throws IOException
	{
		if ( this.buffer == null )
		{
			throw new IOException("InputStream has already been closed.");
		}
	}

	@Override
	public String toString()
	{
		return this.buffer == null ? "<InputStream has already been closed>" : this.buffer.toString();
	}

	@Override
	public void close() throws IOException
	{
		super.close();
		this.buffer = null;
	}
}
