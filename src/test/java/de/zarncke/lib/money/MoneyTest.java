package de.zarncke.lib.money;

import java.math.BigDecimal;
import java.util.Currency;

import de.zarncke.lib.err.GuardedTest;
import de.zarncke.lib.err.Warden;

public class MoneyTest extends GuardedTest {
	public void testZero() {
		MAmount euro0 = MAmount.zero(Money.EURO);
		MAmount usd0 = MAmount.zero(Currency.getInstance("USD"));
		assertEquals(euro0, euro0);
		assertFalse(euro0.equals(usd0));
		assertEquals(euro0, MAmount.of(euro0));
		assertEquals(euro0, MAmount.of(BigDecimal.ZERO, Money.EURO));
		assertEquals(BigDecimal.ZERO, euro0.getAmount());
	}

	public void testSimple() {
		MAmount euro0 = MAmount.zero(Money.EURO);
		MAmount euro1 = MAmount.of(1, 0, Money.EURO);
		MAmount usd1 = MAmount.of(1, 0, Currency.getInstance("USD"));
		assertFalse(euro1.equals(usd1));
		assertEquals(euro1, Cash.plus(euro0, euro1));
		assertEquals(euro0, Cash.plus(euro1, Cash.negate(euro1)));
	}

	public void testComparable() {
		MAmount euro1 = MAmount.of(1, 0, Money.EURO);
		MAmount usd1 = MAmount.of(1, 0, Currency.getInstance("USD"));
		try {
			Cash.plus(euro1, usd1);
			fail("may not add differnt currencies");
		} catch (Exception e) {
			Warden.disregard(e);
		}
		try {
			euro1.compareTo(usd1);
			fail("may not compare differnt currencies");
		} catch (Exception e) {
			Warden.disregard(e);
		}
	}

	public void testCompare() {
		MAmount euro0 = MAmount.zero(Money.EURO);
		MAmount euro1 = MAmount.of(1, 0, Money.EURO);
		assertTrue("=", euro0.compareTo(euro0) == 0);
		assertTrue("<", euro0.compareTo(euro1) < 0);
		assertTrue(">", euro1.compareTo(euro0) > 0);
	}
}
