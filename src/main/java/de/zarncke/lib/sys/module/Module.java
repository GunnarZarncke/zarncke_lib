package de.zarncke.lib.sys.module;

import java.util.List;

import javax.annotation.Nonnull;

import de.zarncke.lib.i18n.Translations;
import de.zarncke.lib.sys.Component;
import de.zarncke.lib.sys.Health;

/**
 * Represents one Module of an Installation.
 * Modules form a logical hierarchy.
 * This hierarchy is meant to be interpreted by {@link de.zarncke.lib.sys.Headquarters}.
 * The Modules themselves are only responsible for doing their own work.
 * Thus a module shouldn't aggregate any health status from its children, nor should it propagate restart/kill
 * calls.
 */
public interface Module extends Component {
	@Override
	@Nonnull
	Translations getName();

	/**
	 * List of Modules nested logically within this module.
	 *
	 * @return List of Module
	 */
	@Nonnull
	List<Module> getSubModules();

	/**
	 * Current 'run level' of this Module.
	 *
	 * @return State
	 */
	@Nonnull
	@Override
	State getState();

	/**
	 * Urges this module to terminate.
	 */
	void shutdown();

	/**
	 * Requires this module to terminate as fast as possible.
	 */
	void kill();

	/**
	 * Hints to this module to try recovery (of a presumed previous crash).
	 */
	void tryRecovery();

	/**
	 * Advises a (re)start of this module.
	 */
	void startOrRestart();

	/**
	 * Asks for a poll of the current health of this module.
	 * Sub modules may be taken into account, but Headquarters will aggregate health too.
	 * Should the check take some time, the module should not poll if the last call is recent and instead return the
	 * previous value.
	 * Reasons for the health value should be evident from {@link #getMetaInformation() meta information} or
	 * errors (recorded by Headquarters).
	 * 
	 * @return Health.
	 */
	@Override
	@Nonnull
	Health getHealth();

	/**
	 * Returns an estimate of the load of this module.
	 * This can take memory and/or processor load into account.
	 * The value should be scaled such that 1.0 is the maximum load the developer of the module deems barely
	 * acceptable for
	 * the system as a whole.
	 * For example if you have a module which caches data and you expect the cache to take only a small fraction of
	 * the
	 * total memory then you should
	 * return a value of 1.0 if the fraction of the memory used by the cache reaches your intended maximum e.g. 5%
	 * of total
	 * memory.
	 * Avoid returning values significantly higher than 10.0 as that might disrupt readability and meaning.
	 * If you have more than one value either take the larger (critical values) or average (otherwise).
	 * If you have no measurable value return 0.0. If measuring fails return 1.0.
	 *
	 * @return double >=0.0 and typically not much larger than 1.0
	 */
	@Override
	double getLoad();
}