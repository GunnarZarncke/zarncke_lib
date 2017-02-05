package de.zarncke.lib.money;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Currency;

import de.zarncke.lib.err.Warden;
import de.zarncke.lib.util.Misc;

/**
 * Mutable Money object.
 *
 * @author Gunnar Zarncke
 */
public class Cash implements Money {

	public static MAmount plus(final Money a, final Money b) {
		if (!Misc.equals(a.getCurrency(), b.getCurrency())) {
			throw Warden.spot(new IllegalArgumentException("cannot add Money of different currencies " + a + "+" + b));
		}
		return new MAmount(a.getAmount().add(b.getAmount()), a.getCurrency());
	}

	public static MAmount times(final Money a, final int factor) {
		if (factor == 0) {
			return MAmount.zero(a.getCurrency());
		}
		if (factor == 1) {
			return MAmount.of(a);
		}
		if (factor == -1) {
			return new MAmount(a.getAmount().negate(), a.getCurrency());
		}
		return new MAmount(a.getAmount().multiply(BigDecimal.valueOf(factor)), a.getCurrency());
	}

	public static MAmount negate(final Money a) {
		return new MAmount(a.getAmount().negate(), a.getCurrency());
	}

	public static Cash with(final MAmount... amounts) {
		if(amounts.length==0) {
			throw Warden.spot(new IllegalArgumentException("at least one amount must be given"));
		}
		Cash c = new Cash(amounts[0].getCurrency());
		c.add(amounts);
		return c;
	}

	public static Cash with(final Collection<? extends Money> amounts) {
		if (amounts.size() == 0) {
			throw Warden.spot(new IllegalArgumentException("at least one amount must be given"));
		}
		Cash c = new Cash(amounts.iterator().next().getCurrency());
		c.add(amounts);
		return c;
	}

	private Money money;

	public Cash(final Currency currency) {
		this.money = MAmount.zero(currency);
	}

	public MAmount toMAmount() {
		return MAmount.of(this.money);
	}

	public void add(final Money... monies) {
		for (Money monie : monies) {
			this.money = plus(this.money, monie);
		}
	}

	public void add(final Collection<? extends Money> monies) {
		for (Money monie : monies) {
			this.money = plus(this.money, monie);
		}
	}

	public void subtract(final Money c) {
		this.money = plus(this.money, times(c, -1));
	}

	@Override
	public Currency getCurrency() {
		return this.money.getCurrency();
	}

	@Override
	public BigDecimal getAmount() {
		return this.money.getAmount();
	}

	@Override
	public String toString() {
		return this.money.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.money == null ? 0 : this.money.hashCode());
		return result;
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
		Cash other = (Cash) obj;
		if (this.money == null) {
			if (other.money != null) {
				return false;
			}
		} else if (!this.money.equals(other.money)) {
			return false;
		}
		return true;
	}

	@Override
	public int compareTo(final Money o) {
		return this.money.compareTo(o);
	}

}
