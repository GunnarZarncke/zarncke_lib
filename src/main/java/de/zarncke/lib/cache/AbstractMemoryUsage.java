package de.zarncke.lib.cache;


/**
 * Base class for memory control. All saved* methods return 0. {@link #getTypicalObjectSize()} SHOULD be overridden.
 *
 * @author Gunnar Zarncke
 */
public abstract class AbstractMemoryUsage implements MemoryUsage {
	private static final int MINIMUM_OBJECT_SIZE = 32;
	private final String name;

	public AbstractMemoryUsage(final String name) {
		this.name = name;
	}

	@Override
	public double getSavedNetworkCallsPerObject() {
		return 0;
	}

	@Override
	public long getSavedNetworkBytesPerObject() {
		return getTypicalObjectSize() / 4;
	}

	@Override
	public double getSavedLocalNetworkCallsPerObject() {
		return 0;
	}

	@Override
	public double getSavedDiskSeeksPerObject() {
		return 0;
	}

	@Override
	public long getSavedDiskBytesPerObject() {
		return 0;
	}

	@Override
	public double getSavedBopsPerObject() {
		return 0;
	}

	@Override
	public String getMemoryName() {
		return this.name;
	}

	@Override
	public int getMaxObjectSize() {
		return getTypicalObjectSize();
	}

	@Override
	public long getAllocatedBytes() {
		return -1;
	}


	@Override
	public String toString() {
		return this.name;
	}
}