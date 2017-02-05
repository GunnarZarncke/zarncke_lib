package de.zarncke.lib.cache;

import java.util.Map;

final class StringMapUsage extends AbstractMapUsage {
	private static final int BYTES_PER_STRING_OBJECT = 32;

	StringMapUsage(final Map<?, String> map, final String name) {
		super(map, name);
	}

	@Override
	public int getTypicalObjectSize() {
		int n = this.map.size();
		if (n == 0) {
			return BYTES_PER_STRING_OBJECT;
		}
		n = (int) Math.sqrt(n);
		int t = 0;
		int i = 0;
		for (String s : ((Map<?, String>) this.map).values()) {
			t += s.length() * 2 + BYTES_PER_STRING_OBJECT;
			if (i++ > n) {
				break;
			}
		}
		return t / n;
	}

}