package de.zarncke.lib.diff;

/**
 * A difference between two elements.
 * Should have a human readable toString method.
 * 
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
public interface Delta {
	class AbstractDelte implements Delta {

		@Override
		public boolean isAddition() {
			return false;
		}

		@Override
		public boolean isRemoval() {
			return false;
		}

		@Override
		public double getDeltaSize() {
			return 0.0;
		}

	}

	/**
	 * Modifications count as removal <em>and</em> addition.
	 *
	 * @return true if this delta consists of something added compared to the first element
	 */
	boolean isAddition();

	/**
	 * @return true if this delta consists of something removed compared to the first element
	 */
	boolean isRemoval();

	/**
	 * @return the size of the difference in units of {@link Diff#addSizeA(double)}, may be 0 or NaN
	 */
	double getDeltaSize();
}