package de.zarncke.lib.id;

import de.zarncke.lib.value.Typed;

/**
 * General form of Id types.
 *
 * @author Gunnar Zarncke
 */
public interface Id extends Typed {
	String toUtf8String();
	String toHexString();
}
