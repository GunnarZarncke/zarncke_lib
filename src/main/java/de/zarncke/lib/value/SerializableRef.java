package de.zarncke.lib.value;

import java.io.Serializable;

/**
 * A {@link Ref reference} to a {@link Serializable} object.
 * 
 * @author Gunnar Zarncke
 * @param <T> type of the object
 */
public class SerializableRef<T> implements Ref<T>, Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * Type-save convenience factory method.
	 *
	 * @param <T> type of argument
	 * @param param any T
	 * @return {@link SerializableRef}
	 */
	public static <T extends Serializable> SerializableRef<T> of(final T param) {
		return new SerializableRef<T>(param);
	}

	private T ref;

	public SerializableRef(final T ref) {
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
		SerializableRef<?> other = (SerializableRef<?>) obj;
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
