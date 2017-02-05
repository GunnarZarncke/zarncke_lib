package de.zarncke.lib.id;

import java.io.Serializable;

import de.zarncke.lib.id.Ids.HasSomeGid;
import de.zarncke.lib.util.Detachable;
import de.zarncke.lib.value.Ref;

/**
 * A {@link Ref} which may be discarded. Recovers by using the {@link Gid}.
 *
 * @author Gunnar Zarncke
 * @param <T> type of referent
 */
public class GidRef<T extends HasSomeGid<T>> implements Ref<T>, HasSomeGid<T>, Detachable<Void>, Serializable {
	private static final long serialVersionUID = 1L;

	public static <T extends HasSomeGid<T>> GidRef<T> of(final T ref) {
		return new GidRef<T>(ref);
	}

	private transient T ref;
	private Gid<? extends T> id;

	public GidRef(final T ref) {
		set(ref);
	}

	public T get() {
		if (this.ref == null && this.id != null) {
			this.ref = Resolving.resolve(this.id);
		}
		return this.ref;
	}

	public final void set(final T newRef) {
		this.ref = newRef;
		this.id = newRef == null ? null : newRef.getId();
	}

	/**
	 * Explicitly forget the reference. Causes resolving on next get().
	 * 
	 * @param v ignored
	 */
	public void detach(final Void v) {
		this.ref = null;
	}

	@Override
	public String toString() {
		return String.valueOf(this.id + "->" + this.ref);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.id == null ? 0 : this.id.hashCode());
		return result;
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
		GidRef<?> other = (GidRef<?>) obj;
		if (this.id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!this.id.equals(other.id)) {
			return false;
		}
		return true;
	}

	public Gid<? extends T> getId() {
		return this.id;
	}

}
