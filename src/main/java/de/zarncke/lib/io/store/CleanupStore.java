package de.zarncke.lib.io.store;

/**
 * A {@link Store} which deletes itself during shutdown.
 *
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
public class CleanupStore extends CreateAwareStore {
	private final boolean onlyElements;

	public CleanupStore(final Store delegate, final boolean onlyElements) {
		super(delegate);
		this.onlyElements = onlyElements;
	}

	@Override
	protected Store wrap(final Store element) {
		if (this.onlyElements) {
			return new CleanupStore(element, false);
		}
		return super.wrap(element);
	}

	/**
	 * Deletes itself (only if onlyElements=false).
	 */
	@Override
	protected void registerCreation() {
		if (!this.onlyElements) {
			StoreUtil.deleteOnExit(this);
		}
	}
}
