package de.zarncke.lib.io.store;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.diff.Diff;
import de.zarncke.lib.err.GuardedTest;
import de.zarncke.lib.io.IOTools;
import de.zarncke.lib.io.LineConsumer;
import de.zarncke.lib.io.OutputProducer;
import de.zarncke.lib.io.store.MapStore.CreateMode;
import de.zarncke.lib.region.RegionUtil;

public class StoreTest extends GuardedTest {
	private static final String TEST_STRING_2 = "end of world";
	private static final String TEST_STRING_1 = "hello world";
	private int readLine = 0;

	@Test
	public void testStoreUtil() throws IOException {
		Store onlyData = new MemStore(RegionUtil.asRegion("hello world!".getBytes("ASCII")));
		HashMap<String, Store> sts = new HashMap<String, Store>();
		sts.put("hello", onlyData);
		MapStore mapStore = new MapStore(sts);
		mapStore.setCreateMode(MapStore.CreateMode.ALLOW_AUTOMATIC);

		assertSame(onlyData, StoreUtil.resolvePath(onlyData, "/", "/"));
		Store s = StoreUtil.resolvePath(onlyData, "/hello", "/");
		assertFalse("element cannot know parent", s.exists());

		Store elementWithData = mapStore.element("hello");
		assertSameContent(onlyData, elementWithData);

		assertSame(mapStore, StoreUtil.resolvePath(mapStore, null, "/"));
		assertSame(mapStore, StoreUtil.resolvePath(mapStore, "", "/"));
		assertEquals(elementWithData, StoreUtil.resolvePath(mapStore, "hello", "/"));
		assertEquals(elementWithData, StoreUtil.resolvePath(mapStore, "/hello", "/"));
		assertEquals(elementWithData, StoreUtil.resolvePath(elementWithData, "/hello", "/"));
		assertSame(mapStore, StoreUtil.resolvePath(elementWithData, "/", "/"));
		assertEquals(mapStore.element("hello"), mapStore.iterator().next());

		Store sub = mapStore.element("sub");
		Store elem = sub.element("new");
		// force creation
		elem.asRegion();
		assertEquals(elem, sub.iterator().next());
	}

	@Test
	public void testMapStoreShadow() {
		MapStore m = new MapStore().setCreateMode(CreateMode.ALLOW_AUTOMATIC);
		assertFalse(m.element("none").exists());
		assertEquals(0, L.copy(m.iterator()).size());
	}

	@Test
	public void testMapStoreDelete() throws IOException {
		MapStore m = new MapStore().setCreateMode(CreateMode.ALLOW_AUTOMATIC);
		add(m, "a");
		add(m, "b");
		assertEquals(2, L.copy(m.iterator()).size());

		// simple top level delete
		m.element("a").delete();
		assertEquals(1, L.copy(m.iterator()).size());

		// delete of sub element
		IOTools.dump("d", m.element("c").element("d").getOutputStream(false));
		assertEquals(1, L.copy(m.element("c").iterator()).size());
		m.element("c").element("d").delete();
		assertEquals(0, L.copy(m.element("c").iterator()).size());

		m.element("c").delete();
		assertEquals(1, L.copy(m.iterator()).size());
	}

	@Test
	public void testMapStoreDeleteEmbedded() throws IOException {
		MapStore m = new MapStore().setCreateMode(CreateMode.ALLOW_AUTOMATIC);
		assertEquals(0, L.copy(m.iterator()).size());

		File tmp = File.createTempFile("test", ".dat");
		tmp.deleteOnExit();
		FileStore fs = new FileStore(tmp);
		IOTools.dump("embedded", fs.getOutputStream(false));
		m.add("embed", fs);

		assertEquals(1, L.copy(m.iterator()).size());

		m.element("embed").delete();

		assertFalse(tmp.canRead());
		assertFalse(tmp.exists());

		assertEquals(0, L.copy(m.iterator()).size());
	}

