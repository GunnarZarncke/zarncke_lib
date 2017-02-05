package de.zarncke.lib.io.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.zarncke.lib.data.HasData;

/**
 * Something which can possibly be read and written streamwise.
 * 
 * @author Gunnar Zarncke
 */
public interface Accessible extends HasData
{
	/**
	 * Returned by {@link #getSize()} when the size is unknown. ={@value #UNKNOWN_SIZE}.
	 */
	long UNKNOWN_SIZE = -1;

	long getSize();

    boolean canRead();

    InputStream getInputStream() throws IOException;

    boolean canWrite();

	OutputStream getOutputStream(boolean append) throws IOException;
}
