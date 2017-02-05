package de.zarncke.lib.init;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;

import org.junit.Assert;

import de.zarncke.lib.io.store.RegionJar;
import de.zarncke.lib.io.store.Store;
import de.zarncke.lib.lang.InfoClassLoader;
import de.zarncke.lib.lang.ListClassLoader;
import de.zarncke.lib.lang.StoreClassLoader;

public class Boot {
	private final Map<String, InfoClassLoader> knownArtifacts = new HashMap<String, InfoClassLoader>();

	private final Store rootStore;

	Boot(final Store rootStore) {
		this.rootStore = rootStore;
	}

	public InfoClassLoader boot(final Store bootStore, final boolean runMain, final ClassLoader parentClassLoader)
			throws Throwable {
		InfoClassLoader bootClassLoader = new StoreClassLoader(parentClassLoader, bootStore);

		Store manifest = bootStore.element("META-INF").element("MANIFEST.MF");
		if (!manifest.canRead()) {
			// without a manifest -> just return the archive as store
			return bootClassLoader;
		}

		// check exists
		InputStream ins = manifest.getInputStream();
		Manifest m;
		try {
			m = new Manifest(ins);
		} finally {
			ins.close();
		}
		String classpath = m.getMainAttributes().getValue("Class-Path").toString();

		List<InfoClassLoader> loaders = new ArrayList<InfoClassLoader>();
		loaders.add(bootClassLoader);

		for (String dependency : classpath.split(" ")) {
			InfoClassLoader depLoader;
			Store dep = bootStore.element(dependency);
			// if(dep.getName().endsWith(".jar")||dep.getName().endsWith(".zip"))
			if (dep.canRead()) {
				RegionJar rj = new RegionJar();
				rj.init(dep.asRegion());
				depLoader = boot(rj.getStore(), false, parentClassLoader);
			} else {
				depLoader = find(dependency);
			}
			loaders.add(depLoader);
		}

		// TODO actually construct the loader
		InfoClassLoader mainClassLoader = new ListClassLoader(loaders, null);

		if (runMain) {
			try {
				String main = m.getMainAttributes().getValue("Main-Class").toString();
				Class<?> mainClass = mainClassLoader.loadClass(main);
				if (mainClass.getClassLoader() == Boot.class.getClassLoader()) {
					Assert.fail("mainClass should be loaded by different classloader.");
				}
				Method mainMethod = mainClass.getMethod("main", new Class[] { String[].class });
				mainMethod.invoke(null, new Object[] { new String[0] });
			} catch (InvocationTargetException e) {
				throw e.getCause();
			} catch (Exception e) {
				throw new Exception("running failed", e);
			}
		}

		return mainClassLoader;
	}

	private InfoClassLoader find(final String dependency) throws IOException {
		InfoClassLoader cand = this.knownArtifacts.get(dependency);
		if (cand != null) {
			return cand;
		}
		Store dep = this.rootStore.element(dependency);
		if (!dep.canRead()) {
			throw new IllegalArgumentException("cannot resolve dependency " + dependency);
		}
		RegionJar rj = new RegionJar();
		rj.init(dep.asRegion());
		cand = new StoreClassLoader(null, rj.getStore());
		this.knownArtifacts.put(dependency, cand);
		return cand;
	}
}
