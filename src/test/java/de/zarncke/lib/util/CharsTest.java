package de.zarncke.lib.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.err.GuardedTest;

public class CharsTest extends GuardedTest {
	private static final String TXT = "Hello World and all the rest!";
	private static final String HELLO_WORLD = "Hello World";
	private static final String EXPECTED_53_CHAR_PART = "Hello World. At the next fullstop we have 53 letters";

	@Test
	public void testLongs() {
		assertEquals(L.l(), Chars.extractLongs(""));
		assertEquals(L.l(), Chars.extractLongs(" "));
		assertEquals(L.l(), Chars.extractLongs(","));
		assertEquals(L.l(), Chars.extractLongs("abc"));
		assertEquals(L.l(), Chars.extractLongs("eins"));
		assertEquals(L.l(Q.l(1)), Chars.extractLongs("1"));
		assertEquals(L.l(Q.l(123)), Chars.extractLongs("123"));
		assertEquals(L.l(Q.l(1)), Chars.extractLongs("1."));
		assertEquals(L.l(Q.l(1)), Chars.extractLongs(" 1 "));
		assertEquals(L.l(Q.l(1)), Chars.extractLongs("a1a"));
		assertEquals(L.l(Q.l(1)), Chars.extractLongs("1, no"));
		assertEquals(L.l(Q.l(1), Q.l(0)), Chars.extractLongs("1.0"));
		assertEquals(L.l(Q.l(1), Q.l(2), Q.l(3)), Chars.extractLongs("1 2 3"));
		assertEquals(L.l(Q.l(1), Q.l(2), Q.l(3)), Chars.extractLongs("1,2,3"));
	}

	@Test
	public void testNumbers() {
		assertEquals(L.l(), Chars.extractDecimals(""));
		assertEquals(L.l(), Chars.extractDecimals(" "));
		assertEquals(L.l(), Chars.extractDecimals(","));
		assertEquals(L.l(), Chars.extractDecimals("abc"));
		assertEquals(L.l(), Chars.extractDecimals("eins"));
		assertEquals(L.l(Q.bd(1)), Chars.extractDecimals("1"));
		assertEquals(L.l(Q.bd(1)), Chars.extractDecimals("1."));
		assertEquals(L.l(Q.bd(1)), Chars.extractDecimals(" 1 "));
		assertEquals(L.l(Q.bd(1)), Chars.extractDecimals("a1a"));
		assertEquals(L.l(Q.bd(1)), Chars.extractDecimals("1, no"));
		assertEquals(L.l(Q.bd(1).setScale(1)), Chars.extractDecimals("1.0"));
		assertEquals(L.l(Q.bd(1.5)), Chars.extractDecimals("1.5"));
		assertEquals(L.l(Q.bd(1), Q.bd(2), Q.bd(3)), Chars.extractDecimals("1 2 3"));
		assertEquals(L.l(Q.bd(1), Q.bd(2), Q.bd(3)), Chars.extractDecimals("1,2,3"));
	}

	@Test
	public void testWrapLines() {
		// CHECKSTYLE:OFF literals
		assertEquals("a,\nb,\nc,\nd,\ne,\nf,\ng,\nh", Chars.lineWrap("a, b, c, d, e, f, g, h", 3)); // NOPMD literals
		assertEquals("a,\nb,\nc,\nd,\ne,\nf,\ng, h", Chars.lineWrap("a, b, c, d, e, f, g, h", 4));
		assertEquals("a, b,\nc, d,\ne, f,\ng, h", Chars.lineWrap("a, b, c, d, e, f, g, h", 5));
		assertEquals("a, b,\nc, d,\ne, f,\ng, h", Chars.lineWrap("a, b, c, d, e, f, g, h", 6));
		assertEquals("a, b,\nc, d,\ne, f,\ng, h", Chars.lineWrap("a, b, c, d, e, f, g, h", 7));
		assertEquals("a, b, c,\nd, e, f,\ng, h", Chars.lineWrap("a, b, c, d, e, f, g, h", 8));
		assertEquals("a, b, c, d, e, f, g, h", Chars.lineWrap("a, b, c, d, e, f, g, h", 100));

		assertEquals("123456\n12\n12345\n7890\n1", Chars.lineWrap("123456\n12\n12345 7890\n1", 8));
		// CHECKSTYLE:ON
	}

