/**
 *
 */
package de.zarncke.lib.index.crit;

import java.util.Collection;
import java.util.Locale;

import de.zarncke.lib.util.HasLocale;

/**
 * Matches the Locale of a {@link HasLocale}.
 *
 * @author Gunnar Zarncke
 */
public final class LocaleMatch<T extends HasLocale> implements Criteria<Locale, T> {
	private final Collection<Locale> languages;

	public LocaleMatch(final Collection<Locale> languages) {
		this.languages = languages;
	}

	public boolean matches(final T broadcast) {
		return this.languages.contains(broadcast.getLocale());
	}

	public Class<Locale> getType() {
		return Locale.class;
	}

	public Collection<Locale> getKeys() {
		return this.languages;
	}

	@Override
	public int hashCode() {
		return this.languages.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		LocaleMatch other = (LocaleMatch) obj;
		if (this.languages == null) {
			if (other.languages != null) {
				return false;
			}
		} else if (!this.languages.equals(other.languages)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "in " + this.languages;
	}
}