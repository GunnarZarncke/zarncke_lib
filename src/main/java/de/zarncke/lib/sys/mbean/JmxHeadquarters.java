package de.zarncke.lib.sys.mbean;

import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.net.URLEncoder;
import java.util.Locale;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import de.zarncke.lib.err.CantHappenException;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.log.Log;
import de.zarncke.lib.sys.Headquarters;
import de.zarncke.lib.sys.module.Module;

/**
 * Provides JMX access to {@link Headquarters}.
 *
 * @author Gunnar Zarncke
 */
public class JmxHeadquarters {

	/**
	 * (Re)register Headquarters and its Modules with the MBean server.
	 * May be called any number of times to update the registry e.g. when new modules are added or another Headquarters is
	 * established..
	 *
	 * @throws MBeanRegistrationException an failure to (re)register
	 */
	public static void startOrRestartHeadquartersMBean() throws MBeanRegistrationException {

		final DateTimeFormatter formatter = DateTimeFormat.longDateTime().withZone(DateTimeZone.UTC).withLocale(Locale.GERMANY);

		HeadquartersAccessMBean hq = new HeadquartersAccess();

		try {
			String name = "de.zarncke.lib.sys.Headquarters:name=\"" + URLEncoder.encode(hq.getName(), "ASCII") + "\"";
			registerMbean(hq, name);
		} catch (UnsupportedEncodingException e) {
			throw Warden.spot(new CantHappenException("ASCII should exist!", e));
		}

		for (Module mm : Headquarters.HEADQUARTERS.get().getAllModules()) {
			final Module m = mm;
			ModuleAccessMBean mb = new ModuleAccess(formatter, m);
			try {
				String name = "de.zarncke.lib.sys.Headquarters:module=\""
						+ URLEncoder.encode(m.getName().getDefault(), "ASCII") + "\"";
				registerMbean(mb, name);
			} catch (UnsupportedEncodingException e) {
				throw Warden.spot(new CantHappenException("ASCII should exist!", e));
			}
		}
	}

	private static void registerMbean(final Object mbean, final String beanName) throws MBeanRegistrationException {
		MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
		try {
			ObjectName hqName = new ObjectName(beanName);
			if (!platformMBeanServer.isRegistered(hqName)) {
				platformMBeanServer.registerMBean(mbean, hqName);
			}
		} catch (MalformedObjectNameException e) {
			throw Warden.spot(new IllegalArgumentException("unexpected: the name should be OK " + beanName, e));
		} catch (InstanceAlreadyExistsException e) {
			Log.LOG.get().report("It seems as if somebody already started the Headquarters MBean.");
			Warden.disregard(e);
		} catch (NotCompliantMBeanException e) {
			throw Warden.spot(new IllegalArgumentException("unexpected: we know we are MBeans", e));
		}
	}
}
