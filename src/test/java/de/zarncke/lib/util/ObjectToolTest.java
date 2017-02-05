package de.zarncke.lib.util;

import java.io.Serializable;

import org.junit.Test;

import de.zarncke.lib.err.GuardedTest;
import de.zarncke.lib.region.Region;

public class ObjectToolTest extends GuardedTest {

	private static class T implements Serializable {
		private static final long serialVersionUID = 1L;
		private final int i;
		private final String s;

		public T(final int i, final String s, final T p) {
			this.i = i;
			this.s = s;
			this.p = p;
		}

		private final T p;
	}

	@Test
	public void testSerialization() {
		T test = new T(1, "t1", null);
		T test2 = new T(2, "t2", test);
		Region serialized = ObjectTool.serialize(test2);
		T res = ObjectTool.deserialize(serialized, T.class);
		assertEquals(res.i, 2);
		assertEquals(res.s, "t2");
		assertEquals(res.p.i, 1);
		assertEquals(res.p.s, "t1");
		assertEquals(res.p.p, null);
	}
}
