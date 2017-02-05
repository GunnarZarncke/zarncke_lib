/**
 *
 */
package de.zarncke.lib.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.joda.time.DateTime;
import org.joda.time.ReadableInterval;

import de.zarncke.lib.coll.Elements;
import de.zarncke.lib.coll.L;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.index.crit.Criteria;
import de.zarncke.lib.index.crit.IntervalMatch;
import de.zarncke.lib.math.Intervals;
import de.zarncke.lib.time.HasDateTime;

/**
 * {@link Index} for date base types. Provides {@link #getSubIndexing() sub indexing} by time.
 *
 * @author Gunnar Zarncke
 * @param <T> type with a date
 */
public class DateTimeIndex<T extends HasDateTime> implements Index<T> {
	private final TreeMap<DateTime, Collection<T>> byStart = new TreeMap<DateTime, Collection<T>>();
	private int size = 0;

	public Indexing<T> getSubIndexing() {
		return new DefaultSubIndexing<T>(ReadableInterval.class) {
			public Index<T> getIndex(final Criteria<?, T> crit) {
				if (!(crit instanceof IntervalMatch)) {
					return null;
				}
				Collection<ReadableInterval> intervals = ((IntervalMatch<?>) crit).getKeys();
				if (((IntervalMatch<?>) crit).getOverlapRelation() != Intervals.Relation.CONTAINS_START) {
					// TODO use tailMap and iterate until no more found?
					// results = ChannelIndex.this.byStart.tailMap(interval.getStart()).values();
					return null;
				}
				Collection<Collection<T>> results = L.l();
				for (ReadableInterval interv : intervals) {
					results.addAll(DateTimeIndex.this.byStart.subMap(interv.getStart(), interv.getEnd()).values());
				}
				return new ListIndex<T>(Elements.flatten(results));
			}

			public void clear() {
				// temporary only
			}
		};
	}

	public Results<T> getAll() {
		Collection<Collection<T>> results;
		results = DateTimeIndex.this.byStart.values();
		return new ListResults<T>(new ArrayList<T>(Elements.flatten(results)));
	}

	// may only be called during setup when no concurrent getXxx() calls are running! Otherwise not thread-safe
	public Index<T> add(final T show) {
		Collection<T> collection = this.byStart.get(show.getTime());
		if (collection == null) {
			collection = new LinkedList<T>();
			this.byStart.put(show.getTime(), collection);
		}
		this.size++;
		// TODO might sort by title
		collection.add(show);
		return this;
	}

	@Override
	public String toString() {
		return "indexed " + this.byStart.size() + " elements";
	}

	public int size() {
		return this.size;
	}

	public void clear() {
		this.byStart.clear();
		this.size = 0;
	}

	/**
	 * Access broadcasts around a given time.
	 * This special case is used to support now-next queries efficiently.
	 * It is not available through the interface and called by special code in BaseViewer.
	 * The reference broadcast airing "now" is determined as follows (note that there may be more than broadcast airing now
	 * actually, but only one is used as reference): In the list of broadcasts sorted by starting time take the first broadcast
	 * before now.
	 *
	 * @param time at which broadcasts are requested
	 * @param startOffset the offset relative to the reference broadcast (as above) of the first returned broadcast; may be
	 * negative
	 * @param endOffset the offset relative to the reference broadcast of the last (inclusive); may be negative
	 * @return List of broadcasts ordered by starting time, if there are enough broadcasts, the endOffset - startOffset + 1 will
	 * be returned
	 */
	public Results<T> getAroundTime(final DateTime time, final int startOffset, final int endOffset) {
		if (endOffset < startOffset) {
			throw Warden.spot(new IllegalArgumentException("end " + endOffset + " must be >= start " + startOffset));
		}
		Entry<DateTime, Collection<T>> entry = DateTimeIndex.this.byStart.floorEntry(time);
		if (entry == null) {
			return new ListResults<T>(L.<T> e());
		}
		DateTime floor = entry.getKey();
		int expectedSize = endOffset - startOffset + 1;
		List<T> results = L.n(expectedSize);
		results.addAll(entry.getValue());
		int zeroSize = results.size();
		if (startOffset < 0) {
			// find extra elements at the start because of negative offset
			while (results.size() - zeroSize < -startOffset) {
				entry = this.byStart.lowerEntry(entry.getKey());
				if (entry == null) {
					break;
				}
				results.addAll(0, entry.getValue());
			}
			// remove elements at the start because of >1 chunks
			while (results.size() - zeroSize > -startOffset) {
				results.remove(0);
			}
			// correct for insufficient elements
			int realStartOffset = -(results.size() - zeroSize);
			if (endOffset < realStartOffset - 1) {
				expectedSize = 0;
			} else {
				expectedSize = endOffset - realStartOffset + 1;
			}
		}
		// find extra elements at the end until sufficiently many
		while (results.size() < Math.max(expectedSize, endOffset + 1)) {
			entry = this.byStart.higherEntry(floor);
			if (entry == null) {
				break;
			}
			floor = entry.getKey();
			results.addAll(entry.getValue());
		}
		// remove elements at the start because of positive offset
		if (startOffset > 0) {
			if (startOffset >= results.size()) {
				return new ListResults<T>(L.<T> e());
			}
			for (int i = 0; i < startOffset; i++) {
				results.remove(0);
			}
		}
		// remove elements at the end because of > 1 chunk
		while (results.size() > expectedSize) {
			results.remove(results.size() - 1);
		}

		return new ListResults<T>(results);
	}

}