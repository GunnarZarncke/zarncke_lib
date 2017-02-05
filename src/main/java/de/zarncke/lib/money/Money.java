package de.zarncke.lib.money;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * Declares that an object has monetary amount information.
 *
 * @author Gunnar Zarncke
 */
public interface Money extends Comparable<Money> {

	Currency EURO = Currency.getInstance("EUR");

	Currency getCurrency();

	BigDecimal getAmount();
}
