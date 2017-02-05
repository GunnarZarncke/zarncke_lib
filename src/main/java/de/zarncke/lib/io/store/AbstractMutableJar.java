package de.zarncke.lib.io.store;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.List;
import java.util.zip.Adler32;
import java.util.zip.ZipEntry;

import de.zarncke.lib.err.Warden;
import de.zarncke.lib.util.Misc;

/**
 * Extends {@link AbstractReadOnlyJar} with write functions.
 *
 * @author gunnar
 */
public abstract class AbstractMutableJar extends AbstractReadOnlyJar {
	private static final short DEFAULT_VERSION = 10;

	public class Directory extends AbstractReadOnlyJar.Directory {
		protected Directory(final List<AbstractReadOnlyJar.Entry> entries, final int dirPos,
				final int endPos) {
			super(entries, dirPos, endPos);
		}

		// TODO must be synchronized
		protected Entry addEntry(final String name, final int size, final String comment) throws IOException {
			if (name == null) {
				throw Warden.spot(new IllegalArgumentException("name must be given"));
			}
			Entry entry = new Entry(name, size, size, this.dirPos, -1, -1,
					ZipEntry.STORED);
			this.entries.add(entry);

			ByteBuffer dirEntryBuffer = entry.toDirEntryBuffer(comment);
			ByteBuffer localEntryBuffer = entry.toLocalEntryBuffer();

			long origSize = getMappedSize();
			long totalIncrease = entry.getSize() + dirEntryBuffer.remaining() + localEntryBuffer.remaining();
			ensureMapping(origSize + totalIncrease);

			long newDirPos = this.dirPos + localEntryBuffer.remaining() + entry.getSize();

			ByteBuffer targetBuffer = getBaseBuffer().duplicate().order(ByteOrder.LITTLE_ENDIAN);

			targetBuffer.position(this.dirPos).limit(this.endPos);
			ByteBuffer directoryUntilEnder = targetBuffer.slice();

			// copy original end area into new buffer (other modifications will overwrite this area!)
			// TODO maybe we should construct the complete new directory and write it in one flush?
			targetBuffer.position(this.endPos).limit((int) origSize);
			ByteBuffer endArea = ByteBuffer.allocate(targetBuffer.remaining()).order(ByteOrder.LITTLE_ENDIAN);
			endArea.put(targetBuffer);
			// update dir pos and size in end hdr
			endArea.putInt(ENDOFF, (int) newDirPos);
			endArea.putInt(ENDSIZ, endArea.getInt(ENDSIZ) + dirEntryBuffer.remaining());
			int entryNum = endArea.getShort(ENDSUB) + 1;
			endArea.putShort(ENDSUB, (short) entryNum);
			endArea.putShort(ENDTOT, (short) entryNum);
			endArea.clear();

			// move directory and insert new entry
			targetBuffer.limit(getBaseBuffer().limit());
			targetBuffer.position((int) newDirPos);
			targetBuffer.put(directoryUntilEnder);
			entry.setOffsetOfDirEntry(targetBuffer.position());
			targetBuffer.put(dirEntryBuffer);
			this.dirPos = (int) newDirPos;
			this.endPos = targetBuffer.position();
			targetBuffer.put(endArea);

			targetBuffer.position(entry.getOffsetOfLocalHeader());
			targetBuffer.put(localEntryBuffer);
			entry.setOffsetOfData(targetBuffer.position());
			return entry;
		}

		// TODO ensure that JAR is consistent at all times (at least contains a valid central directory)
		protected void resizeEntry(final Entry entry, final int newSize) throws IOException {
			if (entry == null) {
				throw Warden.spot(new IllegalArgumentException("entry must be present"));
			}

			long origSize = getMappedSize();
			long extra = newSize - entry.getSize();
			entry.setSize(newSize);

			ensureMapping(getMappedSize() + extra);
			long newDirPos = this.dirPos + extra;

			ByteBuffer targetBuffer = getBaseBuffer().duplicate().order(ByteOrder.LITTLE_ENDIAN);

			targetBuffer.position(this.dirPos).limit((int) origSize);
			ByteBuffer directoryUntilEnd = targetBuffer.slice();
			targetBuffer.clear();

			// move directory and end record
			targetBuffer.position((int) newDirPos);
			targetBuffer.put(directoryUntilEnd);

			entry.moveOffsetOfDirEntry(extra);
			// note: offset of other entries are not updated
			entry.updateSize();

			this.dirPos += extra;
			this.endPos += extra;

			// update dir location in end hdr
			targetBuffer.putInt(this.endPos + ENDOFF, this.dirPos);

			// do not forget to update crc later
		}

