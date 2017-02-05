import de.zarncke.lib.coll.Elements;

public class DummyTest {
	public static void main(final String[] args) {
		int[] a = new int[] { 1, 5, 3 };
		int[] b = new int[] { 3, 9, 3, 3, 6, 5, 7 };

		int d = b.length - a.length + 1;
		int[][] diffs = new int[a.length][];

		for (int aidx = 0; aidx < a.length; aidx++) {
			diffs[aidx] = new int[d];
			for (int bidx = 0; bidx < d; bidx++) {
				diffs[aidx][bidx] = Math.abs(a[aidx] - b[aidx + bidx]);
			}
			System.out.println(Elements.asList(diffs[aidx]));
		}
		System.out.println("------");

		int[] opt = new int[a.length + 1];
		for (int bidx = 0; bidx < b.length; bidx++) {
			int[] opt2 = new int[a.length + 1];
			for (int aidx = 0; aidx < a.length; aidx++) {
				if (bidx < aidx || bidx - aidx >= d) {
					continue;
				}
				if (bidx == aidx) {
					opt2[aidx + 1] = opt[aidx] + diffs[aidx][0];
				} else {
					int diag = diffs[aidx][bidx - aidx];
					opt2[aidx + 1] = Math.min(opt[aidx + 1], opt[aidx] + diag);
				}
			}
			opt = opt2;
			System.out.println(Elements.asList(opt));
		}
		System.out.println(opt[a.length]);
	}
}
