package de.zarncke.lib.unit;

import org.junit.Test;

import de.zarncke.lib.err.GuardedTest;
import de.zarncke.lib.err.Warden;

public class UnitTest extends GuardedTest {
	private UnitSystem units;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		this.units = new UnitSystem();
	}
	@Test
	public void testValues() {
		Value a3 = this.units.byName("A").value(3);
		Value a4 = this.units.byName("A").value(4);
		assertEquals(3 + 4, a3.plus(a4).getAmount(), 0.00001);

		this.units.add(new DerivedUnit("A'", this.units.byName("A"), 2));
		Value as1 = this.units.byName("A'").value(1);
		assertEquals(3 + 2 * 1, a3.plus(as1).getAmount(), 0.00001);
	}

	@Test
	public void testIllegal() {
		Value a3 = this.units.byName("A").value(3);
		Value a4 = this.units.byName("B").value(4);
		try {
			a3.plus(a4);
			fail("must fail -> incompatible");
		} catch (IllegalArgumentException e) {
			Warden.disregard(e);
		}
	}

}
