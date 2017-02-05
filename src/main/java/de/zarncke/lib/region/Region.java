package de.zarncke.lib.region;

import java.util.Comparator;

/**
 * A Region is (a view onto) a sequence of bytes.
 */
public interface Region
{
	Region EMPTY = new EmptyRegion();

	Comparator<Region> LEXICOGRAPHICALLY = new Comparator<Region>() {
		@Override
		public int compare(final Region o1, final Region o2) {
			long l1 = o1.length();
			long l2 = o2.length();
			long l = Math.min(l1, l2);
			// TODO get chunks of bytes to compare with sub regions.toByteArray
			for (long i = 0; i < l; i++) {
				byte b1 = o1.get(i);
				byte b2 = o2.get(i);
				if (b1 < b2) {
					return -1;
				}
				if (b1 > b2) {
					return 1;
				}
			}
			if (l1 < l2) {
				return -1;
			}
			if (l1 > l2) {
				return 1;
			}
			return 0;
		}
	};

	/**
	 * A View is a view of a Region where parts can be replaced yielding the modified base Region. Replacements of a sub
	 * View of a View will still modify the original base Region. Use {@link #realize()} if you want to modify (a copy
	 * of) the content of a view.
	 */
	interface View extends Region
	{
		/**
		 * @param replaceData != null to replace the viewed region with
		 * @return a Region where the full extent of this View has been replaced by the given region
		 */
		Region replace(Region replaceData);
	}

	long length();

	/**
	 * @return all the bytes visible in this Region
	 * @throws ArrayIndexOutOfBoundsException
	 *             if the region is too large to fit within a byte array
	 */
	byte[] toByteArray();

	/**
	 * Fetch a single byte. May be comparatively slow on complex implementations. Consider fetch blocks with
	 * {@link #select(long, long)}.{@link #toByteArray()}.
	 *
	 * @param index
	 *            must be within bounds (>=0, <length)
	 * @return byte at that index
	 */
	byte get(long index);

	/**
	 * @param startOffset in this View relative to its start
	 * @param len length of the selection
	 * @return a view of the selected region within this; a replace() will yield the full region
	 */
	View select(long startOffset, long len);

	/**
	 * Captures the extent of this Region.
	 * Returns a view which contains the visible data of this Region.
	 * <ul>
	 * <li>In the base case the Region itself,</li>
	 * <li>in case of Views only the viewed Region and</li>
	 * <li>in case of changing/temporary data a copy of the data.</li>
	 * </ul>
	 * No further changes to this view will be seen by the returned view
	 * (except possibly of course if you insert the result in this region again).
	 *
	 * @return a Region
	 */
	Region realize();

}
