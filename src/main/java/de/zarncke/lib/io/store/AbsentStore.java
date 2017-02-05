package de.zarncke.lib.io.store;

/**
 * A synthetic Store which stands for absent elements. Its {@link #exists()} method always return false.
 * 
 * @author Gunnar Zarncke
 */
public final class AbsentStore extends AbstractStore {
	private final String name;

	AbsentStore(final String name) {
		this.name = name;
	}

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public String getName() {
		return this.name;
	}
}