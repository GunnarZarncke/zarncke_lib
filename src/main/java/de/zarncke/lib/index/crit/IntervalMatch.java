/**
 *
 */
package de.zarncke.lib.index.crit;

import java.util.Arrays;
import java.util.Collection;

import org.joda.time.ReadableInterval;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.math.Intervals;
import de.zarncke.lib.time.HasInterval;
import de.zarncke.lib.time.IntervalProvider;

/**
 * Matches Broadcasts overlapping or starting in a given {@link ReadableInterval time interval}.
 *
 * @author Gunnar Zarncke
 * @param <T> of object
 */
public final class IntervalMatch<T extends HasInterval> extends CollectionCriteria<ReadableInterval, T> //
		implements IntervalProvider {
	private static final long serialVersionUID = 1L;

	private final Intervals.Relation overlapTest;

	/**
	 * Constructor intended for deserialization only.
	 */
	public IntervalMatch() {
		super(L.<ReadableInterval> e(), ReadableInterval.class);
		this.overlapTest = Intervals.Relation.CONTAINS_START;
	}

	/**
	 * @param overlapTest to apply != null
	 * @param interval one single interval
	 * @deprecated used for backward compatibility
	 */
	@Deprecated
	public IntervalMatch(final ReadableInterval interval, final Intervals.Relation overlapTest) {
		this(overlapTest, interval);
	}

	public IntervalMatch(final Intervals.Relation overlapTest, final ReadableInterval... intervals) {
		this(overlapTest, Arrays.asList(intervals));
	}

	/**
	 * @param intervals any number of
	 * @param overlapTest true: test overlap against full interval; false: test whether only start is contained in interval
	 */
	public IntervalMatch(final Intervals.Relation overlapTest, final Collection<ReadableInterval> intervals) {
		super(intervals, ReadableInterval.class);
		this.overlapTest = overlapTest;
	}

	@Override
	protected Collection<? extends ReadableInterval> getValues(final T entry) {
		throw Warden.spot(new UnsupportedOperationException("matches is implemented directly"));
	}

	@Override
	public boolean matches(final T withInterval) {
		switch (this.overlapTest) {
		case CONTAINS_START:
			for (ReadableInterval interv : getKeys()) {
				if (interv.contains(withInterval.getInterval().getStart())) {
					return true;
				}
			}
			return false;
		case CONTAINS_TOTAL:
			for (ReadableInterval interv : getKeys()) {
				if (interv.contains(withInterval.getInterval())) {
					return true;
				}
			}
			return false;
		case OVERLAPS:
			for (ReadableInterval interv : getKeys()) {
				if (interv.overlaps(withInterval.getInterval())) {
					return true;
				}
			}
			return false;
		case SPANS:
			for (ReadableInterval interv : getKeys()) {
				if (withInterval.getInterval().contains(interv)) {
					return true;
				}
			}
			return false;
		default:
			throw Warden.spot(new IllegalStateException("unknown mode " + this.overlapTest));
		}
	}

	/**
	 * @return true: test overlap against full interval; false: test whether only start is contained
	 */
	@Override
	public Intervals.Relation getOverlapRelation() {
		return this.overlapTest;
	}

	@Override
	public Class<ReadableInterval> getType() {
		return ReadableInterval.class;
	}

	@Override
	public String toString() {
		switch (this.overlapTest) {
		case CONTAINS_START:
			return "starting during " + getKeys();
		case CONTAINS_END:
			return "ending during " + getKeys();
		case CONTAINS_TOTAL:
			return "starting and ending in " + getKeys();
		case OVERLAPS:
			return "overlaping " + getKeys();
		case SPANS:
			return "running all during  " + getKeys();
		default:
			return this.overlapTest + " " + getKeys();
		}
	}

	@Override
	public Collection<? extends ReadableInterval> getIntervals(final ReadableInterval extent) {
		return getKeys();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (this.overlapTest == null ? 0 : this.overlapTest.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		IntervalMatch<?> other = (IntervalMatch<?>) obj;
		if (this.overlapTest != other.overlapTest) {
			return false;
		}
		return true;
	}

}