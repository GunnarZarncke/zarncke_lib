package de.zarncke.lib.io.store;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.Adler32;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;

import de.zarncke.lib.io.ByteBufferInputStream;
import de.zarncke.lib.region.ByteBufferRegion;
import de.zarncke.lib.region.Region;

/**
 * Base class for JARs based on backing ByteBuffer.
 * Provides read-only functions.
 *
 * @author gunnar
 */
public abstract class AbstractReadOnlyJar {
	/*
	 * Header signatures
	 */
	public static long LOCSIG = 0x04034b50L; // "PK\003\004"
	public static long EXTSIG = 0x08074b50L; // "PK\007\008"
	public static long CENSIG = 0x02014b50L; // "PK\001\002"
	public static long ENDSIG = 0x06054b50L; // "PK\005\006"

	/*
	 * Header sizes in bytes (including signatures)
	 */
	public static final int LOCHDR = 30; // LOC header size
	public static final int EXTHDR = 16; // EXT header size
	public static final int CENHDR = 46; // CEN header size
	public static final int ENDHDR = 22; // END header size

	/*
	 * Local file (LOC) header field offsets
	 */
	public static final int LOCVER = 4; // version needed to extract
	public static final int LOCFLG = 6; // general purpose bit flag
	public static final int LOCHOW = 8; // compression method
	public static final int LOCTIM = 10; // modification time
	public static final int LOCCRC = 14; // uncompressed file crc-32 value
	public static final int LOCSIZ = 18; // compressed size
	public static final int LOCLEN = 22; // uncompressed size
	public static final int LOCNAM = 26; // filename length
	public static final int LOCEXT = 28; // extra field length

	/*
	 * Extra local (EXT) header field offsets
	 */
	public static final int EXTCRC = 4; // uncompressed file crc-32 value
	public static final int EXTSIZ = 8; // compressed size
	public static final int EXTLEN = 12; // uncompressed size

	/*
	 * Central directory (CEN) header field offsets
	 */
	public static final int CENVEM = 4; // version made by
	public static final int CENVER = 6; // version needed to extract
	public static final int CENFLG = 8; // encrypt, decrypt flags
	public static final int CENHOW = 10; // compression method
	public static final int CENTIM = 12; // modification time
	public static final int CENCRC = 16; // uncompressed file crc-32 value
	public static final int CENSIZ = 20; // compressed size
	public static final int CENLEN = 24; // uncompressed size
	public static final int CENNAM = 28; // filename length
	public static final int CENEXT = 30; // extra field length
	public static final int CENCOM = 32; // comment length
	public static final int CENDSK = 34; // disk number start
	public static final int CENATT = 36; // internal file attributes
	public static final int CENATX = 38; // external file attributes
	public static final int CENOFF = 42; // LOC header offset

	/*
	 * End of central directory (END) header field offsets
	 */
	public static final int ENDSUB = 8; // number of entries on this disk
	public static final int ENDTOT = 10; // total number of entries
	public static final int ENDSIZ = 12; // central directory size in bytes
	public static final int ENDOFF = 16; // offset of first CEN header
	public static final int ENDCOM = 20; // zip file comment length

	static class DeferredPathEntry extends AbstractStore {

		private final String path;
		private final String name;

		private final Directory topLevelDirectory;

		public DeferredPathEntry(final String path, final String name, final Directory topLevelDirectory) {
			this.name = name;
			this.path = path;
			this.topLevelDirectory = topLevelDirectory;
		}

		@Override
		public boolean exists() {
			return false;
		}

		@Override
		public Store element(final String elementName) {
			return this.topLevelDirectory.locate(this.path, elementName);
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public Store getParent() {
			return this.topLevelDirectory;
		}

		@Override
		public boolean iterationSupported() {
			return true;
		}

		@Override
		public Iterator<Store> iterator() {
			return this.topLevelDirectory.iterator();
		}

	}

	public class Directory extends AbstractStore {
		protected int endPos;

		protected int dirPos;

		protected List<Entry> entries;

		protected Directory(final List<Entry> entries, final int dirPos, final int endPos) {
			this.entries = entries;
			this.dirPos = dirPos;
			this.endPos = endPos;
		}

		@Override
		public boolean exists() {
			return true;
		}

		public List<Entry> getEntries() {
			return this.entries;
		}

		@Override
		public Store element(final String name) {
			return locate(null, name);
		}

		Store locate(final String path, final String name) {
			String childPath = path == null ? name : path + "/" + name;
			for (Entry e : this.entries) {
				if (e.getName().equals(childPath)) {
					return e;
				}
			}
			return new DeferredPathEntry(path, name, this);
		}

		@Override
		public String getName() {
			return null;
		}

		@Override
		@SuppressWarnings("unchecked")
		public Iterator<Store> iterator() {
			// TODO expand path of entries (Map.hierarchify)
			return ((List<Store>) (List) getEntries()).iterator();
		}

		@Override
		public boolean iterationSupported() {
			return true;
		}

		int findMaxExtendOf(final Entry entry) {
			int bestOffset = this.endPos;
			for (Entry e : this.entries) {
				if (e.offsetOfLocalHeader > entry.offsetOfData && e.offsetOfLocalHeader < bestOffset) {
					bestOffset = e.offsetOfLocalHeader;
				}
			}
			return bestOffset;
		}
	}

	public class Entry extends AbstractStore {
		protected String name;

		protected int offsetOfLocalHeader;

		protected int offsetOfDirEntry;

		protected int offsetOfData;

		protected int size;

		protected int csize;

		protected int method;

