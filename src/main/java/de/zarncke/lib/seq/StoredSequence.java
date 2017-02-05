package de.zarncke.lib.seq;

import java.io.IOException;

import de.zarncke.lib.err.NotAvailableException;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.io.IOTools;
import de.zarncke.lib.io.store.Store;

/**
 * A simple integer sequence which is stored in a backing {@link Store}.
 * It is recommended to use a XADiskStore.
 *
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
public class StoredSequence implements Sequence {

	private final Store sequenceStore;

	public StoredSequence(final Store sequenceStore) {
		this.sequenceStore = sequenceStore;
	}

	@Override
	public long addAndGet(final long delta) {
		long current;
		try {
			if (!this.sequenceStore.exists()) {
				current = 0;
			} else {
				current = Long.parseLong(IOTools.getAsString(this.sequenceStore.getInputStream()).toString());
			}
		} catch (NumberFormatException e) {
			throw Warden.spot(new IllegalStateException("sequence in " + this.sequenceStore + " must be numeric! "
					+ "manual corrected needed before process can be retried!", e));
		} catch (IOException e) {
			throw Warden.spot(new NotAvailableException("cannot read " + this.sequenceStore, e));
		}
		if (delta == 0) {
			return current;
		}

		long next = current + delta;
		try {
			IOTools.dump(String.valueOf(next), this.sequenceStore.getOutputStream(false));
		} catch (IOException e) {
			throw Warden.spot(new NotAvailableException("cannot write new sequence value " + next + " to "
					+ this.sequenceStore, e));
		}
		return current;
	}

	@Override
	public long incrementAndGet() {
		return addAndGet(1);
	}
}
