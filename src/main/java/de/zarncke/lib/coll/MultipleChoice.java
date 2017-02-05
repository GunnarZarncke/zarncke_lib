package de.zarncke.lib.coll;

import java.util.Collection;

/**
 * Allows to manipulate a List of values for a multiple choice.
 * It is possible to have an undefined choice which may mean "all".
 * Note: {@link #getAll()} and {@link #iterator()} return empty in this case.
 * If you use this class to constrain a result to only the given values or to "all", then you must explicitly check
 * {@link #isDefined()} or use {@link #match(Collection)}.
 *
 * @author Gunnar Zarncke
 * @param <T> element type
 */
public interface MultipleChoice<T> extends Iterable<T> {
	/**
	 * @param values != null
	 * @return true if any of the given values matches any of the contained values - or if no values are defined
	 */
	boolean match(Collection<T> values);

	void clear();

	boolean isDefined();

	Collection<T> getAll();

	/**
	 * @param value != null, no null elements
	 */
	void setAll(Collection<T> value);

	/**
	 * @param value != null
	 */
	void set(T value);

	/**
	 * @param value != null
	 */
	void add(T value);

	/**
	 * @param value != null
	 */
	void remove(T value);

	/**
	 * @param value != null
	 */
	void toggle(T value);

	/**
	 * @param value may be null to indicate clear
	 * @param enabled true: the value is added; false: the value is removed; null: value is toggled
	 */
	void select(T value, Boolean enabled);

	MultipleChoice<T> copy();
}