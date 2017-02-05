package de.zarncke.lib.i18n;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import de.zarncke.lib.coll.Elements;
import de.zarncke.lib.coll.Pair;

/**
 * A translation table with support for default translation and untranslated text.
 *
 * @author Gunnar Zarncke
 */
public class Translations extends AbstractMap<Locale, String> implements Serializable {
	private static final long serialVersionUID = 1L;
	private Locale defaultLocale = null;
	protected Map<Locale, String> translationsByLocale;

	public static Translations getConstantTranslation(final String defaultTranslation) {
		return new Translations((Void) null) {
			private static final long serialVersionUID = 1L;
			{
				this.translationsByLocale = Collections.emptyMap();
			}

			@Override
			public String get(final Object key) {
				return defaultTranslation;
			}

			@Override
			public String getDefault() {
				return defaultTranslation;
			}
		};
	}

	/**
	 * Create empty Translations with no default and no translations whatsoever. {@link #size()} will be 0.
	 */
	public Translations() {
		super();
		this.translationsByLocale = new HashMap<Locale, String>();
	}

	/**
	 * Constructor for derived classes which want to initialize the translations themselves.
	 *
	 * @param dummy pass null
	 */
	protected Translations(final Void dummy) { // NOPMD
		super();
	}

	/**
	 * Initializes these {@link Translations} with the given ones.
	 *
	 * @param base != null
	 */
	public Translations(final Translations base) {
		this();
		this.defaultLocale = base.defaultLocale;
		this.translationsByLocale = new HashMap<Locale, String>(base.translationsByLocale);
	}

	/**
	 * Create empty Translations with no default but the given untranslated text.
	 * The untranslated text will be used as the default if no default is set. {@link #size()} will be 1.
	 *
	 * @param untranslatedText to use when no locale is given
	 */
	public Translations(final String untranslatedText) {
		this();
		put(null, untranslatedText);
	}

	/**
	 * If no default is set yet (i.e. == null) and if these translations contain a binding for the given locale use the
	 * given
	 * locale as the new default key.
	 *
	 * @param alternativeDefaultIfNonePresent may be null
	 */
	public void ensureDefault(final Locale alternativeDefaultIfNonePresent) {
		if (this.defaultLocale == null) {
			if (!containsKey(alternativeDefaultIfNonePresent)) {
				return;
			}
			this.defaultLocale = alternativeDefaultIfNonePresent;
			if (!containsKey(null)) {
				put(null, get(alternativeDefaultIfNonePresent));
			}
		}
	}

	/**
	 * Test if no translations are present.
	 * Can be used for concatenating lists like this:
	 *
	 * <pre>
	 * Translations title = new Translations();
	 * for (Translations element : elements) {
	 * 	if (!title.isEmpty()) {
	 * 		title.append(new Translations(&quot;, &quot;));
	 * 	}
	 * 	title.append(element);
	 * }
	 * </pre>
	 *
	 * @return true only if the {@link #Translations() default constructor} was used alone.
	 */
	@Override
	public boolean isEmpty() {
		return this.translationsByLocale.isEmpty();
	}

	/**
	 * Returns the default translations (the one for the set default locale).
	 * If none is set the untranslated default is returned.
	 *
	 * @return String, may be null if no untranslated text is set.
	 */
	public String getDefault() {
		String res = this.translationsByLocale.get(this.defaultLocale);
		if (res != null) {
			return res;
		}
		return this.translationsByLocale.get(null);
	}

	@Override
	public final String put(final Locale key, final String value) {
		if (key != null) {
			if (key.getVariant().length() > 0) {
				Locale lc = new Locale(key.getLanguage(), key.getCountry());
				if (!this.translationsByLocale.containsKey(lc)) {
					this.translationsByLocale.put(lc, value);
				}
				Locale l = new Locale(key.getLanguage());
				if (!this.translationsByLocale.containsKey(l)) {
					this.translationsByLocale.put(new Locale(key.getLanguage()), value);
				}
			} else if (key.getCountry().length() > 0) {
				Locale l = new Locale(key.getLanguage());
				if (!this.translationsByLocale.containsKey(l)) {
					this.translationsByLocale.put(new Locale(key.getLanguage()), value);
				}
			}
		}
		return this.translationsByLocale.put(key, value);
	}

	/**
	 * First entry matching the key is returned. Variants are handled in order of specificity. Multiple locales are
	 * handled by
	 * trying the most specific entries of each first.
	 *
	 * @param key can be Locale or Collection of Locale
	 */
	@Override
	public String get(final Object key) {
		return getTextAndLocale(key).getFirst();
	}