	@Test
	public void testMapStoreDeleteEmbeddedNested() throws IOException {
		MapStore m = new MapStore().setCreateMode(CreateMode.ALLOW_AUTOMATIC);
		assertEquals(0, L.copy(m.iterator()).size());

		File tmp = IOTools.createTempDir("test");
		tmp.deleteOnExit();
		FileStore fs = new FileStore(tmp);
		m.add("embed", fs);

		IOTools.dump("embedded", m.element("embed").element("nested").getOutputStream(false));

		assertEquals(1, L.copy(m.iterator()).size());
		assertEquals(1, L.copy(m.element("embed").iterator()).size());

		m.element("embed").element("nested").delete();

		assertFalse(fs.element("nested").canRead());
		assertFalse(fs.element("nested").exists());

		assertEquals(0, L.copy(m.element("nested").iterator()).size());
	}

	@Test
	public void testMapStoreBehave() throws IOException {
		Store a = StoreUtil.asStore("a", null);
		Store b = StoreUtil.asStore("b", null);
		Store q = StoreUtil.asStore("q", null);
		Store r = StoreUtil.asStore("r", null);
		MapStore inner = new MapStore().setCreateMode(CreateMode.ALLOW_AUTOMATIC);
		inner.add("q", q);

		MapStore map = new MapStore().setCreateMode(CreateMode.ALLOW_AUTOMATIC);

		((MutableStore) map.element("x").element("c")).behaveAs(a);
		assertEquals(a.asRegion(), map.element("x").element("c").asRegion());

		// ((MutableStore) map.element("y").element("x").element("c")).behaveAs(a);
		// assertEquals(a.asRegion(), map.element("y").element("x").element("c").asRegion());

		((MutableStore) map.element("a")).behaveAs(a);
		assertEquals(a.asRegion(), map.element("a").asRegion());
		((MutableStore) map.element("a")).behaveAs(b);
		assertEquals(b.asRegion(), map.element("a").asRegion());

		((MutableStore) map.element("b")).behaveAs(inner);
		assertEquals(q.asRegion(), map.element("b").element("q").asRegion());
		((MutableStore) map.element("b").element("q")).behaveAs(r);
		assertEquals(r.asRegion(), map.element("b").element("q").asRegion());
	}

	@Test
	public void testStoreBuildPath() {
		MapStore ms = new MapStore().setCreateMode(CreateMode.ALLOW_AUTOMATIC);
		Store d = StoreUtil.resolvePath(ms, "a/b/c/d", "/");
		assertEquals("", StoreUtil.buildPathFromTo(ms, ms, "/"));
		assertEquals("", StoreUtil.buildPathFromTo(ms.element("d").getParent(), ms, "/"));
		assertEquals("/a/b/c/d", StoreUtil.buildPathFromTo(null, d, "/"));
		assertEquals("a/b/c/d", StoreUtil.buildPathFromTo(ms, d, "/"));
		assertEquals("a/b/c", StoreUtil.buildPathFromTo(ms, d.getParent(), "/"));
		assertEquals("b/c/d", StoreUtil.buildPathFromTo(ms.element("a"), d, "/"));

		assertEquals("", StoreUtil.buildPathFromTo(null, ms, "/"));
		assertEquals("hello", StoreUtil.buildPathFromTo(null, new FileStore("hello"), "/"));
		assertEquals("hello/world", StoreUtil.buildPathFromTo(null, new FileStore("hello").element("world"), "/"));
		assertEquals("world", StoreUtil.buildPathFromTo(null, new FileStore("hello/world"), "/"));
		assertEquals("world", StoreUtil.buildPathFromTo(null, new FileStore("/hello/world"), "/"));
	}