	public void testStripSgml() {
		assertEquals("", Chars.stripTagsSimple(""));
		assertEquals(HELLO_WORLD, Chars.stripTagsSimple(HELLO_WORLD));
		checkContent("");
		checkContent("\n");
		checkContent("cute");
		checkContent("Hello World.");
		checkContent("&lt;notag&gt;");
		assertEquals("Du totaler Held!", Chars.stripTagsSimple("Du <em>totaler</em> Held!", false));
		assertEquals("Du totaler Held!", Chars.stripTagsSimple("Du <em>totaler</em> Held!"));
		assertEquals("Du totaler Held!", Chars.stripTagsSimple("Du<em>totaler</em>Held!"));
	}

	private static void checkContent(final String expected) {
		check(expected, "<>");
		check(expected, "<a>");
		check(expected, "<a >");
		check(expected, "<a/>");
		check(expected, "<a />");
		check(expected, "<a href=\"test\" nix=\"\">");
		check(expected, "<img src=test>");
		check(expected, "<!---->");
		check(expected, "<!-- -->");
		check(expected, "<!-- long and laborious. -->");
	}

	public static void check(final String expected, final String tagDummy) {
		assertEquals("", Chars.stripTagsSimple(tagDummy));
		String expectedClean = Chars.simplifyWhiteSpace(expected);
		assertEquals(expectedClean, Chars.stripTagsSimple(tagDummy + expected));
		assertEquals(expectedClean, Chars.stripTagsSimple(expected + tagDummy));
	}

	public void testIndent() {
		assertEquals("", Chars.indentEveryLine("", ""));
		assertEquals("tab", Chars.indentEveryLine("", "tab"));
		assertEquals(" text", Chars.indentEveryLine("text", " "));
		assertEquals(" \n ", Chars.indentEveryLine("\n", " "));
		assertEquals("  Hello\n  World\n  ", Chars.indentEveryLine("Hello\nWorld\n", "  "));
	}

	public void testSummary() {
		assertEquals("", Chars.summarize(HELLO_WORLD, 0));
		assertEquals("H", Chars.summarize(HELLO_WORLD, 1));
		assertEquals("He", Chars.summarize(HELLO_WORLD, 2));
		assertEquals("Hel", Chars.summarize(HELLO_WORLD, 3));
		assertEquals(HELLO_WORLD, Chars.summarize(HELLO_WORLD, 100));
		assertEquals("must truncate", "abcdefghijklmnopq...", Chars.summarize("abcdefghijklmnopqrstuvwxyz", 20));
		assertEquals("must use space", "Hello World...",
				Chars.summarize("Hello World more than 15 chars.", "Hello World m...".length()));
		assertEquals(EXPECTED_53_CHAR_PART + "...",
				Chars.summarize(EXPECTED_53_CHAR_PART + ". Here words with spaces but thats ignored.", 53 * 5 / 4 - 1));
	}

	public void testEscape() {
		assertEquals(null, Chars.escape(null, "abc", ':'));
		assertEquals("", Chars.escape("", "", ':'));
		assertEquals("", Chars.escape("", "&/", ':'));
		assertEquals(":12:", Chars.escape("&", "&", ':'));
		assertEquals("&12&", Chars.escape("&", "&", '&'));
		assertEquals("hallo", Chars.escape("hallo", "&/", ':'));
		assertEquals("a:12:b=c:1b:3", Chars.escape("a&b=c/3", "&/", ':'));
		assertEquals(":12::12::", Chars.escape("&&:", "&", ':'));
		assertEquals(":12::12::1m:", Chars.escape("&&:", "&:", ':'));

		assertEquals(null, Chars.unescape(null, ':'));
		assertEquals("hallo", Chars.unescape("hallo", ':'));
		assertEquals("a&b=c/3", Chars.unescape("a:12:b=c:1b:3", ':'));
		assertEquals("&&:", Chars.unescape(":12::12::1m:", ':'));

		Random r = new Random();
		for (int i = 0; i < 1000; i++) {
			StringBuilder sb = new StringBuilder();
			for (int j = r.nextInt(20); j > 0; j--) {
				sb.append((char) r.nextInt(300));
			}

			assertEquals("stress " + i, sb.toString(), Chars.unescape(Chars.escape(sb, "%&/$!?%", '%'), '%'));
		}
	}

