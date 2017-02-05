package de.zarncke.lib.err;


/**
 * This exception is intended to report an issue but do not cause further handling.
 * It should never be caught but only be logged.
 * Useful mainly together with {@link de.zarncke.lib.log.Log#report(Throwable)}.
 * Compare with {@link ExceptionNotIntendedToBeThrown}.
 */
public class InformationalException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	public InformationalException(final String msg)
	{
		super(msg);
	}

}
