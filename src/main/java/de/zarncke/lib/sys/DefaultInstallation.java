/**
 *
 */
package de.zarncke.lib.sys;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.collect.MapMaker;

import de.zarncke.lib.block.Block;
import de.zarncke.lib.coll.L;
import de.zarncke.lib.i18n.Translations;
import de.zarncke.lib.io.store.FileStore;
import de.zarncke.lib.lang.CodeResponsible;
import de.zarncke.lib.lang.CodeUtil;
import de.zarncke.lib.log.Report;
import de.zarncke.lib.sys.module.Module;
import de.zarncke.lib.sys.module.PackageModule;
import de.zarncke.lib.util.Misc;

/**
 * This is the default {@link Installation} if none else is {@link de.zarncke.lib.ctx.Context#runWith configured}.
 * It keeps the health state {@link Installation.HealthStore in memory} and the {@link Installation.SeverityStore
 * severities} in {@value #DEFAULT_ISSUE_SEVERITIES_FILE}.
 * The {@link JavaLangModule} is the root module and {@link PackageModule PackageModules} are created on demand for the
 * first
 * two parts of the
 * package name (e.g. "de.zarncke" for "de.zarncke.lib.io.IoTools").
 * Note: Be advised that it logs to stdout/stderror in {@link #reportIncident(Module, Health, Report)}. Consider
 * inheriting and
 * overriding
 * this method.
 *
 * @author Gunnar Zarncke
 */
public class DefaultInstallation implements Installation {

	private static final String CLASSNAME_SEPARATOR = ".";

	/**
	 * {@value #DEFAULT_SHUTDOWN_GRACE_SECONDS} seconds.
	 * Can be overridden with {@link #getShutdownGraceSeconds()}.
	 */
	public static final int DEFAULT_SHUTDOWN_GRACE_SECONDS = 5;

	private static final String DEFAULT_ISSUE_SEVERITIES_FILE = "issue-severities.properties";

	private final Module rootModule = new JavaLangModule(true) {
		@Override
		public List<Module> getSubModules() {
			return getKnownPackageModules();
		}
	};

	private final Map<String, Module> knownModulesByName = new MapMaker().makeMap();

	private HealthStore healths = new MemoryHealth();
	private SeverityStore severities;

	public DefaultInstallation() {
		this(new File(DEFAULT_ISSUE_SEVERITIES_FILE));
	}

	public DefaultInstallation(final File severitiesFile) {
		this.severities = new PropertySeverities(new FileStore(severitiesFile));
	}

	@Override
	public Module getRootModule() {
		return this.rootModule;
	}

	@Override
	public void reportIncident(final Module module, final Health health, final Report report) {
		@SuppressWarnings("resource" /* System.out/err is OK */)
		PrintStream printStream = health.ordinal() >= Health.IMPORTANT.ordinal() ? System.err : System.out;
		if (report.numberOfOccurences() == 1 && health.ordinal() >= Health.WARNINGS.ordinal()) {
			printStream.println(health.name() + " " + module.getName().getDefault() + " "
					+ report.getCaller().getCallerKey() + " " + report.getFullReport());
		} else {
			printStream.println(health.name() + " " + module.getName().getDefault() + " "
					+ report.getCaller().getCallerKey() + " " + report.getSummary());
		}
	}

	@Override
	public void reportImmediate(final Module module, final Health severity, final Report report) {
		System.out.println(report.getEstimatedSeverity() + " immediate " + report.getCaller().getCallerKey() + " " // NOPMD
																													// log
				+ report.getSummary());
	}

	@Override
	public Translations getName() {
		return new Translations(Misc.ENVIRONMENT.get().get("user.name") + "@" + Misc.ENVIRONMENT.get().get("user.dir"));
	}

	@Override
	public Module determineModuleOf(final String className) {
		// first let modules determine applicability
		List<Module> modules = L.copy(getRootModule().getSubModules());
		modules.add(getRootModule());
		for (Module m : modules) {
			if (m instanceof CodeResponsible) {
				if (((CodeResponsible) m).isResponsibleFor(CodeUtil.named(className))) {
					return m;
				}
			}
		}
		final String moduleName = determineModuleName(className);
		Module module = this.knownModulesByName.get(moduleName);
		if (module == null) {
			// create package on the spot, initializing it
			module = new PackageModule(moduleName);
			module.startOrRestart();
			this.knownModulesByName.put(moduleName, module);
		}
		return module;
	}

	private String determineModuleName(final String className) {
		String moduleName = className;
		int p = className.indexOf(CLASSNAME_SEPARATOR);
		if (p >= 0) {
			p = className.indexOf(CLASSNAME_SEPARATOR, p + 1);
			if (p >= 0) {
				moduleName = className.substring(0, p);
			}
		}
		return moduleName;
	}

	@Override
	public HealthStore getHealthStore() {
		return this.healths;
	}

	protected void setHealthStore(final HealthStore healthStore) {
		this.healths = healthStore;
	}

	@Override
	public SeverityStore getSeverityStore() {
		return this.severities;
	}

	protected void setSeverityStore(final SeverityStore severityStore) {
		this.severities = severityStore;
	}

	@Override
	public int getShutdownGraceSeconds() {
		return DEFAULT_SHUTDOWN_GRACE_SECONDS;
	}

	protected List<Module> getKnownPackageModules() {
		return Collections.unmodifiableList(new ArrayList<Module>(new TreeMap<String, Module>(
				DefaultInstallation.this.knownModulesByName).values()));
	}

	/**
	 * Supports only attaching Modules.
	 *
	 * @param participator any, if Module it is added to the known Modules
	 * @return null
	 */
	@Override
	public Object attach(final Object participator) {
		if (participator instanceof Module) {
			addModule((Module) participator);
		}
		return null;
	}

	/**
	 * Adds the given Module to this installation.
	 *
	 * @param module != null
	 */
	void addModule(final Module module) {
		this.knownModulesByName.put(module.getName().getDefault(), module);
	}

	/**
	 * By default simply runs the code.
	 *
	 * @param toBeRunInScope != null
	 * @throws Exception block failure
	 */
	@Override
	public void runInScope(final Block<?> toBeRunInScope) throws Exception {
		toBeRunInScope.execute();
	}

	@Override
	public boolean isMonitoringAllowed() {
		return true;
	}
}