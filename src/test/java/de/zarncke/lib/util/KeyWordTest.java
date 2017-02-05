package de.zarncke.lib.util;

import static de.zarncke.lib.util.Chars.extractKeyWords;
import static de.zarncke.lib.util.Chars.splitKeyWords;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.junit.Test;

import de.zarncke.lib.i18n.Translations;

public class KeyWordTest {

	@Test
	public void testSimpleKeys() {
		HashSet<String> words = new HashSet<String>();

		splitKeyWords("! -.(/ &%$ !?#:", words, false);
		assertEquals(0, words.size());

		splitKeyWords("Hello World!", words, false);
		checkEqualsAndClear(words, "hello", "world");
		splitKeyWords("Hello World!", words, true);
		checkEqualsAndClear(words, "hello", "world");

		splitKeyWords("Hello:World!", words, true);
		checkEqualsAndClear(words, "hello:world");

		splitKeyWords("123,345, 534.121", words, false);
		checkEqualsAndClear(words, "123", "345", "534", "121");

		splitKeyWords("-Häß11ch-", words, false);
		checkEqualsAndClear(words, "häß11ch");

		splitKeyWords("\tEin echter Satz,\nder keinen Sinn macht, aber der Whitespace enthält.  ", words, false);
		checkEqualsAndClear(words, "ein", "echter", "satz", "der", "keinen", "sinn", "macht", "aber", "whitespace", "enthält");

		Translations t = new Translations("nothing");
		t.put(Locale.GERMAN, "Alles Gut!");
		t.put(Locale.ENGLISH, "'everything' OK?");

		extractKeyWords(t, words);
		checkEqualsAndClear(words, "alles", "ok", "everything", "gut", "nothing");
	}

	private void checkEqualsAndClear(final Set<String> words, final String... strings) {
		assertEquals(new HashSet<String>(Arrays.asList(strings)), words);
		words.clear();
	}

}
