package de.zarncke.lib.struct;

import java.util.Collection;
import java.util.HashMap;

/**
 * Represents a union of elements.
 * A Union object always represents the union formed by all joins. All Union objects that have become part of that union are
 * considered equal.
 * A unique Union object for the union can be determined with {@link #find()}. Any Union object can be used to query all
 * elements currently in the union.
 * Unions are formed by {@link #unionWith(Union) joining} one union with another (any Union object for that union will do).
 * Implementing classes should provide a static <code>Union create(T element)</code> method for the creation of a Union of one
 * element initially.
 * Typically only Unions of the same class can be joined.
 * The identity of a Union doesn't depend on the identity of the elements. A Union can hold the same element multiple times.
 * The caller is responsible for creating a Union object for element only once (e.g. use a {@link HashMap}).
 *
 * @author Gunnar Zarncke
 * @param <T> type of elements
 */
public interface Union<T> {

	/**
	 * Join this union with the given union.
	 * For chaining joins the joined Union is returned.
	 * From this moment this, the given and the returned Union are equal.
	 * 
	 * @param union to join with != null
	 * @return one Union object chosen to represent the union (need not be this or the given Union)
	 */
	Union<T> unionWith(final Union<T> union);

	Union<T> find();

	Collection<T> elements();

}
