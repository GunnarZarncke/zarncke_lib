package de.zarncke.lib.region;

import de.zarncke.lib.region.Region.View;

final class ConstRegion implements Region, View
{
    private final class ConstView implements View
    {
        private final long offset;

        private final long length;

        ConstView(long startOffset, long len)
        {
            this.offset = startOffset;
            this.length = len;
        }

        public Region replace(Region replaceData)
        {
            Region res = replaceData.realize();
            if ( this.offset > 0 )
            {
                res = res.select(0, 0).replace(new ConstRegion(this.offset, ConstRegion.this.c));
            }
            if ( this.offset + this.length < ConstRegion.this.l )
            {
                res = res.select(res.length(), 0).replace(
                        new ConstRegion(ConstRegion.this.l - (this.offset + this.length), ConstRegion.this.c));
            }
            return res;
        }

        public byte get(long index)
        {
            return ConstRegion.this.c;
        }

        public long length()
        {
            return this.length;
        }

        public Region realize()
        {
            return new ConstRegion(this.length, ConstRegion.this.c);
        }

        public View select(long startOffset, long len)
        {
            if ( startOffset < 0 || startOffset + len > this.length )
            {
                throw new ArrayIndexOutOfBoundsException("range " + startOffset + "+" + len
                        + " is out of selected region " + this);
            }
            return new ConstView(this.offset + startOffset, len);
        }

        public byte[] toByteArray()
        {
            return realize().toByteArray();
        }
    }

    byte c;

    long l;

    ConstRegion(long l, byte c)
    {
        this.c = c;
        this.l = l;
    }

    public byte[] toByteArray()
    {
        if ( this.l > Integer.MAX_VALUE )
        {
            throw new UnsupportedOperationException("Region too large. " + this.l);
        }
        byte[] ba = new byte[(int) this.l];
        for ( int i = 0; i < this.l; i++ )
        {
            ba[i] = this.c;
        }
        return ba;
    }

    public View select(final long startOffset, final long len)
    {
        return new ConstView(startOffset, len);
    }

    public Region realize()
    {
        return this;
    }

    public long length()
    {
        return this.l;
    }

    public byte get(long index)
    {
        return this.c;
    }

    public Region replace(Region replaceData)
    {
        throw new UnsupportedOperationException("this special constant Region is immutable.");
    }

    @Override
    public String toString()
    {
        return "[" + this.l + "*'" + this.c + "']";
    }
}