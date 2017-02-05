package de.zarncke.lib.cache;

import java.util.Map;

import de.zarncke.lib.lang.ClassTools;

final class BaseMapUsage extends AbstractMapUsage {
	private final Class<?> elementClass;
	private final int size;

	BaseMapUsage(final Map<?, ?> map, final Class<?> elementClass, final String name) {
		super(map, name);
		this.elementClass = elementClass;
		this.size = ClassTools.estimateSize(this.elementClass);
	}

	@Override
	public int getTypicalObjectSize() {
		return this.size;
	}

}