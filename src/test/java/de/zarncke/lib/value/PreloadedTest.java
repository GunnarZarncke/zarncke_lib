package de.zarncke.lib.value;

import java.util.concurrent.atomic.AtomicInteger;

import org.joda.time.Duration;

import de.zarncke.lib.err.NotAvailableException;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.test.ContextUsingTest;
import de.zarncke.lib.test.TestClock;
import de.zarncke.lib.time.Clock;
import de.zarncke.lib.time.Times;
import de.zarncke.lib.util.Q;

public class PreloadedTest extends ContextUsingTest {
	TestClock testClock = new TestClock(0);

	public void testLoad() throws InterruptedException {
		Value<Integer> v = new Value<Integer>() {
			AtomicInteger i = new AtomicInteger(0);

			@Override
			public Integer get() {
				// new Exception().printStackTrace();
				return Q.i(this.i.incrementAndGet());
			}
		};

		assertEquals(Q.i(1), v.get());

		Preloaded<Integer> pre = new Preloaded<Integer>(v, Duration.standardSeconds(1));

		assertEquals(Q.i(2), pre.get());
		assertEquals(Q.i(3), v.get());
		assertEquals(Q.i(2), pre.get());

		this.testClock.incrementSimulatedMillis(1001);
		assertEquals(Q.i(2), pre.get());
		// give thread time to fetch it
		if (!Q.i(4).equals(pre.get())) {
			Thread.sleep(1000);
		}
		assertEquals(Q.i(4), pre.get());
		assertEquals(Q.i(5), v.get());
	}

	public void testFail() throws InterruptedException {
		Value<Integer> v = new Value<Integer>() {
			AtomicInteger i = new AtomicInteger(0);

			@Override
			public Integer get() {
				// new Exception().printStackTrace();
				int c = this.i.incrementAndGet();
				if (c < 4) {
					throw Warden.spot(new NotAvailableException("fail"));
				}
				return Q.i(c);
			}
		};

		try {
			v.get();
			fail("expect exception");
		} catch (NotAvailableException e) {
			Warden.disregard(e);
		}

		Preloaded<Integer> pre = new Preloaded<Integer>(v, Duration.standardSeconds(1)) {
			@Override
			protected void handleFailure(final RuntimeException e) {
				Warden.disregard(e);
			}
		};

		assertNull(pre.get());

		assertEquals(Q.i(4), v.get());
	}

	@Override
	protected long getMaximumTestMillis() {
		return 3 * Times.MILLIS_PER_SECOND;
	}
	@Override
	protected Default<?>[] getContextsToApply() {
		return Default.many(Default.of(this.testClock, Clock.class));
	}
}
