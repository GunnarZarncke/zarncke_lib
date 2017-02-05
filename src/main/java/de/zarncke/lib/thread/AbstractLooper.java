package de.zarncke.lib.thread;

/**
 * The a basic Runnable, that loops until stopped.
 */
public abstract class AbstractLooper implements Runnable, Stoppable
{
	private boolean stopped = false;

	public void run()
	{
		while (!stopped)
		{
			loop();
		}
	}

	/**
	 * implement your continuous process here.
	 */
	protected abstract void loop();

	public void stop()
	{
		this.stopped = true;
	}
}




