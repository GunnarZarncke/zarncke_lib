package de.zarncke.lib.time;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.LocalTime;
import org.joda.time.ReadableDateTime;
import org.joda.time.ReadableInstant;
import org.joda.time.ReadableInterval;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.ISODateTimeFormat;

import com.google.common.base.Function;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import de.zarncke.lib.ctx.Context;
import de.zarncke.lib.err.CantHappenException;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.math.Concrete;
import de.zarncke.lib.math.Intervals;
import de.zarncke.lib.value.Default;

/**
 * Some convenience functions to operate with the Joda time API.
 *
 * @author Gunnar Zarncke
 */
public final class Times {
	private Times() {
		// helper
	}

	public static final Context<DateTimeZone> TIME_ZONE = Context.of(Default.of(DateTimeZone.UTC, DateTimeZone.class));

	public static final int DAYS_PER_WEEK = 7;
	public static final int HOURS_PER_DAY = 24;
	public static final long MINUTES_PER_HOUR = 60;
	public static final long SECONDS_PER_MINUTE = 60;
	public static final long SECONDS_PER_HOUR = MINUTES_PER_HOUR * SECONDS_PER_MINUTE;
	public static final long MILLIS_PER_SECOND = 1000;
	public static final long MILLIS_PER_MINUTE = MILLIS_PER_SECOND * SECONDS_PER_MINUTE;

	// just in case anybody wants to output a DateTime like a LocalTime...
	public static final DateTimeFormatter ISO_DATETIME_FORMAT_NO_Z = new DateTimeFormatterBuilder().append(
			ISODateTimeFormat.date()).appendLiteral('T').append(ISODateTimeFormat.hourMinuteSecondFraction()).toFormatter();

	/**
	 * Europe/Berlin
	 */
	public static final DateTimeZone MEZ = DateTimeZone.forID("Europe/Berlin");

	public static final LocalTime TIME_0 = new LocalTime(0, 0);
	public static final LocalTime TIME_24 = new LocalTime(23, 59, 59, 999);
	public static final DateTime THE_FUTURE = utc(10000, 1, 1, 0, 0);
	public static final DateTime THE_PAST = utc(-10000, 1, 1, 0, 0);
	public static final DateTime THE_EPOCH = utc(1970, 1, 1, 0, 0);

	// TODO optimized ReadableInterval
	public static final Interval ALWAYS = new Interval(THE_PAST, THE_FUTURE);

	/**
	 * Convention of the lib: All {@link DateTime} values are UTC based, except for UI calculations.
	 *
	 * @return current time by context Clock UTC
	 */
	public static DateTime now() {
		return JavaClock.getTheClock().getCurrentDateTime();
	}

	/**
	 * @param year see Joda
	 * @param month see Joda
	 * @param day see Joda
	 * @param hour see Joda
	 * @param minute see Joda
	 * @return DateTime in UTC
	 */
	public static DateTime utc(final int year, final int month, final int day, final int hour, final int minute) {
		return new DateTime(year, month, day, hour, minute, 0, 0, DateTimeZone.UTC);
	}

	/**
	 * @return the current time as a {@link LocalTime} in the time zone from the context
	 */
	public static LocalTime currently() {
		return new LocalTime(JavaClock.getTheClock().getCurrentTimeMillis(), TIME_ZONE.get());
	}

	/**
	 * Tests whether the current time falls within the given daily window.
	 * If start or end is null, then the start or end of the day is used respectively.
	 * If the end is before the start time, then a nightly window is assumed, i.e. the condition is true if the time is before
	 * the end or after the start.
	 *
	 * @param beginWindow may be null
	 * @param endWindow may be null
	 * @param timeZone to use
	 * @return true if we are in the window now
	 */
	public static boolean isCurrentTimeInDailyTimeWindow(final LocalTime beginWindow, final LocalTime endWindow,
			final DateTimeZone timeZone) {
		final DateTime now = JavaClock.getTheClock().getCurrentDateTime().withZone(timeZone);
		final LocalTime time = now.toLocalTime();
		if (beginWindow != null && endWindow != null && endWindow.isBefore(beginWindow)) {
			// if end is before begin we assume that that the night before end time and after begin time is meant
			if (time.isBefore(beginWindow) && time.isAfter(endWindow)) {
				return false;
			}

		} else {
			if (beginWindow != null && time.isBefore(beginWindow)) {
				return false;
			}

			if (endWindow != null && time.isAfter(endWindow)) {
				return false;
			}
		}
		return true;
	}

