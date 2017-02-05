package de.zarncke.lib.data;

import java.util.Map;

import javax.annotation.Nonnull;

/**
 * This interface indicates that an Object provides some structured self information.
 * This is intended for monitoring and debug purposes.
 * The information should have the same basic quality as the {@link #toString()} method.
 *
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
public interface HasSelfInfo {

	/**
	 * The keys are recommended to be like variable names.
	 * The values should at least have a toString method returning output suitable to be displayed in at most 80x25.
	 *
	 * @return a Map of meta info, intended to be immutable, may be created on the spot, lazy or delegate to object
	 */
	@Nonnull
	Map<String, ? extends Object> getSelfInfo();
}
