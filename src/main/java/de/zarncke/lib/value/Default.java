package de.zarncke.lib.value;

import java.util.Collection;

import de.zarncke.lib.log.Log;

/**
 * Represents a default value for a given specification.
 * Use {@link Default#of(Object, Class)} or the variants to create it.
 * The identity of a Default is determined from its type, not the associated default value.
 *
 * @author Gunnar Zarncke
 * @param <T> of default value
 */
public abstract class Default<T> extends Type<T> {

	private static final long serialVersionUID = 2L;

	/**
	 * Shortcut to create an array of defaults from values.
	 *
	 * @param values != null
	 * @return values
	 */
	public static Default<?>[] many(final Default<?>... values) {
		return values;
	}

	/**
	 * Shortcut to create an array of defaults from a single value.
	 *
	 * @param <T> type of default value
	 * @param <S> any type deriving from <T>
	 * @param value to use. Should probably be immutable as it will be shared among all users of this default.
	 * @param clazz T
	 * @return array with single default
	 */
	public static <T, S extends T> Default<?>[] single(final S value, final Class<T> clazz) {
		return many(Default.of(value, clazz));
	}

	/**
	 * Shortcut to create an array of defaults from values.
	 *
	 * @param values != null
	 * @return values
	 */
	public static Default<?>[] many(final Collection<? extends Default<?>> values) {
		return values.toArray(new Default<?>[values.size()]);
	}

	/**
	 * Creates a default with the given default value. The class is inferred from the value.
	 *
	 * @param <T> type of default value given
	 * @param <S> the actual (but unknown) type of the value; it derives from <T>
	 * @param value != null to use. Should probably be immutable as it will be shared among all users of this default.
	 * @return Default != null
	 */
	public static <T, S extends T> Default<S> of(final T value) {
		// we know what we do here: value actually has a sub type of T (called S).
		// It is unknown, but its class is Class<S>.
		// Think of it as a wildcard. Callers will not know it.
		Class<S> sClass = (Class<S>) value.getClass();
		@SuppressWarnings("unchecked")
		S sValue = (S) value;
		return of(sValue, sClass);
	}

	/**
	 * Creates a default String with the given qualifier.
	 *
	 * @param value any String
	 * @param qualifier != null, defaults with the same qualifier are shared
	 * @return Default != null
	 */
	public static Default<String> of(final String value, final String qualifier) {
		return new QualifiedDefault<String>(value, String.class, qualifier);
	}

	/**
	 * Creates a default for the given class.
	 *
	 * @param <T> type of default value
	 * @param <S> any type deriving from <T>
	 * @param value to use. Should probably be immutable as it will be shared among all users of this default.
	 * @param clazz T
	 * @return Default
	 */
	public static <T, S extends T> Default<T> of(final S value, final Class<T> clazz) {
		return new ValueDefault<T>(value, clazz);
	}

	/**
	 * Creates a default for the given class qualified with the given name. This allows to distinguish defaults of the same
	 * class. Should not be used to avoid creating technical types.
	 *
	 * @param <T> type of default value
	 * @param <S> any type deriving from <T>
	 * @param value to use, may be null. Should probably be immutable as it will be shared among all users of this default.
	 * @param clazz T != null
	 * @param qualifier != null, all defaults with the same qualifier are shared.
	 * @return Default != null
	 */
	public static <T, S extends T> Default<T> of(final S value, final Class<T> clazz, final String qualifier) {
		return new QualifiedDefault<T>(value, clazz, qualifier);
	}

	protected Default(final Class<T> clazz) {
		super(clazz);
	}

	public abstract T getValue();


	@Override
	public String toString() {
		String value;
		try {
			T v = getValue();
			value = v == null ? "null" : v.toString();
		} catch (Exception e) {
			Log.LOG.get().report(e);
			value = "can't print";
		}
		return super.toString() + "->" + value;
	}

	public abstract Default<T> withOtherValue(T value);
}
