package de.zarncke.lib.io.store;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.zarncke.lib.io.RegionInputStream;
import de.zarncke.lib.region.Region;
import de.zarncke.lib.region.RegionUtil;
import de.zarncke.lib.time.JavaClock;
import de.zarncke.lib.util.Chars;

/**
 * A Store for a memory region.
 *
 * @author Gunnar Zarncke
 */
public class MemStore extends AbstractStore {
	public static Store makeFrom(final String content, final String name) {
		return new MemStore(RegionUtil.asRegion(content.getBytes()), name);
	}

	// TODO replace with outer region?
	private Region content;

	private long lastModMillis;
	private final String name;

	private final Store parent;

	public MemStore() {
		this(RegionUtil.EMPTY);
	}

	public MemStore(final Region initialContent) {
		this(initialContent, null);
	}

	public MemStore(final Region initialContent, final String name) {
		this(initialContent, name, null);
	}

	public MemStore(final Region initialContent, final String name, final Store parent) {
		this.parent = parent;
		this.content = initialContent;
		this.name = name;
		accessed();
	}

	@Override
	public boolean exists() {
		return true;
	}

	private void accessed() {
		this.lastModMillis = JavaClock.getTheClock().getCurrentTimeMillis();
	}

	@Override
	public Store getParent() {
		return this.parent;
	}

	@Override
	public long getLastModified() {
		return this.lastModMillis;
	}

	@Override
	public boolean setLastModified(final long millis) {
		this.lastModMillis = millis;
		return true;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public boolean canRead() {
		return true;
	}

	@Override
	public boolean canWrite() {
		return true;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return new RegionInputStream(this.content);
	}

	/**
	 * The content of this MemStore will only be replaced (updated) when the OutputStream is flushed or closed.
	 */
	@Override
	public OutputStream getOutputStream(final boolean append) throws IOException {
		final long initLen = this.content.length();
		// TODO this will break down on large updates
		// TODO use Region.select() for incremental updating.
		return new ByteArrayOutputStream() {
			@Override
			public void flush() throws IOException {
				super.flush();
				capture();
			}

			@Override
			public void close() throws IOException {
				capture();
				super.close();
			}
			public void capture() {
				if (append) {
					MemStore.this.content = MemStore.this.content.select(initLen, MemStore.this.content.length() - initLen).replace(
							RegionUtil.asRegion(toByteArray())).realize();
				} else {
					MemStore.this.content = RegionUtil.asRegion(toByteArray());
				}
				accessed();
			}

		};
	}

	@Override
	public long getSize() {
		return this.content.length();
	}

	@Override
	public Region asRegion() {
		return this.content;
	}

	@Override
	public String toString() {
		return Chars.summarize(this.content.toString(), 80).toString();
	}
}