		protected Entry(final String name, final int size, final int csize, final int offsetOfLocalHeader,
				final int offsetOfDirEntry, final int offsetOfData, final int method) {
			this.name = name;
			this.size = size;
			this.csize = csize;
			this.offsetOfLocalHeader = offsetOfLocalHeader;
			this.offsetOfDirEntry = offsetOfDirEntry;
			this.offsetOfData = offsetOfData;
			this.method = method;
		}

		@Override
		public boolean exists() {
			return true;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public long getSize() {
			return this.size;
		}

		@Override
		public String toString() {
			return this.name + " at " + this.offsetOfLocalHeader + "/" + this.offsetOfDirEntry + " with " + this.size
					+ " bytes at " + this.offsetOfData;
		}

		/**
		 * @return a ByteBuffer containing the containers data area with its position set to 0 and its limit set to the
		 * end of the area
		 */
		protected ByteBuffer getDataView() {
			ByteBuffer duplicate = getBaseBuffer().duplicate();
			int end = this.csize >= 0 ? this.csize : AbstractReadOnlyJar.this.directory.findMaxExtendOf(this);
			duplicate.position(this.offsetOfData).limit(this.offsetOfData + end);
			return duplicate.slice();
		}

		@Override
		public InputStream getInputStream() throws IOException {
			ByteBufferInputStream bbin = new ByteBufferInputStream(getDataView());
			switch (this.method) {
			case ZipEntry.STORED:
				return bbin;
			case ZipEntry.DEFLATED:
				return new InflaterInputStream(bbin, new Inflater(true), 4096);
			default:
				throw new IOException("unknown compression method " + this.method);
			}
		}

		@Override
		public Store getParent() {
			return AbstractReadOnlyJar.this.directory;
		}

		@Override
		public boolean canRead() {
			return true;
		}

		@Override
		public Region asRegion() {
			return new ByteBufferRegion(getDataView());
		}

	}

	static class InconsistentDirectory extends Exception {
		private static final long serialVersionUID = 1L;
	}

	long mappedSize;

	private Directory directory;

	protected abstract void ensureMapping(final long newSize) throws IOException;

	public abstract void close();

	protected synchronized Directory getCentralDirectory() throws IOException {
		if (this.directory == null) {
			this.directory = readCentralDirectory();
		}
		return this.directory;
	}

	public Store getStore() throws IOException {
		return getCentralDirectory();
	}

	private Directory readCentralDirectory() throws IOException {
		ByteBuffer fullBuffer = getBaseBuffer().duplicate();
		fullBuffer.order(ByteOrder.LITTLE_ENDIAN);

		List<Entry> entries = null;

		int endPos = fullBuffer.limit() - ENDHDR;
		while (endPos >= 0) {
			int sig = fullBuffer.getInt(endPos);
			try {
				if (sig == ENDSIG) {
					// we only support one disk zips
					if (fullBuffer.getShort(endPos + 4) != 0 || fullBuffer.getShort(endPos + 6) != 0) {
						throw new InconsistentDirectory();
					}
					int entryNum = fullBuffer.getShort(endPos + ENDSUB);
					if (fullBuffer.getShort(endPos + ENDTOT) != entryNum) {
						throw new InconsistentDirectory();
					}

					int len = fullBuffer.getInt(endPos + ENDSIZ);
					int dirPos = fullBuffer.getInt(endPos + ENDOFF);

					if (dirPos + len != endPos) {
						throw new InconsistentDirectory();
					}
					if (dirPos <= 0 || dirPos > endPos) {
						throw new InconsistentDirectory();
					}

					// we found a plausible end marker

					entries = new LinkedList<Entry>();

					int entryPos = dirPos;
					for (int i = 0; i < entryNum; i++) {
						if (fullBuffer.getInt(entryPos) != CENSIG) {
							throw new InconsistentDirectory();
						}

						int nameLen = fullBuffer.getShort(entryPos + CENNAM);
						if (entryPos + nameLen > endPos) {
							throw new InconsistentDirectory();
						}
						String name = extractString(fullBuffer, entryPos + CENHDR, nameLen);

						// int flag = fullBuffer.getShort(entryPos + CENFLG);
						int method = fullBuffer.getShort(entryPos + CENHOW);
						int csize = fullBuffer.getInt(entryPos + CENSIZ);
						int size = fullBuffer.getInt(entryPos + CENLEN);

						int extLen = fullBuffer.getShort(entryPos + CENEXT);
						int commentLen = fullBuffer.getShort(entryPos + CENCOM);
						int localHeaderOffset = fullBuffer.getInt(entryPos + CENOFF);

						int totalHeaderLen = CENHDR + nameLen + extLen + commentLen;

						Entry e = readLocalHeader(localHeaderOffset);
						if (!e.name.equals(name)) {
							throw new InconsistentDirectory();
						}
						Entry createdEntry = createEntry(entryPos, name, method, csize, size, localHeaderOffset, e.offsetOfData);
						entries.add(createdEntry);

						entryPos += totalHeaderLen;
						if (entryPos > endPos) {
							throw new InconsistentDirectory();
						}
					}

					if (entryPos != endPos) {
						throw new InconsistentDirectory();
					}

					return createDirectory(entries, endPos, dirPos);
				}
			} catch (InconsistentDirectory e) {
				// try next possible offset
			}

			endPos--;
		}

		throw new IOException("no valid central directory found");
	}

	protected Entry createEntry(final int entryPos, final String name, final int method, final int csize, final int size,
			final int localHeaderOffset, final int offsetOfData) {
		return new Entry(name, size, csize, localHeaderOffset, entryPos, offsetOfData, method);
	}

	protected Directory createDirectory(final List<Entry> entries, final int endPos, final int dirPos) {
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
		ByteBuffer fullBuffer = getBaseBuffer().duplicate();
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

	protected long getMappedSize() {
		return this.mappedSize;
	}

	protected abstract ByteBuffer getBaseBuffer();
}
