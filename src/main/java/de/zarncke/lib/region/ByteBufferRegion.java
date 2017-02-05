package de.zarncke.lib.region;

import java.nio.ByteBuffer;

import de.zarncke.lib.coll.Elements;

/**
 * A Region backed by a ByteBuffer.
 * Important note: Changes are not reflected in the backing buffer (as that'd need to be resized)
 */
public class ByteBufferRegion implements Region
{
	/**
	 * Encapsulates a selected region of a {@link ByteBufferRegion}. Performs them directly on the backing buffer.
	 */
	class Access implements Region.View
	{
		final int absoluteOffset;

		int selectionLen;

		public Access(final int absoluteOffset, final int selectionLen)
		{
			this.absoluteOffset = absoluteOffset;
			this.selectionLen = selectionLen;
		}

		@Override
		public Region replace(final Region replaceData)
		{
			return replaceInternal(this, replaceData);
		}

		@Override
		public View select(final long startOffset, final long len)
		{
			if ( startOffset < 0 || startOffset + len > this.selectionLen )
			{
				throw new ArrayIndexOutOfBoundsException("range " + startOffset + "+" + len
						+ " is out of selected region " + this);
			}
			return ByteBufferRegion.this.select(this.absoluteOffset + startOffset, len);
		}

		@Override
		public byte get(final long index)
		{
			if ( index < 0 || index >= this.selectionLen )
			{
				throw new ArrayIndexOutOfBoundsException("index " + index + " is out of selected region " + this);
			}
			return ByteBufferRegion.this.get(index + this.absoluteOffset);
		}

		@Override
		public long length()
		{
			return this.selectionLen;
		}

		@Override
		public byte[] toByteArray()
		{
			byte[] ba = new byte[this.selectionLen];
			ByteBuffer dup = ByteBufferRegion.this.data.duplicate();
			dup.limit(this.absoluteOffset + this.selectionLen);
			dup.position(this.absoluteOffset);
			dup.get(ba);

			return ba;
		}

		@Override
		public String toString()
		{
			return ByteBufferRegion.this.toString() + " at " + this.absoluteOffset + "+" + this.selectionLen;
		}

		@Override
		public Region realize()
		{
			return new ByteBufferRegion(toByteArray());
		}

	}

	public static final ByteBufferRegion EMPTY = new ByteBufferRegion(Elements.NO_BYTES);

	ByteBuffer data;

	public ByteBufferRegion()
	{
		this.data = null;
	}

	/**
	 * Just {@link ByteBuffer#duplicate() duplicates} the buffer; no copy.
	 *
	 * @param initialData != null
	 */
	public ByteBufferRegion(final ByteBuffer initialData)
	{
		this.data = initialData.duplicate();
	}

	public ByteBufferRegion(final byte[] initialData)
	{
		init(initialData);
	}

	private void init(final byte[] initialData)
	{
		this.data = ByteBuffer.allocate(initialData.length);
		this.data.put(initialData);
	}

	public ByteBufferRegion(final Region insertedData)
	{
		if ( insertedData instanceof ByteBufferRegion )
		{
			ByteBuffer src = ((ByteBufferRegion) insertedData).data;
			src.clear();
			this.data = copyBuffer(src);
		}
		else
		{
			init(insertedData.toByteArray());
		}
	}

	public static ByteBuffer copyBuffer(final ByteBuffer src)
	{
		ByteBuffer newBuffer = ByteBuffer.allocate(src.remaining());
		newBuffer.put(src);
		return newBuffer;
	}

	@Override
	public byte get(final long index)
	{
		return this.data.get(checkToInt(index));
	}

	private static int checkToInt(final long index)
	{
		if ( index < 0 || index > Integer.MAX_VALUE )
		{
			throw new ArrayIndexOutOfBoundsException("ByteBufferRegion doesn't support long.");
		}
		return (int) index;
	}

	Region replaceInternal(final Access access, final Region replaceData)
	{
		// TODO optimize double copy away
		return delete(access.absoluteOffset, access.selectionLen).insert(access.absoluteOffset,
				replaceData.toByteArray());
	}

	private ByteBufferRegion delete(final long targetPos, final long deletedLen)
	{
		if ( deletedLen == 0 )
		{
			return this;
		}

		int tPos = checkToInt(targetPos);
		int delLen = checkToInt(deletedLen);
		ByteBuffer oldData = this.data;
		int newLen = oldData.capacity() - delLen;
		ByteBuffer newData = ByteBuffer.allocate(newLen);
		oldData.position(0).limit(tPos);
		newData.put(oldData);
		oldData.clear().position(tPos + delLen);
		newData.put(oldData);

		return new ByteBufferRegion(newData);
	}

	private ByteBufferRegion insert(final long targetPos, final byte[] inserted)
	{
		int tpos = checkToInt(targetPos);
		int insLen = inserted.length;
		if ( 0 == insLen )
		{
			return this;
		}

		ByteBuffer oldData = this.data;
		ByteBuffer newData = ByteBuffer.allocate(oldData.capacity() + insLen);
		oldData.position(0).limit(tpos);
		newData.put(oldData);
		newData.put(inserted);
		oldData.limit(oldData.capacity());
		newData.put(oldData);

		return new ByteBufferRegion(newData);
	}

	@Override
	public long length()
	{
		return this.data.capacity();
	}

	/**
	 * Possibly exposes internal structure (copy only if no backing array!)
	 */
	@Override
	public byte[] toByteArray()
	{
		if ( this.data.hasArray() )
		{
			return this.data.array();
		}

		byte[] buf = new byte[this.data.capacity()];
		this.data.get(buf, 0, buf.length);
		return buf;
	}

	public Region replace(final Region replaceData)
	{
		return new ByteBufferRegion(replaceData.toByteArray());
	}

	@Override
	public View select(final long selectionOffset, final long selectionLen)
	{
		long endPos = selectionOffset + selectionLen;
		if ( selectionOffset < 0 || endPos > length() )
		{
			throw new ArrayIndexOutOfBoundsException(selectionOffset + "..." + endPos + " out of range of data at "
					+ length());
		}
		return new Access(checkToInt(selectionOffset), checkToInt(selectionLen));
	}

	@Override
	public String toString()
	{
		return Elements.toString(toByteArray());
	}

	@Override
	public Region realize()
	{
		ByteBuffer copy =
			this.data.isDirect() ? ByteBuffer.allocateDirect(this.data.capacity())
					: ByteBuffer.allocate(this.data.capacity());
			copy.put(toByteBuffer());
			return new ByteBufferRegion(copy);
	}

	public ByteBuffer toByteBuffer()
	{
		ByteBuffer duplicate = this.data.duplicate();
		duplicate.clear();
		return duplicate;
	}
}