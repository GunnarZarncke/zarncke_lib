package de.zarncke.lib.util;

/**
 * class with miscellaneous useful constants.
 */
public final class Constants
{
	private Constants() { // prevent instances
	}
	public static final int KB = 1024;
	public static final int MB = KB * KB;
	public static final long GB = MB * KB;
	public static final long TB = GB * KB;

	public static final String[] NO_STRINGS = new String[0];
	public static final Object[] NO_OBJECTS = new Object[0];
	public static final Class<?>[] NO_CLASSES = new Class[0];

}

