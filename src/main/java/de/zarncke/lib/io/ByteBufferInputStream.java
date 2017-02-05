package de.zarncke.lib.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class ByteBufferInputStream extends InputStream
{
    private ByteBuffer buffer;

    /**
     * @param buffer
     *            is {@link ByteBuffer#duplicate() duplicated}
     */
    public ByteBufferInputStream(final ByteBuffer buffer)
    {
        this.buffer = buffer.duplicate();
    }

    @Override
    public boolean markSupported()
    {
        return true;
    }

    @Override
    public synchronized void mark(final int readlimit)
    {
        this.buffer.mark();
    }

    @Override
    public synchronized void reset() throws IOException
    {
        checkNull();
        this.buffer.reset();
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException
    {
		int count = Math.min(this.buffer.remaining(), len);
		if (count == 0) {
			return -1;
		}
		this.buffer.get(b, off, count);
		return count;
    }

    @Override
    public long skip(final long n) throws IOException
    {
        if ( n <= 0 )
        {
            return 0;
        }
        if ( n <= this.buffer.remaining() )
        {
            this.buffer.position(this.buffer.position() + (int) n);
            return n;
        }
        int m = this.buffer.remaining();
        this.buffer.position(this.buffer.limit());
        return m;
    }

    @Override
    public int read() throws IOException
    {
        try
        {
            return this.buffer.get();
        }
        catch ( BufferUnderflowException e )
        {
            return -1;
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
        checkNull();
        return this.buffer.remaining();
    }

    private void checkNull() throws IOException
    {
        if ( this.buffer == null )
        {
            throw new IOException("InputStream has already been closed.");
        }
    }

    @Override
    public int hashCode()
    {
        return this.buffer == null ? 101 : this.buffer.hashCode();
    }

    @Override
    public boolean equals(final Object obj)
    {
        if ( obj instanceof ByteBufferInputStream )
        {
            return this.buffer.equals(((ByteBufferInputStream) obj).buffer);
        }
        return false;
    }

    @Override
    public String toString()
    {
		return this.buffer == null ? "<closed>" : this.buffer.toString();
    }

    @Override
    public void close() throws IOException
    {
        super.close();
        this.buffer = null;
    }
}
