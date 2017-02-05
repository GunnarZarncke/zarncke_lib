package de.zarncke.lib.io;

import java.io.File;
import java.util.regex.Pattern;

import de.zarncke.lib.ctx.Context;
import de.zarncke.lib.io.store.Store;
import de.zarncke.lib.io.store.StoreUtil;
import de.zarncke.lib.util.Misc;
import de.zarncke.lib.value.Default;

/**
 * Represents a sequence of file specifications
 *
 * @author Gunnar Zarncke
 */
public class Path {
	public static final String ENV_PATH_KEY = "env.PATH";
	public static class SysPath extends Path {

		@Override
		public String[] getElements() {
			String path = Misc.ENVIRONMENT.get().get(ENV_PATH_KEY);
			return path.split(File.pathSeparator, -1);
		}
	}

	public static final Context<Path> CTX = Context.of(Default.of(new SysPath(), Path.class));
	private final String[] elements;

	public static Path of(final String... elements) {
		return new Path(elements);
	}

	protected Path(final String... elements) {
		this.elements = elements.clone();
	}

	public String[] getElements() {
		return this.elements;
	}

	public File locate(final String binary) {
		for (String path : this.elements) {
			File ref = new File(path, binary);
			if (ref.exists()) {
				return ref;
			}
		}
		return null;
	}

	public Store locate(final Store root, final String binary) {
		for (String path : this.elements) {
			Store s = StoreUtil.resolvePath(root, path, Pattern.quote(File.separator));
			if (s.element(binary).exists()) {
				return s;
			}
		}
		return null;
	}
}