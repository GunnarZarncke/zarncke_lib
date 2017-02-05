package de.zarncke.lib.progress;

/**
 * Indicates progress, both relative and absolute.
 */
public interface Progress {
	/**
	 * @return number of items processed so far, always <= {@link #getTotal()}
	 */
	int getCount();

	/**
	 * @return total number of items to process
	 */
	int getTotal();

	/**
	 * @return already processed data volume in bytes
	 */
	long getVolume();

	/**
	 * @return total data volume to process in bytes
	 */
	long getTotalVolume();

	/**
	 * @return current duration in millis
	 */
	long getDuration();

	/**
	 * @return total duration in millis (known or expected)
	 */
	long getTotalDuration();
}
