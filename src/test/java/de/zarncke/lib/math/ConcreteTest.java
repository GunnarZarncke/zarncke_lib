package de.zarncke.lib.math;

import junit.framework.Assert;

import org.junit.Test;

public class ConcreteTest {

	@Test
	public void testMod() {
		Assert.assertEquals(0, Concrete.mod(-303, 3));
		Assert.assertEquals(1, Concrete.mod(-302, 3));
		Assert.assertEquals(2, Concrete.mod(-301, 3));
		Assert.assertEquals(0, Concrete.mod(-3, 3));
		Assert.assertEquals(1, Concrete.mod(-2, 3));
		Assert.assertEquals(2, Concrete.mod(-1, 3));
		Assert.assertEquals(0, Concrete.mod(0, 3));
		Assert.assertEquals(1, Concrete.mod(1, 3));
		Assert.assertEquals(2, Concrete.mod(2, 3));
		Assert.assertEquals(0, Concrete.mod(3, 3));
		Assert.assertEquals(1, Concrete.mod(4, 3));
		Assert.assertEquals(2, Concrete.mod(5, 3));
		Assert.assertEquals(0, Concrete.mod(333, 3));
		Assert.assertEquals(1, Concrete.mod(334, 3));
		Assert.assertEquals(2, Concrete.mod(335, 3));
	}
}