	@Test
	public void testStoreUtilCopy() throws IOException {
		// structure:
		// a
		// a/b
		// a/c/d
		// a/c/e
		// a/f

		Store b = StoreUtil.asStore("b", null);
		Store d = StoreUtil.asStore("d", null);
		Store e = StoreUtil.asStore("e", null);
		Store f = StoreUtil.asStore("f", null);
		MapStore c = new MapStore();
		c.add("d", d);
		c.add("e", e);
		MapStore a = new MapStore();
		a.add("b", b);
		a.add("c", c);
		a.add("f", f);

		MapStore source = new MapStore();
		source.add("a", a);
		Store dst = StoreUtil.copyIntoIterable(source, "a/b", "a/c/e");
		assertTrue(dst.iterationSupported());
		assertTrue(dst.element("a").iterationSupported());
		assertTrue(dst.element("a").element("b").canRead());
		assertTrue(dst.element("a").element("c").iterationSupported());

		assertSameContent(b, StoreUtil.resolvePath(dst, "a/b", "/"));
		assertEquals(2, L.copy(StoreUtil.resolvePath(dst, "a", "/").iterator()).size());
		assertSameContent(e, StoreUtil.resolvePath(dst, "a/c/e", "/"));
		assertEquals(1, L.copy(StoreUtil.resolvePath(dst, "a/c", "/").iterator()).size());

		MapStore copy = new MapStore();
		copy.setCreateMode(CreateMode.ALLOW_AUTOMATIC);
		StoreUtil.copyRecursive(dst, copy, StoreUtil.OVERWRITE);
	}

	protected void assertSameContent(final Store a, final Store b) throws IOException {
		assertEquals(0, RegionUtil.compare(a.asRegion(), b.asRegion()));
	}

	@Test
	public void testAppend() throws IOException {
		File t = File.createTempFile("test", ".txt");
		t.deleteOnExit();
		Store s = new FileStore(t);
		test(s);

		test(new MemStore());
	}

	private void test(final Store s) throws IOException {
		this.readLine = 0;
		new OutputProducer() {
			@Override
			protected Object produce() {
				return TEST_STRING_1;
			}
		}.produce(s);
		new OutputProducer(true) {
			@Override
			protected Object produce() {
				return "next state";
			}
		}.produce(s);
		new LineConsumer<Void>() {
			@Override
			protected void consume(final String line) {
				if (StoreTest.this.readLine == 0) {
					assertEquals(TEST_STRING_1, line);
				} else if (StoreTest.this.readLine == 1) {
					assertEquals("next state", line);
				} else {
					fail("unknown line");
				}
				StoreTest.this.readLine++;
			}
		}.consume(s);
		assertEquals(2, this.readLine);
	}

	public void testZip() throws IOException {
		MapStore store = new MapStore();
		store.setCreateMode(CreateMode.ALLOW_AUTOMATIC);
		store.add("a", StoreUtil.asStore(TEST_STRING_1, null));
		StoreUtil.copy(StoreUtil.asStore(TEST_STRING_2, null), store.element("b").element("c"));

		MemStore temp = new MemStore();
		// Store temp = new FileStore(new File("./t.zip"));
		StoreUtil.zip(store, temp);

		MapStore dest = new MapStore();
		dest.setCreateMode(CreateMode.ALLOW_AUTOMATIC);
		StoreUtil.unzip(temp, dest);

		assertEquals(L.l(), StoreUtil.compareStores(store, dest));
	}

