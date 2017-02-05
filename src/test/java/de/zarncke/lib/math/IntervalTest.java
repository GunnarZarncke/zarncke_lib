package de.zarncke.lib.math;

import static de.zarncke.lib.math.Intervals.EMPTY;
import static de.zarncke.lib.math.Intervals.of;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.log.Log;
import de.zarncke.lib.math.Intervals.Interval;

/**
 * Test Intervals handling comprehensively.
 *
 * @author Gunnar Zarncke
 * @clean 22.03.2012
 */
public class IntervalTest extends TestCase {
	private static final int a = 0;
	private static final int b = 10;
	private static final int c = 20;
	private static final int d = 30;
	private static final int e = 40;
	private static final int f = 50;
	private static final int g = 60;

	private static final Interval aa = Interval.of(a, a);
	private static final Interval bb = Interval.of(b, b);

	private static final Interval ab = Interval.of(a, b);
	private static final Interval bc = Interval.of(b, c);
	private static final Interval cd = Interval.of(c, d);
	private static final Interval de = Interval.of(d, e);
	private static final Interval ef = Interval.of(e, f);
	private static final Interval fg = Interval.of(f, g);

	private static final Interval ac = Interval.of(a, c);
	private static final Interval bd = Interval.of(b, d);
	private static final Interval ce = Interval.of(c, e);
	private static final Interval df = Interval.of(d, f);
	private static final Interval eg = Interval.of(e, g);

	private static final Interval ad = Interval.of(a, d);
	private static final Interval be = Interval.of(b, e);
	private static final Interval cf = Interval.of(c, f);

	private static final Interval ae = Interval.of(a, e);
	private static final Interval bf = Interval.of(b, f);

	private static final Interval af = Interval.of(a, f);
	private static final Interval bg = Interval.of(b, g);
	private static final Interval ag = Interval.of(a, g);

	public void testNormalize() {
		assertEquals(EMPTY, Intervals.ofNormalized());
		assertEquals(EMPTY, Intervals.ofNormalized((Interval) null));
		assertEquals(of(cd), Intervals.ofNormalized(null, cd, null));
		assertEquals(of(ab, cd), Intervals.ofNormalized(cd, ab));
		assertEquals(of(ad), Intervals.ofNormalized(ab, bc, bd));
		assertEquals(of(ad), Intervals.ofNormalized(ad, bc));
		assertEquals(of(ad), Intervals.ofNormalized(bc, ad));
		assertEquals(of(ad), Intervals.ofNormalized(ac, bd));
	}

	public void testSimpleJoin() {
		assertEquals(ab, ab.join(null));
		assertEquals(ac, ab.join(bc));
		assertEquals(ac, ab.join(ac));
		assertEquals(ac, ac.join(ab));
		assertEquals(ad, ab.join(cd));
	}

	public void testSimpleIntersect() {
		assertEquals(null, ab.intersect(null));
		assertEquals(ab, ab.intersect(ab));
		assertEquals(ab, ab.intersect(ac));
		assertEquals(null, ab.intersect(bc));
		assertEquals(null, ab.intersect(cd));
		assertEquals(bc, ad.intersect(bc));
	}

	public void testIntervalsJoin() {
		assertEquals(EMPTY, EMPTY.join(EMPTY));
		assertEquals(of(ab), of(ab).join(EMPTY));
		assertEquals(of(ab), EMPTY.join(of(ab)));
		assertEquals(of(ab), of(ab).join(of(ab)));

		assertEquals(of(ab, cd), EMPTY.join(of(ab, cd)));
		assertEquals(of(ab, cd), of(ab, cd).join(EMPTY));

		// separate cases
		assertEquals(of(ab, cd), EMPTY.join(of(ab, cd)));
		assertEquals(of(ab, cd), of(ab).join(of(cd)));
		assertEquals(of(ab, cd), of(cd).join(of(ab)));

		// neighbor cases
		assertEquals(of(ac), of(ab).join(of(bc)));
		assertEquals(of(ac), of(bc).join(of(ab)));

		// contained cases
		assertEquals(of(ad), of(ad).join(of(bc)));
		assertEquals(of(ad), of(bc).join(of(ad)));

		// overlap cases
		assertEquals(of(ad), of(bd).join(of(ac)));
		assertEquals(of(ad), of(ac).join(of(bd)));

		// multi cases
		assertEquals(of(ad), of(ab).join(of(cd)).join(of(bc)));
		assertEquals(of(ab, cd, ef), of(ab, cd).join(of(cd, ef)));
		assertEquals(of(ae), of(ac, de).join(of(be)));
		assertEquals(of(af), of(ac, de).join(of(bf)));
		assertEquals(of(ag), of(ac, de).join(of(bg)));
		assertEquals(of(af), of(ac, df).join(of(be)));

		Intervals r = of(de).join(fg);
		assertEquals(of(de, fg), r);
		r = r.join(cd);
		assertEquals(of(ce, fg), r);
		r = r.join(bc);
		assertEquals(of(be, fg), r);
		r = r.join(ab);
		assertEquals(of(ae, fg), r);
		r = r.join(ef);
		assertEquals(of(ag), r);

		assertEquals(of(ab, cd, ef), of(ef).join(of(ab)).join(of(cd)));
	}

