package de.zarncke.lib.tmpl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.collect.Multimap;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.io.CompositeInputStream;

/**
 * Provides a simple templating mechanism for CharSequences. Behaves like a Map - but only with the keys defined in the
 * template.
 * An example:
 *
 * <pre>
 * hello
 * #ifdef X
 * this will be replaced
 * #endif
 * </pre>
 *
 * In this case the template will be initially render as
 *
 * <pre>
 * hello
 * this will be replaced
 * </pre>
 *
 * Note that replacements should contain a trailing newline. No nesting of replacements is supported!
 * Can be rendered efficiently into any stream. Can be copied - in which case the content will be copied. <br/>
 * For performance use a (memory mapped) CharBuffer.
 */
public class Template extends AbstractMap<String, CharSequence> {
	public interface Patterns {
		/**
		 * @return a Pattern with exactly one capturing group for the key
		 */
		Pattern getStart();

		/**
		 * @return may be null (implies empty default value)
		 */
		Pattern getEnd();
	}

	private static final Pattern PATTERN_START = Pattern.compile("^#ifdef (.*)$\n", Pattern.MULTILINE);
	private static final Pattern PATTERN_END = Pattern.compile("^#endif.*$\n", Pattern.MULTILINE);
	private static final Pattern PATTERN_SHELL = Pattern.compile("\\$\\{([^}]*)\\}");

	public static final Patterns IFDEF_PATTERN = new Patterns() {

		public Pattern getEnd() {
			return PATTERN_END;
		}

		public Pattern getStart() {
			return PATTERN_START;
		}
	};

	public static final Patterns SHELL_PATTERN = new Patterns() {

		public Pattern getEnd() {
			return null;
		}

		public Pattern getStart() {
			return PATTERN_SHELL;
		}
	};

	private Patterns patterns;
	private final CharSequence data;

	private int[] startOffsets;

	private int[] endOffsets;

	private CharSequence[] inserts;

	private Multimap<String, Integer> keyToPos = ArrayListMultimap.create();

	public Template(final CharSequence data) {
		this(data, IFDEF_PATTERN);
	}

	public Template(final CharSequence data, final Patterns patterns) {
		this.data = data;
		this.patterns = patterns;
		if (patterns.getStart().matcher("").groupCount() == 0) {
			throw Warden.spot(new IllegalArgumentException("start pattern must contain one group for the key"));
		}

		findOffsetsAndKeys();
	}

	public Template(final Template t) {
		this.data = t.data;
		this.startOffsets = t.startOffsets;
		this.endOffsets = t.endOffsets;
		this.keyToPos = t.keyToPos;
		this.inserts = t.inserts.clone(); // allow differing contents
	}

	public void renderTo(final Writer writer) throws IOException {
		int n = this.inserts.length;
		for (int i = 0; i < n; i++) {
			writer.append(this.data.subSequence(this.startOffsets[i], this.endOffsets[i]));
			if (this.inserts[i] != null) {
				writer.append(this.inserts[i]);
			}
		}
		writer.append(this.data.subSequence(this.startOffsets[n], this.endOffsets[n]));
	}

	public InputStream getAsRenderedInputStream(final Charset encoding) {
		List<CharSequence> cs = L.l();
		int n = this.inserts.length;
		for (int i = 0; i < n; i++) {
			cs.add(this.data.subSequence(this.startOffsets[i], this.endOffsets[i]));
			if (this.inserts[i] != null) {
				cs.add(this.inserts[i]);
			}
		}
		cs.add(this.data.subSequence(this.startOffsets[n], this.endOffsets[n]));

		return new CompositeInputStream(L.copy(Collections2.transform(cs, new Function<CharSequence, InputStream>() {
			public InputStream apply(final CharSequence from) {
				// if (from.length() < 10000) {
				// just convert the chunk in memory
				return new ByteArrayInputStream(from.toString().getBytes(encoding));
				// }
				// allow to stream it by using a converted buffer
				// TODO actually do the encoding in chunks
				// return new ByteBufferInputStream(encoding.encode(CharBuffer.wrap(from)));
			}
		})));
	}

	private void findOffsetsAndKeys() {
		ArrayList<Integer> soffs = new ArrayList<Integer>();
		ArrayList<Integer> eoffs = new ArrayList<Integer>();
		ArrayList<CharSequence> inss = new ArrayList<CharSequence>();

		Matcher sm = this.patterns.getStart().matcher(this.data);
		Matcher em = this.patterns.getEnd() == null ? null : this.patterns.getEnd().matcher(this.data);
		int size = 0;
		int currentPos = 0;
		while (sm.find(currentPos)) {
			int matchStartPos = sm.start();
			soffs.add(Integer.valueOf(currentPos));
			eoffs.add(Integer.valueOf(matchStartPos));
			String key = sm.group(1);
			this.keyToPos.put(key, Integer.valueOf(size));
			size++;
			int matchEnd = sm.end();
			if (em == null) {
				inss.add("");
				currentPos = matchEnd;
			} else if (em.find(matchEnd)) {
				inss.add(this.data.subSequence(matchEnd, em.start()));
				currentPos = em.end();
			} else {
				throw new IllegalArgumentException("missing " + PATTERN_END + " at " + matchEnd + " in " + this.data);
			}
		}
		soffs.add(Integer.valueOf(currentPos));
		eoffs.add(Integer.valueOf(this.data.length()));

		this.startOffsets = listToLongArray(soffs);
		this.endOffsets = listToLongArray(eoffs);
		this.inserts = inss.toArray(new CharSequence[inss.size()]);
	}

	private static int[] listToLongArray(final ArrayList<Integer> offs) {
		int[] ints = new int[offs.size()];
		for (int i = 0; i < offs.size(); i++) {
			ints[i] = offs.get(i).intValue();
		}
		return ints;
	}

	@Override
	public int size() {
		return this.inserts.length;
	}

	@Override
	public Set<Map.Entry<String, CharSequence>> entrySet() {
		Set<Map.Entry<String, CharSequence>> entries = new HashSet<Entry<String, CharSequence>>();
		for (final String key : this.keyToPos.keySet()) {
			entries.add(new Map.Entry<String, CharSequence>() {
				public String getKey() {
					return key;
				}

				public CharSequence getValue() {
					return get(key);
				}

				public CharSequence setValue(final CharSequence value) {
					return put(key, value);
				}
			});
		}
		return entries;
	}

	@Override
	public CharSequence put(final String key, final CharSequence value) {
		Collection<Integer> positions = this.keyToPos.get(key);
		if (positions.isEmpty()) {
			throw new IllegalArgumentException("unknown key " + key);
		}
		CharSequence old = null;
		for (Integer pos : positions) {
			int p = pos.intValue();
			old = this.inserts[p];
			this.inserts[p] = value;
		}
		return old;
	}

	public CharSequence get(final String key) {
		Collection<Integer> positions = this.keyToPos.get(key);
		if (positions.size() != 1) {
			throw new IllegalArgumentException("key " + key + " maps to more than one field");
		}
		return this.inserts[positions.iterator().next().intValue()];
	}
	@Override
	public void clear() {
		this.inserts = new CharSequence[this.inserts.length];
	}
}
