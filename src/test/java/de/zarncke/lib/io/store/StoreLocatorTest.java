package de.zarncke.lib.io.store;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;

import de.zarncke.lib.err.GuardedTest;
import de.zarncke.lib.region.RegionUtil;

public class StoreLocatorTest extends GuardedTest {
	public void testStoreLocator() throws IOException {
		Store onlyData = new MemStore(RegionUtil.asRegion("hello world!".getBytes("ASCII")));
		HashMap<String, Store> sts = new HashMap<String, Store>();
		sts.put("hello", onlyData);
		Store mapStore = new MapStore(sts);
		Store elementWithData = mapStore.element("hello");

		StoreLocator sl = new StoreLocator();
		sl.register(mapStore, URI.create("http://zarncke.de/example/map"));
		sl.register(mapStore, URI.create("example/map"));

		assertSame(mapStore, sl.resolve(URI.create("http://zarncke.de/example/map")));
		assertSame(mapStore, sl.resolve(URI.create("http://zarncke.de/example/map/")));
		assertEquals(elementWithData, sl.resolve(URI.create("http://zarncke.de/example/map/hello")));
		assertEquals(elementWithData, sl.resolve(URI.create("http://zarncke.de/example/map/hello/")));
		assertFalse(sl.resolve(URI.create("http://zarncke.de/example/map/hello/world")).exists());

		assertSame(mapStore, sl.resolve(URI.create("example/map")));
		assertSame(mapStore, sl.resolve(URI.create("example/map/")));
		assertEquals(elementWithData, sl.resolve(URI.create("example/map/hello")));
		assertEquals(elementWithData, sl.resolve(URI.create("example/map/hello/")));
		assertFalse(sl.resolve(URI.create("example/map/hello/world")).exists());

		assertEquals(elementWithData, sl.resolve(URI.create("example/map/hello")));
		URI mapUri = sl.determineUri(mapStore);
		assertTrue(mapUri.toASCIIString().equals("example/map/")
				|| mapUri.toASCIIString().equals("http://zarncke.de/example/map/"));
		URI elemUri = sl.determineUri(elementWithData);
		// note: no trailing slash
		assertTrue(elemUri.toASCIIString().equals("example/map/hello")
				|| elemUri.toASCIIString().equals("http://zarncke.de/example/map/hello"));
	}

}
