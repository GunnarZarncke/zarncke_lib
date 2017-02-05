package de.zarncke.lib.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * Extends {@link SocketCommunicator} with a single line read.
 *
 * @author Gunnar Zarncke
 */
public class LineReadSocketCommunicator extends SocketCommunicator<String> {

	public LineReadSocketCommunicator(final String query) {
		super(query);
	}

	/**
	 * Read one line.
	 * 
	 * @param inputStream
	 * @return the read line
	 * @throws IOException on read errors
	 */
	@Override
	public String process(final InputStream inputStream) throws IOException {
		return readLine(inputStream);
	}
}
