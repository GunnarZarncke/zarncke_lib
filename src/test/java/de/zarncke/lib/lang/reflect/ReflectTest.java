package de.zarncke.lib.lang.reflect;

import de.zarncke.lib.coll.Elements;
import de.zarncke.lib.err.GuardedTest;
import de.zarncke.lib.util.Misc;
import de.zarncke.lib.util.Q;

public class ReflectTest extends GuardedTest {
	static class Dummy {
		public String a = "abc";
		public int b = 10;
	}
	public void testGet() {
		// static
		assertSame(Misc.UTF_8, Reflect.get(Misc.class.getName(), "UTF_8").get());
		// dynamic
		assertEquals("abc", new Reflect(new Dummy()).get("a").get());
		// dynamic primitive
		assertEquals(Q.i(10), new Reflect(new Dummy()).get("b").get());
	}

	public void testCall() {
		assertEquals("abc", new Reflect("abc").call("toString").get());
	}

	public void testCreate() {
		assertEquals("", Reflect.create(String.class.getName()).get());
		assertEquals("abc", Reflect.create(String.class.getName(), "abc").get());
	}

	public void testArray() {
		Object array = Reflect.array(String.class.getName(), "", "abc").get();
		assertEquals(String.class, array.getClass().getComponentType());
		assertTrue(Elements.arrayequals(new String[] { "", "abc" }, array));
	}
}
