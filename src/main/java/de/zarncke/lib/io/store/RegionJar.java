package de.zarncke.lib.io.store;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import de.zarncke.lib.io.IOTools;
import de.zarncke.lib.region.ByteBufferRegion;
import de.zarncke.lib.region.Region;

/**
 * @author gunnar
 */
public class RegionJar extends AbstractMutableJar {
	private FileChannel backingChannel;

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

	public void init(final File backingFile) throws IOException {
		// Open the file and then get a channel from the stream
		@SuppressWarnings("resource" /* is closed when the backingChannel is closed */)
		RandomAccessFile raf = new RandomAccessFile(backingFile, "rw");
		this.backingChannel = raf.getChannel();
		this.mappedSize = this.backingChannel.size();

		this.baseBuffer = this.backingChannel.map(FileChannel.MapMode.READ_WRITE, 0, this.mappedSize);
	}

	public void init(final Region dataRegion) {
		this.mappedSize = dataRegion.length();
		if (dataRegion instanceof ByteBufferRegion) {
			this.baseBuffer = ((ByteBufferRegion) dataRegion).toByteBuffer();
		} else {
			// TODO this may cost a lot of memory
			this.baseBuffer = ByteBuffer.wrap(dataRegion.toByteArray());
		}
	}

	@Override
	public void close() {
		this.baseBuffer = null;
		IOTools.forceClose(this.backingChannel);
	}


	@Override
	protected ByteBuffer getBaseBuffer() {
		return this.baseBuffer;
	}

	@Override
	protected long getMappedSize() {
		return this.mappedSize;
	}
}
