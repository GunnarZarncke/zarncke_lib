package de.zarncke.lib.err;

/**
 * An Exception, that wraps another Exception (or a Throwable to be precise).
 * Note: This differs from chaining exceptions where the given exception is the cause.
 * Here the exception is a payload.
 */
public abstract class WrapException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	/**
	 * the wrapped Exception
	 */
	private Throwable wrapped = null;

	public WrapException()
	{
		super();
	}

	public WrapException(final String msg)
	{
		super(msg);
	}

	/**
	 * create a WrapException
	 * @param wrapped is the Throwable to wrap
	 */
	public WrapException(final Throwable wrapped)
	{
		super();
		this.wrapped = wrapped;
	}

	/**
	 * create a WrapException
	 *
	 * @param msg to attach
	 * @param wrapped the Throwable to wrap != null
	 */
	public WrapException(final String msg, final Throwable wrapped)
	{
		super(msg);
		this.wrapped = wrapped;
	}

	/**
	 * extract the wrapped Throwable (may be null)
	 *
	 * @return {@link Throwable} != null
	 */
	public Throwable getThrowable()
	{
		return this.wrapped;
	}

	/**
	 * Usual to String Method
	 *
	 * @return A String represenatation
	 */
	@Override
	public String toString()
	{
		StringBuffer buf = new StringBuffer(super.toString());

		if (this.wrapped != null && this.wrapped.getMessage() != null)
		{
			buf.append(" ( ").append(this.wrapped.getMessage()).append(" ).");
		}

		return buf.toString();
	}

	/**
	 * Prints this <code>Throwable</code> and its backtrace to the
	 * specified print stream.
	 */
	@Override
	public void printStackTrace(final java.io.PrintStream s)
	{
		synchronized (s)
		{
			super.printStackTrace(s);

			if (this.wrapped != null && this.wrapped != this)
			{
				this.wrapped.printStackTrace(s);
			}
		}
	}


	/**
	 * Prints this <code>Throwable</code> and its backtrace to the specified
	 * print writer.
	 *
	 * @since   JDK1.1
	 */
	@Override
	public void printStackTrace(final java.io.PrintWriter s)
	{
		synchronized (s)
		{
			super.printStackTrace(s);

			if (this.wrapped != null && this.wrapped != this)
			{
				this.wrapped.printStackTrace(s);
			}
		}
	}

}
