package de.zarncke.lib.time;

import org.joda.time.ReadableInterval;

/**
 * Declares that this Object has a time interval.
 * 
 * @author Gunnar Zarncke
 */
public interface HasInterval {
	ReadableInterval getInterval();
}
