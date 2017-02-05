package de.zarncke.lib.money;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

import de.zarncke.lib.err.Warden;

/**
 * Immutable Money object.
 *
 * @author Gunnar Zarncke
 */
public class MAmount implements Money {

	private final Currency currency;
	private final BigDecimal amount;

	public static MAmount of(final long major, final int minor, final Currency currency) {
		BigDecimal amount = new BigDecimal(major).add(new BigDecimal(minor).scaleByPowerOfTen(-currency
				.getDefaultFractionDigits()));
		return of(amount, currency);
	}

	public static MAmount of(final Money money) {
		return money instanceof MAmount ? (MAmount) money : of(money.getAmount(), money.getCurrency());
	}

	public static MAmount of(final BigDecimal amount, final Currency currency) {
		if (amount == null) {
			throw Warden.spot(new IllegalArgumentException("amount must be given"));
		}
		return new MAmount(amount, currency);
	}

	public static MAmount of(final Number amount, final Currency currency) {
		if (amount instanceof BigDecimal) {
			return new MAmount((BigDecimal) amount, currency);
		}
		if (amount instanceof BigInteger) {
			return new MAmount(new BigDecimal((BigInteger) amount), currency);
		}
		if (amount instanceof Double || amount instanceof Float) {
			return new MAmount(new BigDecimal(amount.doubleValue()), currency);
		}
		return new MAmount(new BigDecimal(amount.longValue()), currency);
	}

	public static MAmount zero(final Currency currency) {
		return new MAmount(BigDecimal.ZERO, currency);
	}

	protected MAmount(final BigDecimal amount, final Currency currency) {
		this.amount = amount;
		this.currency = currency;
	}

	@Override
	public Currency getCurrency() {
		return this.currency;
	}

	@Override
	public BigDecimal getAmount() {
		return this.amount;
	}

	public String print(final Locale locale) {
		NumberFormat nf = NumberFormat.getCurrencyInstance(locale);
		nf.setCurrency(this.currency);
		return nf.format(this.amount);
	}

	@Override
	public String toString() {
		return this.amount + " " + this.currency;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.amount == null ? 0 : this.amount.hashCode());
		result = prime * result + (this.currency == null ? 0 : this.currency.hashCode());
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
		MAmount other = (MAmount) obj;
		if (this.amount.compareTo(other.amount) != 0) {
			return false;
		}
		if (this.currency == null) {
			if (other.currency != null) {
				return false;
			}
		} else if (!this.currency.equals(other.currency)) {
			return false;
		}
		return true;
	}

	@Override
	public int compareTo(final Money o) {
		if (!this.currency.equals(o.getCurrency())) {
			throw Warden.spot(new IllegalArgumentException("incomparable currencies " + this + "," + o));
		}
		return this.amount.compareTo(o.getAmount());
	}
}
