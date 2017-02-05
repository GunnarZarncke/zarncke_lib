package de.zarncke.lib.lang;

import de.zarncke.lib.err.GuardedTest;

public class ObjectSizeTest extends GuardedTest {
	interface Cloner {
		Object create();
	}

	public void testSize() {
		// getUnbufferedLog().report("null min=" + estimateSizeRetried(22, null));
		// getUnbufferedLog().report("String min=" + estimateSizeRetried(19, new Cloner() {
		// public Object create() {
		// return new String();
		// }
		// }));
		// getUnbufferedLog().report("Object min=" + estimateSizeRetried(20, new Cloner() {
		// public Object create() {
		// return new Object();
		// }
		// }));
		// getUnbufferedLog().report("Integer min=" + estimateSizeRetried(19, new Cloner() {
		// public Object create() {
		// return new Integer((short) 17);
		// }
		// }));
		// getUnbufferedLog().report("Long min=" + estimateSizeRetried(19, new Cloner() {
		// public Object create() {
		// return new Long((short) 17);
		// }
		// }));
		// getUnbufferedLog().report("array min=" + estimateSizeRetried(15, new Cloner() {
		// @Override
		// public Object create() {
		// return new boolean[100];
		// }
		// }));
		// getUnbufferedLog().report("Obj->Obj min=" + estimateSizeRetried(15, new Cloner() {
		// @Override
		// public Object create() {
		// return specific();
		// }
		// }));
	}

	private static Object specific() {
		return new Object() {
			Object a = new Object();
		};

		// HashMap h = new HashMap(3);
		// h.put("g", "f");
		// h.put("h", "f");
		// h.put("i", "f");
		// h.put("j", "f");
		// h.put("k", "f");
		// h.put("l", "f");
		// h.put("m", "f");
		// return h;
	}

	public double estimateSizeRetried(final int start, final Cloner object) {
		double min = Integer.MAX_VALUE;
		estimateSize(start, object);
		for (int i = 0; i < 5; i++) {
			double s = estimateSize(start, object);
			min = Math.min(min, s);
			getUnbufferedLog().report("run " + i + ": min=" + min);
		}
		return min;
	}

	private double estimateSize(final int start, final Cloner object) {
		int i = start;
		while (i < 100) {
			try {
				int size = 1 << i;
				estimateSize(object, size);
			} catch (OutOfMemoryError e) {
				break;
			}
			i++;
		}
		i--;
		double min = Integer.MAX_VALUE;
		int maxsize = 1 << i;
		for (i = 0; i < 100; i++) {
			try {
				int size = (int) (maxsize * Math.pow(1.1, i));
				double objMem = estimateSize(object, size);
				min = Math.min(min, objMem);
			} catch (OutOfMemoryError e) {
				break;
			}
		}
		return min;
	}

	public double estimateSize(final Cloner object, final int size) {
		System.gc();
		Runtime runtime = Runtime.getRuntime();
		long usedMem = runtime.totalMemory() - runtime.freeMemory();
		Object[] objs = new Object[size];
		if (object != null) {
			for (int j = 0; j < size; j++) {
				objs[j] = object.create();
			}
		}
		long usedMem2 = runtime.totalMemory() - runtime.freeMemory();
		double objMem = (usedMem2 - usedMem) / (double) size;

		objs = null;

		int i = 0;
		while (i < 13) {
			long usedMem3 = runtime.totalMemory() - runtime.freeMemory();
			if (usedMem3 < usedMem + (usedMem2 - usedMem) / 10) {
				break;
			}
			getUnbufferedLog().report("waiting for memory free " + usedMem3);
			System.gc();
			if (i > 1) {
				try {
					Thread.sleep(1 << i);
				} catch (InterruptedException e) {
				}
			}
			i++;
		}
		return objMem;
	}

}
