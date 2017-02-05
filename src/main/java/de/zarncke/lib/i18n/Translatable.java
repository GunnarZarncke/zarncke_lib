package de.zarncke.lib.i18n;


/**
 * Something which can be translated.
 *
 * @author Gunnar Zarncke
 */
public interface Translatable {
	/**
	 * The key under which these translations can be accessed and/or changed.
	 * 
	 * @return String, null means translation not applicable (e.g. in case of one-shot external sources
	 */
	String getKey();


	/**
	 * The proposed translations for the key.
	 *
	 * @return {@link Translations}
	 */
	Translations getProposal();
}
