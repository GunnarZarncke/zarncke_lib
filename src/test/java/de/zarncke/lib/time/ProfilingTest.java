package de.zarncke.lib.time;

import java.io.IOException;

import de.zarncke.lib.err.GuardedTest;
import de.zarncke.lib.io.store.MemStore;
import de.zarncke.lib.io.store.Store;

public class ProfilingTest extends GuardedTest {

	private static final String ITEM_END = "end";
	private static final String ITEM_STEP2 = "step2";
	private static final String ITEM_STEP1 = "step1";
	private static final String ITEM_BEGIN = "begin";
	private static final int START = 10;
	private static final int STEP1 = 2;
	private static final int STEP2 = 4;
	private static final int STEP1B = 4;
	private static final int STEP2B = 6;
	private static final int END = 10;
	private static final int END2 = 14;
	private static final int DELTA = 7;

	public void testRecord() throws IOException {
		Store store = new MemStore();
		Profiling prof = new Profiling(store);
		prof.time("r1", ITEM_BEGIN, START);
		prof.time(ITEM_STEP1, START + STEP1);
		prof.time(ITEM_STEP2, START + STEP2);
		prof.time(ITEM_END, START + END);

		prof.time("r2", ITEM_BEGIN, START + DELTA);
		prof.time(ITEM_STEP1, START + STEP1B + DELTA);
		prof.time(ITEM_STEP2, START + STEP2B + DELTA);
		prof.time(ITEM_END, START + END2 + DELTA);

		prof.flush();

		Profiling average = Profiling.evaluate(store);
		assertEquals(0, average.getTiming(ITEM_BEGIN).longValue());
		assertEquals((STEP1 + STEP1B) / 2, average.getTiming(ITEM_STEP1).longValue());
		assertEquals((STEP2 + STEP2B) / 2, average.getTiming(ITEM_STEP2).longValue());
		assertEquals((END + END2) / 2, average.getTiming(ITEM_END).longValue());
	}
}
