package de.zarncke.lib.id;

import de.zarncke.lib.err.Warden;
import de.zarncke.lib.value.Type;

/**
 * Id type. Long based.
 *
 * @author Gunnar Zarncke
 * @param <T> type of id
 */
public class Nid<T> extends Type<T> implements Id, Comparable<Nid<T>> {
	private static final long serialVersionUID = 1L;

	public static <T> Nid<T> ofUtf8(final String id, final Class<T> clazz) {
		return new Nid<T>(Long.parseLong(id), clazz);
	}

	public static <T> Nid<T> ofHex(final String id, final Class<T> clazz) {
		return new Nid<T>(Long.parseLong(id, 16), clazz);
	}

	public static <T> Nid<T> of(final long id, final Class<T> clazz) {
		return new Nid<T>(id, clazz);
	}

	private final long id;

	protected Nid(final long id, final Class<T> clazz) {
		super(clazz);
		this.id = id;
	}

	public long getId() {
		return this.id;
	}

	public String toUtf8String() {
		return Long.toHexString(this.id);
	}
	public String toHexString() {
		return String.valueOf(this.id);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (int) (this.id ^ this.id >>> 32);
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Nid<?> other = (Nid<?>) obj;
		if (this.id != other.id) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return toHexString() + "(" + super.toString() + ")";
	}

	public int compareTo(final Nid<T> o) {
		if (!getType().equals(o.getType())) {
			throw Warden.spot(new RuntimeException("cannot compare Nids of different kind " + getType() + "=" + o.getType()));
		}
		return new Long(this.id).compareTo(new Long(o.id));
	}
}
