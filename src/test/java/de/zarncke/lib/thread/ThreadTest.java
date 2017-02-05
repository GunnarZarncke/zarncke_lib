package de.zarncke.lib.thread;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.util.Misc;

/**
 * Tests classes of thread package.
 */
public class ThreadTest extends TestCase {
	private static class LockStress extends AbstractLooper {
		public int should = 0;
		public int must = 0;
		public Lock lock;

		@Override
		public void loop() {
			this.lock.lock();
			int a = this.should;
			Misc.sleep(10);

			this.should = ++a;
			synchronized (this) {
				this.must++;
			}

			this.lock.unlock();
			Misc.sleep(1);
		}
	}

	public ThreadTest() {
		super("ThreadTest");
	}

	public static void testLock() {
		final Lock l = Lock.makeLock("test");
		LockStress ls = new LockStress();
		ls.lock = l;
		MultiThread mt = new MultiThread(ls, 10);
		mt.start();
		Misc.sleep(100);
		mt.stop();
		assertEquals(ls.must, ls.should);
	}

	public static void testMultiThread() {
		// TODO add test
	}

	public static void testInterruption() throws InterruptedException {
		final int[] step = new int[1];
		Thread t = new InterruptibleThread(new Runnable() {
			@Override
			public void run() {
				step[0] = 1;

				try {
					Thread.sleep(1000);
					step[0] = 99;
				} catch (InterruptedException e) {
					step[0] = 2;
				}
			}
		});
		t.start();
		Thread.sleep(100);
		assertEquals(1, step[0]);

		InterruptibleThread.interruptAllInterruptibleThreads(L.l(t), true);
		Thread.sleep(100);
		assertEquals(2, step[0]);
	}

	public static Test suite() {
		return new TestSuite(ThreadTest.class);
	}
}
