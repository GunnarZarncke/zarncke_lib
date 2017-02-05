package de.zarncke.lib.time;

import org.joda.time.DateTime;

/**
 * Declares that this Object has a time.
 *
 * @author Gunnar Zarncke
 */
public interface HasDateTime {
	DateTime getTime();
}
