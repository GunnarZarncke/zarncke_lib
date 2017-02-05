package de.zarncke.lib.sys.module;

import de.zarncke.lib.i18n.Translations;
import de.zarncke.lib.sys.Health;

/**
 * Checks that the JVM and version are as expected.
 *
 * @author Gunnar Zarncke
 */
public class JavaVersionModule extends AbstractModule {

	/**
	 * Default: Currently prefers 1.6 latest.
	 */
	public JavaVersionModule() {

	}

	@Override
	public Translations getName() {
		return new Translations("JVM");
	}

	@Override
	public String getMetaInformation() {
		String javaRuntime = System.getProperty("java.runtime.name");
		String javaVersion = System.getProperty("java.runtime.version");
		return javaRuntime + " " + javaVersion;
	}

	@Override
	protected Health getHealthProtected() {
		String javaRuntime = System.getProperty("java.runtime.name");
		String javaVersion = System.getProperty("java.runtime.version");
		String msg = "";
		Health jvm;
		if (javaRuntime.equals("OpenJDK Runtime Environment")) {
			jvm = Health.CLEAN;
		} else if (javaRuntime.startsWith("OpenJDK")) {
			msg = msg + "unknown kind of OpenJDK\n";
			jvm = Health.WARNINGS;
		} else if (javaRuntime.startsWith("Java(TM) SE")) {
			msg = msg + "Sub JDK is not actively developed against\n";
			jvm = Health.OK;
		} else {
			msg = msg + "unknown kind of JDK\n";
			jvm = Health.ERRORS;
		}

		Health version;
		if (javaVersion.equals("1.6.0_20-b20") || javaVersion.startsWith("1.6.0_24")) {
			version = Health.CLEAN;
		} else if (javaVersion.startsWith("1.6.0_18")) {
			msg = msg + "1.6.0_18 is acceptable\n";
			version = Health.OK;
		} else if (javaVersion.startsWith("1.6.0_3")) {
			msg = msg + "old version\n";
			version = Health.MINOR;
		} else if (javaVersion.startsWith("1.7")) {
			msg = msg + "1.7 is too new to be considered stable\n";
			version = Health.WARNINGS;
		} else if (javaVersion.startsWith("1.6")) {
			msg = msg + "unknown kind of 1.6\n";
			version = Health.WARNINGS;
		} else {
			msg = msg + "unknown Java version\n";
			version = Health.ERRORS;
		}
		setLastMessage(msg.isEmpty() ? null : msg);
		return version.ordinal() > jvm.ordinal() ? version : jvm;
	}

}