	public static List<ReadableInterval> join(final Collection<? extends ReadableInterval> intervals,
			final Collection<? extends ReadableInterval> intervals2) {
		return fromIntervals(toIntervals(intervals).join(toIntervals(intervals2)));
	}

	public static List<ReadableInterval> intersect(final Collection<? extends ReadableInterval> intervals,
			final Collection<? extends ReadableInterval> intervals2) {
		return fromIntervals(toIntervals(intervals).intersect(toIntervals(intervals2)));
	}

	public static Intervals toIntervals(final Collection<? extends ReadableInterval> intervals) {
		return toIntervals(intervals, true);
	}

	/**
	 * Converts {@link Interval}s into {@link Intervals} by taking the millis.
	 *
	 * @param intervals != null
	 * @param normalize true: intervals are sorted and normalized, false: intervals must be sorted by start
	 * @return Intervals
	 */
	public static Intervals toIntervals(final Collection<? extends ReadableInterval> intervals, final boolean normalize) {
		final Intervals.Interval[] ints = new Intervals.Interval[intervals.size()];
		int i = 0;
		for (final ReadableInterval in : intervals) {
			ints[i++] = Intervals.Interval.of(in.getStartMillis(), in.getEndMillis());
		}
		return normalize ? Intervals.ofNormalized(ints) : Intervals.of(ints);
	}

	/**
	 * Reverses {@link #toIntervals(Collection)}.
	 *
	 * @param intervals != null
	 * @return {@link Interval}s
	 */
	public static List<ReadableInterval> fromIntervals(final Intervals intervals) {
		return intervals.transform(new Function<Intervals.Interval, ReadableInterval>() {
			@Override
			public ReadableInterval apply(final Intervals.Interval from) {
				return new Interval(from.getStart(), from.getEnd(), DateTimeZone.UTC);
			}
		});
	}

	/**
	 * @param a one Interval
	 * @param b one Interval
	 * @return the overlaping part of both intervals or null if no overlap
	 */
	public static ReadableInterval intersect(final ReadableInterval a, final ReadableInterval b) {
		if (a == null) {
			return b;
		}
		if (b == null) {
			return a;
		}
		final DateTime start = Times.max(a.getStart(), b.getStart());
		final DateTime end = Times.min(a.getEnd(), b.getEnd());
		if (end.isBefore(start)) {
			return null;
		}
		return new Interval(start, end);
	}

	/**
	 * Removes one interval from a list of intervals.
	 * This is done by reducing or splitting intervals if needed.
	 *
	 * @param fragments list of intervals, != null, may be empty
	 * @param toRemove interval, may be null
	 * @return list of framgents where the given interval is left out (unchanged if it is null); may contain one more fragment
	 * in
	 * case the removed interval is fully contain within one interval which is split
	 */
	public static Collection<? extends ReadableInterval> subtract(final Collection<? extends ReadableInterval> fragments,
			final ReadableInterval toRemove) {
		if (toRemove == null) {
			return fragments;
		}
		final Collection<ReadableInterval> res = new ArrayList<ReadableInterval>(fragments.size() + 1);
		for (final ReadableInterval fragment : fragments) {
			if (fragment.overlaps(toRemove)) {
				final Interval overlap = fragment.toInterval().overlap(toRemove);
				if (overlap.equals(fragment)) {
					continue;
				}
				if (!overlap.getStart().equals(fragment.getStart())) {
					res.add(new Interval(fragment.getStart(), overlap.getStart()));
				}
				if (!overlap.getEnd().equals(fragment.getEnd())) {
					res.add(new Interval(overlap.getEnd(), fragment.getEnd()));
				}
			} else {
				res.add(fragment);
			}
		}
		return res;
	}

	/**
	 * Determines the interval spanning both given intervals.
	 * If a given interval is null the other one is returned.
	 *
	 * @param a may be null
	 * @param b may be null
	 * @return total interval or null if both are null
	 */
	public static ReadableInterval join(final ReadableInterval a, final ReadableInterval b) {
		if (a == null) {
			return b;
		}
		if (b == null) {
			return a;
		}
		return new Interval(Math.min(a.getStartMillis(), b.getStartMillis()), Math.max(a.getEndMillis(), b.getEndMillis()),
				DateTimeZone.UTC);
	}

	/**
	 * Determines the interval spanning one interval and a time.
	 * If the time is null the interval is returned.
	 * If the interval is null then an interval containing the time is returned.
	 *
	 * @param a may be null
	 * @param b may be null
	 * @return total interval or null if both are null
	 */
	public static ReadableInterval join(final ReadableInterval a, final ReadableInstant b) {
		return join(a, b == null ? null : new Interval(b, b));
	}

