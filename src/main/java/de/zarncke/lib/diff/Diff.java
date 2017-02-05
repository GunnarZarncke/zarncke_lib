package de.zarncke.lib.diff;

import java.util.Collection;
import java.util.List;

import com.google.common.base.Functions;
import com.google.common.collect.Lists;

import de.zarncke.lib.coll.L;

/**
 * Captures a number of {@link Delta differences} between two objects.
 *
 * @author Gunnar Zarncke
 */
public class Diff {
	private final List<Delta> deltas = L.l();
	private boolean onlyAdditions = true;
	private boolean onlyRemovals = true;
	private double aSize = 0.0;
	private double bSize = 0.0;

	public Collection<String> asStrings() {
		return Lists.transform(this.deltas, Functions.toStringFunction());
	}

	public boolean isOnlyAdditions() {
		return this.onlyAdditions;
	}

	public boolean isOnlyRemovals() {
		return this.onlyRemovals;
	}

	public boolean isIdentity() {
		return this.deltas.isEmpty();
	}

	public int size() {
		return this.deltas.size();
	}

	@Override
	public String toString() {
		return isIdentity() ? "identical" : this.deltas.toString();
	}

	public void addSizeA(final double aSizePart) {
		this.aSize += aSizePart;
	}

	public void addSizeB(final double bSizePart) {
		this.bSize += bSizePart;
	}

	public void add(final Delta delta) {
		this.deltas.add(delta);
		if (delta.isAddition()) {
			this.onlyRemovals = false;
		}
		if (delta.isRemoval()) {
			this.onlyAdditions = false;
		}
	}

	/**
	 * @return a fresh copy of the current deltas
	 */
	public List<Delta> getDeltas() {
		return L.copy(this.deltas);
	}

	/**
	 * Inputs without differences return 1.0 (even if empty).
	 * Sameness decreases linearly with differences.
	 *
	 * @return 0.0<=sameness<=1.0; NaN indicates inconsistent values
	 */
	public double getSameness() {
		double total = Math.max(this.aSize, this.bSize);
		double differences = 0.0;
		double additions = 0.0;
		double removals = 0.0;
		for (Delta d : this.deltas) {
			double delta = d.getDeltaSize();
			if (d.isAddition()) {
				if (d.isRemoval()) {
					differences += delta;
				} else {
					additions += delta;
				}

			} else if (d.isRemoval()) {
				removals += delta;
			}
		}
		differences += Math.max(additions, removals);
		if (differences > total) {
			return Double.NaN;
		}
		if (total == 0.0) {
			return 1.0;
		}
		return (total - differences) / total;
	}
}