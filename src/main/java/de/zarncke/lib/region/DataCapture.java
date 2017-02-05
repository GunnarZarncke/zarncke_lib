package de.zarncke.lib.region;

import java.io.IOException;
import java.io.Serializable;

/**
 * This class is a Serializable HasData, that captures the data of another HasData as needed to serialize it.
 * <p>
 * Until capture it acts as a proxy for the handled HasData.<br>
 * Capture occurs when
 * <ul>
 * <li>toByteArray() called</li>
 * <li>equals() called</li>
 * <li>toString() called</li>
 * <li>serialized</li>
 * </ul>
 */
public class DataCapture implements Region, Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * the captured data
	 */
    private Region captured;

    private transient int hash;

    /**
     * this holds the data until capture.
     */
    private transient Region data;

    /**
     * wrap data.
     *
     * @param data
     *            byte array != null (length=0 is ok)
     */
    public DataCapture(final Region data)
    {
        if ( data == null )
        {
            throw new IllegalArgumentException("data may not be null!");
        }

        this.data = data;
    }

    protected DataCapture()
    {
    // for deserialization
    }

    private void writeObject(final java.io.ObjectOutputStream out) throws IOException
    {
        toByteArray();
        out.defaultWriteObject();
    }

    private void readObject(final java.io.ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        this.hash = this.captured.hashCode();
    }

    /**
     * get the bytes. this captures the data
     *
     * @return the byte array
     */
    public byte[] toByteArray()
    {
        if ( this.captured == null )
        {
            this.captured = RegionUtil.asRegion(this.data.toByteArray());
            this.hash = this.data.hashCode();
            this.data = null;
        }
        return this.captured.toByteArray();
    }

    @Override
    public int hashCode()
    {
        return this.captured == null ? this.data.hashCode() : this.hash;
    }

    /**
     * get the length of the array
     *
     * @return length of array
     *
     *         public int length() { return this.bytes == null ? this.data.length() : this.bytes.length; }
     */

    /**
     * compares all bytes! this captures the data as a side effect!
     *
     * @return true if length and all bytes are equal
     */
    @Override
    public boolean equals(final Object obj)
    {
        if ( !(obj instanceof Region) )
        {
            return false;
        }
        return current().equals(obj);

    }

    private Region current()
    {
        return this.captured != null ? this.captured : this.data;
    }

    /**
     * print data.
     *
     * @return String
     */
    @Override
    public String toString()
    {
        return current().toString();
    }

    public byte get(final long index)
    {
        return current().get(index);
    }

    public long length()
    {
        return current().length();
    }

    public Region realize()
    {
        return current().realize();
    }

    public View select(final long startOffset, final long len)
    {
        return current().select(startOffset, len);
    }
}
