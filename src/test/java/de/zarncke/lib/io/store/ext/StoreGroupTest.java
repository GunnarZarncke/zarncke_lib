package de.zarncke.lib.io.store.ext;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;

import org.junit.Test;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.coll.Pair;
import de.zarncke.lib.err.GuardedTest;
import de.zarncke.lib.io.store.MapStore;
import de.zarncke.lib.io.store.MemStore;
import de.zarncke.lib.io.store.Store;
import de.zarncke.lib.io.store.StoreUtil;
import de.zarncke.lib.io.store.ext.StoreGroup.Controller;
import de.zarncke.lib.io.store.ext.StoreGroup.Grouper;
import de.zarncke.lib.region.RegionUtil;
import de.zarncke.lib.util.Chars;

public class StoreGroupTest extends GuardedTest {

	@Test
	public void testStoreGroup() throws IOException {
		testStoreGroup(false);
		testStoreGroup(true);
	}

	private void testStoreGroup(final boolean fail) throws IOException {
		Store a = make("a");
		Store b = make("b");
		Store c = make("c");
		Store d = make("d");
		Store e = make("e");
		MapStore mapStore = new MapStore();
		mapStore.setCreateMode(MapStore.CreateMode.ALLOW_AUTOMATIC);
		mapStore.add("a.1", a);
		if (fail) {
			// add an element that is designed to cause the two different groups with the same name "a" to be created
			mapStore.add("collision", a);
		}
		mapStore.add("b.1", b);
		mapStore.add("c.2", c);
		mapStore.add("d.2", d);
		mapStore.add("e", e);

		Store group = null;
		try {
			group = StoreGroup.group(mapStore, new Grouper() {
				@Override
				public Pair<Controller, ? extends Iterable<Store>> getMatchingsFor(final Store container,
						final Store selectedElement) {
					String name = selectedElement.getName();
					if (name.contains(".")) {
						String suffix = Chars.rightAfter(name, ".");
						return Pair.pair((Controller) new StoreGroup.SimpleController(suffix),
								(Iterable<Store>) StoreGroup.filterByName(container, ".*\\." + Pattern.quote(suffix)));
					}
					if (name.equals("collision")) {
						return Pair.pair((Controller) new StoreGroup.SimpleController("1"), L.s(selectedElement));
					}
					return null;
				}
			});
			if (fail) {
				fail("we expected failure");
			}
		} catch (IllegalArgumentException e1) {
			if (!fail) {
				throw e1;
			}
			// no further tests needed
			return;
		}

		assertSameContent(a, StoreUtil.resolvePath(group, "1/a.1", "/"));
		assertSameContent(b, StoreUtil.resolvePath(group, "1/b.1", "/"));
		assertSameContent(c, StoreUtil.resolvePath(group, "2/c.2", "/"));
		assertSameContent(d, StoreUtil.resolvePath(group, "2/d.2", "/"));

		assertEquals(2, L.copy(group.iterator()).size());

		// TODO add test that no two matchings get the name name (from controller)
	}

	public void testStoreGrouper2() throws IOException {
		Store a = make("1");
		Store ab = make("2");
		Store abc = make("3");
		Store abd = make("4");
		Store b = make("5");
		Store bcd = make("6");
		Store bcde = make("7");
		MapStore mapStore = new MapStore();
		mapStore.setCreateMode(MapStore.CreateMode.ALLOW_AUTOMATIC);
		mapStore.add("a", a);
		mapStore.add("ab", ab);
		mapStore.add("abc", abc);
		mapStore.add("abd", abd);
		mapStore.add("b", b);
		mapStore.add("bcd", bcd);
		mapStore.add("bcde", bcde);
		Store group = null;
		group = StoreGroup.group(mapStore, StoreGroup.makePrefixLengthGrouper(2));

		assertEquals("" + group, 2 + 2, L.copy(group.iterator()).size());
		assertSameContent(a, StoreUtil.resolvePath(group, "a/a", "/"));
		assertSameContent(ab, StoreUtil.resolvePath(group, "ab/ab", "/"));
		assertSameContent(abc, StoreUtil.resolvePath(group, "ab/abc", "/"));
		assertSameContent(abd, StoreUtil.resolvePath(group, "ab/abd", "/"));
		assertSameContent(b, StoreUtil.resolvePath(group, "b/b", "/"));
		assertSameContent(bcd, StoreUtil.resolvePath(group, "bc/bcd", "/"));
		assertSameContent(bcde, StoreUtil.resolvePath(group, "bc/bcde", "/"));
	}

	public Store make(final String content) throws UnsupportedEncodingException {
		Store onlyData = new MemStore(RegionUtil.asRegion(content.getBytes("ASCII")));
		return onlyData;
	}

	protected void assertSameContent(final Store a, final Store b) throws IOException {
		assertEquals(0, RegionUtil.compare(a.asRegion(), b.asRegion()));
	}

}
