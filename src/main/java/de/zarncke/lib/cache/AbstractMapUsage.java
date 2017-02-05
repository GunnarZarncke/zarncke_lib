package de.zarncke.lib.cache;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;

import de.zarncke.lib.err.Warden;
import de.zarncke.lib.lang.ClassTools;
import de.zarncke.lib.util.ObjectTool;

/**
 * Base class for memory control. All saved* methods return 0. {@link #getTypicalObjectSize()} SHOULD be overridden.
 *
 * @author Gunnar Zarncke
 */
public class AbstractMapUsage extends AbstractMemoryUsage {
	private static final int MINIMUM_OBJECT_SIZE = 32;
	protected final Map<?, ?> map;

	public AbstractMapUsage(final Map<?, ?> map, final String name) {
		super(name);
		this.map = map;
	}

	/**
	 * Estimates the size by sampling map entries.
	 * SHOULD be overridden.
	 *
	 * @return bytes
	 */
	@Override
	public int getTypicalObjectSize() {
		int numReports = this.map.size();
		if (numReports == 0) {
			return 32;
		}
		int actualSize = 0;
		int size = 0;
		try {
			int sampleSize = (int) Math.sqrt(numReports);
			Iterator<?> it = this.map.values().iterator();
			for (actualSize = 0; actualSize < sampleSize; actualSize++) {
				Object r = it.next();
				size += estimateSizeOfSampleEntry(r);
				size += MINIMUM_OBJECT_SIZE; // for map overhead
			}
		} catch (Exception e) {
			Warden.disregard(e);
			// ignore
		}
		if (actualSize == 0) {
			actualSize = 1;
			size = MINIMUM_OBJECT_SIZE;
		}
		return size * numReports / actualSize;
	}

	protected int estimateSizeOfSampleEntry(final Object r) {
		if (r instanceof Serializable) {
			return ObjectTool.estimateSize((Serializable) r);
		}
		if (r != null) {
			return ClassTools.estimateSize(r.getClass());
		}
		return 0;
	}

	@Override
	public int getAllocatedObjects() {
		return this.map.size();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.map == null ? 0 : this.map.hashCode());
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
		AbstractMapUsage other = (AbstractMapUsage) obj;
		return this.map == other.map;
	}

	@Override
	public String toString() {
		return getMemoryName() + "(size " + this.map.size() + "*" + getTypicalObjectSize() + ")";
	}
}