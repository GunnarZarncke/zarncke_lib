/**
 *
 */
package de.zarncke.lib.sys.mbean;

import javax.management.MBeanRegistrationException;

import de.zarncke.lib.err.Warden;
import de.zarncke.lib.sys.Headquarters;
import de.zarncke.lib.sys.Health;

/**
 * Exposes Headquarters state for monitoring.
 * 
 * @author Gunnar Zarncke
 */
public class HeadquartersAccess implements HeadquartersAccessMBean {
	@Override
	public String getName() {
		return Headquarters.HEADQUARTERS.get().getInstallation().getName().getDefault();
	}

	@Override
	public String getHealth() {
		return Headquarters.HEADQUARTERS.get().getHealth().name();
	}

	@Override
	public String getState() {
		return Headquarters.HEADQUARTERS.get().getState().name();
	}

	@Override
	public double getLoad() {
		return Headquarters.HEADQUARTERS.get().getLoad();
	}

	@Override
	public void minimumFor(final String callerKey, final int minimum) {
		Headquarters.HEADQUARTERS.get().assignMinimumCount(callerKey, minimum);
	}

	@Override
	public void importantFor(final String callerKey) {
		Headquarters.HEADQUARTERS.get().assignSeverity(callerKey, Health.IMPORTANT);
	}

	@Override
	public void errorsFor(final String callerKey) {
		Headquarters.HEADQUARTERS.get().assignSeverity(callerKey, Health.ERRORS);
	}

	@Override
	public void failuresFor(final String callerKey) {
		Headquarters.HEADQUARTERS.get().assignSeverity(callerKey, Health.FAILURE);
	}

	@Override
	public void infoFor(final String callerKey) {
		Headquarters.HEADQUARTERS.get().assignSeverity(callerKey, Health.INFO);
	}

	@Override
	public void discardableFor(final String callerKey) {
		Headquarters.HEADQUARTERS.get().assignSeverity(callerKey, Health.DISCARDABLE);
	}

	@Override
	public void warningsFor(final String callerKey) {
		Headquarters.HEADQUARTERS.get().assignSeverity(callerKey, Health.WARNINGS);
	}

	@Override
	public void okFor(final String callerKey) {
		Headquarters.HEADQUARTERS.get().assignSeverity(callerKey, Health.OK);
	}

	@Override
	public void refreshModules() {
		try {
			JmxHeadquarters.startOrRestartHeadquartersMBean();
		} catch (MBeanRegistrationException e) {
			Warden.report(e);
		}
	}
}