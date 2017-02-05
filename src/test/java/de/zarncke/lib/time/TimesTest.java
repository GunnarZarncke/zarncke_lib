package de.zarncke.lib.time;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.ReadableInterval;
import org.junit.Test;

import de.zarncke.lib.coll.L;

public class TimesTest {
	DateTime a = t(1);
	DateTime b = t(2);
	DateTime c = t(3);
	DateTime d = t(4);
	DateTime e = t(5);

	@Test
	public void testJoin() {
		ReadableInterval ab = Times.join(new Interval(this.a, this.b), (DateTime) null);
		assertEquals(new Interval(this.a, this.b), ab);

		ReadableInterval ac = Times.join(new Interval(this.a, this.c), this.b);
		assertEquals(new Interval(this.a, this.c), ac);

		ac = Times.join(new Interval(this.a, this.b), this.c);
		assertEquals(new Interval(this.a, this.c), ac);

		ab = Times.join(new Interval(this.a, this.b), (Interval) null);
		assertEquals(new Interval(this.a, this.b), ab);

		ac = Times.join(new Interval(this.a, this.b), new Interval(this.b, this.c));
		assertEquals(new Interval(this.a, this.c), ac);

		ReadableInterval ad = Times.join(new Interval(this.a, this.c), new Interval(this.b, this.d));
		assertEquals(new Interval(this.a, this.d), ad);

		ad = Times.join(new Interval(this.a, this.b), new Interval(this.c, this.d));
		assertEquals(new Interval(this.a, this.d), ad);

		ad = Times.join(new Interval(this.a, this.c), new Interval(this.b, this.d));
		assertEquals(new Interval(this.a, this.d), ad);

		ad = Times.join(new Interval(this.a, this.d), new Interval(this.b, this.c));
		assertEquals(new Interval(this.a, this.d), ad);
	}

	private DateTime t(final int i) {
		return new DateTime(2000, 1, i, 0, 0, 0, 0, DateTimeZone.UTC);
	}

	@Test
	public void testJoin2() {
		assertEquals(L.l(new Interval(this.a, this.d)),
				Times.join(L.s(new Interval(this.a, this.c)), L.s(new Interval(this.b, this.d))));
	}

	@Test
	public void testSubtract() {
		Collection<Interval> ref = L.l(new Interval(this.a, this.b), new Interval(this.c, this.e));

		Collection<? extends ReadableInterval> res = Times.subtract(ref, null);
		assertEquals(ref, res);

		res = Times.subtract(ref, new Interval(this.a, this.e));
		assertTrue(res.isEmpty());

		res = Times.subtract(ref, new Interval(this.b, this.d));
		assertEquals(L.l(new Interval(this.a, this.b), new Interval(this.d, this.e)), res);

		res = Times.subtract(ref, new Interval(this.a, this.d));
		assertEquals(L.l(new Interval(this.d, this.e)), res);

		res = Times.subtract(ref, new Interval(this.d, this.d));
		assertEquals(L.l(new Interval(this.a, this.b), new Interval(this.c, this.d), new Interval(this.d, this.e)), res);
	}
}
