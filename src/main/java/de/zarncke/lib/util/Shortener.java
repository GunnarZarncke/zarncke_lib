package de.zarncke.lib.util;

import java.io.Serializable;

public interface Shortener extends Serializable {
	/**
	 * Leaves text unchanged.
	 */
	Shortener NONE = new Shortener() {
		public CharSequence shorten(final CharSequence longText) {
			return longText;
		}
	};
	/**
	 * Shortens to 80 characters by truncating. See {@link Chars#summarize(CharSequence, int)}.
	 */
	Shortener SIMPLE = new Shortener() {
		public CharSequence shorten(final CharSequence longText) {
			return Chars.summarize(longText, 80);
		}
	};

	/**
	 * @param longText != null
	 * @return text as most as long as the given text
	 */
	CharSequence shorten(CharSequence longText);
}
