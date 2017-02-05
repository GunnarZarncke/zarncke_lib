package de.zarncke.lib.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import de.zarncke.lib.coll.Pair;
import de.zarncke.lib.coll.Remaining;
import de.zarncke.lib.ctx.Context;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.io.IOTools;
import de.zarncke.lib.io.StoreConsumer;
import de.zarncke.lib.io.store.Store;
import de.zarncke.lib.io.store.StreamStore;
import de.zarncke.lib.jna.LinuxFunctions;
import de.zarncke.lib.jna.WindowsFunctions;
import de.zarncke.lib.region.Region;
import de.zarncke.lib.struct.PartialOrder;
import de.zarncke.lib.value.Default;

/**
 * helper class with miscellaneous static methods.
 */
public final class Misc {
	public static final String ENVIRONMENT_VARIABLE_NAME_PREFIX = "env.";

	public static final Charset UTF_8 = Charset.forName("UTF-8");
	public static final Charset ASCII = Charset.forName("ASCII");

	public static final Object NULL = new Object();

	public static final long BYTES_PER_KB = 1024;
	public static final long BYTES_PER_MB = 1024 * BYTES_PER_KB;
	public static final long BYTES_PER_GB = 1024 * BYTES_PER_MB;

	public static final int BYTES_PER_SHORT = 2;
	public static final int BYTES_PER_INT = 4;
	public static final int BYTES_PER_LONG = 8;

	public static final int BITS_PER_BYTE = 8;
	public static final int BITS_PER_SHORT = BITS_PER_BYTE * BYTES_PER_SHORT;
	public static final int BITS_PER_INT = BITS_PER_BYTE * BYTES_PER_INT;
	public static final int BITS_PER_LONG = BITS_PER_BYTE * BYTES_PER_LONG;

	private Misc() { // prevent instances
	}

