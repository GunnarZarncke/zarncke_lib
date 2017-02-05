package de.zarncke.lib.seq;

import org.junit.Test;

import de.zarncke.lib.err.GuardedTest4;
import de.zarncke.lib.io.store.MemStore;
import de.zarncke.lib.io.store.Store;
import de.zarncke.lib.region.RegionUtil;

public class SequenceTest extends GuardedTest4 {
	@Test
	public void testStoredSequence() {
		Store store = new MemStore(RegionUtil.asRegionUtf8("0"));
		Sequence s = new StoredSequence(store);

		testSequence(s);
		assertEquals(26, s.incrementAndGet());

		// test parallel access cases
		Sequence sPar = new StoredSequence(store);
		assertEquals(27, sPar.incrementAndGet());
	}

	@Test
	public void testBatchSequence() {
		Store store = new MemStore(RegionUtil.asRegionUtf8("0"));
		Sequence s0 = new StoredSequence(store);
		Sequence s = new BatchedSequence(s0, 100) {
			@Override
			protected void discard(final long firstDiscarded, final long numberOfDiscarded) {
				assertEquals(27, firstDiscarded);
				assertEquals(73, numberOfDiscarded);
			}
		};

		testSequence(s);

		// test some gap and block cases
		assertEquals(26, s.incrementAndGet());
		assertEquals(100, s.addAndGet(100));
		assertEquals(200, s.addAndGet(100));
		assertEquals(300, s.incrementAndGet());
		assertEquals(301, s.incrementAndGet());

		// test parallel access cases
		Sequence sPar = new BatchedSequence(s0, 100);
		assertEquals(400, sPar.incrementAndGet());
		assertEquals(401, sPar.incrementAndGet());

		assertEquals(302, s.incrementAndGet());
		assertEquals(303, s.addAndGet(97));
		assertEquals(500, s.incrementAndGet());
	}

	protected void testSequence(final Sequence sequence) {
		assertEquals(0, sequence.incrementAndGet());
		assertEquals(1, sequence.incrementAndGet());
		assertEquals(2, sequence.incrementAndGet());
		assertEquals(3, sequence.addAndGet(10));
		assertEquals(13, sequence.addAndGet(10));
		assertEquals(23, sequence.incrementAndGet());
		assertEquals(24, sequence.addAndGet(2));
	}
}
