package de.zarncke.lib.data;

import java.io.IOException;

import de.zarncke.lib.region.Region;

/**
 * <p>
 * This interface allows access to some data of the Object.
 * </p>
 */
public interface HasData
{
	/**
	 * get the data of the Object
	 * 
	 * @return the region, may be null or the empty array.
	 * @throws IOException if loading data fails
	 */
    public Region asRegion() throws IOException;

}
