package de.zarncke.lib.region;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Comparator;

import de.zarncke.lib.region.Region.View;
import de.zarncke.lib.util.Misc;

public class RegionUtil {
	private static class ReadOnlyRegion implements Region {
		private final Region region;

		private ReadOnlyRegion(final Region region) {
			this.region = region;
		}

		public byte get(final long index) {
			return this.region.get(index);
		}

		public long length() {
			return this.region.length();
		}

		public Region realize() {
			return this.region.realize();
		}

		public View select(final long startOffset, final long len) {
			return new ReadOnlyView(this.region.select(startOffset, len));
		}

		public byte[] toByteArray() {
			return this.region.toByteArray();
		}

		@Override
		public String toString() {
			return this.region.toString();
		}
	}

	private static final class ReadOnlyView extends ReadOnlyRegion implements View {
		public ReadOnlyView(final View view) {
			super(view);
		}

		public Region replace(final Region replaceData) {
			throw new UnsupportedOperationException("cannot modify read-only region " + this);
		}

	}

	private RegionUtil() {
		//
	}

	public static final byte[] NO_BYTES = new byte[0];

	public static final Region EMPTY = new EmptyRegion();

	public static Region asRegion(final byte[] ba) {
		return new PrimitiveRegion(ba);
	}

	public static Region asRegionUtf8(final String data) {
		return new PrimitiveRegion(data.getBytes(Misc.UTF_8));
	}

	public static Region.View at(final Region region, final long position) {
		return region.select(position, 0);
	}

	public static Region.View selectUntilEnd(final Region region, final long startOffset) {
		return region.select(startOffset, region.length() - startOffset);
	}

	public static Region.View selectMax(final Region region, final long startOffset, final int maxLength) {
		long rest = region.length() - startOffset;
		return region.select(startOffset, rest < maxLength ? rest : maxLength);
	}

	public static byte[] toBytesMax(final Region region, final long startOffset, final int maxLength) {
		return selectMax(region, startOffset, maxLength).toByteArray();
	}

	public static Region delete(final Region.View view) {
		return view.replace(Region.EMPTY);
	}

	public static Region delete(final Region region, final long startOffset, final long deletedLength) {
		return delete(region.select(startOffset, deletedLength));
	}

	public static int getBigEndianInt(final Region region, final long offset) {
		byte[] b4 = region.select(offset, 4).toByteArray();
		return b4[0] << 24 | (b4[1] & 0xFF) << 16 | (b4[2] & 0xFF) << 8 | b4[3] & 0xFF;
	}

	public static CharSequence getString(final Region region, final long startOffset, final int length, final String encoding)
			throws UnsupportedEncodingException {
		return new String(region.select(startOffset, length).toByteArray(), encoding);
	}

	public static Region readOnly(final Region region) {
		return new ReadOnlyRegion(region);
	}

	/**
	 * Compares two Regions lexicographically.
	 * This may be expensive as it fetches bytes until a difference arises.
	 *
	 * @param a Region != null
	 * @param b Region != null
	 * @return int see {@link Comparator#compare(Object, Object)}
	 */
	public static int compare(final Region a, final Region b) {
		long p = 0;
		long al = a.length();
		long bl = b.length();

		while (p < al && p < bl) {
			byte ab = a.get(p);
			byte bb = b.get(p);
			if (ab < bb) {
				return -1;
			}
			if (ab > bb) {
				return 1;
			}
			p++;
		}
		if (al < bl) {
			return -1;
		}
		if (al > bl) {
			return 1;
		}
		return 0;
	}

	public static ByteBuffer asByteBuffer(final Region r) {
		if (r instanceof ByteBufferRegion) {
			return ((ByteBufferRegion) r).toByteBuffer();
		}
		return ByteBuffer.wrap(r.toByteArray());
	}
}
