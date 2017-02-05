package de.zarncke.lib.coll;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Compare in order until != 0 reached.
 * 
 * @author Gunnar Zarncke
 * @param <T>
 */
public class CompositeComparator<T> implements Comparator<T> {
	// TODO consider replacement by Ordering.compound

	private final List<? extends Comparator<T>> comparators;

	public static <T> CompositeComparator<T> create(final List<? extends Comparator<T>> comparators) {
		return new CompositeComparator<T>(comparators);
	}

	public static <T> CompositeComparator<T> create(final Comparator<T>... comparators) {
		return new CompositeComparator<T>(Arrays.asList(comparators));
	}

	public CompositeComparator(final List<? extends Comparator<T>> comparators) {
		this.comparators = comparators;
	}

	public int compare(final T o1, final T o2) {
		for (Comparator<T> cmp : this.comparators) {
			int res = cmp.compare(o1, o2);
			if (res != 0) {
				return res;
			}
		}
		return 0;
	}

}