		// void updateCrc() {
		// this.container.updateCrc(getContainerView());
		// }

	}

	public class Entry extends AbstractReadOnlyJar.Entry {
		public Entry(final String name, final int size, final int csize, final int offsetOfLocalHeader,
				final int offsetOfDirEntry, final int offsetOfData, final int method) {
			super(name, size, csize, offsetOfLocalHeader, offsetOfDirEntry, offsetOfData, method);
		}

		public void moveOffsetOfDirEntry(final long delta) {
			this.offsetOfDirEntry += delta;
		}

		public void setSize(final int newSize) {
			this.size = newSize;
		}

		public void setOffsetOfData(final int position) {
			this.offsetOfData = position;
		}

		public int getOffsetOfLocalHeader() {
			return this.offsetOfLocalHeader;
		}

		public void setOffsetOfDirEntry(final int position) {
			this.offsetOfDirEntry = position;
		}

		ByteBuffer toDirEntryBuffer(final String comment) {
			ByteBuffer encodedName = Misc.UTF_8.encode(this.name);
			ByteBuffer encodedComment = Misc.UTF_8.encode(comment);
			ByteBuffer buffer = ByteBuffer.allocate(CENHDR + encodedName.remaining() + encodedComment.remaining());
			buffer.order(ByteOrder.LITTLE_ENDIAN);

			buffer.putInt((int) CENSIG);
			buffer.putShort(DEFAULT_VERSION);
			buffer.putShort(DEFAULT_VERSION);
			buffer.putShort((short) 0); // flag = 0
			buffer.putShort((short) 0); // method = 0 = stored
			buffer.putInt(AbstractMutableJar.msdosDateTime());
			buffer.putInt(0); // crc-32 - to be calculated later
			buffer.putInt(this.csize);
			buffer.putInt(this.size);
			buffer.putShort((short) encodedName.remaining());
			buffer.putShort((short) 0); // extra len
			buffer.putShort((short) encodedComment.remaining());
			buffer.putShort((short) 0); // disk
			buffer.putShort((short) 0); // int attribs
			buffer.putInt((short) 0); // ext attribs
			buffer.putInt(this.offsetOfLocalHeader);
			buffer.put(encodedName);
			buffer.put(encodedComment);

			buffer.clear();
			return buffer;
		}

		ByteBuffer toLocalEntryBuffer() {
			ByteBuffer encodedName = Misc.UTF_8.encode(this.name);
			ByteBuffer buffer = ByteBuffer.allocate(LOCHDR + encodedName.remaining());
			buffer.order(ByteOrder.LITTLE_ENDIAN);

			buffer.putInt((int) LOCSIG);
			buffer.putShort(DEFAULT_VERSION);
			buffer.putShort((short) 0); // flag = 0
			buffer.putShort((short) 0); // method = 0 = stored
			buffer.putInt(AbstractMutableJar.msdosDateTime());
			buffer.putInt(0); // crc-32 - to be calculated later
			buffer.putInt(this.csize);
			buffer.putInt(this.size);
			buffer.putShort((short) encodedName.remaining());
			buffer.putShort((short) 0); // extra len
			buffer.put(encodedName);

			buffer.clear();
			return buffer;
		}

		public void updateCrc(final ByteBuffer content) {
			int crc = AbstractMutableJar.adler32(content);
			ByteBuffer local = getBaseBuffer().duplicate();
			local.order(ByteOrder.LITTLE_ENDIAN);
			local.putInt(this.offsetOfLocalHeader + LOCCRC, crc);
			local.putInt(this.offsetOfDirEntry + CENCRC, crc);
		}

		@Override
		public ByteBuffer getDataView() {
			return super.getDataView();
			}

		public void updateSize() {
			ByteBuffer local = getBaseBuffer().duplicate();
			local.order(ByteOrder.LITTLE_ENDIAN);
			local.putInt(this.offsetOfLocalHeader + LOCSIZ, this.csize);
			local.putInt(this.offsetOfLocalHeader + LOCLEN, this.size);
			local.putInt(this.offsetOfDirEntry + CENSIZ, this.csize);
			local.putInt(this.offsetOfDirEntry + CENLEN, this.size);
		}

	}

