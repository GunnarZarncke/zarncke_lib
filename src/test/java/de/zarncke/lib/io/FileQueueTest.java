package de.zarncke.lib.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;

import de.zarncke.lib.block.Running;
import de.zarncke.lib.coll.Elements;
import de.zarncke.lib.coll.L;
import de.zarncke.lib.err.Guarded;
import de.zarncke.lib.err.Guarded.MaxTestTimeMillis;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.io.FileQueue.ClearMode;
import de.zarncke.lib.time.Times;
import de.zarncke.lib.util.Misc;

@RunWith(Guarded.class)
@MaxTestTimeMillis(10 * Times.MILLIS_PER_SECOND)
public class FileQueueTest {

	@Test
	public void testFileQueue() throws Exception {
		testFileQueue(new FileQueue<CharSequence>(CharSequence.class, false));
	}

	@Test
	public void testFileQueue17() throws Exception {
		if (Misc.isJava7()) {
			testFileQueue(new FileQueue<CharSequence>(CharSequence.class, true));
		} else {
			Guarded.getUnbufferedLog().report("Java 1.7 Test skipped");
		}
	}

	private void testFileQueue(final FileQueue<CharSequence> queue) throws Exception {
		assertTrue(queue.isEmpty());
		assertTrue(queue.add("Hello"));
		assertTrue(queue.offer("World"));
		queue.put("!");
		queue.put("");
		queue.put(" ");

		assertEquals(5, queue.size());

		assertEquals("Hello", queue.take());
		assertEquals("World", queue.remove());
		assertEquals("!", queue.poll(1, TimeUnit.SECONDS));

		Collection<CharSequence> l = L.l();
		queue.drainTo(l, 1);
		assertEquals(1, queue.size());
		assertEquals(L.l(""), l);
		queue.drainTo(l);
		assertEquals(L.l("", " "), l);

		assertTrue(queue.isEmpty());
	}

	public void testFileQueueLong() throws Exception {
		String s = "\nTest!!!!!\n";
		for (int i = 0; i < 1000; i++) {
			s = s += "Hello brave new and very long world number " + i + "!\n";
		}
		byte[] data = s.getBytes(Misc.UTF_8);
		Guarded.getUnbufferedLog().report("writing 1000x" + data.length);

		FileQueue<byte[]> queue = new FileQueue<byte[]>(byte[].class);
		// queue.setClearMode(ClearMode.SPARSE);
		queue.setClearMode(ClearMode.NONE);
		assertTrue(queue.isEmpty());

		long start = System.currentTimeMillis();

		int elementsTested = 1000;
		for (int i = 0; i < elementsTested; i++) {
			queue.add(data);
		}
		assertEquals(elementsTested, queue.size());
		byte[] d = queue.remove();
		assertTrue(Elements.arrayequals(data, d));
		for (int i = 0; i < elementsTested - 1; i++) {
			d = queue.remove();
			assertTrue(Elements.arrayequals(data, d));
		}
		assertTrue(queue.isEmpty());
		long time = System.currentTimeMillis() - start;
		long vol = elementsTested * data.length;
		Guarded.getUnbufferedLog().report(
				"duration: " + time + "ms\ndata: " + vol / 1000 + "kb\nrate: " + vol / time + "kb/s");
		queue.close();

	}

	public void testFileQueueConcurrentLong() throws Exception {
		String s = "\nTest!!!!!\n";
		for (int i = 0; i < 1000; i++) {
			s = s += "Hello brave new and very long world number " + i + "!\n";
		}
		final byte[] data = s.getBytes(Misc.UTF_8);
		Guarded.getUnbufferedLog().report("writing 1000x" + data.length);

		final FileQueue<byte[]> queue = new FileQueue<byte[]>(byte[].class);
		// queue.setClearMode(ClearMode.SPARSE);
		queue.setClearMode(ClearMode.ZERO);
		assertTrue(queue.isEmpty());

		long start = System.currentTimeMillis();
		final int elementsTested = 1000;
		final CyclicBarrier barrier = new CyclicBarrier(3);

		Thread producer = new Thread(Warden.guarded(new Running() {
			@Override
			public void run() {
				for (int i = 0; i < elementsTested; i++) {
					queue.add(data);
				}
				try {
					barrier.await();
				} catch (InterruptedException e) {
					throw Warden.spot(new RuntimeException("", e));
				} catch (BrokenBarrierException e) {
					throw Warden.spot(new RuntimeException("", e));
				}
			}
		}), "Producer");
		producer.start();
		Thread consumer = new Thread(Warden.guarded(new Running() {
			@Override
			public void run() {
				byte[] d;

				for (int i = 0; i < elementsTested; i++) {
					while (true) {
						d = queue.poll();
						if (d != null) {
							break;
						}
						try {
							Thread.sleep(1);
						} catch (InterruptedException e) {
							throw Warden.spot(new RuntimeException(i + "/" + queue.size(), e));
						}
						}
					if (!Elements.arrayequals(data, d)) {
						fail("unexpected at" + i + "/" + queue.size() + ":" + data.length + ":"
								+ Elements.byteArrayToHumanReadable(data));
						}
					}
				try {
					barrier.await();
				} catch (InterruptedException e) {
					throw Warden.spot(new RuntimeException("", e));
				} catch (BrokenBarrierException e) {
					throw Warden.spot(new RuntimeException("", e));
					}

			}
		}), "Consumer");
		consumer.start();
		try {
			barrier.await(10 * Times.MILLIS_PER_SECOND, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			producer.interrupt();
			consumer.interrupt();
			Thread.sleep(100);
			producer.stop(new RuntimeException("stopped"));
			consumer.stop(new RuntimeException("stopped"));
			Thread.sleep(100);
			throw e;
		}

		long time = System.currentTimeMillis() - start;
		long vol = elementsTested * data.length;
		Guarded.getUnbufferedLog().report(
				"duration: " + time + "ms\ndata: " + vol / 1000 + "kb\nrate: " + vol / time + "kb/s");
		queue.close();
	}

}