	/**
	 * Allows to override and test {@link System#getProperties() system properties} and {@link System#getenv()
	 * environment variables}.
	 * Environment variable names within this Contexts Map are prefixed with "{@value #ENVIRONMENT_VARIABLE_NAME_PREFIX}
	 * ".
	 * Note: Overriding must always return a Map<String,String>.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" /* compromise to make usable */})
	public static final Context<Map<String, String>> ENVIRONMENT = Context.of(Default.of(
			new AbstractMap<String, String>() {
				;
				@Override
				public Set<Map.Entry<String, String>> entrySet() {
					Map<String, String> properties = new HashMap<String, String>();
					populateWithDefaultVariables(properties);
					return properties.entrySet();
				}
			}, (Class<Map<String, String>>) (Class) Map.class, System.class.getName()));

	public static void populateWithDefaultVariables(final Map<String, String> properties) {
		for (Map.Entry<Object, Object> me : System.getProperties().entrySet()) {
			// Properties contract is to map Strings to Strings
			properties.put(me.getKey().toString(), me.getValue().toString());
		}
		for (Map.Entry<String, String> me : System.getenv().entrySet()) {
			properties.put(ENVIRONMENT_VARIABLE_NAME_PREFIX + me.getKey(), me.getValue());
		}
	}

	/**
	 * Memoizes the given function by caching its responses. The cache has soft keys and values.
	 *
	 * @param <A> argument
	 * @param <R> result
	 * @param function any != null
	 * @return the memoized function
	 */
	public static <A, R> Function<A, R> memoize(final Function<A, R> function) {
		return new Function<A, R>() {
			private final LoadingCache<A, R> cache = CacheBuilder.newBuilder().softValues().build(new CacheLoader<A, R>() {
				@Override
				public R load(final A key) throws Exception {
					return function.apply(key);
				}
			});

			@Override
			public R apply(final A from) {
				try {
					return this.cache.get(from);
				} catch (ExecutionException e) {
					throw Warden.spot(new IllegalArgumentException("failed on " + from, e));
				}
			}
		};
	}

	@SuppressWarnings({ "rawtypes", "unchecked"/* works for all comparables */})
	public static final Comparator COMPARABLE_COMPARATOR = new Comparator() {
		@Override
		public int compare(final Object a, final Object b) {
			return ((Comparable) a).compareTo(b);
		}
	};

	public abstract static class PartialOrderComparator implements Comparator<Object>, PartialOrder {
		@Override
		public int compare(final Object a, final Object b) {
			return lessEqual(a, b) ? lessEqual(b, a) ? 0 : -1 : 1;
		}

		@Override
		public abstract boolean lessEqual(Object a, Object b);
	}

	private static final Map<String, String> MIME_KEYS = new HashMap<String, String>(193);

	static {
		// CHECKSTYLE:OFF
		MIME_KEYS.put("*.cssl", "text/css");
		MIME_KEYS.put("*.css", "text/css");
		MIME_KEYS.put("*.html", "text/html");
		MIME_KEYS.put("*.htm", "text/html");
		MIME_KEYS.put("*.txt", "text/plain");
		MIME_KEYS.put("*.asc", "text/plain");
		MIME_KEYS.put("*.rtf", "text/rtf");
		MIME_KEYS.put("*.rtx", "text/rtf");
		MIME_KEYS.put("*.bib", "text/x-bibtex");
		MIME_KEYS.put("*.h", "text/x-c++hdr");
		MIME_KEYS.put("*.hh", "text/x-c++hdr");
		MIME_KEYS.put("*.hpp", "text/x-c++hdr");
		MIME_KEYS.put("*.c", "text/x-c++src");
		MIME_KEYS.put("*.cpp", "text/x-c++src");
		MIME_KEYS.put("*.cc", "text/x-c++src");
		MIME_KEYS.put("*.c++", "text/x-c++src");
		MIME_KEYS.put("*.csv", "text/x-csv");
		MIME_KEYS.put("*.diff", "text/x-diff");
		MIME_KEYS.put("*.patch", "text/x-diff");
		MIME_KEYS.put("*.java", "text/x-java");
		MIME_KEYS.put("*.log", "text/x-log");
		MIME_KEYS.put("*.make", "text/");
		MIME_KEYS.put("Makefile*", "text/x-makefile");
		MIME_KEYS.put("*.latex", "text/x-tex");
		MIME_KEYS.put("*.ltx", "text/x-tex");
		MIME_KEYS.put("*.sty", "text/x-tex");
		MIME_KEYS.put("*.tex", "text/x-tex");
		MIME_KEYS.put("*.xml", "text/xml");

		MIME_KEYS.put("*.gif", "image/gif");
		MIME_KEYS.put("*.jpg", "image/jpeg");
		MIME_KEYS.put("*.jpeg", "image/jpeg");
		MIME_KEYS.put("*.png", "image/png");
		MIME_KEYS.put("*.tif", "image/tiff");
		MIME_KEYS.put("*.tiff", "image/tiff");
		MIME_KEYS.put("*.bmp", "image/x-bmp");
		MIME_KEYS.put("*.eps", "image/x-eps");
		MIME_KEYS.put("*.epsi", "image/x-eps");
		MIME_KEYS.put("*.epsf", "image/x-eps");
		MIME_KEYS.put("*.svg", "image/x-svg");
		MIME_KEYS.put("*.xcf", "image/x-xcf");

		MIME_KEYS.put("*.doc", "application/msword");
		MIME_KEYS.put("*.dot", "application/msword");
		MIME_KEYS.put("*.bin", "application/octet-stream");
		MIME_KEYS.put("*.ps", "application/postscript");
		MIME_KEYS.put("*.pdf", "application/pdf");
		MIME_KEYS.put("*.bz", "application/x-bzip");
		MIME_KEYS.put("*.bz2", "application/x-bzip2");
		MIME_KEYS.put("*.dvi", "application/x-dvi");
		MIME_KEYS.put("*.exe", "application/x-executable");
		MIME_KEYS.put("*.gz", "application/x-gzip");
		MIME_KEYS.put("*.jar", "application/x-jar");
		MIME_KEYS.put("*.class", "application/x-java");
		MIME_KEYS.put("*.lha", "application/x-lha");
		MIME_KEYS.put("*.lzh", "application/x-lha");
		MIME_KEYS.put("*.o", "application/x-object");
		MIME_KEYS.put("*.pl", "application/x-perl");
		MIME_KEYS.put("*.perl", "application/x-perl");
		MIME_KEYS.put("*.rpm", "application/x-rpm");
		MIME_KEYS.put("*.spm", "application/x-rpm");
		MIME_KEYS.put("*.tar", "application/x-tar");
		MIME_KEYS.put("*.tgz", "application/x-tgz");
		MIME_KEYS.put("*.tar.gz", "application/x-tgz");
		MIME_KEYS.put("*.zip", "application/x-zip");
		// CHECKSTYLE:ON
	}

	/**
	 * a program wide global source for random numbers
	 */
	public static final java.util.Random RAND = new java.util.Random();

	public static void sleep(final long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException ie) {
			;
		}
	}

	/**
	 * equals() which supports null.
	 * Note: because an object may (but should not) return true on comparison against null we forward null comparisons.
	 *
	 * @param a may be null
	 * @param b may be null
	 * @return true if both are null or both are equals
	 */
	public static boolean equals(final Object a, final Object b) {
		// IDEA add arrayequals?
		return a == b ? true : a == null ? b == null || b.equals(null) : a.equals(b); // NOPMD
	}

	/**
	 * compare() which supports null. Null is always less then non-null value.
	 *
	 * @param <T> comparable type
	 * @param a may be null
	 * @param b may be null
	 * @return 0 if both are null or both are equals
	 */
	public static <T extends Comparable<T>> int compare(final T a, final T b) {
		if (a == null) {
			if (b == null) {
				return 0;
			}
			return -1;
		}
		if (b == null) {
			return 1;
		}
		return a.compareTo(b);

	}

	/**
	 * try a bit to establish the mime type of a file.
	 *
	 * @param filename is the name of the file, assumes "/" as separator
	 * @param data is the data of the file (may be null)
	 * @return mime type != null
	 */
	public static String getMimeTypeOf(final String filename, final Region data) {
		String name = filename;
		if (name == null) {
			throw new IllegalArgumentException("name must be given!");
		}

		// strip path part
		int p = name.lastIndexOf('/');
		if (p >= 0) {
			name = name.substring(p + 1);
		}

		// search for file name patterns
		for (Map.Entry<String, String> me : MIME_KEYS.entrySet()) {
			if (Chars.simpleMatch(name, me.getKey())) {
				return me.getValue();
			}
		}

		// without data we assume binary
		if (data == null) {
			return "application/octet-stream";
		}

		// for the rest use heuristics:
		// if first 16 characters are ascii assume text/plain
		// otherwise use application/octet-stream
		int minl = (int) Math.min(16, data.length());
		byte[] bs = data.select(0, minl).toByteArray();

		for (int i = minl - 1; i >= 0; i--) {
			byte b = bs[i];
			if (!(b == 9 || b == 10 || b == 13 || b >= 32 && b < 127)) {
				return "text/plain";
			}
		}

		return "application/octet-stream";
	}

	/**
	 * Wait for a process to terminate and return its result.
	 * Extension of {@link Process#waitFor()} with timeout option.
	 *
	 * @param process != null
	 * @param timeoutMillis >=0
	 * @return return value of null if timeout reached
	 */
	public static Integer waitFor(final Process process, final long timeoutMillis) {
		// IDEA set timer to interrupt Process.waitFor
		long start = System.currentTimeMillis();
		do {
			try {
				return Integer.valueOf(process.exitValue());
			} catch (IllegalThreadStateException e) {
				// we will wait a bit
			}
		} while (System.currentTimeMillis() - start <= timeoutMillis);

		return null;
	}

	public static Properties getProperties(final Store store) throws IOException {
		Properties p = new Properties();
		InputStream ins = store.getInputStream();
		try {
			p.load(ins);
		} finally {
			IOTools.forceClose(ins);
		}
		return p;
	}

	public static boolean even(final int n) {
		return (n & 1) == 0;
	}

	public static boolean odd(final int n) {
		return !even(n);
	}

	public static boolean even(final long n) {
		return (n & 1) == 0;
	}

	public static boolean odd(final long n) {
		return !even(n);
	}

	/**
	 * True > False.
	 *
	 * @param a
	 * @param b
	 * @return
	 */
	public static int compare(final boolean a, final boolean b) {
		return a && !b ? 1 : !a && b ? -1 : 0;
	}

	public static int compare(final long a, final long b) {
		return a > b ? 1 : a < b ? -1 : 0;
	}

	public static int compare(final double a, final double b) {
		return a > b ? 1 : a < b ? -1 : 0;
	}

	public static boolean empty(final CharSequence body) {
		return body == null || body.length() == 0;
	}

	/**
	 * @param fileOnFileSystem
	 * @return free and total
	 * @deprecated use {@link IOTools#getAvailableSystemDiskBytes} instead
	 */
	@Deprecated
	public static Pair<Long, Long> getAvailableSystemDiskBytes(final File fileOnFileSystem) {
		return IOTools.getAvailableSystemDiskBytes(fileOnFileSystem);
	}

	/**
	 * @param command to execute
	 * @param consumer to use to process output of command
	 * @return Pair of processor result and status code
	 * @throws IOException
	 */
	public static <T> Pair<T, Integer> processCommand(final String command, final StoreConsumer<T> consumer)
			throws IOException {
		StringTokenizer st = new StringTokenizer(command);
		String[] cmdarray = new String[st.countTokens()];
		for (int i = 0; st.hasMoreTokens(); i++) {
			cmdarray[i] = st.nextToken();
		}

		return processCommand(new ProcessBuilder(cmdarray), consumer);
	}

	/**
	 * @param processBuilder to use to create Process
	 * @param consumer to use to process output of command
	 * @return Pair of processor result and status code
	 * @throws IOException on failures to execute the process
	 */
	public static <T> Pair<T, Integer> processCommand(final ProcessBuilder processBuilder,
			final StoreConsumer<T> consumer) throws IOException {
		try {
			return processCommandInterruptible(processBuilder, consumer, InterruptionMode.IGNORE, true);
		} catch (Exception e) {
			throw Warden.spot(new IOException("failed to execute " + processBuilder.command(), e));
		}
	}

	/**
	 * @param processBuilder to use to create Process
	 * @param consumer to use to process output of command
	 * @param imode how to handle interruptions
	 * @param ignoreStdError true: any stderr output is ignored; false: if stderr output occurs it is made into an
	 * IllegalArgumentException
	 * @return Pair of processor result and status code
	 * @throws IOException on IO error
	 * @throws InterruptedException on interruption
	 * @throws IllegalArgumentException see ignoreStdError
	 */
	public static <T> Pair<T, Integer> processCommandInterruptible(final ProcessBuilder processBuilder,
			final StoreConsumer<T> consumer, final InterruptionMode imode, final boolean ignoreStdError)
			throws IOException, InterruptedException {
		Process proc = null;
		try {
			proc = processBuilder.start();
			InputStream ins = proc.getInputStream();
			T res = consumer == null ? null : consumer.consume(new StreamStore(ins));
			if (!ignoreStdError) {
				try {
					String error = new String(IOTools.getAllBytes(proc.getErrorStream()), Misc.UTF_8);
					if (!error.trim().isEmpty()) {
						throw Warden.spot(new IllegalArgumentException(error));
					}
				} catch (IOException e) {
					throw Warden.spot(new IOException("failed to read stderr of " + processBuilder, e));
				}
			}
			Integer statuscode;
			try {
				statuscode = Q.i(proc.waitFor());
			} catch (InterruptedException e) {
				if (imode == InterruptionMode.IGNORE) {
					Warden.disregard(e);
					statuscode = null;
				} else {
					throw Warden.spot(e);
				}
			}
			return Pair.pair(res, statuscode);
		} finally {
			if (proc != null) {
				proc.destroy();
			}
		}
	}

	/**
	 * @return total size of RAM in bytes, null if not available
	 */
	public static Long getTotalSystemRamBytes() {
		if (Misc.isWindows()) {
			return WindowsFunctions.getMeminfoWindows().getSecond();
		}
		return LinuxFunctions.getMeminfoLinux(LinuxFunctions.PROC_MEMINFO_MEM_TOTAL);
	}

	/**
	 * @return free size of RAM in bytes, null if not available
	 */
	public static Long getFreeSystemRamBytes() {
		if (Misc.isWindows()) {
			return WindowsFunctions.getMeminfoWindows().getFirst();
		}
		return LinuxFunctions.getMeminfoLinux(LinuxFunctions.PROC_MEMINFO_MEM_FREE);
	}

	public static boolean isWindows() {
		return System.getProperty("os.name").startsWith("Windows");
	}

	public static boolean isLinux() {
		return "Linux".equals(System.getProperty("os.name"));
	}

	public static String getJavaBinaryPath() {
		return System.getProperty("java.home") + File.separatorChar + "bin" + File.separatorChar + "java";
	}

	/**
	 * May or may not iterate over all elements.
	 *
	 * @param iterator to count
	 * @return number of elements in iterator
	 */
	public static int sizeOfIterator(final Iterator<?> iterator) {
		if (iterator instanceof Remaining) {
			long l = ((Remaining) iterator).available();
			if (l != Remaining.UNKNOWN) {
				return (int) l;
			}
		}
		int n = 0;
		while (iterator.hasNext()) {
			iterator.next();
			n++;
		}
		return n;
	}

	public static boolean isJava5() {
		String version = System.getProperty("java.version");
		return version.startsWith("1.5") || version.startsWith("1.6") || version.startsWith("1.7");

	}

	public static boolean isJava7() {
		return System.getProperty("java.version").startsWith("1.7");
	}

	/**
	 * From http://stackoverflow.com/questions/9399393/find-self-location-of-a-jar-file.
	 *
	 * @param classInBootJar
	 * @return
	 */
	public static File getBootJar(final Class<?> classInBootJar) {
		try {
			return new File(classInBootJar.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
		} catch (URISyntaxException e) {
			throw Warden.spot(new IllegalArgumentException("cannot get URI", e));
		}
	}

	@SuppressWarnings("rawtypes" /* null is always allowed */)
	public static final Function NULL_FUNCTION = new Function() {
		@Override
		public Object apply(@Nullable final Object from) {
			return null;
		}
	};

	/**
	 * @return a Function which return null on all calls
	 */
	@SuppressWarnings("unchecked" /* null is always allowed */)
	public static final <S, T> Function<S, T> nullFunction() {
		return NULL_FUNCTION;
	}

	/**
	 * Convenience to create a Predicate which matches against a {@link Pattern regular expression}.
	 *
	 * @param regExp to match
	 * @return Predicate matching
	 */
	@Nonnull
	public static final Predicate<String> matchPredicate(@Nonnull final String regExp) {
		return matchPredicate(Pattern.compile(regExp));
	}

	/**
	 * Convenience to create a Predicate which matches against a {@link Pattern regular expression}.
	 *
	 * @param regExp to match
	 * @return Predicate matching
	 */
	@Nonnull
	public static final Predicate<String> matchPredicate(@Nonnull final Pattern regExp) {
		return new Predicate<String>() {
			@Override
			public boolean apply(@Nullable final String input) {
				return regExp.matcher(input).matches();
			}
		};
	}
}
