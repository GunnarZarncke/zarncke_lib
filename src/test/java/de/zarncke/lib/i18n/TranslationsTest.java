package de.zarncke.lib.i18n;

import java.util.Locale;

import org.junit.Test;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.err.GuardedTest;

public class TranslationsTest extends GuardedTest {

	@Test
	public void testTranslations() {
		Translations t = new Translations("?");
		t.put(Locale.ENGLISH, "Hello");
		t.put(Locale.GERMAN, "Hallo");
		t.put(Locale.GERMANY, "Hi");
		assertEquals("?", t.get(null));
		assertEquals("?", t.getDefault());
		t.setDefaultLocale(Locale.GERMAN);

		assertEquals("?", t.get(null));
		assertEquals("Hallo", t.getDefault());
		assertEquals("Hallo", t.get(Locale.GERMAN));
		assertEquals("Hi", t.get(Locale.GERMANY));
		assertEquals("Hello", t.get(Locale.ENGLISH));
		assertEquals("Hello", t.get(Locale.UK));
		assertEquals("Hallo", t.get(Locales.forIso3("pol", "", "")));
	}

	@Test
	public void testTranslationsMark() {
		Translations t = new Translations("?");
		t.put(Locale.GERMAN, "Hallo");
		t.setDefaultLocale(Locale.GERMANY);

		assertEquals("?", t.get(null));
		assertEquals("Hallo", t.getDefault());
		assertEquals("Hallo", t.get(Locale.GERMANY));
		assertEquals("Hallo", t.get(Locale.GERMAN));
	}

	@Test
	public void testTranslationsMark2() {
		Translations t = new Translations("?");
		t.put(Locale.GERMANY, "Hallo");

		assertEquals("?", t.get(null));
		assertEquals("?", t.getDefault());
		assertEquals("Hallo", t.get(Locale.GERMANY));
		assertEquals("Hallo", t.get(Locale.GERMAN));
	}

	@Test
	public void testTranslationsAdd() {
		Translations t = new Translations("HO");
		t.put(Locale.ENGLISH, "Hello");
		t.put(Locale.GERMAN, "Hallo");
		t.put(Locale.GERMANY, "Hi");

		Translations t2 = new Translations("EARTH");
		t2.put(Locale.ENGLISH, "World");
		t2.put(Locale.US, "USA");
		t2.put(Locale.GERMAN, "Welt");

		Translations ts = new Translations();
		ts.append(t);
		ts.append(new Translations(" "));
		ts.append(t2);

		assertEquals("HO EARTH", ts.getDefault());
		assertEquals("Hallo Welt", ts.get(Locale.GERMAN));
		assertEquals("Hi Welt", ts.get(Locale.GERMANY));
		assertEquals("Hello World", ts.get(Locale.ENGLISH));
		assertEquals("Hello USA", ts.get(Locale.US));
	}

	@Test
	public void testTranslationsMultiple() {
		Translations t = new Translations("?");
		t.put(Locale.ENGLISH, "Hello");
		t.put(Locale.GERMAN, "Hallo");
		t.put(Locale.GERMANY, "Hi");

		assertEquals("Hi", t.get(L.l(Locale.UK, Locale.GERMANY)));
	}

}
