package de.zarncke.lib.io;

import javax.annotation.Nullable;

/**
 * Indicates that a service or Exception has an associated HTTP status-code indicating the status of the result.
 * 
 * @author Gunnar Zarncke
 */
public interface HasHttpStatusCode {
	/**
	 * @return HTTP status code, optional
	 */
	@Nullable
	Integer getHttpStatusCode();
}
