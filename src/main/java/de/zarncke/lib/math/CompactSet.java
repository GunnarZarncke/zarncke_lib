package de.zarncke.lib.math;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import de.zarncke.lib.coll.EmptyIterator;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.region.Binary;
import de.zarncke.lib.region.Binary.Decoder;
import de.zarncke.lib.region.Region;
import de.zarncke.lib.region.RegionUtil;

/**
 * A Set which uses a compact, but slow encoding for its elements.
 * Compact means smaller than an ArrayList or smaller than all the bytes of the stored elements.
 * Slow means insert is expensive (comparable to HashSet-reorg).
 * Lookup is comparable to binary search.
 * Can only store {@link Binary} objects.
 * The method is somewhat comparable to a binary decision diagram without reusing nodes.
 *
 * @author Gunnar Zarncke
 * @param <T> the specific binary object type
 */
public class CompactSet<T extends Binary> {
	private Decoder<T> decoder;

	private final Region encodedData = RegionUtil.EMPTY;

	public CompactSet(final Binary.Decoder<T> decoder) {
		this.decoder = decoder;
	}

	public CompactSet() {
		this(null);
	}

	public void add(final T element) {

	}

	public void addAll(final Collection<T> elements) {
		ArrayList<Region> regionsToBeSorted = new ArrayList<Region>(elements.size());
		int total = 0;
		for (T elem : elements) {
			Region enc = elem.encode();
			regionsToBeSorted.add(enc);
			total += enc.length();
		}
		Collections.sort(regionsToBeSorted, Region.LEXICOGRAPHICALLY);
		ArrayList<BitSet> bitSets = new ArrayList<BitSet>(elements.size());
		for (Region r : regionsToBeSorted) {
			// horribly slow, but simple
			BitSet bs = new BitSet((int) r.length() * 8);
			for (int i = 0; i < r.length(); i++) {
				byte b = r.get(i);
				for (int bit = 0; bit < 8; bit++) {
					if ((b & 1 << bit) != 0) {
						bs.set(i * 8 + bit);
					}
				}
			}
			bitSets.add(bs);
		}

		byte[] dataBuffer = new byte[total];

		process(bitSets, 0, regionsToBeSorted.size(), dataBuffer);
	}

	private void process(final ArrayList<BitSet> bitSets, final int i, final int size, final byte[] dataBuffer) {

	}

	public boolean contains(final T element) {
		Region r = element.encode();
		return false;
	}

	public Iterator<T> iterator() {
		if (this.decoder == null) {
			throw Warden.spot(new UnsupportedOperationException("iteration only works when a decoder is set"));
		}
		return EmptyIterator.getInstance();
	}
}
