package de.zarncke.lib.io.store;

import java.io.IOException;
import java.io.OutputStream;

import de.zarncke.lib.region.Region;

/**
 * A {@link Store} which is aware of creation actions.
 * Derived classes should consider implementing {@link DelegateStore#wrap(Store)}.
 * Note: No {@link #registerCreation()} will not be called if the element already exists during construction of this
 * wrapper object.
 *
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
public abstract class CreateAwareStore extends DelegateStore {

	public CreateAwareStore(final Store delegate) {
		super(delegate);
	}

	@Override
	public OutputStream getOutputStream(final boolean append) throws IOException {
		registerCreation();
		return super.getOutputStream(append);
	}

	@Override
	public Region asRegion() throws IOException {
		registerCreation();
		return super.asRegion();
	}

	/**
	 * Called on creation of content.
	 */
	protected abstract void registerCreation();
}
