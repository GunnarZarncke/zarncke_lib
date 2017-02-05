package de.zarncke.lib.value;

public class QualifiedDefault<T> extends ValueDefault<T> {
	private static final long serialVersionUID = 1L;
	private final String qualifier;

	QualifiedDefault(final T value, final Class<T> clazz, final String qualifier) {
		super(value, clazz);
		this.qualifier = qualifier;
	}

	@Override
	public Default<T> withOtherValue(final T otherValue) {
		return new QualifiedDefault<T>(otherValue, getType(), this.qualifier);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ this.qualifier.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		QualifiedDefault<?> other = (QualifiedDefault<?>) obj;
		if (!super.equals(obj)) {
			return false;
		}
		if (this.qualifier == null) {
			if (other.qualifier != null) {
				return false;
			}
		} else if (!this.qualifier.equals(other.qualifier)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return this.qualifier + ":" + super.toString();
	}
}