	public Pair<String, Locale> getTextAndLocale(final Object key) {
		String val = super.get(key);
		if (val != null) {
			return Pair.pair(val, (Locale) key);
		}
		if (key == null) {
			return Pair.pair(getDefault(), getDefaultLocale());
		}
		if (key instanceof Locale) {
			Locale locale = (Locale) key;
			if (locale.getVariant() != null) {
				Locale localeWithoutVariant = new Locale(locale.getLanguage(), locale.getCountry());
				val = this.translationsByLocale.get(localeWithoutVariant);
				if (val != null) {
					return Pair.pair(val, localeWithoutVariant);
				}
			}
			if (locale.getCountry() != null) {
				Locale localeLanguageOnly = new Locale(locale.getLanguage());
				val = this.translationsByLocale.get(localeLanguageOnly);
				if (val != null) {
					return Pair.pair(val, localeLanguageOnly);
				}
			}
		} else if (key instanceof Collection<?>) {
			Collection<?> list = (Collection<?>) key;
			Pair<String, Locale> valAndLoc = findByCollectionOfLocale(Elements.checkedIterable(list, Locale.class));
			if (valAndLoc != null) {
				return valAndLoc;
			}
		}
		return Pair.pair(getDefault(), getDefaultLocale());
	}

	protected Pair<String, Locale> findByCollectionOfLocale(final Iterable<Locale> list) {
		String val;
		for (Locale locale : list) {
			val = this.translationsByLocale.get(locale);
			if (val != null) {
				return Pair.pair(val, locale);
			}
		}
		for (Locale locale : list) {
			if (locale.getVariant() != null) {
				Locale localeWithoutVariant = new Locale(locale.getLanguage(), locale.getCountry());
				val = this.translationsByLocale.get(localeWithoutVariant);
				if (val != null) {
					return Pair.pair(val, localeWithoutVariant);
				}
			}
		}
		for (Locale locale : list) {
			if (locale.getCountry() != null) {
				Locale localeLanguageOnly = new Locale(locale.getLanguage());
				val = this.translationsByLocale.get(localeLanguageOnly);
				if (val != null) {
					return Pair.pair(val, localeLanguageOnly);
				}
			}
		}
		return null;
	}

	public void setDefaultLocale(final Locale newDefaultLocale) {
		// ensure a direct binding
		if (!containsKey(newDefaultLocale)) {
			put(newDefaultLocale, get(newDefaultLocale));
		}
		this.defaultLocale = newDefaultLocale;
	}

	public Locale getDefaultLocale() {
		return this.defaultLocale;
	}

	@Override
	public String toString() {
		return getDefault() + "(in " + size() + " languages)";
	}

	/**
	 * Appends other {@link Translations} to this one. In case of missing corresponding translations the applicable
	 * defaults are
	 * used. <br/>
	 * Notes:
	 * <ul>
	 * <li>Doesn't consider writing direction of the {@link Locale}, always appends to the right.</li>
	 * <li>Doen's add a separator. Use e.g.
	 * <code>{@link #append(Translations) append}(new {@link Translations}(" "))</code> in such a case.</li>
	 * </ul>
	 *
	 * @param toAdd Translations != null.
	 * @return this
	 */
	public Translations append(final Translations toAdd) {
		HashMap<Locale, String> sumTranslations = new HashMap<Locale, String>(size() * 2);
		for (Map.Entry<Locale, String> me : toAdd.entrySet()) {
			Locale key = me.getKey();
			String orig = get(key);
			String added = me.getValue();
			sumTranslations.put(key, orig == null ? added : orig + added);
		}

		for (Map.Entry<Locale, String> me : this.translationsByLocale.entrySet()) {
			Locale key = me.getKey();
			if (!toAdd.containsKey(key)) {
				sumTranslations.put(key, me.getValue() + toAdd.get(key));
			}
		}
		this.translationsByLocale = sumTranslations;
		return this;
	}

	@Override
	public Set<java.util.Map.Entry<Locale, String>> entrySet() {
		return this.translationsByLocale.entrySet();
	}

	@Override
	public String remove(final Object key) {
		return this.translationsByLocale.remove(key);
	}

	@Override
	public void putAll(final Map<? extends Locale, ? extends String> localeToText) {
		for (Map.Entry<? extends Locale, ? extends String> me : localeToText.entrySet()) {
			put(me.getKey(), me.getValue());
		}
	}

	@Override
	public void clear() {
		this.translationsByLocale.clear();
	}

	public Translations add(final Locale locale, final String translation) {
		put(locale, translation);
		return this;
	}
}
