package de.zarncke.lib.diff;

import java.io.IOException;

import de.zarncke.lib.io.store.Store;

/**
 * Allows to plug in specific content comparators.
 *
 * @author Gunnar Zarncke
 */
public interface ContentComparer {
	ContentComparer BINARY = new ContentComparer() {
		@Override
		public void compareContent(final Store a, final Store b, final String path, final Diff differences)
				throws IOException {
			DiffUtil.compareContent(a, b, path, differences);
		}

		@Override
		public void removed(final Store a, final String path, final Diff differences) throws IOException {
			DiffUtil.addSizeRemoveDifference(a, path, differences);
		}


		@Override
		public void add(final Store b, final String path, final Diff differences) throws IOException {
			DiffUtil.addSizeAddDifference(b, path, differences);
		}
	};
	ContentComparer SUPERFICIAL = new ContentComparer() {
		@Override
		public void compareContent(final Store a, final Store b, final String path, final Diff differences) {
			differences.addSizeA(a.getSize());
			differences.addSizeB(b.getSize());
			if (a.getSize() != b.getSize()) {
				differences.add(new Delta() {
					@Override
					public boolean isAddition() {
						return true;
					}

					@Override
					public boolean isRemoval() {
						return true;
					}

					@Override
					public String toString() {
						return path + a.getName() + " size differs " + a.getSize() + "!=" + b.getSize();
					}

					@Override
					public double getDeltaSize() {
						return Math.abs(a.getSize() - b.getSize());
					}
				});
			}

			if (a.getLastModified() != b.getLastModified()) {
				differences.add(new Delta() {
					@Override
					public boolean isAddition() {
						return true;
					}

					@Override
					public boolean isRemoval() {
						return true;
					}

					@Override
					public String toString() {
						return path + a.getName() + " size modification date differs " + a.getLastModified() + "!="
								+ b.getLastModified();
					}

					@Override
					public double getDeltaSize() {
						return 0;
					}
				});
			}
		}

		@Override
		public void removed(final Store a, final String path, final Diff differences) throws IOException {
			DiffUtil.addSizeRemoveDifference(a, path, differences);
		}


		@Override
		public void add(final Store b, final String path, final Diff differences) throws IOException {
			DiffUtil.addSizeAddDifference(b, path, differences);
		}
	};

	/**
	 * @param a one Store
	 * @param b corresponding Store in other location
	 * @param path relative to base location
	 * @param differences to accumulate difference(s) in
	 * @throws IOException if comparison failed (because data couldn't be read)
	 */
	void compareContent(Store a, Store b, String path, Diff differences) throws IOException;

	/**
	 * Store is present in first but not present in second source.
	 *
	 * @param a one Store
	 * @param path relative to base location
	 * @param differences to accumulate difference(s) in
	 * @throws IOException if comparison failed (because data couldn't be read)
	 */
	void removed(Store a, String path, Diff differences) throws IOException;

	/**
	 * Store is not present in first but present in second source.
	 *
	 * @param b one Store
	 * @param path relative to base location
	 * @param differences to accumulate difference(s) in
	 * @throws IOException if comparison failed (because data couldn't be read)
	 */
	void add(Store b, String path, Diff differences) throws IOException;

}