package de.zarncke.lib.index;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Test;
import org.mockito.Mockito;

import de.zarncke.lib.err.GuardedTest;
import de.zarncke.lib.index.crit.Criteria;
import de.zarncke.lib.index.crit.IntervalMatch;
import de.zarncke.lib.math.Intervals;
import de.zarncke.lib.time.HasDateTime;
import de.zarncke.lib.time.HasInterval;
import de.zarncke.lib.time.Times;

/**
 * Test Criteria in this package.
 *
 * @author Gunnar Zarncke
 */
public class CriteriaTest extends GuardedTest {

	static interface Mix extends HasInterval, HasDateTime {
	}

	private static final DateTime BEFORE_1 = Times.utc(2010, 1, 25, 9, 30);
	private static final DateTime START_1 = Times.utc(2010, 1, 25, 10, 0);
	private static final DateTime MID_1 = Times.utc(2010, 1, 25, 10, 15);
	private static final DateTime MID_1B = Times.utc(2010, 1, 25, 10, 45);
	private static final DateTime END_1 = Times.utc(2010, 1, 25, 11, 0);
	private static final DateTime START_2 = END_1;
	private static final DateTime END_2 = Times.utc(2010, 1, 25, 12, 0);
	private Mix negative;
	private Mix positive;


	@Override
	public void setUp() throws Exception {
		super.setUp();
		this.positive = Mockito.mock(Mix.class);
		Mockito.when(this.positive.getTime()).thenReturn(START_1);
		Mockito.when(this.positive.getInterval()).thenReturn(new Interval(START_1, END_1));
		// Mockito.when(this.positive.getGenreIds()).thenReturn(L.l(Genre.makeId(Gid.of(0, GenreBase.class),
		// GenreType.ROOT_TYPE)));

		this.negative = Mockito.mock(Mix.class);
		Mockito.when(this.negative.getTime()).thenReturn(START_2);
		Mockito.when(this.negative.getInterval()).thenReturn(new Interval(START_2, END_2));
	}

	@Test
	public void testIntervalMatcher() {
		check(new IntervalMatch<Mix>(new Interval(MID_1, MID_1B), Intervals.Relation.OVERLAPS));
		check(new IntervalMatch<Mix>(new Interval(BEFORE_1, MID_1), Intervals.Relation.CONTAINS_START));
	}

	private void check(final Criteria<?, Mix> matcher) {
		assertTrue("should match " + matcher, matcher.matches(this.positive));
		assertFalse("should not match " + matcher, matcher.matches(this.negative));
	}
}
