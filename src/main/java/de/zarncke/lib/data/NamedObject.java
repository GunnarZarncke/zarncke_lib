package de.zarncke.lib.data;

import javax.annotation.Nullable;

/**
 * An Object, that has some kind of name.
 * please note, that two NamedObjects are considered equal if they
 * have the same name!
 */
public class NamedObject implements HasName, java.io.Serializable
{
	static final long serialVersionUID = -9055540121104044759L; // 17.1.2003

	/**
	 * our name
	 */
	private String name;

	/**
	 * construct a NamedObject with the given name.
	 * @param name is the name
	 */
	public NamedObject(@Nullable final String name)
	{
		this.name = name;
	}


	protected NamedObject()
	{
		// for potential deserialization
	}

	/**
	 * Get the name of this Object.
	 * @return the name as a String
	 */
	@Override
	public String getName()
	{
		return this.name;
	}

	/**
	 * compare (see {@link java.lang.Comparable#compareTo}).
	 */
	@Override
	public int compareTo(final HasName obj)
	{
		if (getName() == null) {
			return obj.getName() == null ? 0 : -1;
		}
		if (obj.getName() == null) {
			return 1;
		}
		return getName().compareTo(obj.getName());
	}

	@Override
	public String toString()
	{
		return this.name == null ? "<null>" : this.name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.name == null ? 0 : this.name.hashCode());
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
		NamedObject other = (NamedObject) obj;
		if (this.name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!this.name.equals(other.name)) {
			return false;
		}
		return true;
	}
}




