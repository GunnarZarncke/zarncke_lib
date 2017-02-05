// Copyright (C) 2003 Gunnar Zarncke
// This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License version 2.1 as published by the Free Software Foundation. This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details. You should have received a copy of the GNU Lesser General Public License along with this library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA.

package de.zarncke.lib.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** 
 * A FileOutputStream that counts the bytes written and handles 
 * {link #flush} as expected, i.e. forces all buffers to synchronize 
 * with the underlying device.
 */
public final class ByteCountStream extends FileOutputStream 
{
	/**
	 * counter for the bytes written so far
	 */
    private long bytesWritten;

    public ByteCountStream(File file) throws IOException 
	{
		super(file);
	}

    public void flush() throws IOException 
	{
		/* 
		 * "The flush method of OutputStream does nothing." 
		 * - JDK1.3 API documentation. 
		 * calling it just to be correct.
		 */
        super.flush();   

		// instead flush by fd:
		// "Force all system buffers to synchronize with the underlying device"
		// - JDK1.3 API documentation. 
        getFD().sync();  
    }

    public void write(byte[] b) throws IOException 
	{
		super.write(b);
        bytesWritten += b.length;
	}

	public void write(byte[] b, int off, int len) throws IOException 
	{
      	super.write(b, off, len);
      	bytesWritten += len;
	}

    public void write(int b) throws IOException 
	{
		super.write(b);
        ++bytesWritten;
	}

	/**
	 * get the number of bytes written so far.
	 * @return long
	 */
    public long getBytesWritten() 
	{
        return bytesWritten;
	}

}
