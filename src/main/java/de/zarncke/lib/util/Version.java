package de.zarncke.lib.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.ctx.Context;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.io.IOTools;
import de.zarncke.lib.io.store.ResourceStore;
import de.zarncke.lib.io.store.Store;
import de.zarncke.lib.value.Default;

/**
 * Encapsulates version information about a build artifact.
 * Can be {@link #parse(Store) read} from a file or directory tree.
 *
 * @author Gunnar Zarncke
 */
public class Version implements Serializable {
	public static final Context<Version> CTX = Context.of(Default.of(defVersion(), Version.class));

	private static final long serialVersionUID = 1L;
	public static final String VERSION_FILE_NAME = "version.properties";
	public static final List<String> EXCLUDED_PATHS = L.l(VERSION_FILE_NAME, "src", "target", "classes");

	public static final String KEY_DESCRIPTION = "description";
	public static final String KEY_DATE = "date";
	public static final String KEY_NAME = "name";
	public static final String KEY_URL = "url";
	public static final String KEY_PATH = "path";
	public static final String KEY_CLASSIFIER = "classifier";
	public static final String KEY_REVISION = "revision";
	public static final String KEY_VERSION = "version";
	public static final String KEY_GROUP = "group";
	public static final String KEY_ARTIFACT = "artifact";

	public static final Comparator<Version> VERSION_BY_NAME = new Comparator<Version>() {
		@Override
		public int compare(final Version o1, final Version o2) {
			return (o1.getGroup() + " " + o1.getArtifact()).compareTo(o2.getGroup() + " " + o2.getArtifact());
		}
	};

	private String group;
	private String artifact;
	private String version;
	private String revision;
	private String classifier;
	private String date;
	private String url;
	private String path;
	private String name;
	private String description;
	private String metaInfo;

	private List<Version> dependentVersions = L.l();

	public String getGroup() {
		return this.group;
	}

	private static Version defVersion() {
		try {
			Store defVersionStore = new ResourceStore(Version.class, "/version.properties");
			if (defVersionStore.canRead()) {
				return parseSingleVersion(defVersionStore);
			}
			return null;
		} catch (IOException e) {
			return null;
		}
	}

	public String getArtifact() {
		return this.artifact;
	}

	public String getVersion() {
		return this.version;
	}

	public String getRevision() {
		return this.revision;
	}

	public String getClassifier() {
		return this.classifier;
	}

	public String getUrl() {
		return this.url;
	}

	public String getPath() {
		return this.path;
	}

	public List<Version> getDependentVersions() {
		return this.dependentVersions;
	}

	public void setGroup(final String group) {
		this.group = group;
	}

	public void setArtifact(final String artifact) {
		this.artifact = artifact;
	}

	public void setVersion(final String version) {
		this.version = version;
	}

	public void setRevision(final String revision) {
		this.revision = revision;
	}

	public void setClassifier(final String classifier) {
		this.classifier = classifier;
	}

	public void setUrl(final String url) {
		this.url = url;
	}

	public void setPath(final String path) {
		this.path = path;
	}

	public void setDependentVersions(final List<Version> dependentVersions) {
		this.dependentVersions = dependentVersions;
	}

	/**
	 * Parses a Store for version information.
	 * If the Store is readable, then it is read as properties.
	 * Otherwise it is recursively searched for {@value Version#VERSION_FILE_NAME}-files and an aggregated
	 * {@link Version} is
	 * returned.
	 * Notes:
	 * The {@link #dependentVersions} are sorted {@link #VERSION_BY_NAME by name}.
	 * A synthetic to-level Version may be created.
	 *
	 * @param store != null
	 * @return Version != null
	 * @throws IOException
	 */
	public static Version parse(final Store store) throws IOException {
		List<Version> vs = parseDirectory(store);
		if (vs.isEmpty()) {
			throw Warden.spot(new IOException("no versions found in " + store));
		}
		if (vs.size() == 1) {
			return vs.get(0);
		}
		Version v = new Version();
		v.setDescription("top level version object unknown");
		v.setDependentVersions(vs);
		return v;
	}

	private static List<Version> parseDirectory(final Store store) throws IOException {
		if (store.iterationSupported()) {
			Store props = store.element(VERSION_FILE_NAME);
			List<Version> vs = L.l();
			for (Store s : store) {
				if (EXCLUDED_PATHS.contains(s.getName())) {
					continue;
				}
				vs.addAll(parseDirectory(s));
			}
			Collections.sort(vs, VERSION_BY_NAME);
			if (!props.exists()) {
				return vs;
			}
			Version v = parseSingleVersion(props);
			v.setDependentVersions(vs);
			return L.s(v);
		} else if (store.canRead()) {
			return L.s(parseSingleVersion(store));
		}
		return L.e();
	}

	public static Version parseSingleVersion(final Store store) throws IOException {
		Version v = new Version();
		if (!store.canRead()) {
			throw Warden.spot(new IOException("cannot read " + store));
		}
		Properties p = new Properties();
		InputStream ins = store.getInputStream();
		try {
			p.load(ins);
		} finally {
			IOTools.forceClose(ins);
		}

		v.setArtifact(p.getProperty(KEY_ARTIFACT));
		v.setGroup(p.getProperty(KEY_GROUP));
		v.setVersion(p.getProperty(KEY_VERSION));
		v.setRevision(p.getProperty(KEY_REVISION));
		v.setClassifier(p.getProperty(KEY_CLASSIFIER));
		v.setDate(p.getProperty(KEY_DATE));
		v.setPath(p.getProperty(KEY_PATH));
		v.setUrl(p.getProperty(KEY_URL));
		v.setName(p.getProperty(KEY_NAME));
		v.setDescription(p.getProperty(KEY_DESCRIPTION));

		List<Version> dependent = L.l();
		v.setDependentVersions(dependent);

		return v;
	}

	@Override
	public String toString() {
		return getGroup() + "." + getArtifact() + "@" + getVersion() + ":" + getRevision();
	}

	public String toDetailedInfo() {
		return getGroup() + "." + getArtifact() + "\n" //
				+ "version: " + getVersion() + "." + getRevision() + "\n"//
				+ "date: " + getDate() + "\n" //
				+ "name: " + getName() + "\n" //
				+ "url: " + getUrl() + "\n" //
				+ "path: " + getPath() + "\n" //
				+ "description: " + getDescription() + "\n"//
				+ "meta-info: " + getMetaInfo() + "\n";
	}

	public String getName() {
		return this.name;
	}

	public String getDescription() {
		return this.description;
	}

	public String getMetaInfo() {
		return this.metaInfo;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public void setMetaInfo(final String metaInfo) {
		this.metaInfo = metaInfo;
	}

	public String getDate() {
		return this.date;
	}

	public void setDate(final String date) {
		this.date = date;
	}
}
