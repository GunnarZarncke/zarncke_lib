package de.zarncke.lib.thread;

/**
 * Distributes a Runnable across a number of Threads and
 * provides blocked start/stop.
 * Please note, that the SAME Runnable is run by the Threads, i.e.
 * it must be written reentrant!
 */
public class MultiThread
{
	private Runnable runnable;
	private Thread[] threads;

	public MultiThread(Runnable runnable, int num)
	{
		this.runnable = runnable;
		this.threads = new Thread[num];
		for (int i = 0; i < num; i++)
		{
			this.threads[i] = new Thread(runnable);
		}
	}

	public void start()
	{
		for (int i = 0; i < this.threads.length; i++)
		{
			this.threads[i].start();
		}
	}

	public void stop()
	{
		if (this.runnable instanceof Stoppable)
		{
			((Stoppable) this.runnable).stop();
		}
		else
		{
			throw new UnsupportedOperationException
				("The Runnable cannot be stopped, because " + 
				 "it does not implement the Stoppable interface.");
		}
	}

}




