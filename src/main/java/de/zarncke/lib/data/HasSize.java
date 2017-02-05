package de.zarncke.lib.data;

/**
 * Describes an Object, which has some kind of size in terms of number of elements.
 * Size doesn't mean bytes or low level size but rather abstract size like number of items or entries.
 * The intention is to make possible iteration by index over that element.
 */
public interface HasSize {
	int size();
}
