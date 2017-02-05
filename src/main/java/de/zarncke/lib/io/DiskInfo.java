package de.zarncke.lib.io;

import de.zarncke.lib.coll.Pair;
import de.zarncke.lib.data.HasName;
import de.zarncke.lib.data.HasSize;
import de.zarncke.lib.util.Misc;

/**
 * Bean for disk space. {@link HasName} is path. {@link HasSize} is MB.
 *
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
public class DiskInfo implements HasName, HasSize {
	private String path;
	private long free;
	private long total;

	public DiskInfo(final String path, final long free, final long total) {
		this.path = path;
		this.free = free;
		this.total = total;
	}

	public String getPath() {
		return this.path;
	}

	public void setPath(final String path) {
		this.path = path;
	}

	public long getFree() {
		return this.free;
	}

	public void setFree(final long free) {
		this.free = free;
	}

	public long getTotal() {
		return this.total;
	}

	public void setTotal(final long total) {
		this.total = total;
	}

	@Override
	public String toString() {
		return this.path + " " + this.free + "/" + this.total;
	}

	@Override
	public int compareTo(final HasName o) {
		return 0;
	}

	@Override
	public int size() {
		return (int) (this.total / Misc.BYTES_PER_MB);
	}

	@Override
	public String getName() {
		return this.path;
	}

	public Pair<Long, Long> toFreeAndTotal() {
		return Pair.pair(Long.valueOf(this.free), Long.valueOf(this.total));
	}
}