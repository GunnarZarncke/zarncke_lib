package de.zarncke.lib.index;

/**
 * Tools for indizes.
 * 
 * @author Gunnar Zarncke
 */
public final class IndexUtil {
	private IndexUtil() {
		// helper
	}

	public static <T> Index<T> makeConservative(final Index<T> index) {
		return new Index.DelegateIndex<T>(index) {
			@Override
			public boolean isConservativeEstimate() {
				return true;
			}
		};
	}

}
