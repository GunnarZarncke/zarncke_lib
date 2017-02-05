package de.zarncke.lib.i18n;

import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;

import org.joda.time.DateTimeZone;

import com.google.common.collect.MapMaker;

import de.zarncke.lib.ctx.Context;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.log.Log;
import de.zarncke.lib.util.Chars;
import de.zarncke.lib.value.Default;

/**
 * Handle {@link Locale}s.
 * Support for ISO-639s 3-letter codes.
 * Support for guessing time-zone from locale.
 *
 * @author Gunnar Zarncke
 */
public final class Locales {
	// TODO support "und" undefined locale

	public static final Context<Locale> LOCALE = Context.of(Default.of(Locale.getDefault(), Locale.class));

	public static final String LANGUAGE_CODE_UNDEFINED2 = "un";
	public static final String LANGUAGE_CODE_UNDEFINED3 = "und";

	public static final Locale LOCALE_UNDEFINED = new Locale(LANGUAGE_CODE_UNDEFINED2, "", "");

	private static final Map<String, DateTimeZone> COUNTRY_2_TIMEZONE = new MapMaker().makeMap();
	private static final Map<String, String> ISO3_TO_ISO2_LANG = new MapMaker().makeMap();
	private static final Map<String, String> ISO3_TO_ISO2_COUNTRY = new MapMaker().makeMap();
	static {
		for (String iso : Locale.getISOLanguages()) {
			ISO3_TO_ISO2_LANG.put(new Locale(iso).getISO3Language(), iso);
		}

		for (String iso : Locale.getISOCountries()) {
			ISO3_TO_ISO2_COUNTRY.put(new Locale("us", iso).getISO3Country(), iso);
		}

		COUNTRY_2_TIMEZONE.put("de", DateTimeZone.forID("Europe/Berlin"));
		COUNTRY_2_TIMEZONE.put("DE", DateTimeZone.forID("Europe/Berlin"));
		COUNTRY_2_TIMEZONE.put("AT", DateTimeZone.forID("Europe/Zurich"));
		COUNTRY_2_TIMEZONE.put("it", DateTimeZone.forID("Europe/Rome"));
		COUNTRY_2_TIMEZONE.put("IT", DateTimeZone.forID("Europe/Rome"));
		COUNTRY_2_TIMEZONE.put("fr", DateTimeZone.forID("Europe/Paris"));
		COUNTRY_2_TIMEZONE.put("FR", DateTimeZone.forID("Europe/Paris"));
		COUNTRY_2_TIMEZONE.put("SE", DateTimeZone.forID("Europe/Stockholm"));
		COUNTRY_2_TIMEZONE.put("NO", DateTimeZone.forID("Europe/Oslo"));
		COUNTRY_2_TIMEZONE.put("DK", DateTimeZone.forID("Europe/Copenhagen"));
		COUNTRY_2_TIMEZONE.put("GR", DateTimeZone.forID("Europe/Athens"));
		COUNTRY_2_TIMEZONE.put("CH", DateTimeZone.forID("Europe/London"));
		COUNTRY_2_TIMEZONE.put("TR", DateTimeZone.forID("Europe/Istanbul"));
		COUNTRY_2_TIMEZONE.put("UK", DateTimeZone.forID("Europe/London"));
		COUNTRY_2_TIMEZONE.put("en", DateTimeZone.forID("Europe/London"));
		COUNTRY_2_TIMEZONE.put("GB", DateTimeZone.forID("Europe/London"));
		COUNTRY_2_TIMEZONE.put("IR", DateTimeZone.forID("Europe/Dublin"));
		COUNTRY_2_TIMEZONE.put("US", DateTimeZone.forID("US/Central"));
		COUNTRY_2_TIMEZONE.put("CA", DateTimeZone.forID("Canada/Central"));
		COUNTRY_2_TIMEZONE.put("jp", DateTimeZone.forID("Asia/Tokyo"));
		COUNTRY_2_TIMEZONE.put("JP", DateTimeZone.forID("Asia/Tokyo"));
		COUNTRY_2_TIMEZONE.put("ch", DateTimeZone.forID("Asia/Hong_Kong"));
		COUNTRY_2_TIMEZONE.put("CH", DateTimeZone.forID("Asia/Hong_Kong"));
		COUNTRY_2_TIMEZONE.put("AU", DateTimeZone.forID("Australia/Melbourne"));
		COUNTRY_2_TIMEZONE.put("RU", DateTimeZone.forID("Europe/Moscow"));
		// TODO very incomplete - and unverified
		// use e.g. System.out.println(Arrays.asList(TimeZone.getAvailableIDs()));
	}

	private Locales() {
		// Helper
	}

	/**
	 * Provides an initial guess about the TimeZone based on the locale
	 *
	 * @param locale may be null
	 * @return DateTimeZone
	 */
	public static DateTimeZone timeZoneFor(final Locale locale) {
		if (locale == null) {
			return DateTimeZone.getDefault();
		}
		DateTimeZone zone = COUNTRY_2_TIMEZONE.get(locale.getCountry());
		if (zone == null) {
			return DateTimeZone.getDefault();
		}
		return zone;
	}

	/**
	 * @see Locale
	 * @param languageIso3 3-letter ISO 639 language code
	 * @param countryIso3 3-letter ISO 639 country != null, may be ""
	 * @param variant != null, may be ""
	 * @return Locale != null, some codes may be mapped to "un" (this case is logged)
	 */
	public static Locale forIso3(final String languageIso3, final String countryIso3, final String variant) {
		String lang = iso2ForIso3Language(languageIso3);
		if (lang == null) {
			if (!LANGUAGE_CODE_UNDEFINED3.equals(languageIso3)) {
				Log.LOG.get().report("cannot resolve 3-letter language ISO 639 code " + languageIso3 + " - using 'un'.");
			}
			lang = LANGUAGE_CODE_UNDEFINED2;
		}
		String country = "";
		if (countryIso3 != null) {
			country = iso2ForIso3Country(countryIso3);
			if (country == null) {
				throw Warden.spot(new MissingResourceException("cannot resolve 3-letter country ISO 639 code " + countryIso3,
						Locale.class.getName(), countryIso3));
			}
		}
		return new Locale(lang, country, variant == null ? "" : variant);
	}

	public static String iso2ForIso3Language(final String iso639_3) {
		return iso639_3 == null ? null : iso639_3.length() == 0 ? "" : ISO3_TO_ISO2_LANG.get(iso639_3);
	}

	public static String iso2ForIso3Country(final String iso639_3) {
		return iso639_3 == null ? null : iso639_3.length() == 0 ? "" : ISO3_TO_ISO2_COUNTRY.get(iso639_3);
	}

	public static Locale[] getMoreGeneralLocalesInOrderOfSpecificity(final Locale localeToExpand) {
		if (!Chars.isEmpty(localeToExpand.getVariant())) {
			return new Locale[] { localeToExpand, new Locale(localeToExpand.getLanguage(), localeToExpand.getCountry()),
					new Locale(localeToExpand.getLanguage()) };
		}
		if (!Chars.isEmpty(localeToExpand.getCountry())) {
			return new Locale[] { localeToExpand, new Locale(localeToExpand.getLanguage()) };
		}
		return new Locale[] { localeToExpand };
	}

}