	public void testCompare() throws IOException {
		MapStore o = new MapStore();
		MapStore a = new MapStore();
		a.add("1", StoreUtil.asStore("hello", a));
		MapStore b = new MapStore();
		b.add("2", StoreUtil.asStore("world", b));
		MapStore a2 = new MapStore();
		a2.add("1", StoreUtil.asStore("world", b));
		assertEquals(L.l(), StoreUtil.compareStores(o, o));
		assertEquals(L.l(), StoreUtil.compareStores(a, a));
		assertEquals(L.l(), StoreUtil.compareStores(b, b));
		assertEquals(L.l("null/1-"), StoreUtil.compareStores(a, o));
		assertEquals(L.l("null/2+"), StoreUtil.compareStores(o, b));
		assertEquals(L.l("null/1-", "null/2+"), StoreUtil.compareStores(a, b));
		assertEquals(L.l("null/1 'hello'!='world'"), StoreUtil.compareStores(a, a2));

		assertTrue(StoreUtil.compareStoresDetailed(o, o).isIdentity());
		assertEquals(1.0, StoreUtil.compareStoresDetailed(o, o).getSameness(), 0.0);
		assertTrue(StoreUtil.compareStoresDetailed(a, a).isIdentity());
		assertEquals(1.0, StoreUtil.compareStoresDetailed(a, a).getSameness(), 0.0);

		assertEquals(0.0, StoreUtil.compareStoresDetailed(a, b).getSameness(), 0.0);

		assertEquals(0.0, StoreUtil.compareStoresDetailed(a, a2).getSameness(), 0.0);

		// todo move below tests to DiffTest

		// prefix tests
		MapStore a3 = new MapStore();
		a3.add("1", StoreUtil.asStore("hello you!", b));
		assertEquals(0.5, StoreUtil.compareStoresDetailed(a, a3).getSameness(), 0.01);
		a3.add("1", StoreUtil.asStore("hello you! ninechars", b));
		assertEquals(0.25, StoreUtil.compareStoresDetailed(a, a3).getSameness(), 0.01);

		// suffix tests
		a.add("1", StoreUtil.asStore(" ninechars", b));
		assertEquals(0.5, StoreUtil.compareStoresDetailed(a, a3).getSameness(), 0.01);
		a.add("1", StoreUtil.asStore("chars", b));
		assertEquals(0.25, StoreUtil.compareStoresDetailed(a, a3).getSameness(), 0.01);
		a.add("1", StoreUtil.asStore("only five chars", b));
		assertEquals(0.25, StoreUtil.compareStoresDetailed(a, a3).getSameness(), 0.01);

		// overlap tests
		a.add("1", StoreUtil.asStore("a b a b a", b));
		a3.add("1", StoreUtil.asStore("a b a", b));
		assertEquals(0.555, StoreUtil.compareStoresDetailed(a, a3).getSameness(), 0.01);
	}

	public void testMemStore() throws IOException {
		MemStore store = new MemStore();
		assertEquals(0, store.asRegion().length());

		IOTools.dump(TEST_STRING_1, store.getOutputStream(false));
		assertEquals(TEST_STRING_1, new String(store.asRegion().toByteArray()));
		assertEquals(TEST_STRING_1, new String(IOTools.getAllBytes(store.getInputStream())));

		IOTools.dump(TEST_STRING_2, store.getOutputStream(false));
		assertEquals(TEST_STRING_2, new String(store.asRegion().toByteArray()));

		IOTools.dump(TEST_STRING_1, store.getOutputStream(true));
		assertEquals(TEST_STRING_2 + TEST_STRING_1, new String(store.asRegion().toByteArray()));
	}

	public void testFlattenMap() {
		// structure:
		// a
		// a/b
		// a/c/d
		// a/c/e
		// a/f

		MapStore c = new MapStore();
		add(c, "d");
		add(c, "e");
		MapStore a = new MapStore();
		a.add("c", c);
		add(a, "b");
		add(a, "f");

		Store res = StoreUtil.flatten(a, ".", Store.class);

		List<Store> elements = L.copy(res.iterator());
		Collections.sort(elements, StoreUtil.COMPARE_BY_NAME);
		assertEquals(elements.toString(), 4, elements.size());
		assertEquals("b", elements.get(0).getName());
		assertEquals("c.d", elements.get(1).getName());
		assertEquals("c.e", elements.get(2).getName());
		assertEquals("f", elements.get(3).getName());

	}

	private static void add(final MapStore parent, final String path) {
		parent.add(path, StoreUtil.asStore(path, null));

	}

