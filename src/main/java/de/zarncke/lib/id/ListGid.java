package de.zarncke.lib.id;

import java.io.UnsupportedEncodingException;
import java.util.Collection;

import de.zarncke.lib.err.CantHappenException;
import de.zarncke.lib.err.Warden;

public class ListGid<T> extends Gid<Collection<T>> {
	private static final long serialVersionUID = 1L;
	private final Class<T> elementType;

	public ListGid(final String utf8, final Class<T> elementType) {
		this(toBytes(utf8), elementType);
	}

	private static byte[] toBytes(final String utf8) {
		try {
			return utf8.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw Warden.spot(new CantHappenException("UTF-8 must be there", e));
		}
	}

	@SuppressWarnings( { "unchecked", "rawtypes" /* we know the collection element type */})
	protected ListGid(final byte[] id, final Class<T> elementType) {
		super(id, (Class) Collection.class);
		this.elementType = elementType;
	}

	public Class<T> getElementType() {
		return this.elementType;
	}

	// the following code only to declare that we don't want elementType in equals
	@Override
	public boolean equals(final Object obj) { // NOPMD
		return super.equals(obj);
	}

	@Override
	public int hashCode() { // NOPMD
		return super.hashCode();
	}

}
