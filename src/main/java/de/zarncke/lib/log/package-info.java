/**
 * Provides a very lightweight logging API.
 * Less efficient than Log4J, but simpler and better supports nesting and testing.
 * Can log from/to SLF4J.
 * Can log from/to Commons Logging.
 * Works by accessing a {@link de.zarncke.lib.log.Log} in a {@link de.zarncke.lib.ctx.Context}.
 */
package de.zarncke.lib.log;


