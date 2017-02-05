/**
 *
 */
package de.zarncke.lib.unit;

import de.zarncke.lib.err.Warden;


public class Unit implements Comparable<Unit> {
	private final String unit;

	public Unit(final String unit) {
		this.unit = unit;
	}

	public Value value(final double amount) {
		return new Value(this, amount);
	}

	public int compareTo(final Unit o) {
		return this.unit.compareTo(o.unit);
	}

	public Unit getBaseUnit() {
		return this;
	}

	public Value rebase(final Value value) {
		if (value.getUnit().equals(this)) {
			return value;
		}
		throw Warden.spot(new IllegalArgumentException("cannot rebase " + value + " to " + this));
	}

	@Override
	public int hashCode() {
		return this.unit.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Unit other = (Unit) obj;
		if (this.unit == null) {
			if (other.unit != null) {
				return false;
			}
		} else if (!this.unit.equals(other.unit)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return this.unit;
	}

	public String getName() {
		return this.unit;
	}
}