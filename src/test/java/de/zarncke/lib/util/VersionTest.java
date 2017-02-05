package de.zarncke.lib.util;

import de.zarncke.lib.err.GuardedTest;
import de.zarncke.lib.io.store.MapStore;
import de.zarncke.lib.io.store.MapStore.CreateMode;
import de.zarncke.lib.io.store.MemStore;
import de.zarncke.lib.io.store.Store;
import de.zarncke.lib.region.RegionUtil;

/**
 * Test {@link Version} parsing.
 *
 * @author Gunnar Zarncke
 */
public class VersionTest extends GuardedTest {
	public void testSimple() throws Exception {
		Store store = new MemStore(RegionUtil.asRegionUtf8("artifact=test\nversion=0.1"));
		Version v = Version.parseSingleVersion(store);
		assertEquals("test", v.getArtifact());
		assertEquals("0.1", v.getVersion());
		assertEquals(0, v.getDependentVersions().size());
	}

	public void testNested() throws Exception {
		Store config = new MemStore(RegionUtil.asRegionUtf8("artifact=parent\nversion=0.1"));

		MapStore parent = new MapStore().setCreateMode(CreateMode.ALLOW_AUTOMATIC);
		parent.add(Version.VERSION_FILE_NAME, config);
		{
			MapStore child1 = new MapStore().setCreateMode(CreateMode.ALLOW_AUTOMATIC);
			Store nested1 = new MemStore(RegionUtil.asRegionUtf8("artifact=anested\nversion=0.2"));
			child1.add(Version.VERSION_FILE_NAME, nested1);
			{
				MapStore child12 = new MapStore().setCreateMode(CreateMode.ALLOW_AUTOMATIC);
				Store nested12 = new MemStore(RegionUtil.asRegionUtf8("artifact=deep\nversion=0.3"));
				child12.add(Version.VERSION_FILE_NAME, nested12);
				child1.add("child12", child12);
			}

			parent.add("child1", child1);
		}
		{
			MapStore child2 = new MapStore().setCreateMode(CreateMode.ALLOW_AUTOMATIC);
			{
				MapStore child22 = new MapStore().setCreateMode(CreateMode.ALLOW_AUTOMATIC);
				Store nestedDouble = new MemStore(RegionUtil.asRegionUtf8("artifact=double\nversion=0.4"));
				child22.add(Version.VERSION_FILE_NAME, nestedDouble);
				child2.add("child22", child22);
			}

			parent.add("child2", child2);
		}

		Version v = Version.parse(parent);
		assertEquals("parent", v.getArtifact());
		assertEquals("0.1", v.getVersion());
		assertEquals(2, v.getDependentVersions().size());

		Version v1 = v.getDependentVersions().get(0);
		assertEquals("anested", v1.getArtifact());
		assertEquals("0.2", v1.getVersion());

		Version v12 = v1.getDependentVersions().get(0);
		assertEquals("deep", v12.getArtifact());
		assertEquals("0.3", v12.getVersion());

		Version v2 = v.getDependentVersions().get(1);
		assertEquals("double", v2.getArtifact());
		assertEquals("0.4", v2.getVersion());
	}
}
