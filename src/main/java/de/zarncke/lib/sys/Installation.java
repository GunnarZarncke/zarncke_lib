/**
 *
 */
package de.zarncke.lib.sys;

import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import de.zarncke.lib.block.Block;
import de.zarncke.lib.ctx.Context;
import de.zarncke.lib.i18n.Translations;
import de.zarncke.lib.log.Report;
import de.zarncke.lib.sys.module.Module;

/**
 * Represents all of the currently installed application.
 * Provides the necessary information for {@link Headquarters} to form an application overview status. <br/>
 * Implementation hints:
 * <ul>
 * <li>Implementations for one-VM installations should always return all modules.</li>
 * <li>Distributed installations should consider the following two scenarios:
 * <ul>
 * <li>Represent one system as the "master" installation which knows all distant components as modules. These distant
 * modules may or may not have their own custom Installation which represents only their own inner state.</li>
 * <li>Or all distributed components have a total view and implement the Modules such that local modules are querried
 * locally and remote modules are queried remotely.</li>
 * </ul>
 * </ul>
 *
 * @author Gunnar Zarncke
 */
public interface Installation {

	/**
	 * Keeps track of the last state of the Installation.
	 * Implementations should keep the store as simple and robust as possible.
	 * This store works in the Headquarters and if it fails {@link Headquarters} will fall back to default values.
	 *
	 * @author Gunnar Zarncke
	 */
	interface HealthStore {
		void remember(@Nonnull Module module, @Nonnull Health health);

		@Nonnull
		Map<Module, Health> getHealthByModule();
	}

	/**
	 * Keeps track of the severities of issues reported by the Installation.
	 * Implementations should keep the store as simple and robust as possible.
	 * This store works in the Headquarters and if it fails {@link Headquarters} will fall back to default values.
	 */
	interface SeverityStore {
		/**
		 * @param callerKey != null
		 * @param severity {@link Health}
		 * @param immediate true: messages associated with this key should be reported immediately without aggregation
		 * @param minimumCount required count for this caller to be reported
		 */
		void remember(@Nonnull String callerKey, @Nonnull Health severity, boolean immediate, int minimumCount);

		/**
		 * @return caller key to assigned severity
		 */
		@Nonnull
		Map<String, Health> getSeveritiesByCallerKey();

		/**
		 * @return caller keys marked important
		 */
		@Nonnull
		Set<String> getImmediateCallerKeys();

		/**
		 * @return caller keys to required count to be reported
		 */
		@Nonnull
		Map<String, Integer> getMinimumCounts();
	}

	@Nonnull
	Translations getName();

	@Nonnull
	Module getRootModule();

	@Nullable
	Module determineModuleOf(@Nonnull String className);

	/**
	 * This is the key output channel for incidents.
	 * Incidents will already have been accumulated and grouped.
	 *
	 * @param module where the incident applied (most likely) != null
	 * @param severity estimate after severity lookup
	 * @param report the original report
	 */
	void reportIncident(@Nonnull Module module, @Nonnull Health severity, @Nonnull Report report);

	/**
	 * This allows immediate reporting of special issues.
	 * The intention is to make the reports available on some temporary channel, e.g. std out.
	 * The volume of data over this channel might be limited to avoid congestion.
	 *
	 * @param module where the incident applied (most likely)
	 * @param severity estimate after severity lookup
	 * @param report the original report
	 */
	void reportImmediate(@Nonnull Module module, @Nonnull Health severity, @Nonnull Report report);

	/**
	 * Implementations may supply their own storage.
	 *
	 * @return {@link MemoryHealth} is recommended
	 */
	@Nonnull
	HealthStore getHealthStore();

	/**
	 * Implementations may supply their own storage.
	 *
	 * @return {@link PropertySeverities} is recommended
	 */
	@Nonnull
	SeverityStore getSeverityStore();

	/**
	 * Allows for a grace period between {@link Module#shutdown()} and {@link Module#kill()}.
	 *
	 * @return seconds until all Modules receive a {@link Module#kill()}.
	 */
	int getShutdownGraceSeconds();

	/**
	 * Allows classes to participate in the Installation even though they are or can not be started directly by the
	 * Installation.
	 * Example: If the Installation is run by an application server.
	 * The Installation may for example
	 * <ul>
	 * <li>use the given argument</li>
	 * <li>return helpful information</li>
	 * <li>attach values to the calling Thread</li>
	 * </ul>
	 *
	 * @param participtor the caller may pass in an argument about itself.
	 * @return helpful values or null
	 */
	Object attach(Object participtor);

	/**
	 * Allows code of the client to be run in the scope of the Installation.
	 * Example: {@link Context} may be added to the Thread.
	 *
	 * @param toBeRunInScope != null
	 * @throws Exception passed on by the Block
	 */
	void runInScope(@Nonnull Block<?> toBeRunInScope) throws Exception; // NOPMD top level enough

	/**
	 * @return true: installation may be monitored, false: monitoring not allowed
	 */
	boolean isMonitoringAllowed();
}