	static class InconsistentDirectory extends Exception {
		private static final long serialVersionUID = 1L;
	}

	private FileChannel backingChannel;

	long mappedSize;

	protected ByteBuffer baseBuffer;

	private Directory directory;

	@Override
	protected void ensureMapping(final long newSize) throws IOException {
		if (newSize > this.mappedSize) {
			if (this.backingChannel == null) {
				throw new IOException("cannot resize mapping for in-memory data");
			}

			this.mappedSize = newSize;
			int pos = 0;
			if (this.baseBuffer != null) {
				pos = this.baseBuffer.position();
				this.baseBuffer = null;
			}
			this.baseBuffer = this.backingChannel.map(FileChannel.MapMode.READ_WRITE, 0, this.mappedSize);
			this.baseBuffer.position(pos);
		}
		this.baseBuffer.limit((int) newSize);
	}

	@Override
	protected Entry createEntry(final int entryPos, final String name, final int method, final int csize, final int size,
			final int localHeaderOffset, final int offsetOfData) {
		return new Entry(name, size, csize, localHeaderOffset, entryPos, offsetOfData, method);
	}

	@Override
	protected Directory createDirectory(final List<AbstractReadOnlyJar.Entry> entries, final int endPos,
			final int dirPos) {
		return new Directory(entries, dirPos, endPos);
	}

	private static String extractString(final ByteBuffer buffer, final int startPos, final int len) {
		String name = "";
		if (len > 0) {
			ByteBuffer nameBuf = buffer.duplicate();
			nameBuf.position(startPos).limit(startPos + len);
			name = Charset.forName("UTF-8").decode(nameBuf).toString();
		}
		return name;
	}

	private Entry readLocalHeader(final int offset) throws InconsistentDirectory {
		ByteBuffer fullBuffer = this.baseBuffer.duplicate();
		fullBuffer.position(offset);
		ByteBuffer buffer = fullBuffer.slice();
		buffer.order(ByteOrder.LITTLE_ENDIAN);

		int sig = buffer.getInt();
		if (sig != LOCSIG) {
			throw new InconsistentDirectory();
		}

		// get the entry name and create the ZipEntry first
		int nameLen = buffer.getShort(LOCNAM);
		String name = extractString(buffer, LOCHDR, nameLen);

		int flag = buffer.getShort(LOCFLG);
		int method = buffer.getShort(LOCHOW);
		int size = -1;
		int csize = -1;
		if ((flag & 1) == 1) {
			// throw new ZipException("encrypted ZIP entry not supported");
		}
		if ((flag & 8) == 8) {
			/* EXT descriptor present */
			// return new Entry(name, -1, -1, offset, -1, offsetOfData, method);
		} else {
			csize = buffer.getInt(LOCSIZ);
			size = buffer.getInt(LOCLEN);
		}

		int extLen = buffer.getShort(LOCEXT);
		int totalHeaderLen = LOCHDR + nameLen + extLen;
		int offsetOfData = offset + totalHeaderLen;

		return new Entry(name, size, csize, offset, -1, offsetOfData, method);
	}

	public static int adler32(final ByteBuffer buffer) {
		ByteBuffer local = buffer.duplicate();
		local.clear();
		Adler32 a = new Adler32();
		byte[] temp = new byte[4096];
		while (local.hasRemaining()) {
			int l = Math.min(local.remaining(), 4096);
			local.get(temp, 0, l);
			a.update(temp, 0, l);
		}
		return (int) a.getValue();
	}

	public static int msdosDateTime() {
		Calendar c = Calendar.getInstance();
		int dateTime = c.get(Calendar.SECOND);
		dateTime += c.get(Calendar.MINUTE) << 5;
		dateTime += c.get(Calendar.HOUR) << 11;
		dateTime += c.get(Calendar.DAY_OF_MONTH) << 16;
		dateTime += c.get(Calendar.MONTH) << 21;
		dateTime += c.get(Calendar.YEAR) - 1980 << 25;
		return dateTime;
	}

	@Override
	protected long getMappedSize() {
		return this.mappedSize;
	}
}
