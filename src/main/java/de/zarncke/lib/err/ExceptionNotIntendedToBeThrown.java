package de.zarncke.lib.err;

/**
 * This exception is only intended to be used for getting the stacktrace of the location of creation.
 * It is handled specially during logging. If this Exception is encountered as a {@link Throwable#getCause() cause}, a misuse
 * will be reported.
 */
public final class ExceptionNotIntendedToBeThrown extends Exception {
	private static final long serialVersionUID = 1L;
}