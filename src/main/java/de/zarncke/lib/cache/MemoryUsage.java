package de.zarncke.lib.cache;

/**
 * Describes a large part of memory used for storage of objects.
 * Intended for monitoring this part.
 * Examples: Medium to large caches, queues and IO buffers.
 * May be associated with a limited form of control: {@link MemoryControl}.
 * Results are aggregated by a {@link MemoryMonitor} where instances may register.<br/>
 * Implementers must implement {@link #hashCode()} and {@link #equals(Object)} such that the monitor may distinuish separate
 * instances properly.
 *
 * @author Gunnar Zarncke
 */
public interface MemoryUsage {

	/**
	 * @return a human readable name of the cache for the control UI
	 */
	String getMemoryName();

	/**
	 * @return an estimate, possibly rough of the number of bytes allocated by this case. Dont't spend too much time in
	 * calculating an exact value. May be <0 to indicate that the object based methods are used to caluclate the guess.
	 */
	long getAllocatedBytes();

	/**
	 * Returns an estimate of the current number of objects stored in this memory area.
	 * The intention is to count semantic entities to give a high level view.
	 * So {@link java.util.Map#size()} is a good candidate. Don't count all the Map.Entry objects too.
	 *
	 * @return number of entities
	 */
	int getAllocatedObjects();

	/**
	 * @return number of bytes per typical object
	 */
	int getTypicalObjectSize();

	/**
	 * @return number of bytes per object maximum; may be an educated guess; may be -1 to indicate no upper limit known
	 */
	int getMaxObjectSize();

	/**
	 * This is used to estimate the delay incurred by opening a connection to a port in the same subnet.
	 * 
	 * @return typical number of opened local network connections per stored object
	 */
	double getSavedLocalNetworkCallsPerObject();

	/**
	 * This is used to estimate the delay incurred by opening a connection to a port in the internet.
	 * 
	 * @return typical number of opened network connections per stored object
	 */
	double getSavedNetworkCallsPerObject();

	/**
	 * This is used to estimate the delay incurred by transferring bytes over the local network.
	 * 
	 * @return typical number of bytes accessed over the network per stored object
	 */
	long getSavedNetworkBytesPerObject();

	/**
	 * This is used to estimate the delay incurred by hard disk seeks.
	 * 
	 * @return typical number of accessed to distinct areas on disk per stored object
	 */
	double getSavedDiskSeeksPerObject();

	/**
	 * This is used to estimate the delay incurred by transferring bytes from disk.
	 * 
	 * @return typical number of accessed bytes on disk per stored object
	 */
	long getSavedDiskBytesPerObject();

	/**
	 * This is used to estimate the processor time incurred by not having the object available.
	 * Note: BOPs are estimated
	 * 
	 * @return typical number of BOPs saved per present object
	 */
	double getSavedBopsPerObject();
}
