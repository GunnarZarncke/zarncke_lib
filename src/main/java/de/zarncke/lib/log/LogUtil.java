package de.zarncke.lib.log;


/**
 * Helper for dealing with Log and other logging systems.
 *
 * @author Gunnar Zarncke
 */
public final class LogUtil {

	private LogUtil() {
		// helper
	}

	/**
	 * Redirects the java.util.logging to the {@link Log#LOG current Log}.
	 * MUST be called before the first reference to any Logger to take effect.
	 */
	public static void redirectJavaUtilLoggerToLog() {
		// the class name must be given literally - otherwise the class reference will cause the LogManager
		// to be initialized *before* the property is set!
		System.getProperties().put("java.util.logging.manager", "de.zarncke.lib.log.JavaUtilLoggingLogManagerBridge");
	}
}
