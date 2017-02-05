package de.zarncke.lib.region;

import java.io.Serializable;
import java.util.Arrays;

import de.zarncke.lib.coll.Elements;

/**
 * A Region backed by a byte array. New arrays are created on all modifications.
 */
class PrimitiveRegion implements Region, Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * Encapsulates a selected region of a PrimitiveRegion. Performs them directly on the backing array.
	 */
	class Access implements Region.View {
		final int absoluteOffset;

		int selectionLen;

		public Access(final int absoluteOffset, final int selectionLen) {
			this.absoluteOffset = absoluteOffset;
			this.selectionLen = selectionLen;
		}

		@Override
		public Region replace(final Region replaceData) {
			return replaceInternal(this, replaceData);
		}

		@Override
		public View select(final long startOffset, final long len) {
			if (startOffset < 0 || startOffset + len > this.selectionLen) {
				throw new ArrayIndexOutOfBoundsException("range " + startOffset + "+" + len
						+ " is out of selected region " + this);
			}
			return PrimitiveRegion.this.select(this.absoluteOffset + startOffset, len);
		}

		@Override
		public byte get(final long index) {
			if (index < 0 || index >= this.selectionLen) {
				throw new ArrayIndexOutOfBoundsException("index " + index + " is out of selected region " + this);
			}
			return PrimitiveRegion.this.get(index + this.absoluteOffset);
		}

		@Override
		public long length() {
			return this.selectionLen;
		}

		@Override
		public byte[] toByteArray() {
			byte[] ba = new byte[this.selectionLen];
			System.arraycopy(PrimitiveRegion.this.data, this.absoluteOffset, ba, 0, this.selectionLen);

			return ba;
		}

		@Override
		public String toString() {
			return PrimitiveRegion.this.toString() + " at " + this.absoluteOffset + "+" + this.selectionLen;
		}

		@Override
		public Region realize() {
			return new PrimitiveRegion(toByteArray());
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(toByteArray());
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof Region)) {
				return false;
			}
			return Arrays.equals(toByteArray(), ((Region) obj).toByteArray());
		}
	}

	public static final byte[] NO_BYTES = new byte[0];

	public static final PrimitiveRegion EMPTY = new PrimitiveRegion(NO_BYTES);

	byte[] data;

	public PrimitiveRegion() {
		this.data = new byte[0];
	}

	// check copy on call
	public PrimitiveRegion(final byte[] initialData) // NOPMD intended
	{
		this.data = initialData;
	}

	public PrimitiveRegion(final Region insertedData) {
		this.data = insertedData.toByteArray();
	}

	@Override
	public byte get(final long index) {
		return this.data[checkToInt(index)];
	}

	int checkToInt(final long index) {
		if (index < 0 || index > Integer.MAX_VALUE) {
			throw new ArrayIndexOutOfBoundsException("PrimitveRegion doesn't support long.");
		}
		return (int) index;
	}

	Region replaceInternal(final Access access, final Region replaceData) {
		int repLen = checkToInt(replaceData.length());
		if (access.absoluteOffset == 0 && access.selectionLen == this.data.length) {
			return replaceData.realize();
		}
		if (access.selectionLen == 0 && repLen == 0) {
			return this;
		}
		if (access.selectionLen == repLen) {
			byte[] res = this.data.clone();
			System.arraycopy(replaceData.toByteArray(), 0, res, access.absoluteOffset, repLen);
			return new PrimitiveRegion(res);
		}
		return replace(access.absoluteOffset, access.selectionLen, replaceData.toByteArray());
	}

	// solve long vs int problem:
	// - long and int method
	// - more efficient check

	private PrimitiveRegion replace(final int targetPosition, final int deletedSpan, final byte[] replacement) {
		int insLen = replacement.length;
		int rightPos = targetPosition + deletedSpan;

		byte[] oldData = this.data;
		byte[] newData = new byte[oldData.length + insLen - deletedSpan];
		System.arraycopy(oldData, 0, newData, 0, targetPosition);
		System.arraycopy(replacement, 0, newData, targetPosition, insLen);
		System.arraycopy(oldData, rightPos, newData, targetPosition + insLen, oldData.length - rightPos);

		return new PrimitiveRegion(newData);
	}

	// private PrimitiveRegion delete(long targetPos, long deletedLen)
	// {
	// if ( deletedLen == 0 )
	// {
	// return this;
	// }
	//
	// int tPos = checkToInt(targetPos);
	// int delLen = checkToInt(deletedLen);
	// byte[] oldData = this.data;
	// int newLen = oldData.length - delLen;
	// byte[] newData = new byte[newLen];
	// System.arraycopy(oldData, 0, newData, 0, tPos);
	// System.arraycopy(oldData, tPos + delLen, newData, tPos, newLen - tPos);
	//
	// return new PrimitiveRegion(newData);
	// }

	// private PrimitiveRegion insert(long targetPos, byte[] inserted)
	// {
	// int tpos = checkToInt(targetPos);
	// int insLen = inserted.length;
	// if ( 0 == insLen )
	// {
	// return this;
	// }
	//
	// byte[] oldData = this.data;
	// byte[] newData = new byte[oldData.length + insLen];
	// System.arraycopy(oldData, 0, newData, 0, tpos);
	// System.arraycopy(inserted, 0, newData, tpos, insLen);
	// System
	// .arraycopy(oldData, tpos, newData, checkToInt(targetPos + insLen), checkToInt(oldData.length
	// - targetPos));
	//
	// return new PrimitiveRegion(newData);
	// }

	@Override
	public long length() {
		return this.data.length;
	}

	/**
	 * Exposes internal structure (no copy!)
	 */
	@Override
	public byte[] toByteArray() {
		return this.data;
	}

	@Override
	public View select(final long selectionOffset, final long selectionLen) {/*
																			 * if ( selectionOffset == 0 && selectionLen
																			 * == 0 ) { return new Region.View() {
																			 * public Region replace(Region
																			 * replaceData) { // fails here!: return
																			 * replaceData.realize(); }
																			 * public byte get(long index) { throw new
																			 * IndexOutOfBoundsException
																			 * ("we are empty"); }
																			 * public long length() { return 0; }
																			 * public Region realize() { return this; }
																			 * public View select(long startOffset, long
																			 * len) { if ( len != 0 || startOffset != 0
																			 * ) { throw new
																			 * IndexOutOfBoundsException("we are empty");
																			 * } return this; }
																			 * public byte[] toByteArray() { return
																			 * Elements.NO_BYTES; } }; }
																			 */
		long endPos = selectionOffset + selectionLen;
		if (selectionOffset < 0 || endPos > length()) {
			throw new ArrayIndexOutOfBoundsException(selectionOffset + "..." + endPos + " out of range of data at "
					+ length());
		}
		return new Access(checkToInt(selectionOffset), checkToInt(selectionLen));
	}

	@Override
	public String toString() {
		if (length() > 1000) {
			return Elements.byteArrayToHumanReadable(select(0, 500).toByteArray()) + "... (" + length() + " total)";
		}
		return Elements.byteArrayToHumanReadable(toByteArray());
	}

	@Override
	public Region realize() {
		byte[] copy = new byte[this.data.length];
		System.arraycopy(this.data, 0, copy, 0, this.data.length);
		return new PrimitiveRegion(copy);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(this.data);
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
		PrimitiveRegion other = (PrimitiveRegion) obj;
		if (!Arrays.equals(this.data, other.data)) {
			return false;
		}
		return true;
	}

}