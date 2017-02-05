package de.zarncke.lib.io;

import java.io.IOException;

import de.zarncke.lib.io.store.Store;
import de.zarncke.lib.util.Misc;

/**
 * Provides a resource-save way to process the contents of a store.
 *
 * @author Gunnar Zarncke
 * @param <T> return type, may be Void
 */
public interface StoreConsumer<T> {
	StoreConsumer<String> TO_UTF_STRING = new StoreConsumer<String>() {
		@Override
		public String consume(final Store storeToProcess) throws IOException {
			return new String(IOTools.getAllBytes(storeToProcess.getInputStream()), Misc.UTF_8);
		}
	};

	/**
	 * Consumes the contents of the Store.
	 *
	 * @param storeToProcess != null
	 * @return result of type T
	 * @throws IOException on IO failure
	 */
	T consume(final Store storeToProcess) throws IOException;

}
