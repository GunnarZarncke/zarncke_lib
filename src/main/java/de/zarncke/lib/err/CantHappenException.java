package de.zarncke.lib.err;

/**
 * An Exception, that indicates a case/state, that should not happen
 * according to specification/expectation, but did occur nonetheless.
 */
public class CantHappenException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	public CantHappenException()
	{
		super();
	}

	public CantHappenException(final String msg)
	{
		super(msg);
	}

	public CantHappenException(final Throwable thr)
	{
		super(thr);
	}

	public CantHappenException(final String msg, final Throwable thr)
	{
		super(msg, thr);
	}

}
