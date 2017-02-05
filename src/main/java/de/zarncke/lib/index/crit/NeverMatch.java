/**
 *
 */
package de.zarncke.lib.index.crit;

import java.util.Collection;

import de.zarncke.lib.coll.L;

/**
 * Never matches anything.
 *
 * @author Gunnar Zarncke
 * @param <T> any type
 */
public final class NeverMatch<T> implements Criteria<Object, T> {

	@SuppressWarnings("rawtypes")
	private static final NeverMatch INSTANCE = new NeverMatch();

	/**
	 * Singleton.
	 *
	 * @param <T> any type
	 * @return {@link NeverMatch}
	 */
	@SuppressWarnings("unchecked")
	public static <T> NeverMatch<T> getInstance() {
		return INSTANCE;
	}

	private NeverMatch() {
		// empty
	}

	public Class<Object> getType() {
		return Object.class;
	}

	public boolean matches(final T entry) {
		return false;
	}

	@Override
	public String toString() {
		return "NEVER";
	}

	public Collection<Object> getKeys() {
		return L.e();
	}
}