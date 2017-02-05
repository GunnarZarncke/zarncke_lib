package de.zarncke.lib.region;

import de.zarncke.lib.coll.Elements;
import de.zarncke.lib.region.Region.View;

final class EmptyRegion implements Region, View
{
    public byte[] toByteArray()
    {
        return Elements.NO_BYTES;
    }

    public View select(final long startOffset, final long len)
    {
        if ( len > 0 || startOffset > 0 )
        {
			throw new IndexOutOfBoundsException("empty " + startOffset + "+" + len);
        }
        return this;
    }

    public Region realize()
    {
        return this;
    }

    public long length()
    {
        return 0;
    }

    public byte get(final long index)
    {
        throw new IndexOutOfBoundsException("empty");
    }

    public Region replace(final Region replaceData)
    {
		return replaceData;
    }

    @Override
    public String toString()
    {
        return "[]";
    }
}