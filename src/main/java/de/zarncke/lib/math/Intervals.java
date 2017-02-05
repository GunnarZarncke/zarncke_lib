package de.zarncke.lib.math;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import de.zarncke.lib.coll.L;

/**
 * This class represents a collection of integer intervals.
 * It supports easy construction and intersection and join. {@link Intervals} is immutable.
 *
 * @author Gunnar Zarncke
 */
public final class Intervals {
	public static final class EndComparator implements Comparator<Interval> {
		@Override
		public int compare(final Interval o1, final Interval o2) {
			return o1.end < o2.end ? -1 : o1.end > o2.end ? 1 : 0;
		}
	}

	static final class StartComparator implements Comparator<Interval> {
		@Override
		public int compare(final Interval o1, final Interval o2) {
			return o1.start < o2.start ? -1 : o1.start > o2.start ? 1 : 0;
		}
	}

	/**
	 * Generic definition for ways intervals may be tested for overlap.
	 */
	public enum Relation {
		/**
		 * true if the first interval contains the start of the second.
		 */
		CONTAINS_START,
		/**
		 * true if the first interval contains the end of the second.
		 */
		CONTAINS_END,
		/**
		 * true if the first interval contains the second one.
		 */
		CONTAINS_TOTAL,
		/**
		 * true if the first interval overlaps the second partly or totally.
		 */
		OVERLAPS,
		/**
		 * true if the first interval is contained by the second one.
		 */
		SPANS
	}

	/**
	 * An interval.
	 * The convention is, that the end is not included in an interval. Empty intervals are represented by null.
	 * End points must be strictly larger than start points.
	 * Operations may return null to indicate the empty interval. {@link Interval} is immutable.
	 *
	 * @author Gunnar Zarncke
	 */
	public static class Interval {
		private final long start;
		private final long end;

		public Interval(final long start, final long end) {
			assert end >= start;
			// if (end <= start) {
			// throw Warden.spot(new IllegalArgumentException("start>end"));
			// }
			this.start = start;
			this.end = end;
		}

		public long getStart() {
			return this.start;
		}

		public long getEnd() {
			return this.end;
		}

		public boolean overlaps(final Interval any) {
			if (any == null) {
				return false;
			}
			return any.end > this.start && any.start < this.end;
		}

		public Interval join(final Interval any) {
			if (any == null) {
				return this;
			}
			return new Interval(Math.min(this.start, any.start), Math.max(this.end, any.end));
		}

		public Interval intersect(final Interval any) {
			if (!overlaps(any)) {
				return null;
			}
			return new Interval(Math.max(this.start, any.start), Math.min(this.end, any.end));
		}

		@Override
		public String toString() {
			return "(" + this.start + "," + this.end + ")";
		}

		public static Interval of(final long a, final long b) {
			return new Interval(a, b);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (this.end ^ this.end >>> 32);
			result = prime * result + (int) (this.start ^ this.start >>> 32);
			return result;
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			Interval other = (Interval) obj;
			if (this.end != other.end) {
				return false;
			}
			if (this.start != other.start) {
				return false;
			}
			return true;
		}

	}

	public static final Intervals EMPTY = new Intervals();

	public static Intervals point(final long p) {
		return new Intervals(new Interval(p, p + 1));
	}

	public static Intervals of(final Interval... intervals) {
		return new Intervals(intervals);
	}

	public static Intervals ofNormalized(final Interval... intervals) {
		List<Interval> res = L.l(intervals);
		// remove nulls
		for (int i = 0; i < res.size(); i++) {
			if (res.get(i) == null) {
				res.remove(i);
				i--;
			}
		}
		if (res.size() == 0) {
			return EMPTY;
		}
		Collections.sort(res, new StartComparator());

		// merge successive overlaps
		Interval prev = res.get(0);
		for (int i = 1; i < res.size(); i++) {
			Interval cur = res.get(i);
			if (prev.end >= cur.start) {
				prev = new Interval(prev.start, Math.max(cur.end, prev.end));
				res.set(i - 1, prev);
				res.remove(i);
				i--;
			} else {
				prev = cur;
			}
		}
		return new Intervals(res.toArray(new Interval[res.size()]), null);
	}

	private final Interval[] intervals;

	/**
	 * @param intervals must be ordered, no overlap
	 */
	private Intervals(final Interval... intervals) {
		this.intervals = intervals.clone();
	}

	/**
	 * Create from an internally created private array.
	 *
	 * @param intervals
	 * @param v dummy to distinguish from public constructor
	 */
	private Intervals(final Interval[] intervals, final Void v) {
		this.intervals = intervals;
	}

	Intervals simplify() {
		Interval[] copy = new Interval[this.intervals.length];
		int s = 0;
		int t = 0;
		while (s < this.intervals.length - 1) {
			Interval is = this.intervals[s];
			Interval is1 = this.intervals[s + 1];
			if (is.end == is1.start) {
				this.intervals[s + 1] = Interval.of(is.start, is1.end);
			} else {
				copy[t] = is;
				t++;
			}
			s++;
		}

		Interval[] shrunk = new Interval[t];
		System.arraycopy(copy, 0, shrunk, 0, t);
		return new Intervals(shrunk, null);
	}

	public Interval[] getIntervals() {
		return this.intervals.clone();
	}

	public List<Interval> getIntervalsAsList() {
		return Collections.unmodifiableList(Arrays.asList(this.intervals));
	}

