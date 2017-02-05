package de.zarncke.lib.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import junit.framework.TestSuite;

import org.junit.Test;

import de.zarncke.lib.block.ABlock;
import de.zarncke.lib.coll.Elements;
import de.zarncke.lib.coll.L;
import de.zarncke.lib.coll.Pair;
import de.zarncke.lib.ctx.Context;
import de.zarncke.lib.err.GuardedTest;
import de.zarncke.lib.err.TunnelException;
import de.zarncke.lib.io.IOTools.StreamPump;
import de.zarncke.lib.io.store.FileStore;
import de.zarncke.lib.io.store.MapStore;
import de.zarncke.lib.io.store.MapStore.CreateMode;
import de.zarncke.lib.io.store.MemStore;
import de.zarncke.lib.io.store.MutableStore;
import de.zarncke.lib.io.store.Store;
import de.zarncke.lib.io.store.StoreUtil;
import de.zarncke.lib.test.TestClock;
import de.zarncke.lib.time.Clock;
import de.zarncke.lib.util.Chars;
import de.zarncke.lib.util.InterruptionMode;
import de.zarncke.lib.util.Misc;
import de.zarncke.lib.value.Default;

/**
 * Tests classes of io package.
 */
public class IOTest extends GuardedTest {
	private static final int CRC_FOR_4_ZERO_BYTES = 558161692;
	private static final byte[] TEST_BYTE_ARRAY = new byte[] { 2, 3, 5, 7, 11, 13 };

	public IOTest() {
		super();
	}

	@Test
	public void testGetLines() throws Exception {
		List<String> lines = IOTools.getAllLines(new ByteArrayInputStream("a\n\nb\n\n".getBytes()));
		assertEquals("a", lines.get(0));
		assertEquals("", lines.get(1));
		assertEquals("b", lines.get(2));
		assertEquals("", lines.get(3));
		assertEquals(4, lines.size());
	}

	@Test
	public void testProcess() throws Exception {
		if (Misc.isLinux()) {
			List<String> res = Misc.processCommand("/bin/df -P", new StoreConsumer<List<String>>() {
				@Override
				public List<String> consume(final Store storeToProcess) throws IOException {
					return IOTools.getAllLines(storeToProcess.getInputStream());
				}
			}).getFirst();
			assertNotNull(res);
			getUnbufferedLog().report(res);
		}
	}

	public static void testCompositeInputStream() throws Exception {
		InputStream i1 = new ByteArrayInputStream("hello".getBytes(Misc.UTF_8));
		InputStream i2 = new ByteArrayInputStream("".getBytes(Misc.UTF_8));
		InputStream i3 = new ByteArrayInputStream("world".getBytes(Misc.UTF_8));
		InputStream i4 = new ByteArrayInputStream("".getBytes(Misc.UTF_8));
		InputStream isum = new CompositeInputStream(L.l(i1, i2, i3, i4));
		assertEquals("helloworld", new String(IOTools.getAllBytes(isum)));
	}

	public void testDeleteAll() throws Exception {
		File dir = IOTools.createTempDir("deleteall");
		IOTools.dump("test", new File(dir, "test.txt"));
		File subdir = new File(dir, "sub");
		subdir.mkdir();
		IOTools.dump("test", new File(subdir, "subtest.txt"));
		IOTools.deleteAll(dir);
		assertFalse(dir.exists());
	}

	public void testHardLink() throws Exception {
		if (Misc.isWindows() || Misc.isLinux()) {
			File tmp = File.createTempFile("hardlinktest", ".empty");
			File tmp2 = new File(tmp.getAbsolutePath() + ".target");
			IOTools.hardlink(tmp, tmp2);
			IOTools.dump("test", tmp);
			assertEquals("test", IOTools.getAsString(new FileInputStream(tmp2)));
			tmp.delete();
			tmp2.delete();
		}
	}