	private static LoadingCache<Locale, Integer> localeToFirstDayOfWeek = CacheBuilder.newBuilder().build(
			new CacheLoader<Locale, Integer>() {
		@Override
				public Integer load(final Locale locale) throws Exception {
			switch (Calendar.getInstance(locale).getFirstDayOfWeek()) {
			case Calendar.MONDAY:
				return Integer.valueOf(DateTimeConstants.MONDAY);
			case Calendar.TUESDAY:
				return Integer.valueOf(DateTimeConstants.TUESDAY);
			case Calendar.WEDNESDAY:
				return Integer.valueOf(DateTimeConstants.WEDNESDAY);
			case Calendar.THURSDAY:
				return Integer.valueOf(DateTimeConstants.THURSDAY);
			case Calendar.FRIDAY:
				return Integer.valueOf(DateTimeConstants.FRIDAY);
			case Calendar.SATURDAY:
				return Integer.valueOf(DateTimeConstants.SATURDAY);
			case Calendar.SUNDAY:
				return Integer.valueOf(DateTimeConstants.SUNDAY);
			default:
				throw Warden.spot(new CantHappenException("Calendar returned invalid value"));
			}
		}
	});

	public static int jodaFirstDayOfWeek(final Locale locale) {
		try {
			return localeToFirstDayOfWeek.get(locale).intValue();
		} catch (ExecutionException e) {
			throw Warden.spot(new IllegalArgumentException("cannot determine dow " + locale, e));
		}
	}

	public static int jodaNormalizeDayOfWeek(final int lenientDayOfWeek) {
		final int inRange = Concrete.mod(lenientDayOfWeek, 7);
		return inRange == 0 ? 7 : inRange;
	}

	public static DateTime prevDayOfWeek(final ReadableDateTime dateTime, final int dayOfWeek) {
		final DateTime back = dateTime.toDateTime().withDayOfWeek(dayOfWeek);
		if (back.isAfter(dateTime)) {
			return back.minusWeeks(1);
		}
		return back;
	}

	public static DateTime nextDayOfWeek(final ReadableDateTime dateTime, final int dayOfWeek) {
		final DateTime back = dateTime.toDateTime().withDayOfWeek(dayOfWeek);
		if (back.isBefore(dateTime)) {
			return back.plusWeeks(1);
		}
		return back;
	}

	public static ReadableInterval expandToFullWeeks(final ReadableInterval interval, final Locale locale,
			final DateTimeZone timeZone) {
		final int firstDayOfWeek = jodaFirstDayOfWeek(locale);
		final DateTime start = prevDayOfWeek(interval.getStart(), firstDayOfWeek).withZone(timeZone).withTime(0, 0, 0, 0).withZone(
				DateTimeZone.UTC);
		final DateTime end = nextDayOfWeek(interval.getEnd(), firstDayOfWeek).withZone(timeZone).withTime(0, 0, 0, 0).withZone(
				DateTimeZone.UTC);
		return new Interval(start, end);
	}

	public static <T extends ReadableInstant> T min(final T a, final T b) {
		return a == null ? b : b == null ? a : a.isBefore(b) ? a : b;
	}

	public static <T extends ReadableInstant> T max(final T a, final T b) {
		return a == null ? b : b == null ? a : a.isBefore(b) ? b : a;
	}

