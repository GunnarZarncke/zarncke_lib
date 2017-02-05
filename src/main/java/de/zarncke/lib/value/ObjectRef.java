package de.zarncke.lib.value;

/**
 * A {@link Ref reference} to an object.
 *
 * @author Gunnar Zarncke
 * @param <T> type of the object
 */
public class ObjectRef<T> implements Ref<T> {
	/**
	 * Type-save convenience factory method.
	 *
	 * @param <T> type of argument
	 * @param param any T
	 * @return {@link ObjectRef}
	 */
	public static <T> ObjectRef<T> of(final T param) {
		return new ObjectRef<T>(param);
	}

	private T ref;

	public ObjectRef(final T ref) {
		this.ref = ref;
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
		ObjectRef<?> other = (ObjectRef<?>) obj;
		if (this.ref == null) {
			if (other.ref != null) {
				return false;
			}
		} else if (!this.ref.equals(other.ref)) {
			return false;
		}
		return true;
	}

	public T get() {
		return this.ref;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.ref == null ? 0 : this.ref.hashCode());
		return result;
	}

	public void set(final T newRef) {
		this.ref = newRef;
	}

	@Override
	public String toString() {
		return String.valueOf(this.ref);
	}

}
