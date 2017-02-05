/**
 *
 */
package de.zarncke.lib.sys;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import de.zarncke.lib.block.Block;
import de.zarncke.lib.coll.L;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.i18n.Translations;
import de.zarncke.lib.log.Report;
import de.zarncke.lib.sys.module.Module;

/**
 * This is an {@link Installation} for testing.
 * It doesn't track health or report anything.
 * If a Report with severity >= WARNING is reported it calls {@link Assert#fail}.
 *
 * @author Gunnar Zarncke
 */
public class TestInstallation implements Installation {

	private final Module rootModule = new Module() {

		private State state = State.UP;

		@Override
		public State getState() {
			return this.state;
		}

		@Override
		public Health getHealth() {
			return Health.VIRGIN;
		}

		@Override
		public Translations getName() {
			return new Translations("TestInstallation.Module");
		}

		@Override
		public List<Module> getSubModules() {
			return L.e();
		}

		@Override
		public void kill() {
			//
		}

		@Override
		public void shutdown() {
			this.state = State.DOWN;
		}

		@Override
		public void startOrRestart() {
			//
		}

		@Override
		public void tryRecovery() {
			//
		}

		@Override
		public double getLoad() {
			return 0.0;
		}

		@Override
		public String getMetaInformation() {
			return "";
		}
	};

	@Override
	public Module getRootModule() {
		return this.rootModule;
	}

	@Override
	public void reportIncident(final Module module, final Health health, final Report report) {
		if (health.ordinal() > Health.WARNINGS.ordinal()) {
			Assert.fail( report.getFullReport().toString());
		}
	}

	@Override
	public void reportImmediate(final Module module, final Health severity, final Report report) {
		// ignore
	}

	@Override
	public Translations getName() {
		return new Translations("TestInstallation");
	}

	@Override
	public Module determineModuleOf(final String className) {
		return this.rootModule;
	}

	@Override
	public HealthStore getHealthStore() {
		return new HealthStore() {

			@Override
			public Map<Module, Health> getHealthByModule() {
				return Collections.singletonMap(TestInstallation.this.rootModule,
						TestInstallation.this.rootModule.getHealth());
			}

			@Override
			public void remember(final Module module, final Health health) {
				// nop
			}
		};
	}

	@Override
	public SeverityStore getSeverityStore() {
		return new SeverityStore() {

			@Override
			public void remember(final String callerKey, final Health severity, final boolean immediate,
					final int minCount) {
				// nop
			}

			@Override
			public Map<String, Health> getSeveritiesByCallerKey() {
				return new HashMap<String, Health>();
			}

			@Override
			public Set<String> getImmediateCallerKeys() {
				return new HashSet<String>();
			}

			@Override
			public Map<String, Integer> getMinimumCounts() {
				return new HashMap<String, Integer>();
			}
		};
	}

	@Override
	public int getShutdownGraceSeconds() {
		return 0;
	}

	/**
	 * Does nothing.
	 *
	 * @param participtor any
	 * @return null
	 */
	@Override
	public Object attach(final Object participtor) {
		return null;
	}

	/**
	 * @see Installation#runInScope(Block)
	 */
	@Override
	public void runInScope(final Block<?> toBeRunInScope) {
		try {
			toBeRunInScope.execute();
		} catch (Exception e) {
			throw Warden.spot(new RuntimeException("wrapped", e)); // NOPMD see signature
		}
	}

	@Override
	public String toString() {
		return getName().getDefault();
	}

	@Override
	public boolean isMonitoringAllowed() {
		return false;
	}
}