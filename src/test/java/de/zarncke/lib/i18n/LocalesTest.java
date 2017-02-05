package de.zarncke.lib.i18n;

import java.util.Locale;

import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.Test;

import de.zarncke.lib.err.GuardedTest;

public class LocalesTest extends GuardedTest {

	@Test
	public void testLocales() {

		Assert.assertEquals(Locale.GERMAN, Locales.forIso3("deu", null, null));
		Assert.assertEquals(Locale.GERMANY, Locales.forIso3("deu", "DEU", null));

		Assert.assertEquals(DateTimeZone.forID("Europe/Berlin"), Locales.timeZoneFor(Locale.GERMANY));
		Assert.assertEquals(DateTimeZone.forID("Europe/London"), Locales.timeZoneFor(Locale.UK));
	}

	public void testLocaleExpand() {
		Assert.assertArrayEquals(new Locale[] { Locale.GERMAN },
				Locales.getMoreGeneralLocalesInOrderOfSpecificity(Locale.GERMAN));
		Assert.assertArrayEquals(new Locale[] { Locale.GERMANY, Locale.GERMAN },
				Locales.getMoreGeneralLocalesInOrderOfSpecificity(Locale.GERMANY));
		Locale l = new Locale("de", "DE", "test");
		Assert.assertArrayEquals(new Locale[] { l, Locale.GERMANY, Locale.GERMAN },
				Locales.getMoreGeneralLocalesInOrderOfSpecificity(l));
	}
}
