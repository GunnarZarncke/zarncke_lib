package de.zarncke.lib.err;

import java.io.IOException;
import java.text.ParseException;

/**
 * An Exception, that can be used to tunnel a non-RuntimeException thru
 * a context, that doesn't throw that Exception normally (as for example
 * happens with white box frameworks).
 * intended use:
 *
 * <pre>
 * class myMap implements Map {
 * 	// ...
 * 	void add(Object o) {
 * 		// won't work:
 * 		//throw new IOException();
 * 		// will work:
 * 		throw new TunnelException(new IOException());
 * 	}
 * 	// ...
 * }
 * try {
 * 	// yes...  makes no sense, just to have an example
 * 	new myMap().add(null);
 * } catch (TunnelException te) {
 * 	throw te.unpack(IndexOutOfBoundsException.class);
 * 	// or: te.unpackIOException();
 * }
 * </pre>
 */
public class TunnelException extends WrapException {
	private static final long serialVersionUID = 1L;

	/**
	 * wrap up the Throwable.
	 * Errors are not wrapped but rethrown
	 *
	 * @param thr != null
	 */
	public TunnelException(final Throwable thr) {
		super(thr);
		if (thr instanceof Error) {
			throw (Error) thr;
		}
	}

	/**
	 * Upacks and throws the payload Exception.
	 *
	 * @throws Throwable thrown
	 */
	public void unpack() throws Throwable { // NOPMD general
		throw getThrowable();
	}

	/**
	 * Unpacks and return the payload Exception. The return should immediately be thrown.
	 * Intended for cases where only one exception is known to be possible (except for unchecked exceptions which are
	 * thrown immediately).
	 * 
	 * @param <T> Exception type
	 * @param expectedException Class of the expected exception
	 * @return Exception thrown
	 */
	public <T extends Exception> T unpack(final Class<T> expectedException) {
		Throwable t = getThrowable();
		if (t instanceof RuntimeException) {
			throw (RuntimeException) t;
		}
		if (t instanceof Error) {
			throw (Error) t;
		}
		if (expectedException.isAssignableFrom(t.getClass())) {
			@SuppressWarnings("unchecked" /* we just checked this manually*/)
			T actual = (T) t;
			return actual; // NOPMD check
		}
		throw new RuntimeException("Some unexpected Exception was packed", t); // NOPMD special
	}

	/**
	 * Feature unpacker: Unpacks and rethrows a contained {@link IOException} - if present.
	 *
	 * @throws IOException if contained
	 */
	public void unpackIOException() throws IOException {
		Throwable t = getThrowable();
		if (t instanceof IOException) {
			throw (IOException) t;
		}
	}

	/**
	 * Feature unpacker: Unpacks and rethrows a contained {@link InterruptedException} - if present.
	 *
	 * @throws InterruptedException if contained
	 */

	public void unpackInterruptedException() throws InterruptedException {
		Throwable t = getThrowable();
		if (t instanceof InterruptedException) {
			throw (InterruptedException) t;
		}
	}

	/**
	 * Feature unpacker: Unpacks and rethrows a contained {@link ParseException} - if present.
	 *
	 * @throws ParseException if contained
	 */
	public void unpackParseException() throws ParseException {
		Throwable t = getThrowable();
		if (t instanceof ParseException) {
			throw (ParseException) t;
		}
	}
}