	public Intervals join(final Interval anywhere) {
		if (anywhere == null) {
			return this;
		}
		long insertStart;

		int insertPos = findByStart(anywhere);
		if (insertPos < 0) {
			insertPos = -insertPos - 1;
			if (insertPos == 0) {
				// before first: test overlap at 0
				insertStart = anywhere.start;
			} else if (insertPos == this.intervals.length) {
				// after last: add and early exit
				Interval last = this.intervals[insertPos - 1];
				if (anywhere.start <= last.end) {
					Interval[] rearr = new Interval[insertPos];
					System.arraycopy(this.intervals, 0, rearr, 0, insertPos - 1);
					rearr[insertPos - 1] = new Interval(last.start, Math.max(last.end, anywhere.end));
					return new Intervals(rearr, null);
				}
				Interval[] rearr = new Interval[insertPos + 1];
				System.arraycopy(this.intervals, 0, rearr, 0, insertPos);
				rearr[insertPos] = anywhere;
				return new Intervals(rearr, null);
			} else if (anywhere.start <= this.intervals[insertPos - 1].end) {
				// a overlaps with p-1
				insertStart = this.intervals[insertPos - 1].start;
				insertPos--;
			} else {
				insertStart = anywhere.start;
				// insert a (and overlap with >=p)
			}
		} else {
			if (anywhere.end <= this.intervals[insertPos].end) {
				return this;
			}
			// old at p is completely contained in anywhere
			insertStart = anywhere.start;
		}

		// now insertStart is the start of the new target interval to be inserted at position p

		int pickUpPos = insertPos;
		while (true) {
			if (pickUpPos >= this.intervals.length) {
				break;
			}
			if (anywhere.end < this.intervals[pickUpPos].start) {
				break;
			}
			pickUpPos++;
		}
		// pickUpPos is now the position after the last overlapped element (which are not copied)
		assert pickUpPos == 0 || anywhere.end >= this.intervals[pickUpPos - 1].start;

		long insertEnd = anywhere.end;
		Interval[] rearr = new Interval[insertPos + this.intervals.length - pickUpPos + 1];
		if (insertPos > 0) {
			System.arraycopy(this.intervals, 0, rearr, 0, insertPos);
		}

		if (pickUpPos <= this.intervals.length) {
			if (pickUpPos > 0 && anywhere.end < this.intervals[pickUpPos - 1].end) {
				insertEnd = this.intervals[pickUpPos - 1].end;
			}
			System.arraycopy(this.intervals, pickUpPos, rearr, insertPos + 1, this.intervals.length - pickUpPos);
		}
		rearr[insertPos] = new Interval(insertStart, insertEnd);

		return new Intervals(rearr);
	}

	public Intervals join(final Intervals anywhere) {
		Intervals res = anywhere;
		for (Interval i : this.intervals) {
			res = res.join(i);
		}
		return res;
	}

	public Intervals intersect(final Interval anywhere) {
		if (anywhere == null) {
			return EMPTY;
		}
		int firstKeptPos = findByStart(anywhere);
		Interval left;
		if (firstKeptPos < 0) {
			firstKeptPos = -firstKeptPos - 1;
			if (firstKeptPos == 0) {
				left = null;
			} else {
				left = this.intervals[firstKeptPos - 1].intersect(anywhere);
			}
		} else {
			left = null;
		}

		int afterLastKeptPos = findByEnd(anywhere); // could be improved by searching in the interval from firstKeptPos to end
		Interval right;
		if (afterLastKeptPos < 0) {
			afterLastKeptPos = -afterLastKeptPos - 1;
			if (afterLastKeptPos < firstKeptPos) {
				// can happen only if there is only one central possibly overlapping interval: early exit
				Interval center = this.intervals[afterLastKeptPos].intersect(anywhere);
				return center == null ? EMPTY : new Intervals(center);
			}
			if (afterLastKeptPos == this.intervals.length) {
				right = null;
			} else {
				right = this.intervals[afterLastKeptPos].intersect(anywhere);
			}
		} else {
			right = null;
			if (afterLastKeptPos < firstKeptPos) {
				// can happen only if there is only one central possibly overlapping interval: early exit
				Interval center = this.intervals[firstKeptPos - 1].intersect(anywhere);
				return center == null ? EMPTY : new Intervals(center);
			}
			afterLastKeptPos++;
		}

		int l = (left != null ? 1 : 0) + (right != null ? 1 : 0) + afterLastKeptPos - firstKeptPos;
		if (l <= 0) {
			return EMPTY;
		}

		Interval[] cutout = new Interval[l];
		int t = 0;
		if (left != null) {
			cutout[t++] = left;
		}
		if (afterLastKeptPos > firstKeptPos) {
			System.arraycopy(this.intervals, firstKeptPos, cutout, t, afterLastKeptPos - firstKeptPos);
		}
		if (right != null) {
			cutout[l - 1] = right;
		}

		return new Intervals(cutout, null);
	}

	public Intervals intersect(final Intervals anywhere) {
		Intervals res = Intervals.EMPTY;
		for (Interval i : this.intervals) {
			res = res.join(anywhere.intersect(i));
		}
		return res;

	}

	private int findByStart(final Interval anywhere) {
		return Arrays.binarySearch(this.intervals, anywhere, new StartComparator());
	}

	private int findByEnd(final Interval anywhere) {
		return Arrays.binarySearch(this.intervals, anywhere, new EndComparator());
	}

	public <T> List<T> transform(final Function<Interval, T> transform) {
		return Lists.transform(Arrays.asList(this.intervals), transform);
	}

	public boolean isEmpty() {
		return this.intervals.length == 0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(this.intervals);
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Intervals other = (Intervals) obj;
		if (!Arrays.equals(this.intervals, other.intervals)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return Arrays.asList(this.intervals).toString();
	}

	public int size() {
		return this.intervals.length;
	}
}
