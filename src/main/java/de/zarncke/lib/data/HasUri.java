package de.zarncke.lib.data;

/**
 * Describes an Object, that has some URI (typically the URL for a link) associated with it.
 */
public interface HasUri
{
	/**
	 * Get the URI of this Object.
	 * 
	 * @return the URI as a String, may be null
	 */
	String getUri();
}




