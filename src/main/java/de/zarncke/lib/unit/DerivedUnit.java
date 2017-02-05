/**
 *
 */
package de.zarncke.lib.unit;


public class DerivedUnit extends Unit {
	private final Unit otherUnit;
	private final double factor;

	public DerivedUnit(final String unit, final Unit otherUnit, final double factor) {
		super(unit);
		this.otherUnit = otherUnit;
		this.factor = factor;
	}

	@Override
	public Value value(final double amount) {
		return this.otherUnit.value(amount * this.factor);
	}

	@Override
	public Unit getBaseUnit() {
		return this.otherUnit.getBaseUnit();
	}

	@Override
	public Value rebase(final Value value) {
		Value inOther = this.otherUnit.rebase(value);
		return new Value(this, inOther.amount / this.factor);
	}

	@Override
	public boolean equals(final Object obj) {
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}
}