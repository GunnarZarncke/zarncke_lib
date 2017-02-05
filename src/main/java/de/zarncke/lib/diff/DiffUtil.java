package de.zarncke.lib.diff;

import java.io.IOException;

import de.zarncke.lib.coll.Elements;
import de.zarncke.lib.io.IOTools;
import de.zarncke.lib.io.store.Store;

/**
 * Helper for comparing content with {@link Diff} objects.
 * 
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
public final class DiffUtil {
	private DiffUtil() {
		// hidden constructor of helper class
	}

	public static void compareContent(final Store a, final Store b, final String path, final Diff differences)
			throws IOException {
		differences.addSizeA(a.getSize());
		differences.addSizeB(b.getSize());
		final byte[] ab = IOTools.getAllBytes(a.getInputStream());
		final byte[] bb = IOTools.getAllBytes(b.getInputStream());
		if (!Elements.arrayequals(ab, bb)) {
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
					return path + a.getName() + " " + Elements.byteArrayToHumanReadable(ab) + "!="
							+ Elements.byteArrayToHumanReadable(bb);
				}

				@Override
				public double getDeltaSize() {
					final int commonPrefix = Elements.findCommonPrefix(ab, bb);
					final int commonSuffix = Elements.findCommonSuffix(ab, bb);

					final int maxLength = Math.max(ab.length, bb.length);
					if (commonPrefix + commonSuffix > maxLength) {
						return maxLength - Math.max(commonPrefix, commonSuffix);
					}
					return maxLength - commonPrefix - commonSuffix;
				}
			});
		}
	}

	public static void addSizeRemoveDifference(final Store a, final String path, final Diff differences) {
		differences.addSizeA(a.getSize());
		differences.add(new Delta() {
			@Override
			public boolean isAddition() {
				return true;
			}

			@Override
			public boolean isRemoval() {
				return false;
			}

			@Override
			public String toString() {
				return path + a.getName() + "-";
			}

			@Override
			public double getDeltaSize() {
				return a.getSize();
			}
		});
	}

	public static void addSizeAddDifference(final Store b, final String path, final Diff differences) {
		differences.addSizeB(b.getSize());
		differences.add(new Delta() {
			@Override
			public boolean isAddition() {
				return false;
			}

			@Override
			public boolean isRemoval() {
				return true;
			}

			@Override
			public String toString() {
				return path + b.getName() + "+";
			}

			@Override
			public double getDeltaSize() {
				return b.getSize();
			}
		});
	}

}
