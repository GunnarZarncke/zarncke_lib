package de.zarncke.lib.data;

import org.joda.time.DateTime;

import de.zarncke.lib.time.Times;

/**
 * This interface indicates how long an Object is expected to stay unchanged.
 * This may be evaluated by a cache mechanism.
 *
 * @author Gunnar Zarncke
 */
public interface HasValidity {
	// arbitrary past doesn't work
	DateTime CHANGES_IMMEDIATELY = new DateTime(1980, 1, 1, 0, 0, 0, 0);
	DateTime NEVER_CHANGES = Times.THE_FUTURE;

	/**
	 * @return the time when the Object is expected to change (no guarantee), null means {@link #CHANGES_IMMEDIATELY}.
	 */
	DateTime validUntil();

	/**
	 * A token which identifies this specific state of this Object.
	 * The token should be different when the Object changes. A good hash value should suffice.
	 * The token may be inspected to determine whether
	 * If the token is different a new representation of the Object must be used/created/rendered.
	 * If the token is the same a cached representation of the Object may be used
	 * <em>even when the validity period has expired</em>.
	 *
	 * @return != null, should contain only non-control ASCII characters
	 */
	String getTag();
}
