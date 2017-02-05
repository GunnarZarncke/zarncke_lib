package de.zarncke.lib.id;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;

import de.zarncke.lib.err.Warden;
import de.zarncke.lib.id.Ids.HasSomeGid;
import de.zarncke.lib.id.Ids.IdInjected;

/**
 * Provides support for serializing objects by their {@link HasSomeGid ID}.
 * Implies {@link IdEquality}.
 * Classes inheriting from this class may declare all their fields as transient (if this doesn't conflict with id-formation).
 * Only the id will be serialized and be used to retrieve the original object later by a {@link Resolver}.
 * Do not use together with {@link SerializingDbResolver}: When only the id is stored nothing can be deserialized any more.
 * 
 * @author Gunnar Zarncke
 * @param <T> type of concrete deriving class
 */
public abstract class IdSerializable<T> extends IdEquality<T> implements Serializable {
	private static final long serialVersionUID = 1L;
	private Gid<? extends T> id;

	public final Gid<? extends T> getId() {
		return this.id;
	}

	/**
	 * Must be called during construction with the effective id.
	 * The ID must always correspond exactly to the object.
	 *
	 * @param newId != null
	 */
	protected void setId(final Gid<? extends T> newId) {
		this.id = newId;
	}

	private void writeObject(final java.io.ObjectOutputStream out) throws IOException {
		out.writeObject(getId());
	}

	@SuppressWarnings("unchecked")
	private void readObject(final java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		setId((Gid<? extends T>) in.readObject());
	}

	protected Object readResolve() throws ObjectStreamException {
		Gid<? extends T> gotId = getId();
		try {
			Object r = Resolving.resolve(gotId);
			if (r == null) {
				throw Warden.spot(new IllegalStateException("IdSerializable "+getId()+" should resolve to non-null!"));
			}
			if (r instanceof IdInjected<?>) {
				@SuppressWarnings("unchecked" /*we assume same class*/)
				IdInjected<T> cast = (IdInjected<T>) r;
				cast.setId(gotId);
			}
			return r;
		} catch (IllegalArgumentException e) {
			throw Warden.spot((InvalidObjectException) new InvalidObjectException("invalid id " + gotId
					+ " during deserialization").initCause(e));
		}
	}

}
