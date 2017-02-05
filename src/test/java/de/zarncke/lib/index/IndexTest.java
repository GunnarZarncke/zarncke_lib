package de.zarncke.lib.index;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Test;
import org.mockito.Mockito;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.err.GuardedTest;
import de.zarncke.lib.index.crit.IntervalMatch;
import de.zarncke.lib.math.Intervals;
import de.zarncke.lib.time.HasDateTime;
import de.zarncke.lib.time.HasInterval;
import de.zarncke.lib.time.Times;

public class IndexTest extends GuardedTest {

	private static final int OUT_OF_LIST = 3;

	private static final DateTime DT1 = Times.utc(2010, 1, 1, 9, 30);
	private static final DateTime DT2 = Times.utc(2010, 1, 2, 9, 30);
	private static final DateTime DT3 = Times.utc(2010, 1, OUT_OF_LIST, 9, 30);
	private static final DateTime DT4 = Times.utc(2010, 1, 4, 9, 30);
	private static final DateTime DT5 = Times.utc(2010, 1, 5, 9, 30);
	private static final DateTime DT6 = Times.utc(2010, 1, 6, 9, 30);

	static interface Mix extends HasInterval, HasDateTime {
		// just both
	}

	@Test
	public void testIndex() {
		Mix dt1 = makeMix(DT1);
		Mix dt2 = makeMix(DT2);
		Mix dt2b = makeMix(DT2);
		Mix dt3 = makeMix(DT3);
		Mix dt4 = makeMix(DT4);
		Mix dt5 = makeMix(DT5);
		Mix dt6 = makeMix(DT6);

		DateTimeIndex<Mix> dti = new DateTimeIndex<Mix>();
		List<Mix> l = L.l(dt1, dt2, dt2b, dt3, dt4, dt5, dt6);
		for (Mix dt : l) {
			dti.add(dt);
		}

		assertEquals(l.size(), dti.getAll().size());
		Index<Mix> index = dti.getSubIndexing().getIndex(
				new IntervalMatch<Mix>(new Interval(DT3, DT4.plusHours(1)), Intervals.Relation.CONTAINS_START));
		assertEquals(2, index.getAll().size());
		Index<Mix> index2 = dti.getSubIndexing().getIndex(
				new IntervalMatch<Mix>(new Interval(DT2, DT2.plusHours(1)), Intervals.Relation.CONTAINS_START));
		assertEquals(2, index2.getAll().size());

		// normal cases
		assertContentEquals(L.l(dt3), dti.getAroundTime(DT4, -1, -1).realize());
		assertContentEquals(L.l(dt4), dti.getAroundTime(DT4, 0, 0).realize());
		assertContentEquals(L.l(dt5), dti.getAroundTime(DT4, 1, 1).realize());
		assertContentEquals(L.l(dt3, dt4, dt5), dti.getAroundTime(DT4, -1, 1).realize());

		// cases with chunk of 2
		assertContentEquals(L.l(dt1), dti.getAroundTime(DT2, -1, -1).realize());
		assertContentEquals(L.l(dt1, dt2), dti.getAroundTime(DT2, -1, 0).realize());
		assertContentEquals(L.l(dt1, dt2, dt2b), dti.getAroundTime(DT2, -1, 1).realize());
		assertContentEquals(L.l(dt1, dt2, dt2b, dt3), dti.getAroundTime(DT2, -1, 2).realize());
		assertContentEquals(L.l(dt2), dti.getAroundTime(DT2, 0, 0).realize());
		assertContentEquals(L.l(dt2, dt2b), dti.getAroundTime(DT2, 0, 1).realize());
		assertContentEquals(L.l(dt2, dt2b, dt3), dti.getAroundTime(DT2, 0, 2).realize());
		assertContentEquals(L.l(dt2b), dti.getAroundTime(DT2, 1, 1).realize());
		assertContentEquals(L.l(dt2b, dt3), dti.getAroundTime(DT2, 1, 2).realize());

		assertContentEquals(l, dti.getAroundTime(DT4, -(l.size() - 2 - 1), 2).realize());

		// border cases
		assertContentEquals(L.l(), dti.getAroundTime(DT1, -OUT_OF_LIST, -OUT_OF_LIST).realize());
		assertContentEquals(L.l(), dti.getAroundTime(DT1, -OUT_OF_LIST, -1).realize());
		assertContentEquals(L.l(), dti.getAroundTime(DT1, -1, -1).realize());
		assertContentEquals(L.l(dt1), dti.getAroundTime(DT1, -1, 0).realize());
		assertContentEquals(L.l(dt1), dti.getAroundTime(DT2, -1, -1).realize());
		assertContentEquals(L.l(dt6), dti.getAroundTime(DT6, 0, 0).realize());
		assertContentEquals(L.l(dt6), dti.getAroundTime(DT6, 0, 1).realize());
		assertContentEquals(L.l(), dti.getAroundTime(DT6, 1, 1).realize());
		assertContentEquals(L.l(), dti.getAroundTime(DT6, 1, OUT_OF_LIST).realize());
		assertContentEquals(L.l(), dti.getAroundTime(DT6, OUT_OF_LIST, OUT_OF_LIST).realize());
	}

	public Mix makeMix(final DateTime time) {
		Mix dt1 = Mockito.mock(Mix.class);
		Mockito.when(dt1.getTime()).thenReturn(time);
		Mockito.when(dt1.getInterval()).thenReturn(new Interval(time, time.plusHours(2)));
		return dt1;
	}
}
