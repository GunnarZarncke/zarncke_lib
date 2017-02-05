package de.zarncke.lib.cache;

/**
 * Defines a limited form of memory control. Intended for caches.
 * 
 * @author Gunnar Zarncke
 */
public interface MemoryControl {

	/**
	 * Callback by the monitor. Signals a hint to this memory area to do cleanup work to limit size.
	 * May be ignored.
	 */
	void cleanUp();

	/**
	 * Callback by the monitor. Signals a hint to this memory area to dump all data or at least free up much.
	 * May be ignored.
	 *
	 * @return true if done
	 */
	boolean clear();
}
