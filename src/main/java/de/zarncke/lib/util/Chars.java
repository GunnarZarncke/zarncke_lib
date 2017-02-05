package de.zarncke.lib.util;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.i18n.Translations;

/**
 * helper class with miscellaneous static methods.
 */
public final class Chars {

	private static final int MAX_BASE = 36;
	private static final String COMMENT_CLOSE = "-->";
	private static final String COMMENT_OPEN = "<!--";
	public static final int TYPICAL_LINE_LENGTH = 80;
	public static final char LEAST_PRINTABLE_ASCII = 32;
	public static final String VALID_SENTENCE_END_CHARACTERS = "?!:.;,";
	public static final String VALID_WORD_BREAK_CHARACTERS = "-.,_";
	public static final String CONSECUTIVE_WHITESPACE = "\\s+";
	private static final Pattern CONSECUTIVE_WHITESPACE_PATTERN = Pattern.compile(CONSECUTIVE_WHITESPACE);
	public static final String ELLIPSIS = "...";

	private static final double FRACTION_OF_TEXT_ACCEPTABLE_FOR_EARLY_GOOD_BREAK = .8;
	private static final int VERY_SHORT_SUMMARY_LENGTH = 15;
	private static final Pattern WORD_PATTERN_WITH_COLON = Pattern.compile("[:\\p{javaLetter}\\p{Digit}]+");
	private static final Pattern WORD_PATTERN = Pattern.compile("[\\p{javaLetter}\\p{Digit}]+");
	private static final Pattern ASSIGNMENT_PATTERN = Pattern
			.compile("\\b([\\p{Alnum}_]+)=((\"[^\"]*\")|([^\\s\\]\\)\\}]*))");

	private Chars() {
		// helper
	}

	/**
	 * Replace everything that looks like an SGML tag (e.g. "&lt/em&gt;") by a space in the.
	 * XML comments are removed.
	 * HTML entities are left as is, only "&amp;nbsp;" is replaced by a space.
	 * Space is simplified.
	 * Unescaped <code>&gt;</code> in attributes breaks this simpleminded function.
	 * It cannot deal with CDATA or any other advanced xml including processing instructions.
	 *
	 * @param text != null
	 * @return sanitized text
	 */
	public static CharSequence stripTagsSimple(final String text) {
		return stripTagsSimple(text, true);
	}

	/**
	 * Remove everything that looks like an SGML tag (e.g. "&lt/em&gt;") from a text.
	 * XML comments are removed.
	 * HTML entities are left as is, only "&amp;nbsp;" is replaced by a space.
	 * Space is simplified.
	 * Unescaped <code>&gt;</code> in attributes breaks this simpleminded function.
	 * It cannot deal with CDATA or any other advanced xml including processing instructions.
	 *
	 * @param text != null
	 * @param replaceTagBySpace true: tags are replaced by space; false: just removed
	 * @return sanitized text
	 */
	public static CharSequence stripTagsSimple(final String text, final boolean replaceTagBySpace) {
		StringBuilder sb = new StringBuilder(text.length());
		int pos = 0;
		while (pos < text.length()) {
			int openPos = text.indexOf('<', pos);
			if (openPos < 0) {
				sb.append(text.substring(pos));
				break;
			}
			sb.append(text.substring(pos, openPos));
			if (replaceTagBySpace) {
				sb.append(' ');
			}
			if (openPos + COMMENT_OPEN.length() <= text.length()
					&& text.substring(openPos, openPos + COMMENT_OPEN.length()).equals(COMMENT_OPEN)) {
				pos = text.indexOf(COMMENT_CLOSE, openPos + COMMENT_OPEN.length()) + COMMENT_OPEN.length() - 1;
			} else {
				pos = text.indexOf('>', openPos + 1) + 1;
			}
			if (pos <= 0) {
				break;
			}
		}
		return simplifyWhiteSpace(sb.toString().replaceAll("\\&nbsp;", " "));
	}

