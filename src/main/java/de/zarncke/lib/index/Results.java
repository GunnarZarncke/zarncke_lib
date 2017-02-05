package de.zarncke.lib.index;

import java.util.List;

/**
 * Results of a query. May be returned immediately with a size indication, but results may take some time.
 *
 * @author Gunnar Zarncke
 * @param <T> type of results.
 */
public interface Results<T> extends Iterable<T> {
	/**
	 * @return List of T
	 */
	List<T> realize();

	/**
	 * @return actual result size
	 */
	int size();

	/**
	 * Loading data up to the position. Blocks until data is there.
	 *
	 * @param position 0<=position<size
	 * @return maximum position now actually available
	 */
	int readTo(int position);

	/**
	 * @return 0<=available<size(); number of elements already available for #get. readTo() this position will not block.
	 */
	int available();
}