	public void testStringTools() {
		// TEST commonPrefix
		assertEquals(0, Chars.commonPrefix("", ""));
		assertEquals(0, Chars.commonPrefix("a", "b"));
		assertEquals(1, Chars.commonPrefix("a", "a"));
		assertEquals(1, Chars.commonPrefix("ab", "ac"));
		assertEquals(6, Chars.commonPrefix("Hallo Welt", "Hallo Otto"));

		// TEST split

		try {
			Chars.split("", "", true, true);
			fail();
		} catch (IllegalArgumentException iae) {
			;
		}
		assertEquals(Collections.EMPTY_LIST, Chars.split("  ", " ", true, true));
		assertEquals(Collections.EMPTY_LIST, Chars.split("  ", ",", true, true));
		assertEquals(Collections.EMPTY_LIST, Chars.split(" , ", ",", true, true));
		assertEquals(Arrays.asList(new String[] { "a", "b", "c" }), Chars.split("a,b,c", ",", true, true));
		assertEquals(Arrays.asList(new String[] { "a", "b", "c" }), Chars.split("a,b ,c,,", ",", true, true));
		assertEquals(Arrays.asList(new String[] { "a", "b", "c" }), Chars.split(", a, b, ,c", ",", true, true));

		assertEquals(Arrays.asList(new String[] { "", "b", "", "c" }), Chars.split(", b, ,c", ",", true)); // ,false
		assertEquals(Arrays.asList(new String[] { "", "b", "", "c" }), Chars.split(", b, ,c", ",", true, false));

		assertEquals(Arrays.asList(new String[] { "", " b", " ", "c " }), Chars.split(", b, ,c ", ",", false, false));

		assertEquals(Arrays.asList(new String[] { " b", "c ", " " }), Chars.split(", b,,,c , ", ",", false, true));
		assertEquals(Arrays.asList(new String[] { "abc", "--" }), Chars.split("abc..--", "..", false, false));

		// TEST simpleMatch
		assertTrue("trivial case", Chars.simpleMatch("", ""));
		assertTrue("trivial case2", Chars.simpleMatch("hallo", "hallo"));
		assertTrue("complete match", Chars.simpleMatch("hallo", "*"));
		assertTrue("start match", Chars.simpleMatch("hallo", "*lo"));
		assertTrue("middle match", Chars.simpleMatch("hallo", "ha*o"));
		assertTrue("end match", Chars.simpleMatch("hallo", "hal*"));
		assertTrue("none-match simple", !Chars.simpleMatch("hallo", "halo"));
		assertTrue("none-match too long", !Chars.simpleMatch("hallo", "hall*allo"));

		// testing replace(All)

		// cases equal for both
		assertEquals(Chars.replace("abc", "abc", "dd"), "dd");
		assertEquals(Chars.replaceAll("abc", "abc", "dd"), "dd");
		assertEquals(Chars.replace("aabcaabcaa", "bc", "d"), "aadaadaa");
		assertEquals(Chars.replaceAll("aabcaabcaa", "bc", "d"), "aadaadaa");
		assertEquals(Chars.replace("xxabcabcabcxx", "abc", "dd"), "xxddddddxx");
		assertEquals(Chars.replaceAll("xxabcabcabcxx", "abc", "dd"), "xxddddddxx");
		assertEquals(Chars.replace("xabcabcabcx", "abc", "cba"), "xcbacbacbax");
		assertEquals(Chars.replaceAll("xabcabcabcx", "abc", "cba"), "xcbacbacbax");
		assertEquals(Chars.replace("abcde", "bd", "yyy"), "abcde");
		assertEquals(Chars.replaceAll("abcde", "bd", "yyy"), "abcde");

		// cases different
		assertEquals(Chars.replace("abb", "ab", "a"), "ab");
		assertEquals(Chars.replaceAll("abb", "ab", "a"), "a");
		assertEquals(Chars.replace("aab", "ab", "b"), "ab");
		assertEquals(Chars.replaceAll("aab", "ab", "b"), "b");
		assertEquals(Chars.replace("xxaaabbbyy", "ab", ""), "xxaabbyy");
		assertEquals(Chars.replaceAll("xxaaabbbyy", "ab", ""), "xxyy");

		// cases dependent on left-right order,
		// these may or may not be what you expect!
		assertEquals(Chars.replace("abababa", "aba", "ba"), "babba");
		assertEquals(Chars.replaceAll("abababa", "aba", "ba"), "babba");
		assertEquals(Chars.replace("abababa", "aba", "ab"), "abbab");
		assertEquals(Chars.replaceAll("abababa", "aba", "ab"), "abbab");

		// TEST misc
		assertEquals("", Chars.capitalize(""));
		assertEquals("123", Chars.capitalize("123"));
		assertEquals("Hallo", Chars.capitalize("hallo"));
		assertEquals("HALLO Du Da", Chars.capitalize("HALLO Du Da"));

		assertEquals("", Chars.simplifyWhiteSpace(""));
		assertEquals("Ja und Nein.", Chars.simplifyWhiteSpace("Ja und Nein."));
		assertEquals("- --", Chars.simplifyWhiteSpace("   -  -- "));
		assertEquals("", Chars.simplifyWhiteSpace("\n"));
		assertEquals("", Chars.simplifyWhiteSpace("\t\t\t"));
		assertEquals("a b c", Chars.simplifyWhiteSpace("a\t b\n\t c"));
		assertEquals("Tag Nacht.", Chars.simplifyWhiteSpace("Tag\n\t\u000c\rNacht."));

		assertEquals("H", Chars.summarize(TXT, 1));
		assertEquals("Hel", Chars.summarize(TXT, 3));
		assertEquals("Hello Worl", Chars.summarize(TXT, 10));
		assertEquals("Hello World an", Chars.summarize(TXT, 14));
		assertEquals(TXT, Chars.summarize(TXT, 100));

		assertEquals("Hello World...", Chars.summarize(TXT + TXT, 16));
		assertEquals("Hello World and...", Chars.summarize(TXT + TXT, 18));
		assertEquals(TXT, Chars.summarize(TXT, 100));
	}

