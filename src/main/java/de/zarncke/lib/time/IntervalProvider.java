package de.zarncke.lib.time;

import java.util.Collection;

import org.joda.time.ReadableInterval;

import de.zarncke.lib.math.Intervals;

/**
 * Specialized supplier for Interval lists.
 *
 * @author Gunnar Zarncke
 */
public interface IntervalProvider {
	Collection<? extends ReadableInterval> getIntervals(ReadableInterval extent);

	Intervals.Relation getOverlapRelation();
}
