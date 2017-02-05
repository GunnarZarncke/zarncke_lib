package de.zarncke.lib.util;

import java.util.Collection;
import java.util.Locale;

/**
 * Indicates that an Object has key words for which it can be searched.
 *
 * @author Gunnar Zarncke
 */
public interface HasKeyWords {
	/**
	 * @param locale to query
	 * @return all key words in the given locale
	 */
	Collection<String> getSearchWords(Locale locale);
}
