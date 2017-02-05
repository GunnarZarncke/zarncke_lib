package de.zarncke.lib.progress;

/**
 * Default {@link Progress} implementation returning default values.
 * 
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
public abstract class AbstractProgress implements Progress {
	@Override
	public int getCount() {
		return 1;
	}

	@Override
	public int getTotal() {
		return getCount();
	}

	@Override
	public long getVolume() {
		return 0;
	}

	@Override
	public long getTotalVolume() {
		return getVolume();
	}

	@Override
	public long getDuration() {
		return 0;
	}

	@Override
	public long getTotalDuration() {
		return getDuration();
	}
}
