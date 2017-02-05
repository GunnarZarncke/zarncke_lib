package de.zarncke.lib.tmpl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Map;

import de.zarncke.lib.io.IOTools;
import de.zarncke.lib.io.store.DelegateStore;
import de.zarncke.lib.io.store.Store;
import de.zarncke.lib.region.Region;
import de.zarncke.lib.region.RegionUtil;
import de.zarncke.lib.tmpl.Template.Patterns;

/**
 * A Store which templates a delegate Store.
 *
 * @author Gunnar Zarncke
 */
public class TemplatedStore extends DelegateStore {

	private final Map<String, String> values;
	private final Charset encoding;
	private final Patterns patterns;
	private Template template;
	private long lastModified;

	public TemplatedStore(final Store delegate, final Template.Patterns patterns, final Map<String, String> values,
			final Charset encoding) {
		super(delegate);
		this.values = values;
		this.encoding = encoding;
		this.patterns = patterns;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		// TODO cache the template
		Template t = getTemplate();
		for (String key : t.keySet()) {
			t.put(key, this.values.get(key));
		}
		return t.getAsRenderedInputStream(this.encoding);
	}

	@Override
	protected Store wrap(final Store element) {
		// inherit everything to children
		return new TemplatedStore(element, this.patterns, this.values, this.encoding);
	}

	public Template getTemplate() throws IOException {
		if (this.template == null || this.lastModified == Store.UNKNOWN_MODIFICATION || this.lastModified < getLastModified()) {
			this.lastModified = getLastModified();
			Region r = super.asRegion();
			ByteBuffer bb = RegionUtil.asByteBuffer(r);
			CharBuffer cb = this.encoding.decode(bb);
			this.template = new Template(cb, this.patterns);
		}
		return this.template;
	}

	@Override
	public Region asRegion() throws IOException {
		// TODO inefficient
		return RegionUtil.asRegion(IOTools.getAllBytes(getInputStream()));
	}

}
