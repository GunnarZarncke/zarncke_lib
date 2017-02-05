package de.zarncke.lib.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import de.zarncke.lib.err.Warden;
import de.zarncke.lib.util.Misc;

/**
 * Convenience class for dealing with single socket connections.
 * Open a socket, write a query and process the response.
 * This class may be overridden to provide other queries and process the response differently (default is to read nothign and
 * return null).
 *
 * @author Gunnar Zarncke
 * @param <T> type of result
 */
public class SocketCommunicator<T> {

	/**
	 * Wrapped around an original {@link IOException cause} during sending.
	 * May indicate that the other end is not available.
	 */
	public static class SendIoException extends IOException {
		private static final long serialVersionUID = 1L;

		public SendIoException(final String msg, final IOException e) {
			super(msg, e);
		}
	}

	/**
	 * Wrapped around an original {@link IOException cause} during receiving.
	 * May indicate that the other end failed.
	 */
	public static class ReceiveIoException extends IOException {
		private static final long serialVersionUID = 1L;

		public ReceiveIoException(final String msg, final IOException e) {
			super(msg, e);
		}
	}

	private static final int CONNECT_TIMEOUT_MS = 60 * 1000;
	private final String query;

	/**
	 * @param query to send != null may or may not have a newline
	 */
	public SocketCommunicator(final String query) {
		this.query = query;
	}

	/**
	 * Return immediately.
	 *
	 * @param inputStream
	 * @return null always
	 * @throws IOException on read errors
	 */
	public T process(final InputStream inputStream) throws IOException {
		return null;
	}

	public static String readLine(final InputStream inputStream) throws IOException {
		LineNumberReader lineNumberReader = new LineNumberReader(new InputStreamReader(inputStream));
		return lineNumberReader.readLine();
	}

	/**
	 * Send bytes to the socket.
	 * Sending defaults to the query converted to UTF-8.
	 *
	 * @param outputStream != null
	 * @throws IOException may be thrown
	 */
	public void writeQuery(final OutputStream outputStream) throws IOException {
		outputStream.write(this.query.getBytes(Misc.UTF_8));
	}

	/**
	 * Sends a request to the given tcp port and processes the result with the given processor.
	 *
	 * @param <T> type of result
	 * @param host != null
	 * @param port valid tcp port
	 * @param processor != null,
	 * @return as the processor returns
	 * @throws IOException
	 */
	public static <T> T sendRequest(final String host, final int port, final SocketCommunicator<T> processor)
			throws IOException {
		Socket socket = new Socket();
		InputStream inputStream = null;
		OutputStream outputStream = null;
		OutputStream bufferedStream = null;
		IOException writeEx = null;
		T result = null;
		try {
			try {
				SocketAddress addr = new InetSocketAddress(host, port);
				socket.connect(addr, CONNECT_TIMEOUT_MS);
				outputStream = socket.getOutputStream();
			} catch (IOException e) {
				throw Warden.spot(new SendIoException("cannot open connection to target " + host + ":" + port, e));
			}
			try {
				try {
					bufferedStream = new BufferedOutputStream(outputStream);
					processor.writeQuery(bufferedStream);
					bufferedStream.flush();
				} catch (IOException e) {
					writeEx = e;
				}

				inputStream = socket.getInputStream();
				BufferedInputStream bufIn = new BufferedInputStream(inputStream);
				result = processor.process(bufIn);
				if (writeEx != null) {
					throw Warden.spot(new ReceiveIoException("IO problem during sending, but received " + result, writeEx));
				}
				return result;
			} catch (IOException e) {
				if (writeEx != null) {
					Warden.disregardAndReport(e);
					throw Warden.spot(new SendIoException("cannot send data to target; IO problem; but got " + result, e));
				}
				throw Warden.spot(new ReceiveIoException("cannot receive response; IO problem", e));
			}
		} finally {
			IOTools.forceClose(outputStream);
			IOTools.forceClose(inputStream);
			IOTools.forceClose(socket);
		}
	}

}
