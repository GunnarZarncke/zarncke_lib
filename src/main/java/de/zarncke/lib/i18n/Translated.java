package de.zarncke.lib.i18n;

import de.zarncke.lib.id.Id;

/**
 * Something which will get translated.
 *
 * @author Gunnar Zarncke
 */
public class Translated extends Translations implements Translatable {
	private static final long serialVersionUID = 1L;
	private final String key;

	public Translated(final String key, final String defaultTranslation) {
		this.key = key;
		put(null, defaultTranslation);
	}

	public Translated(final String key, final Translations proposedTranslations) {
		super(proposedTranslations);
		this.key = key;
	}

	/**
	 * Custom constructor which builds the key from the key and id.
	 *
	 * @param id may be null (becomes "UNKNOWN")
	 * @param key suffixed
	 * @param proposedTranslations != null
	 */
	public Translated(final Id id, final String key, final Translations proposedTranslations) {
		this((id == null ? "UNKNOWN." : id.getType().getName() + "." + id.toHexString() + ".") + key, proposedTranslations);
	}

	public String getKey() {
		return this.key;
	}

	public Translations getProposal() {
		return this;
	}
}