	public void testFindOverlap() {
		assertEquals(-1, Chars.findOverlap("", ""));
		assertEquals(-1, Chars.findOverlap("test", ""));
		assertEquals(-1, Chars.findOverlap("", "test"));
		assertEquals(-1, Chars.findOverlap("hello", "world"));
		assertEquals(0, Chars.findOverlap("hello", "hello"));
		assertEquals(1, Chars.findOverlap("hello", "ello"));
		assertEquals(1, Chars.findOverlap("hello", "ello world"));
		assertEquals(4, Chars.findOverlap("hello", "o"));
		assertEquals(3, Chars.findOverlap("hello", "lo"));
		assertEquals(4, Chars.findOverlap("hello", "oma"));
		assertEquals(0, Chars.findOverlap("hello", "hello"));
		assertEquals(0, Chars.findOverlap("hello", "hello world"));

		assertEquals("", Chars.overlap("", ""));
		assertEquals("hello world", Chars.overlap("hello ", "world"));
		assertEquals("hello world", Chars.overlap("hello ", " world"));
		assertEquals("hello world", Chars.overlap("hello world", "world"));
		assertEquals("hello world!", Chars.overlap("hello world", "world!"));
	}

	public void testFindAssignments() {
		assertTrue(Chars.extractAssignments("").isEmpty());
		assertTrue(Chars.extractAssignments("Hello").isEmpty());
		assertTrue(Chars.extractAssignments("Hello = World").isEmpty());
		assertTrue(Chars.extractAssignments("a = 1").isEmpty());
		assertTrue(Chars.extractAssignments("a =1").isEmpty());
		assertTrue(Chars.extractAssignments("a[3]=1").isEmpty());

		testAssignments("a=", "a", "");
		testAssignments("a=1", "a", "1");
		testAssignments("a=1 ", "a", "1");
		testAssignments("a=1\n2", "a", "1");
		testAssignments("(a=1)", "a", "1");
		testAssignments("[a=1]", "a", "1");
		testAssignments("{a=1}", "a", "1");
		testAssignments("a=1)", "a", "1");
		testAssignments("a=1.", "a", "1.");
		testAssignments("a=1,", "a", "1,");
		testAssignments("a=1;", "a", "1;");
		testAssignments("a=1-2-3", "a", "1-2-3");
		testAssignments("a=1,123.56E-2", "a", "1,123.56E-2");

		testAssignments("a_b=1", "a_b", "1"); // underscore and digits counts as identifier parts
		testAssignments("3=1", "3", "1");
		testAssignments("a.b=1", "b", "1"); // caution!
		testAssignments("a-b=1", "b", "1");
		testAssignments("a12=1", "a12", "1");
		testAssignments("a.3=1", "3", "1"); // caution!

		testAssignments("Hello a=1 Worlds", "a", "1");
		testAssignments("Hello a=1 Worlds a=1 !", "a", "1");
		testAssignments("Hello a=2 Worlds a=1 !", "a", "1");
		testAssignments("Hello a=1 b=2 Worlds", "a", "1", "b", "2");
		testAssignments("a=1 b=2 c=3 b=0", "a", "1", "b", "0", "c", "3");

		testAssignments("Hello a=!!! Worlds", "a", "!!!");
		testAssignments("Hello CamelCase=1 Worlds", "CamelCase", "1");
		testAssignments("Hello 123=1 Worlds", "123", "1");
		testAssignments("Hello javaId123=1 Worlds", "javaId123", "1");
		testAssignments("Hello javaId123=Value!!! Worlds", "javaId123", "Value!!!");
		testAssignments("Hello a=\" a long string \"whatever", "a", " a long string ");
	}