	public void testIntervalsJoinStress() {
		Random rnd = new Random(4711);
		Interval[] ivs = new Interval[] { ab, bc, cd, de, ef, fg };
		for (int i = 0; i < 1000; i++) {
			Intervals res = EMPTY;
			List<Intervals.Interval> control = L.l();
			Collections.shuffle(Arrays.asList(ivs), rnd);
			for (Interval iv : ivs) {
				res = res.join(iv);

				// the control is created by using the naturally ordered intervals
				control.add(iv);
				Collections.sort(control, new Intervals.StartComparator());
				assertEquals(Intervals.ofNormalized(control.toArray(new Interval[control.size()])), res);
			}
			if (!of(ag).equals(res)) {
				String msg = "stress try " + i + " with " + Arrays.asList(ivs) + " didn't lead to " + ag + " but " + res;
				Log.LOG.get().report(msg);
				fail(msg);
			}
		}
	}

	public void testIntervalsIntersectSingle() {
		assertEquals(EMPTY, EMPTY.intersect(EMPTY));
		assertEquals(EMPTY, of(ab).intersect(EMPTY));
		assertEquals(EMPTY, EMPTY.intersect(of(ab)));
		assertEquals(EMPTY, of(ab).intersect(of(bc)));
		assertEquals(EMPTY, of(bc).intersect(of(ab)));
		assertEquals(EMPTY, of(ab, cd).intersect(of(bc)));

		// single intersects
		assertEquals(of(ab), of(ab).intersect(of(ab)));
		assertEquals(of(ab), of(ab).intersect(of(ac)));
		assertEquals(of(ab), of(ac).intersect(of(ab)));
		assertEquals(of(bc), of(bc).intersect(of(ac)));
		assertEquals(of(bc), of(ac).intersect(of(bc)));
		assertEquals(of(bc), of(ad).intersect(of(bc)));
		assertEquals(of(bc), of(bc).intersect(of(ad)));

		// cut away
		assertEquals(of(ab), of(ab, cd).intersect(of(ab)));
		assertEquals(of(ab), of(ac, de).intersect(of(ab)));
		assertEquals(of(cd), of(cd, ef).intersect(of(ae)));
		assertEquals(of(bc), of(ac, de).intersect(of(bd)));

		assertEquals(of(cd), of(ab, cd, ef).intersect(of(be)));

		// double overlap
		assertEquals(of(bc, ef), of(ac, eg).intersect(of(bf)));

		// longer
		assertEquals(of(ab, cd), of(ab, cd, ef).intersect(of(ae)));
	}

	public void testIntervalsIntersectMulti() {
		assertEquals(EMPTY, of(ab, cd).intersect(of(bc, de, ef)));

		assertEquals(of(ab, de), of(ac, df).intersect(of(ab, ce)));
	}

	public void testIntervalsIntersectStress() {
		Random rnd = new Random(4711);
		Interval[] ivs = new Interval[] { ab, bc, cd, de, ef, fg };
		for (int i = 0; i < 1000; i++) {
			Intervals res = of(ag);
			Collections.shuffle(Arrays.asList(ivs), rnd);
			for (Interval iv : ivs) {
				Intervals neg = of(ag).intersect(iv);
				res = res.intersect(neg);
			}
			if (!EMPTY.equals(res)) {
				String msg = "stress try " + i + " with " + Arrays.asList(ivs) + " didn't lead to " + ag + " but " + res;
				Log.LOG.get().report(msg);
				fail(msg);
			}
		}
	}

	// public void testEmptyIntervals() {
	// assertEquals(EMPTY, of(aa).simplify());
	//
	// // single intersects
	// assertEquals(of(ab), of(aa).join(of(ab)));
	// assertEquals(of(ab), of(ab).join(of(bb)));
	//
	// assertEquals(EMPTY, of(aa).intersect(of(ab)));
	// assertEquals(EMPTY, of(aa).intersect(of(ab)));
	// assertEquals(EMPTY, of(ab).intersect(of(bb)).simplify());
	// }

}
