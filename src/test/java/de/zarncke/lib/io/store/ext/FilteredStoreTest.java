package de.zarncke.lib.io.store.ext;

import java.util.List;

import org.junit.Test;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.err.GuardedTest4;
import de.zarncke.lib.io.store.MapStore;
import de.zarncke.lib.io.store.MemStore;
import de.zarncke.lib.io.store.Store;

public class FilteredStoreTest extends GuardedTest4 {
	@Test
	public void testFilter() throws Exception {
		MapStore ms = MapStore.newAllowAutomatic();
		ms.add("a", new MemStore());
		ms.add("b", new MemStore());
		FilteredStore fs = new FilteredStore(ms, "b");
		List<? extends Store> children = L.copy(fs.enhancedIterator());
		assertEquals(1, children.size());
		assertEquals("b", children.get(0).getName());
	}
}