	private void testAssignments(final String test, final String... keyOrVal) {
		assert keyOrVal != null;
		assert keyOrVal.length % 2 == 0;
		Map<String, String> m = Chars.extractAssignments(test);
		for (int i = 0; i < keyOrVal.length; i += 2) {
			String val = keyOrVal[i + 1];
			String key = keyOrVal[i];
			assertEquals("expected " + val + " for " + key + " in " + test, keyOrVal[i + 1], m.get(keyOrVal[i]));
		}
		assertEquals(keyOrVal.length / 2, m.size());
	}

	public void testList() {
		assertContentEquals(L.l(), Chars.gracfulSplitUserEnteredList(""));
		assertContentEquals(L.l("a"), Chars.gracfulSplitUserEnteredList("a"));
		assertContentEquals(L.l("a"), Chars.gracfulSplitUserEnteredList(" a "));
		assertContentEquals(L.l("a", "b"), Chars.gracfulSplitUserEnteredList("a,b"));
		assertContentEquals(L.l("a", "b"), Chars.gracfulSplitUserEnteredList("a b"));
		assertContentEquals(L.l("a", "b"), Chars.gracfulSplitUserEnteredList("a, b"));
		assertContentEquals(L.l("a", "b"), Chars.gracfulSplitUserEnteredList("a  b"));
		assertContentEquals(L.l("a", "b"), Chars.gracfulSplitUserEnteredList("a,  b"));
	}
}
