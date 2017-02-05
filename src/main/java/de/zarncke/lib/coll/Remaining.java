package de.zarncke.lib.coll;

/**
 * Indicates the remaining size of a stream or comparable object.
 *
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
public interface Remaining {
	long UNKNOWN = -1;

	/**
	 * @return number of remaining elements or {@link #UNKNOWN}
	 */
	long available();
}
