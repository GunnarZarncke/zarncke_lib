package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

/**
 * Implementation {@link LoggerFactoryBinder} for our own Logging
 *
 * @author Gunnar Zarncke
 */
public class StaticLoggerBinder implements LoggerFactoryBinder {

	/**
	 * SLF4 Version compiled against. Must match value given in POM. Value: {@value #REQUESTED_API_VERSION}.
	 */
	public static final String REQUESTED_API_VERSION = "1.7.2";
	/**
	 * The unique instance of this class.
	 */
	public static final StaticLoggerBinder SINGLETON = new StaticLoggerBinder();
	private static final String loggerFactoryClassStr = "de.zarncke.lib.log.Slf4jLoggerFactory";

	private final ILoggerFactory loggerFactory;

	private StaticLoggerBinder() {
		this.loggerFactory = new de.zarncke.lib.log.Slf4jLoggerFactory();
	}

	public static StaticLoggerBinder getSingleton() {
		return SINGLETON;
	}

	public ILoggerFactory getLoggerFactory() {
		return this.loggerFactory;
	}

	public String getLoggerFactoryClassStr() {
		return loggerFactoryClassStr;
	}
}
