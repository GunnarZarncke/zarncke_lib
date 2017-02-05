package de.zarncke.lib.value;

/**
 * A default value.
 * Important note: A {@link ValueDefault} loses its value on serialization!
 *
 * @author Gunnar Zarncke
 * @param <T> type of default
 */
public class ValueDefault<T> extends Default<T> {
	private static final long serialVersionUID = 1L;
	private transient T value = null;

	protected ValueDefault(final T value, final Class<T> clazz) {
		super(clazz);
		this.value = value;
	}

	@Override
	public T getValue() {
		return this.value;
	}

	@Override
	public Default<T> withOtherValue(final T otherValue) {
		return new ValueDefault<T>(otherValue, getType());
	}
}
