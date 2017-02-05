package de.zarncke.lib.thread;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import de.zarncke.lib.io.IOTools;
import de.zarncke.lib.util.Misc;

/**
 * realizes some kind of system-wide inter-process locking.
 * this is currently VERY beta, it uses mkdir/sleep and keys are
 * plain path/file names.
 * the user is responsible for proper lock/unlock nesting.
 */
public final class Lock
{
	private static final int LOCK_SLEEP_MILLIS = 10;

	private static final Map<String, Lock> LOCKS = new HashMap<String, Lock>();
	private static final File LOCK_DIR;

	static
	{
		File ld = IOTools.getTempDir();

		LOCK_DIR = new File(ld, "locks");
	}

	private final File key;

	private Lock(final String key)
	{
		this.key = new File(LOCK_DIR, key);
		this.key.getParentFile().mkdirs();
	}

	public static synchronized Lock makeLock(final String key)
	{
		Lock l = LOCKS.get(key);
		if (l == null)
		{
			l = new Lock(key);
			LOCKS.put(key, l);
		}
		return l;
	}

	public void lock()
	{
		while (!this.key.mkdir())
		{
			Misc.sleep(LOCK_SLEEP_MILLIS);
		}
	}

	public  void unlock()
	{
		this.key.delete();
	}
}




