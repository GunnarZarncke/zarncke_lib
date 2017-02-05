package de.zarncke.lib.unit;

import java.util.HashMap;
import java.util.Map;

public class UnitSystem {
	private final Map<String, Unit> units = new HashMap<String, Unit>();

	/**
	 * Resolves a Unit. Creates an ad-hoc unit if it is unknown.
	 * 
	 * @param name != null
	 * @return Unit != null
	 */
	public Unit byName(final String name) {
		Unit u = this.units.get(name);
		if (u == null) {
			u = new Unit(name);
			add(u);
		}
		return u;
	}

	public void add(final Unit unit) {
		this.units.put(unit.getName(), unit);
	}

	public Value value(final String unit, final double amount) {
		return byName(unit).value(amount);
	}
}
