package de.zarncke.lib.util;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.zarncke.lib.coll.L;

/**
 * Replaces multiple parts of a string with corresponding replacements.
 *
 * @author Gunnar Zarncke
 * @clean 19.03.2012
 */
public class Replacer {
	interface Rule {
		CharSequence apply(CharSequence source);
	}

	public static class RegExRule implements Rule {
		private final Pattern pattern;
		private final String replacement;

		public RegExRule(final Pattern pattern, final String replacement) {
			this.pattern = pattern;
			this.replacement = replacement;
		}

		@Override
		public CharSequence apply(final CharSequence source) {
			Matcher m = this.pattern.matcher(source);
			return m.replaceAll(this.replacement);
		}

		@Override
		public String toString() {
			if (this.replacement.indexOf("\n") >= 0) {
				return unquoteRegex(this.pattern.toString()) + "\n->\n" + this.replacement + "\n";
			}
			return unquoteRegex(this.pattern.toString()) + "->" + this.replacement;
		}
	}

	public static class Builder {
		private final List<Rule> rules = L.l();
		private boolean multiline;

		public Builder(final boolean multiline) {
			this.multiline = multiline;
		}

		public Replacer done() {
			return new Replacer(this.rules);
		}

		public Builder add(final Rule rule) {
			this.rules.add(rule);
			return this;
		}

		public Builder replaceAllRegex(final String regex, final String replacement) {
			return replaceAllRegex(Pattern.compile(regex, this.multiline ? Pattern.MULTILINE : 0), replacement);
		}

		public Builder replaceAll(final String rawText, final String replacement) {
			return replaceAllRegex(Pattern.compile(Pattern.quote(rawText), this.multiline ? Pattern.MULTILINE : 0), replacement);
		}

		public Builder replaceAllRaw(final String rawText, final String rawReplacement) {
			return replaceAllRegex(Pattern.compile(Pattern.quote(rawText), this.multiline ? Pattern.MULTILINE : 0),
					Matcher.quoteReplacement(rawReplacement));
		}

		public Builder replaceAllRegex(final Pattern regex, final String replacement) {
			this.rules.add(new RegExRule(regex, replacement));
			return this;
		}


		public boolean isMultiline() {
			return this.multiline;
		}

		public Builder setMultiline(final boolean matchMultiline) {
			this.multiline = matchMultiline;
			return this;
		}
	}

	public static Builder build() {
		return new Builder(false);
	}

	public static Builder multiline() {
		return new Builder(true);
	}
	private final List<Rule> rules;

	public Replacer(final List<Rule> rules) {
		this.rules = rules;
	}

	public CharSequence apply(final CharSequence source) {
		CharSequence intermediate = source;
		for (Rule r : this.rules) {
			intermediate = r.apply(intermediate);
		}
		return intermediate;
	}

	private static String unquoteRegex(final String regex) {
		return regex.replaceAll("\\\\", "\\").replaceAll("\\\\r\\?\\\\n", "\n");
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Rule r : this.rules) {
			sb.append(r);
		}
		return sb.toString();
	}
}
