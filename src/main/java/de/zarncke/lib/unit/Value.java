/**
 *
 */
package de.zarncke.lib.unit;

import de.zarncke.lib.err.Warden;

/**
 * An {@link Double value} with a {@link Unit}.
 *
 * @author Gunnar Zarncke
 */
public class Value {
	// TODO rename to Measure
	final Unit unit;
	final double amount;

	public Value(final Unit unit, final double amount) {
		this.unit = unit;
		this.amount = amount;
	}

	public Unit getUnit() {
		return this.unit;
	}

	public double getAmount() {
		return this.amount;
	}

	// public Value normalize() {
	// return unit.normalize(amount);
	// }

	public Value plus(final Value value) {
		if (!this.unit.equals(value.unit)) {
			throw Warden.spot(new IllegalArgumentException("incompatible unit"));
		}
		return new Value(this.unit, this.amount + value.amount);
	}

	public Value times(final double factor) {
		return new Value(this.unit, this.amount * factor);
	}

	@Override
	public String toString() {
		if (Math.floor(this.amount) == this.amount) {
			return String.valueOf((int) this.amount) + this.unit;
		}
		return String.valueOf(this.amount) + this.unit;
	}

}