	public void testHierarchify() throws IOException {
		MapStore a = new MapStore();
		add(a, "b");
		assertEquals("b", IOTools.getAsString(a.element("b").getInputStream()));
		add(a, "c.d");
		add(a, "c.e");
		add(a, "f");
		// just to have a file in it
		File file = new File("target/tmp");
		IOTools.deleteAll(file);
		a.add("f", new FileStore(file));
		Store deep = StoreUtil.hierarchify(a, ".");
		assertEquals(deep.toString(), "b", IOTools.getAsString(deep.element("b").getInputStream()));
		assertEquals("c.d", IOTools.getAsString(deep.element("c").element("d").getInputStream()));
		assertEquals("c.e", IOTools.getAsString(deep.element("c").element("e").getInputStream()));
		assertEquals(file, FileStore.getFile( deep.element("f")));

		Store rea = StoreUtil.flatten(deep, ".", MapStore.class);
		// TODO slow for some reason!?
		Diff diff = StoreUtil.compareStoresDetailed(a, rea);
		if (!diff.isIdentity()) {
			fail(diff.toString());
		}
	}


	public void testFlattenFile() throws IOException {
		// structure:
		// a
		// a/b
		// a/c/d
		// a/c/e
		// a/f

		Store a = new FileStore(IOTools.createTempDir("flatten"));
		StoreUtil.deleteOnExit(a);

		IOTools.dump("b", a.element("b").getOutputStream(false));
		IOTools.dump("c.d", a.element("c").element("d").getOutputStream(false));
		IOTools.dump("c.e", a.element("c").element("e").getOutputStream(false));
		IOTools.dump("f", a.element("f").getOutputStream(false));

		Store res = StoreUtil.flatten(a, ".", Store.class);

		List<Store> elements = L.copy(res.iterator());
		Collections.sort(elements, StoreUtil.COMPARE_BY_NAME);
		assertEquals(elements.toString(), 4, elements.size());
		assertEquals("b", elements.get(0).getName());
		assertEquals("c.d", elements.get(1).getName());
		assertEquals("c.e", elements.get(2).getName());
		assertEquals("f", elements.get(3).getName());
	}

	public void testFlattenFederated() throws IOException {
		// structure:
		// a
		// a/b
		// a/c/d
		// a/c/e
		// a/f[file]/q/r

		Store b = StoreUtil.asStore("b", null);
		Store d = StoreUtil.asStore("d", null);
		Store e = StoreUtil.asStore("e", null);
		Store f = new FileStore(IOTools.createTempDir("federated"));
		StoreUtil.deleteOnExit(f);
		IOTools.dump("test data", f.element("q").element("r").getOutputStream(false));
		MapStore c = new MapStore();
		c.add("d", d);
		c.add("e", e);
		MapStore a = new MapStore();
		a.add("b", b);
		a.add("c", c);
		a.add("f", f);

		Store res = StoreUtil.flatten(a, ".", Store.class);

		List<Store> elements = L.copy(res.iterator());
		Collections.sort(elements, StoreUtil.COMPARE_BY_NAME);
		assertEquals(elements.toString(), 4, elements.size());
		assertEquals("b", elements.get(0).getName());
		assertEquals("c.d", elements.get(1).getName());
		assertEquals("c.e", elements.get(2).getName());
		assertEquals("f.q.r", elements.get(3).getName());

		res = StoreUtil.flatten(a, ".", MapStore.class);

		elements = L.copy(res.iterator());
		Collections.sort(elements, StoreUtil.COMPARE_BY_NAME);
		assertEquals(elements.toString(), 4, elements.size());
		assertEquals("b", elements.get(0).getName());
		assertEquals("c.d", elements.get(1).getName());
		assertEquals("c.e", elements.get(2).getName());
		assertEquals("f", elements.get(3).getName());
		assertEquals(FileStore.class, DelegateStore.unwrap(elements.get(3)).getClass());
	}

	@Override
	protected long getMaximumTestMillis() {
		return super.getMaximumTestMillis() / 2;
	}
}