	public void testHardLinkRecursive() throws Exception {
		if (Misc.isWindows() || Misc.isLinux()) {
			File tmp = IOTools.createTempDir("hardlinktest");
			IOTools.dump("test1", new File(tmp, "t1"));
			File dirT2 = new File(tmp, "t2");
			dirT2.mkdir();
			File tmpT3 = new File(dirT2, "t3");
			IOTools.dump("test23", tmpT3);

			File tmpTarget = new File(tmp.getAbsolutePath() + ".target");
			StoreUtil.hardlinkRecursiveInterruptible(new FileStore(tmp), new FileStore(tmpTarget), StoreUtil.FAIL,
					InterruptionMode.PASS_ON);

			assertEquals("test1", IOTools.getAsString(new FileInputStream(new File(tmpTarget, "t1"))));
			assertEquals("test23", IOTools.getAsString(new FileInputStream(new File(new File(tmpTarget, "t2"), "t3"))));
			IOTools.deleteAll(tmp);

			assertEquals("test23", IOTools.getAsString(new FileInputStream(new File(new File(tmpTarget, "t2"), "t3"))));
			IOTools.deleteAll(tmpTarget);
		}
	}

	public void testDiskSpace() {
		if (Misc.isWindows()) {
			Pair<Long, Long> res = IOTools.getAvailableSystemDiskBytes(new File("C:\\"));
			assertNotNull(res);
			getUnbufferedLog().report(res);
			assertTrue(res.getFirst().longValue() >= 0);
			assertTrue(res.getSecond().longValue() > 0);
			assertTrue(res.getSecond().longValue() >= res.getFirst().longValue());
		}
		if (Misc.isLinux()) {
			Pair<Long, Long> res = IOTools.getAvailableSystemDiskBytes(new File("/"));
			assertNotNull(res);
			getUnbufferedLog().report(res);
			assertTrue(res.getFirst().longValue() >= 0);
			assertTrue(res.getSecond().longValue() > 0);
			assertTrue(res.getSecond().longValue() >= res.getFirst().longValue());
		}
	}

	public void testTimeoutStream() throws TunnelException {
		final TestClock tc = new TestClock(1);
		Context.runWith(new ABlock<Void>() {
			@Override
			public Void execute() throws Exception {
				InputStream is = new ByteArrayInputStream("hello".getBytes(Misc.UTF_8));
				InputStream tis = new TimeoutInputStream(is, 5);
				assertEquals('h', tis.read());
				tc.setSimulatedMillis(2);
				assertEquals('e', tis.read());
				tc.setSimulatedMillis(5);
				assertEquals('l', tis.read());
				tc.setSimulatedMillis(6);
				try {
					tis.read();
					fail("expected timeout");
				} catch (TimeoutInputStream.TimeoutException e) {
					// expected
				}
				tis.close();
				return null;
			}
		}.tunnelExceptions(), Default.of(tc, Clock.class));
	}

	public static void testIOTools() throws Exception {
		// TEST StreamPump & co
		byte[] ba = TEST_BYTE_ARRAY;
		assertTrue("stream must have expected content",
				Elements.arrayequals(ba, IOTools.getAllBytes(new ByteArrayInputStream(ba))));

		ByteArrayOutputStream bo = new ByteArrayOutputStream();
		IOTools.copy(new ByteArrayInputStream(ba), bo);
		assertTrue("stream must have expected content", Elements.arrayequals(ba, bo.toByteArray()));

		bo = new ByteArrayOutputStream();
		StreamPump sp = IOTools.copyAsync(new ByteArrayInputStream(ba), bo);
		sp.problems();
		assertTrue("stream must have expected content", Elements.arrayequals(ba, bo.toByteArray()));
	}

	public static void testPath() {
		MapStore root = new MapStore().setCreateMode(CreateMode.ALLOW_AUTOMATIC);
		((MutableStore) root.element("dir").element("test")).behaveAs(new MemStore());
		root.add("test2", new MemStore());
		assertNull(new Path("missing").locate(root, "test"));
		assertNotNull(new Path("dir").locate(root, "test"));
		assertNotNull(new Path("").locate(root, "test2"));
		assertNotNull(new Path("dir", "").locate(root, "test2"));
	}

	public void testByteBufferStream() throws Exception {
		byte[] ba = TEST_BYTE_ARRAY;

		ByteBuffer bb = ByteBuffer.wrap(ba);
		ByteBufferInputStream bbis = new ByteBufferInputStream(bb);
		byte[] ba2 = IOTools.getAllBytes(bbis);
		assertTrue("stream must have expected content", Elements.arrayequals(ba, ba2));
	}

	public void testCrc() throws IOException {
		assertEquals(CRC_FOR_4_ZERO_BYTES, IOTools.crc32(new ByteArrayInputStream(new byte[] { 0, 0, 0, 0 })));
	}