	/**
	 * Replaces all problematicCharacters in the problematicString with escape, alnum, escape.
	 * Example: escape("a=b:c","=:",":")="a:x:b:x:c"
	 *
	 * @param problematicString any, may be null
	 * @param problematicCharacters != null, adding the escape here is usually sensible
	 * @param escape any
	 * @return String null if problematic is null, otherwise without any of the problematic characters (except the
	 * escape)
	 */
	public static String escape(final CharSequence problematicString, final String problematicCharacters,
			final char escape) {
		if (problematicString == null) {
			return null;
		}
		CharArrayWriter sb = new CharArrayWriter();
		for (int i = 0; i < problematicString.length(); i++) {
			char c = problematicString.charAt(i);
			if (problematicCharacters.indexOf(c) >= 0) {
				sb.append(escape).append(Integer.toString(c, MAX_BASE)).append(escape);
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * Reverses the {@link #escape(CharSequence, String, char)}.
	 *
	 * @param escapedString may be null
	 * @param escape used by escape()
	 * @return the original String (null if the escapedString was null)
	 */
	public static String unescape(final CharSequence escapedString, final char escape) {
		if (escapedString == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < escapedString.length(); i++) {
			char c = escapedString.charAt(i);
			if (c == escape) {
				i++;
				int j = i;
				while (i < escapedString.length()) {
					if (escapedString.charAt(i) == escape) {
						break;
					}
					i++;
				}
				sb.append((char) Integer.parseInt(escapedString.subSequence(j, i).toString(), MAX_BASE));
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * @param chars any {@link CharSequence}
	 * @return true if chars is null or empty (="")
	 */
	public static boolean isEmpty(final CharSequence chars) {
		return chars == null || chars.length() == 0;
	}

	public static void extractKeyWords(final Collection<String> texts, final HashSet<String> words) {
		for (String txt : texts) {
			splitKeyWords(txt, words, false);
		}
	}

	public static void extractKeyWords(final Translations text, final HashSet<String> words) {
		if (text == null) {
			return;
		}
		for (String trans : text.values()) {
			splitKeyWords(trans, words, false);
		}
	}

	/**
	 * Extracts key words from a text. The key words are consecutive runs of word character ({@value #WORD_PATTERN}).
	 *
	 * @param text any text, may be null
	 * @param words where words are collected (may be null, then a new {@link HashSet} is created)
	 * @param acceptColon true: the colon is also accepted as a valid word character
	 * @return the given or created collection
	 */
	public static Collection<String> splitKeyWords(final String text, final Collection<String> words,
			final boolean acceptColon) {
		Collection<String> theWords = words == null ? new HashSet<String>() : words;
		if (text == null) {
			return theWords;
		}
		Matcher word = (acceptColon ? WORD_PATTERN_WITH_COLON : WORD_PATTERN).matcher(text);
		while (word.find()) {
			theWords.add(word.group().toLowerCase());
		}
		return theWords;
	}

	/**
	 * Concat the string n times.
	 *
	 * @param str != null
	 * @param times >=0
	 * @return String != null
	 */
	public static CharSequence repeat(final String str, final int times) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < times; i++) {
			sb.append(str);
		}
		return sb;
	}

	/**
	 * Escape all characters that may not occur in a Java identifier with "$<num>$".
	 * All spaces contracted and replaced by a single "_". "-" is replaced by "_".
	 * No case conversion whatsoever is done.
	 *
	 * @param name != null
	 * @return String != null
	 */
	public static String escapeForJavaIdentifier(final String name) {
		String base = name.trim().replaceAll(CONSECUTIVE_WHITESPACE, " ");
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < base.length(); i++) {
			char c = base.charAt(i);
			if (Character.isLetterOrDigit(c)) {
				sb.append(c);
			} else if (c == '_' || c == '-' || c == ' ') {
				sb.append('_');
			} else {
				sb.append('$').append((int) c).append('$');
			}
		}
		return sb.toString();
	}

	/**
	 * Get length of common prefix of two Strings.
	 *
	 * @param a
	 * String
	 * @param b
	 * String
	 * @return length of longest common prefix
	 */
	public static int commonPrefix(final String a, final String b) {
		int i = 0;
		int l = Math.min(a.length(), b.length());
		while (i < l) {
			if (a.charAt(i) != b.charAt(i)) {
				return i;
			}
			i++;
		}
		return i;
	}

	public static List<String> parseLines(final Reader fullyReadReader) throws IOException {
		LineNumberReader lnr = new LineNumberReader(fullyReadReader);
		List<String> list = new ArrayList<String>();
		String line;
		while ((line = lnr.readLine()) != null) {
			list.add(line);
		}
		return list;
	}

	public static String left(final String s, final int n) {
		return s.length() < n ? s : s.substring(0, n);
	}

	public static String right(final String s, final int n) {
		int l = s.length();
		return l < n ? s : s.substring(l - n);
	}

	public static CharSequence join(final Collection<?> strings, final String separator) {
		if (strings.isEmpty()) {
			return "";
		}
		if (strings.size() == 1) {
			return String.valueOf(strings.iterator().next());
		}
		StringBuilder sb = new StringBuilder();
		for (Object s : strings) {
			sb.append(s).append(separator);
		}
		sb.setLength(sb.length() - separator.length());
		return sb;
	}

	/**
	 * Split a string into its sub-Strings. Trims fields and ignores empty ones.
	 *
	 * @param str the String to split
	 * @param sep the separator String
	 * @return List of Strings
	 */
	public static List<String> split(final String str, final String sep) {
		return split(str, sep, true, true);
	}

	/**
	 * Split a string into its sub-Strings. Uses all (empty also) fields
	 *
	 * @param str the String to split
	 * @param sep the separator String
	 * @param trim whether to trim white space
	 * @return List of Strings
	 */
	public static List<String> split(final String str, final String sep, final boolean trim) {
		return split(str, sep, trim, false);
	}

	/**
	 * Split a string into its sub-Strings.
	 *
	 * @param str
	 * the String to split
	 * @param sep
	 * the separator String
	 * @param trim
	 * whether to trim white space
	 * @param ignore
	 * whether to ignore empty sub-Srings
	 * @return List of Strings
	 */
	public static List<String> split(final String str, final String sep, final boolean trim, final boolean ignore) {
		if ("".equals(sep)) {
			throw new IllegalArgumentException("empty separator not valid!");
		}

		List<String> list = new ArrayList<String>();

		int q = 0;
		while (true) {
			int p = str.indexOf(sep, q);
			if (p < 0) {
				String sub = str.substring(q);
				if (trim) {
					sub = sub.trim();
				}
				if (!ignore || sub.length() != 0) {
					list.add(sub);
				}
				break;
			}

			String sub = str.substring(q, p);
			if (trim) {
				sub = sub.trim();
			}
			if (!ignore || sub.length() != 0) {
				list.add(sub);
			}

			q = p + sep.length();
		}

		return list;
	}

	/**
	 * a simple string match, that understands at most one single "*" as wildcard.
	 *
	 * @param str
	 * the String to test
	 * @param pat
	 * a pattern, that may contain ONE "*"
	 * @return true if str matches pat
	 */
	public static boolean simpleMatch(final String str, final String pat) {
		int p = pat.indexOf('*');
		if (p < 0) {
			return str.equals(pat);
		}

		return str.startsWith(pat.substring(0, p)) && str.endsWith(pat.substring(p + 1))
				&& str.length() >= pat.length() - 1;
	}

	/**
	 * capitalize (toUpper()) the first letter of the String.
	 *
	 * @param s != null
	 * @return String
	 */
	public static String capitalize(final String s) {
		return s.length() == 0 ? "" : s.substring(0, 1).toUpperCase() + s.substring(1);
	}

	public static String toHex(final byte i) {
		String s = Integer.toHexString(i);
		return "00".substring(s.length()) + s;
	}

	public static String toHex(final short i) {
		String s = Integer.toHexString(i);
		return "0000".substring(s.length()) + s;
	}

	public static String toHex(final int i) {
		String s = Integer.toHexString(i);
		return "00000000".substring(s.length()) + s;
	}

	public static String toHex(final long i) {
		String s = Long.toHexString(i);
		return "0000000000000000".substring(s.length()) + s;
	}

	public static String toHex(final long i, final int hexits) {
		String s = Long.toHexString(i);
		return "0000000000000000".substring(s.length() + 16 - hexits) + s;
	}

	public static String toLeadingZeros(final long i, final int digits) {
		String s = Long.toString(i);
		return "00000000000000000000".substring(s.length() + 20 - digits) + s;
	}

	/**
	 * replaces all pat in src with repl and returns the result. Does only one scan from left to right.
	 * does NOT do
	 * replace("abb","ab","a") -> "a", BUT -> "ab"!
	 *
	 * @param src != null
	 * @param pat != null
	 * @param repl replacement != null
	 * @return String != null
	 */
	public static String replace(final String src, final String pat, final String repl) {
		StringBuffer sb = new StringBuffer();
		int l = pat.length();
		if (l == 1) {
			StringTokenizer st = new StringTokenizer(src, pat, true);
			while (st.hasMoreTokens()) {
				String tok = st.nextToken();
				if (tok.equals(pat)) {
					sb.append(repl);
				} else {
					sb.append(tok);
				}
			}
		} else {
			int ap = 0;
			int p = -1;
			while (true) {
				p = src.indexOf(pat, ap);
				if (p < 0) {
					sb.append(src.substring(ap));
					break;
				}
				sb.append(src.substring(ap, p)).append(repl);
				ap = p + l;
			}
		}
		return sb.toString();
	}

	/**
	 * replaces all pat in src with repl and returns the result. Does left to right scans until no more matches of pat.
	 * does do "abb","ab","a" -> "a"!
	 *
	 * @param src != null
	 * @param pat != null
	 * @param repl replacement != null
	 * @return String != null
	 */
	public static String replaceAll(final String src, final String pat, final String repl) {
		String res = src;
		StringBuffer sb = new StringBuffer();
		int l = pat.length();
		if (l == 1) {
			int toks;
			do {
				toks = 0;
				StringTokenizer st = new StringTokenizer(res, pat, true);
				while (st.hasMoreTokens()) {
					String tok = st.nextToken();
					if (tok.equals(pat)) {
						sb.append(repl);
					} else {
						sb.append(tok);
					}
					toks++;
				}
				res = sb.toString();
				sb.setLength(0);
			} while (toks > 1);
		} else {
			boolean found;
			do {
				found = false;
				int ap = 0;
				int p = -1;
				while (true) {
					p = res.indexOf(pat, ap);
					if (p < 0) {
						sb.append(res.substring(ap));
						break;
					}
					found = true;
					sb.append(res.substring(ap, p)).append(repl);
					ap = p + l;
				}
				res = sb.toString();
				sb.setLength(0);
			} while (found);
		}

		return res;
	}

	/**
	 * Wraps lines at - hopefully - suitable lcoations.
	 *
	 * @param text
	 * @param maxLineLength > 0
	 * @return String
	 */
	public static String lineWrap(@Nonnull final String text, final int maxLineLength) {
		if (text.length() <= maxLineLength) {
			return text;
		}
		StringBuilder sb = new StringBuilder();
		String remaining = text;
		while (remaining.length() > maxLineLength) {
			int p = findSuitableBreak(remaining, maxLineLength - 1);
			sb.append(remaining.substring(0, p + 1).trim()).append("\n");
			remaining = remaining.substring(p + 1).trim();
		}
		return sb.append(remaining).toString();
	}

	/**
	 * See {@link #summarize(CharSequence, int, boolean)}. Always compresses space.
	 *
	 * @param msg != null
	 * @param maxLength to fit in
	 * @return String with length<=maxLength
	 */
	public static CharSequence summarize(final CharSequence msg, final int maxLength) {
		return summarize(msg, maxLength, true);
	}

	/**
	 * Summarizes a text. Removes words, sentences or characters to fit the size.
	 * Uses only simple character operations.
	 *
	 * @param msg != null
	 * @param maxLength to fit in
	 * @param compressSpace true: all runs of consecutive space are replaced by single white-spaces (useful for
	 * one-liner)
	 * @return String with length<=maxLength
	 */
	public static CharSequence summarize(final CharSequence msg, final int maxLength, final boolean compressSpace) {
		if (msg == null) {
			return "null";
		}

		String shorter;
		if (compressSpace) {
			shorter = simplifyWhiteSpace(msg);
		} else {
			shorter = msg.toString().trim();
		}
		if (shorter.length() <= maxLength) {
			return shorter;
		}
		if (shorter.length() < VERY_SHORT_SUMMARY_LENGTH) {
			return shorter.substring(0, maxLength);
		}
		if (maxLength < VERY_SHORT_SUMMARY_LENGTH) {
			return shorter.substring(0, maxLength);
		}

		int p = findSuitableBreak(shorter, maxLength - ELLIPSIS.length());
		return shorter.substring(0, p) + ELLIPSIS;
	}

	/**
	 * @param text
	 * @param latestBreak
	 * @return position of break (index of first char belonging to next line
	 */
	public static int findSuitableBreak(final String text, final int latestBreak) {
		int goodEnd = 0;
		// use existing line breaks as is
		int p = text.indexOf("\n");
		if (p >= 0 && p < latestBreak) {
			return p;
		}
		// try to find an end at a sentence boundary
		for (char sentenceEnd : VALID_SENTENCE_END_CHARACTERS.toCharArray()) {
			p = text.lastIndexOf(sentenceEnd, latestBreak);
			if (p > goodEnd && text.charAt(p + 1) == ' ') {
				goodEnd = p;
			}
		}
		if (goodEnd >= latestBreak * FRACTION_OF_TEXT_ACCEPTABLE_FOR_EARLY_GOOD_BREAK) {
			return goodEnd;
		}
		for (char sentenceEnd : VALID_WORD_BREAK_CHARACTERS.toCharArray()) {
			p = text.lastIndexOf(sentenceEnd, latestBreak);
			if (p > goodEnd) {
				goodEnd = p;
			}
		}
		if (goodEnd >= latestBreak * FRACTION_OF_TEXT_ACCEPTABLE_FOR_EARLY_GOOD_BREAK) {
			return goodEnd;
		}

		// try to find an end at a word boundary
		goodEnd = text.lastIndexOf(' ', latestBreak);
		if (goodEnd >= (int) (latestBreak * FRACTION_OF_TEXT_ACCEPTABLE_FOR_EARLY_GOOD_BREAK)) {
			return goodEnd;
		}
		return latestBreak;
	}

	/**
	 * Replaces newlines and other space by single spaces.
	 *
	 * @param text != null
	 * @return same text trimmed and with all whitespace runs replaced by single space
	 */
	public static String simplifyWhiteSpace(final CharSequence text) {
		return CONSECUTIVE_WHITESPACE_PATTERN.matcher(text).replaceAll(" ").trim();
	}

	public static String leftBefore(final String str, final String border) {
		int p = str.indexOf(border);
		return p < 0 ? str : str.substring(0, p);
	}

	public static String rightAfter(final String str, final String border) {
		int p = str.lastIndexOf(border);
		return p < 0 ? "" : str.substring(p + border.length());
	}

	public static String dropRight(final String str, final int charsCutAwayAtEnd) {
		return str.length() < charsCutAwayAtEnd ? "" : str.substring(0, str.length() - charsCutAwayAtEnd);
	}

	public static String dropTrailingSlash(final String path) {
		return path.endsWith("/") ? dropRight(path, 1) : path;
	}

	public static String indentEveryLine(final String text, final String indentation) {
		return indentation + text.replaceAll("\n", "\n" + indentation);
	}

	/**
	 * Finds the longest possible overlap between two strings.
	 * Note: Potentially quadratic search. Use only for short strings (<100 chars).
	 *
	 * @param left != null
	 * @param right != null
	 * @return index in left where the right parts starts or -1 if no overlap
	 */
	public static int findOverlap(final String left, final String right) {
		StringBuilder sb = new StringBuilder(right);
		while (sb.length() > 0) {
			if (left.endsWith(sb.toString())) {
				return left.length() - sb.length();
			}
			sb.setLength(sb.length() - 1);
		}
		return -1;
	}

	/**
	 * Overlaps at the {@link #findOverlap(String, String)} point.
	 *
	 * @param left != null
	 * @param right != null
	 * @return overlapping of both.
	 */
	public static String overlap(final String left, final String right) {
		int p = findOverlap(left, right);
		return p < 0 ? left + right : left.subSequence(0, p) + right;
	}

	public static boolean containSpecialCars(final String string) {
		for (char c : string.toCharArray()) {
			if (c < 32 || c > 128) {
				return true;
			}
		}
		return false;
	}

	public static String join(final String s1, final String s2) {
		return s1 == null ? s2 : s2 == null ? s1 : s1 + " " + s2;
	}

	public static void dropLastChar(final StringBuilder line) {
		if (line.length() > 0) {
			line.setLength(line.length() - 1);
		}
	}

	/**
	 * @param string to match
	 * @param pattern see {@link Pattern}
	 * @return Matcher if pattern is {@link Matcher#find() found}, null otherwise
	 */
	public static Matcher match(final CharSequence string, final String pattern) {
		Matcher m = Pattern.compile(pattern).matcher(string);
		return m.find() ? m : null;
	}

	/**
	 * Null-safe comparison of Strings.
	 *
	 * @param a any
	 * @param b any
	 * @return as {@link String#compareTo(String)}, but null is considered smaller then any String
	 */
	public static int compare(@Nullable final String a, @Nullable final String b) {
		return a == null ? b == null ? 0 : -1 : b == null ? 1 : a.compareTo(b);
	}

	/**
	 * Finds all places which look like key-value assignments in the given free text.
	 * An assignment looks like <word-boundary><identifier>="<any>"
	 * or <word-boundary><identifier>=<any-until-whitespace-or-closing-brace>.
	 *
	 * @param freeText any
	 * @return Map of found key value pars (no duplicates possible, last assignment wins)
	 */
	@Nonnull
	public static Map<String, String> extractAssignments(@Nonnull final String freeText) {
		Map<String, String> assignments = new HashMap<String, String>();
		Matcher m = ASSIGNMENT_PATTERN.matcher(freeText);
		while (m.find()) {
			String group = m.group(2);
			if (group.startsWith("\"") && group.endsWith("\"") && group.length() >= 2) {
				group = group.substring(1, group.length() - 1);
			}
			assignments.put(m.group(1), group);
		}
		return assignments;
	}

	/**
	 * Splits human entered lists. Surrounding space is trimmed.
	 * Separator: , and spaces.
	 *
	 * @param textList
	 * @return List of items, may be empty
	 */
	@Nonnull
	public static List<String> gracfulSplitUserEnteredList(@Nonnull final String textList) {
		return isEmpty(textList) ? L.<String> e() : L.l(textList.trim().split("(, *| +)"));
	}

	public static List<Long> extractLongs(@Nonnull final String textWithNumbers) {
		Pattern p = Pattern.compile("[0-9]+");

		List<Long> res = L.l();

		Matcher matcher = p.matcher(textWithNumbers);
		while (matcher.find()) {
			res.add(Long.valueOf(matcher.group(0)));
		}

		return res;
	}

	public static List<BigDecimal> extractDecimals(@Nonnull final String textWithNumbers) {
		Pattern p = Pattern.compile("[0-9]+(\\.[0-9]*)?");

		List<BigDecimal> res = L.l();

		Matcher matcher = p.matcher(textWithNumbers);
		while (matcher.find()) {
			res.add(new BigDecimal(matcher.group(0)));
		}

		return res;
	}
}
