package de.zarncke.lib.util;

/**
 * Indicates that an object has certain possibly high memory demands.
 * Intended for monitoring an overall system composed of large individual pieces.
 *
 * @author Gunnar Zarncke
 */
public interface MemoryRequirements {
	/**
	 * @return bytes in RAM expected to be needed by this object (including all external dependencies)
	 */
	long getRequiredRamBytes();

	/**
	 * @return bytes in DISK expected to be needed by this object (including all external dependencies)
	 */
	long getRequiredDiskBytes();
}
