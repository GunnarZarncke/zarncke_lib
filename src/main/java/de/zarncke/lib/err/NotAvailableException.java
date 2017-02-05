package de.zarncke.lib.err;

/**
 * This exception should be thrown if a requested Resource is not available for whatever reasons in general.
 */
public class NotAvailableException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	public NotAvailableException()
	{
		super();
	}

	public NotAvailableException(final String msg)
	{
		super(msg);
	}

	public NotAvailableException(final Throwable wrapped)
	{
		super(wrapped);
	}

	public NotAvailableException(final String msg, final Throwable wrapped)
	{
		super(msg, wrapped);
	}

}
