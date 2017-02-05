/**
 *
 */
package de.zarncke.lib.sys.module;

import de.zarncke.lib.i18n.Translations;
import de.zarncke.lib.lang.CodeResponsible;
import de.zarncke.lib.lang.Piece;
import de.zarncke.lib.sys.Health;

/**
 * A simple {@link Module} which is intended to capture issues of a package.
 *
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
public class PackageModule extends AbstractModule implements CodeResponsible {

	private final String moduleName;
	private final String[] packages;

	public PackageModule(final String moduleName, final String... packages) {
		this.moduleName = moduleName;
		this.packages = packages;
	}

	@Override
	public Health getHealth() {
		return Health.VIRGIN;
	}

	@Override
	public Translations getName() {
		return new Translations(this.moduleName);
	}

	@Override
	public boolean isResponsibleFor(final Piece code) {
		for (String pack : this.packages) {
			if (code.getName().startsWith(pack)) {
				return true;
			}
		}
		return false;
	}

}