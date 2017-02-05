package de.zarncke.lib.progress;

import java.io.Serializable;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * {@link Progress} implementation allowing to set values.
 *
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
@NotThreadSafe
public class ProgressIndication implements Progress, Serializable {
	private static final long serialVersionUID = 1L;

	public static final long UNKNOWN_END = 0;

	private int count = 0;
	private int total;
	private volatile long volume = 0;
	private volatile long totalVolume;
	private final long start;
	private long end = UNKNOWN_END;

	private String currentItem;

	public ProgressIndication() {
		this.start = System.currentTimeMillis();
	}

	@Override
	public int getCount() {
		return this.count;
	}

	@Override
	public int getTotal() {
		return this.total;
	}

	@Override
	public long getVolume() {
		return this.volume;
	}

	@Override
	public long getTotalVolume() {
		return this.totalVolume;
	}

	@Override
	public long getDuration() {
		long now = System.currentTimeMillis();
		if (this.end != 0 && now > this.end) {
			return this.end-this.start;
		}
		return now - this.start;
	}

	@Override
	public long getTotalDuration() {
		return getDuration();
	}

	/**
	 * @return estimated end time (real end time if process is done
	 */
	public long getEnd() {
		return this.end;
	}

	public void setEnd(final long end) {
		this.end = end;
	}

	public long getStart() {
		return this.start;
	}

	public void setCount(final int count) {
		this.count = count;
		if (count > this.total) {
			this.total = count;
		}
	}

	public void setTotal(final int total) {
		this.total = total;
	}

	public void setVolume(final long volume) {
		this.volume = volume;
		if (volume > this.totalVolume) {
			this.totalVolume=volume;
		}
	}

	public void setTotalVolume(final long totalVolume) {
		this.totalVolume = totalVolume;
	}

	public void estimateEnd() {
		long now = System.currentTimeMillis();
		long vol = this.volume;
		if (this.totalVolume != 0 && vol != 0) {
			this.end = now + (now - this.start) * this.totalVolume / vol;
		}
		int cnt = this.count;
		if (this.total != 0 && cnt != 0) {
			this.end = now + (now - this.start) * this.total / cnt;
		}
	}

	public String getCurrentItem() {
		return this.currentItem;
	}

	public void setCurrentItem(final String getCurrentItem) {
		this.currentItem = getCurrentItem;
	}

	public int incrementAndGetCount() {
		return ++this.count;
	}

	public long addAndGetVolume(final long addedVolume) {
		this.volume += addedVolume;
		return this.volume;
	}

	public void done() {
		setCurrentItem(null);
		setEnd(System.currentTimeMillis());
	}

	@Override
	public String toString() {
		return this.count + "/" + this.total + ";" + this.volume + "/" + this.totalVolume;
	}
}