	// TODO implement faster version of joda ISODateTimeFormat
	// static int decimalValueLimited(final CharSequence numberString, final int maxlen) {
	// int value = 0;
	// int idx;
	//
	// gchar *ptr;
	// guchar c;
	//
	// ptr = *string;
	// if (G_UNLIKELY (!*ptr)) {
	// return -1; }
	// if (G_UNLIKELY (!maxlen)) {
	// maxlen--; }
	// idx = 0;
	// while (TRUE) {
	// if (G_UNLIKELY (idx > maxlen)) {
	// break; }
	// c = ptr[idx++];
	// if (G_UNLIKELY (!c)) {
	// idx--;
	// break; }
	// c -= '0';
	// if (G_UNLIKELY (c > 9)) {
	// break; }
	// value = value * 10 + c; }
	// *string += idx;
	// return value;
	// }
	//
	// /**
	// * Parses ISO 8601 date time format. Week and millis is not supported.
	// *
	// * @param timeString {@link CharSequence}
	// * @return DateTime
	// */
	// public static DateTime parseDateTimeDirect(final CharSequence timeString) {
	// int val;
	// char c;
	// while (true) {
	// int index = 0;
	// while (index < timeString.length()) {
	// c = timeString.charAt(index);
	// if (!Character.isSpace(c)) {
	// break;
	// }
	// index++;
	// }
	// if (timeString.subSequence(0, 10).equals("0000-00-00")) { // Special case
	// return new DateTime(0);
	// }
	// }
	// 2. Read year or hour
	// val = decimalValueLimited (timeString, 4);
	// if (val == -1) {
	// return (time_t) val; }
	// c = **string;
	// if (!c) {
	// break; }
	// if (c == ' ' || c == '-' || c == ':' || c == 'T') {
	// (*string)++;
	// if (c == ':') {
	// tm.tm_hour = val;
	// goto _read_minutes; }
	// if (c == 'T') {
	// goto _read_hours; } }
	// else if (!strutil_is_digit (c)) {
	// break; }
	// tm.tm_year = val;
	//
	// // 3. Read month
	// val = _strutil_read_decimal_value_limited (string, 2);
	// if (val == -1) {
	// return (time_t) val; }
	// tm.tm_mon = val - 1;
	// c = **string;
	// if (!c) {
	// break; }
	// if (c == ' ' || c == '-' || c == 'T') {
	// (*string)++;
	// if (c == 'T') {
	// goto _read_hours; } }
	// else if (!strutil_is_digit (c)) {
	// break; }
	//
	// // 4. Read day
	// val = _strutil_read_decimal_value_limited (string, 2);
	// if (val == -1) {
	// return (time_t) val; }
	// tm.tm_mday = val;
	// c = **string;
	// if (!c) {
	// break; }
	// if (c == ' ' || c == 'T') {
	// (*string)++; }
	// else if (!strutil_is_digit (c)) {
	// break; }
	//
	// // 5. Read hours
	// _read_hours:
	// val = _strutil_read_decimal_value_limited (string, 2);
	// if (val == -1) {
	// return (time_t) val; }
	// tm.tm_hour = val;
	// c = **string;
	// if (!c) {
	// break; }
	// if (c == ' ' || c == ':' || c == 'Z' || c == '-' || c == '+') {
	// (*string)++;
	// if (c == 'Z') {
	// break; }
	// if (c == '-' || c == '+') {
	// goto _read_timezone; } }
	// else if (!strutil_is_digit (c)) {
	// break; }
	//
	// // 6. Read minutes
	// _read_minutes:
	// val = _strutil_read_decimal_value_limited (string, 2);
	// if (val == -1) {
	// return (time_t) val; }
	// tm.tm_min = val;
	// c = **string;
	// if (!c) {
	// break; }
	// if (c == ' ' || c == ':' || c == 'Z' || c == '-' || c == '+') {
	// (*string)++;
	// if (c == 'z') {
	// break; }
	// if (c == '-' || c == '+') {
	// goto _read_timezone; } }
	// else if (!strutil_is_digit (c)) {
	// break; }
	//
	// // 7. Read seconds
	// val = _strutil_read_decimal_value_limited (string, 2);
	// if (val == -1) {
	// return (time_t) val; }
	// tm.tm_sec = val;
	// c = **string;
	// if (!c) {
	// break; }
	// if (c == 'Z' || c == '-' || c == '+') {
	// (*string)++;
	// if (c != 'Z') {
	// goto _read_timezone; }
	// break; }
	//
	// // 8. Read timezone offset
	// _read_timezone:
	// c = *((*string) - 1);
	// val = _strutil_read_decimal_value_limited (string, 4);
	// if (val == -1) {
	// return (time_t) val; }
	// if (val >= 100) {
	// val /= 100; }
	// tm.tm_hour += c == '-' ? -val : +val;
	// c = **string;
	// if (c == ':') {
	// _strutil_read_decimal_value_limited (string, 2); } }
	//
	// while (FALSE);
	// if (!tm.tm_year) {
	// return (time_t) -1; }
	// if (tm.tm_year >= 100) {
	// if (tm.tm_year < 1900) {
	// return (time_t) -1; }
	// tm.tm_year -= 1900; }
	// if ((unsigned) tm.tm_mon > 11) {
	// return (time_t) -1; }
	// if ((unsigned) tm.tm_mday > 31 || !tm.tm_mday) {
	// return (time_t) -1; }
	// // if ((unsigned) tm.tm_hour > 23) {
	// // return (time_t) -1; }
	// if ((unsigned) tm.tm_min >= 60) {
	// return (time_t) -1; }
	// if ((unsigned) tm.tm_sec > 60) { // Note: leap seconds
	// return (time_t) -1; }
	// }

}