	public static junit.framework.Test suite() {
		return new TestSuite(IOTest.class);
	}

	public void testCountInputStream() throws Exception {
		ByteArrayInputStream bais = new ByteArrayInputStream("HelloWorld".getBytes());
		CountInputStream cins = new CountInputStream(bais);
		assertEquals(bais.available(), cins.available());
		assertEquals(0, cins.getCount());
		cins.read();
		assertEquals(1, cins.getCount());
		IOTools.getAllBytes(cins);
		assertEquals(10, cins.getCount());
	}

	public void testCountInputStreamClose() throws Exception {
		ByteArrayInputStream bais = new ByteArrayInputStream("HelloWorld".getBytes());
		CountInputStream cins = new CountInputStream(bais);
		cins.setCloseAt(5);
		assertEquals(5, cins.available());
		assertEquals(0, cins.getCount());
		byte[] res = IOTools.getAllBytes(cins);
		assertEquals(5, cins.getCount());
		assertEquals(5, res.length);
	}

	public void testPipe() throws Exception {
		Pair<OutputStream, InputStream> io = IOTools.createPipe();
		IOTools.dump("Hello World", io.getFirst());
		assertEquals("Hello World", IOTools.getAsString(io.getSecond()));
	}

	public void testPipeDeflate() throws Exception {
		Pair<OutputStream, InputStream> io = IOTools.createPipe();
		String test = Chars.repeat("Hello World\n", 42).toString();
		IOTools.dump(test, new DeflaterOutputStream(io.getFirst()));
		assertEquals(test, IOTools.getAsString(new InflaterInputStream(io.getSecond())));
	}

	@Deprecated
	public void testPipeQueue() throws Exception {
		PipeQueue<String> queue = new PipeQueue<String>(String.class);
		assertTrue(queue.isEmpty());
		queue.add("Hello");
		queue.add("World");
		queue.flush();
		assertEquals(2, queue.size());
		assertEquals("Hello", queue.remove());
		assertEquals("World", queue.remove());
		assertTrue(queue.isEmpty());
	}

	public void testHolePunching() throws Exception {
		if (Misc.isWindows() || Misc.isLinux()) {
			File file = File.createTempFile("sparse", ".data", new File("target"));
			// File file = new File("C:\\cygwin\\tmp\\test");
			// File file = new File("C:\\cygwin\\home\\Gunnar Zarncke\\t.x\\test");
			// File file = new File("C:\\Users\\gunnar\\AppData\\Local\\Temp\\zzz");
			file.getParentFile().mkdirs();
			// IOTools.dump("", file);

			Long freeBeforeDump = IOTools.getAvailableSystemDiskBytes(file).getFirst();

			// file.deleteOnExit();
			RandomAccessFile raf = new RandomAccessFile(file, "rw");
			raf.setLength(Misc.BYTES_PER_MB);
			raf.close();

			byte[] data = new byte[(int) Misc.BYTES_PER_MB];
			for (int i = 0; i < data.length; i++) {
				data[i] = (byte) (64 + i % 64);
			}
			IOTools.dump(data, file);

			Long freeAfterDump = IOTools.getAvailableSystemDiskBytes(file).getFirst();

			boolean supported = IOTools.setSparseZeros(file, 0, Misc.BYTES_PER_MB);
			if (!supported) {
				getUnbufferedLog().report("sparse file hole punching not supported on file system on " + file);
				return;
			}

			Long freeAfterPunch = IOTools.getAvailableSystemDiskBytes(file).getFirst();

			data = IOTools.getAllBytes(file);
			assertEquals("0th byte should be 0", 0, data[0]);
			assertEquals("last byte should be 0", 0, data[data.length - 1]);

			if (freeBeforeDump != null) {
				getUnbufferedLog().report(
						"before dump: " + freeBeforeDump + "\nafter dump:  " + freeAfterDump + "\nafter punch: "
								+ freeAfterPunch);
				if (freeAfterDump.longValue() == freeBeforeDump.longValue()) {
					fail("memory didn't decrease by dump");
				}
				if (freeAfterPunch.longValue() == freeAfterDump.longValue()) {
					fail("memory didn't increase by dump");
				}
			}
		}
	}
}
