package de.zarncke.lib.lang;

/**
 * Indicates responsibility for {@link Piece}s of code.
 *
 * @author Gunnar Zarncke
 */
public interface CodeResponsible {
	boolean isResponsibleFor(Piece code);
}
