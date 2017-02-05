package de.zarncke.lib.ctx;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import com.google.common.collect.MapMaker;

import de.zarncke.lib.block.StrictBlock;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.facet.MapMosaic;
import de.zarncke.lib.facet.Mosaic;
import de.zarncke.lib.value.Default;

/**
 * Provides a Context where {@link Default default values} of different kinds can be accessed.
 * The context can be
 * <ol>
 * <li>{@link #THREAD} local to the current {@link Thread}</li>
 * <li>{@link #INHERITED} local to the current thread and its children (see {@link InheritableThreadLocal})</li>
 * <li>{@link #CLASS_LOADER} local to the {@link ClassLoader} of the current {@link Thread}</li>
 * <li>{@link #GLOBAL} global to the application (effectively only local to the ClassLoader or the Context class).</li>
 * <li>local to the library (the {@link Default} supplied in the Context definition)</li>
 * </ol>
 * <br/>
 * Contexts are querried in this order until a value if found. <br/>
 * Usage:
 *
 * <pre>
 * // define a context which provides a Clock (the JavaClock by default).
 * private static final Context&lt;Clock&gt; CLOCK_SOURCE = Context.of(Default.of(new JavaClock(), Clock.class));
 *
 * // a test method, which if called returns the &quot;current time&quot; (by default the above Java time).
 * private static void test() {
 * 	System.out.println(CLOCK_SOURCE.get().getCurrentTimeMillis() + &quot;ms&quot;);
 * }
 *
 * // this main method supplies a specific Clock (one always returning the same supplied value).
 * public static void main(final String[] args) {
 * 	Context.runWith(new Runnable() {
 * 		public void run() {
 * 			test();
 * 		}
 * 	}, Default.of(new TestClock(0), Clock.class));
 * }
 * </pre>
 *
 * If the coding needing the value querries it with {@link Context#get()} the value is looked up as follows:
 * <ol>
 * <li>The current threads value is querried - if one is available it is returned,</li>
 * <li>the current threads inherited value is querried - if one it availale it is returned,</li>
 * <li>the default value is returned.</li>
 * </ol>
 * This is useful for
 * <ul>
 * <li>writing generic library code which should run anywhere (using a default implementation),</li>
 * <li>writing tests where test implementations are supplied,</li>
 * <li>passing/setting the specific value is not practical (too many chains of possibly foreign functions),</li>
 * <li>a configuration framework is not necessary, available or wanted and</li>
 * <li>when the default implementation is seldom overridden.</li>
 * </ul>
 * Basically this differs from a ThreadLocal in the following ways:
 * <ul>
 * <li>Simpler to write than a ThreadLocal (no need to provide an initialValue() method).</li>
 * <li>No coupling of the user (who defined the Context/ThreadLocal) with the provider (who'd need the ThreadLocal to
 * set the value).</li>
 * <li>Advanced {@link Context.Scope scopes} under a uniform hood.</li>
 * <li>Different default values (=Context objects) for the same type (Class) in different places possible.</li>
 * </ul>
 *
 * @author Gunnar Zarncke
 * @param <T> type of value
 */
public final class Context<T> implements Serializable {
	private static final long serialVersionUID = 1L;

	// TODO add permission checking against the SecurityManager
	// IDEA consider CLASS_LOADER_CHAIN for checking parent class loaders
	// IDEA consider setting all super classes/interfaces too
	// problems:
	// - requires unsetting those that were set
	// - some super interfaces might sensibly require qualifiers
	// - add flag to enable this extension

	// IDEA consider adding pluggable scopes e.g. for session (requires e.g. a Context<Scope[]>)

	/**
	 * Here we keep data.
	 */
	private static final class Data extends MapMosaic {
		private static final long serialVersionUID = 1L;

		public Data() {
			super();
		}

		public Data(final Mosaic parentValue) {
			super(parentValue);
		}

		@Override
		protected <U> Object initializeFor(final Default<U> spec) {
			return null;
		}
	}

	/**
	 * A visibility scope of changes to the value of this Context.
	 * The get method can be used to access values in this scope. This usage is not recommended.
	 */
	public abstract static class Scope {

		public abstract Data get();
	}

	/**
	 * Globally visible.
	 * Use this scope only if you want to coordinate across multiple class loaders but cannot control the main Thread <br/>
	 * Example: A web application where the application server calls your multiple web applications by different
	 * Threads. <br/>
	 * Sufficient {@link java.security.Permission Permissions} are required. <br/>
	 * Be careful with String or other simple values. Use {@link Default#of(String, String)} with a class name
	 * qualifier.
	 */
	public static final Scope GLOBAL = new Scope() {

		@Override
		public Data get() {
			return globalValues;
		}

		@Override
		public String toString() {
			return "GLOBAL";
		}
	};

	protected static final Data NULL_CLASS_LOADER_DATA = new Data(); // NOPMD efficient access

	/**
	 * Visible in the application loaded by the {@link Thread#getContextClassLoader() current ClassLoader}.
	 * Use this scope only if your cannot control the main Thread but must coordinate values across multiple Thread.
	 * Example: A web application where the application server calls you by different Threads. <br/>
	 * Be careful with String or other simple values. Use {@link Default#of(String, String)} with a class name
	 * qualifier.
	 */
	public static final Scope CLASS_LOADER = new Scope() {

		@Override
		public Data get() {
			final ClassLoader cl = Thread.currentThread().getContextClassLoader();
			if (cl == null) {
				return NULL_CLASS_LOADER_DATA;
			}
			Data mosaic = classLoaderValues.get(cl);
			if (mosaic == null) {
				mosaic = new Data();
				classLoaderValues.put(cl, mosaic);
			}
			return mosaic;
		}

		@Override
		public String toString() {
			return "CLASS_LOADER";
		}
	};

	/**
	 * Visible in the current Thread and all its child Threads.
	 * This is the most common use:
	 * <ul>
	 * <li>in main() method: set Contexts that should be available to the whole application. Example: Logging.</li>
	 * <li>in run() method of new Thread: to supply a Context for the operations executed by the Thread</li>
	 * </ul>
	 */
	public static final Scope INHERITED = new Scope() {

		@Override
		public Data get() {
			Data mosaic = inheritableThreadLocalValues.get();
			if (mosaic == null) {
				mosaic = new Data();
				inheritableThreadLocalValues.set(mosaic);
			}
			return mosaic;
		}

		@Override
		public String toString() {
			return "INHERITED";
		}
	};

	/**
	 * Visible in the current Thread alone.
	 * This is the use case for smaller changes:
	 * <ul>
	 * <li>in servlet filters: set Contexts that should be available to the current request. Example: HttpRequest.</li>
	 * <li>in service methods: to set transation and persistence Contexts that should be available to the current
	 * Thread.</li>
	 * <li>in test methods: to supply a Context for the code under test</li>
	 * </ul>
	 */
	public static final Scope THREAD = new Scope() {

		@Override
		public Data get() {
			Data mosaic = threadLocalValues.get();
			if (mosaic == null) {
				mosaic = new Data();
				threadLocalValues.set(mosaic);
			}
			return mosaic;
		}

		@Override
		public String toString() {
			return "THREAD";
		}
	};

	// TODO Data is not thread safe!
	private static Data globalValues = new Data();
	private static Map<ClassLoader, Data> classLoaderValues = new MapMaker().weakKeys().makeMap();
	private static ThreadLocal<Data> inheritableThreadLocalValues = new InheritableThreadLocal<Data>() {
		@Override
		protected Data childValue(final Data parentValue) {
			return new Data(parentValue);
		}
	};
	private static ThreadLocal<Data> threadLocalValues = new ThreadLocal<Data>();

	private final Default<T> defaulter;

	private Context(final Default<T> defaulter) {
		this.defaulter = defaulter;
	}

	/**
	 * @return the current value of this context (respects scopes), null if no value (not even a default) is set
	 */
	public T get() {
		return get(this.defaulter);
	}

	/**
	 * @return the current value of this context (respects scopes) != null
	 * @throws InsufficientContextException if no non-null value is set for this context, contains detailed context info
	 */
	@Nonnull
	public T getChecked() {
		final T t = get(this.defaulter);
		if (t == null) {
			throw Warden.spot(new InsufficientContextException("No value configured for " + this + " in "
					+ currentContextReport()));
		}
		return t;
	}

	// TODO Consider going up the super class chain when searching for a value.
	private static <T> T get(final Default<T> def) {
		T v = get(def, THREAD);
		if (v != null) {
			return v;
		}
		v = get(def, INHERITED);
		if (v != null) {
			return v;
		}
		v = get(def, CLASS_LOADER);
		if (v != null) {
			return v;
		}
		v = get(def, GLOBAL);
		if (v != null) {
			return v;
		}
		return def.getValue();
	}

	private static <T> T get(final Default<T> def, final Scope scope) {
		return scope.get().get(def);
	}

	private static <T> void set(final Default<T> def, final T value, final Scope scope) {
		scope.get().set(def, value);
	}

	/**
	 * Create context with the given default value.
	 *
	 * @param <T> type of value
	 * @param value to use as default if none supplied by context
	 * @return Context of T
	 */
	public static <T> Context<T> of(final Default<T> value) {
		return new Context<T>(value);
	}

	/**
	 * Create context with the given value and type as default.
	 * Convenience for "normal" default.
	 * Same as <code>{@link #of(Default) of}({@link Default#of(Object, Class) Default.of}(value, type)</code>.
	 *
	 * @param <T> type of value
	 * @param value to use as default if none supplied by context
	 * @param type of value
	 * @return Context of T
	 */
	public static <T> Context<T> of(final T value, final Class<T> type) {
		return of(Default.of(value, type));
	}

	/**
	 * A Context which provides a null value for the given class..
	 *
	 * @param <T> type of value
	 * @param clazz of the null value
	 * @return Context of T
	 */
	public static <T> Context<T> of(final Class<T> clazz) {
		return of(Default.of((T) null, clazz));
	}

	/**
	 * Put the given value into the Context.
	 * Note: Avoid using this method where possible. Use {@link Context#runWith(StrictBlock, Default...)} instead
	 * because it ensures cleanup.
	 * A sanity check is done when setting GLOBAL bindings. No existing bindings in other scopes may exist.
	 * 
	 * @param <T> type of value
	 * @param value to put in context
	 * @param scope see {@link Scope Scopes} for details
	 * @return the previous Default as seen by this thread
	 */
	public static <T> Default<T> setFromNowOn(final Default<T> value, final Scope scope) {
		assert value != null;

		if (scope == GLOBAL) {
			ensureNoBindingInScope(THREAD, value);
			ensureNoBindingInScope(INHERITED, value);
			ensureNoBindingInScope(CLASS_LOADER, value);
		}

		final T previous = get(value, scope);

		set(value, value.getValue(), scope);
		return Default.of(previous, value.getType());
	}

	public static <T> void ensureNoBindingInScope(final Scope scope, final Default<T> value) {
		T prev = scope.get().get(value);
		if (prev != null) {
			throw Warden.spot(new IllegalStateException("setting " + value + " will have no effect while binding "
					+ prev + " in scope " + scope + " is set!"));
		}
	}

	/**
	 * Run {@link Runnable} with the given value in its context.
	 * The value is not inherited to child Threads.
	 *
	 * @param runnable to execute with the default value in its context
	 * @param values to provide in the context
	 * @return passed thru result
	 * @param <T> type of result
	 */
	public static <T> T runWith(final StrictBlock<T> runnable, final Default<?>... values) {
		return runWith(runnable, THREAD, values);
	}

	/**
	 * Run {@link Runnable} with the given value in its context.
	 *
	 * @param runnable to execute with the default value in its context
	 * @param scope see {@link Scope} for details
	 * @param values to provide in the context of that scope
	 * @return passed thru result
	 * @param <T> type of result
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" } /* we know that the type matches, but cannot tell Java */)
	public static <T> T runWith(final StrictBlock<T> runnable, final Scope scope, final Default... values) {
		// avoid repeated lookup of Data
		final Object[] olds = new Object[values.length];
		for (int i = 0; i < values.length; i++) {
			olds[i] = get(values[i], scope);
			set(values[i], values[i].getValue(), scope);
		}

		try {
			return runnable.execute();
		} finally {
			for (int i = 0; i < values.length; i++) {
				set(values[i], olds[i], scope);
			}
		}
	}

	/**
	 * Provides a Default array which contains the complete currently known Context.
	 * Useful for speeding up Context access by taking this context at some entry point and setting it as the current
	 * {@link #THREAD thread-context} (which is checked first). Also useful for diagnostic output.
	 * See also {@link #runWithOptimizedContext(StrictBlock)}.
	 *
	 * @param additionalValues optional additional Defaults to include
	 * @return Default array != null, may be empty, the order is unspecified but basically goes from general to specific
	 * entries
	 */
	public static Default<?>[] bundleCurrentContext(final Default<?>... additionalValues) {
		final Map<Default<?>, Object> contextToAggregate = new LinkedHashMap<Default<?>, Object>(GLOBAL.get());
		contextToAggregate.putAll(CLASS_LOADER.get());
		contextToAggregate.putAll(INHERITED.get());
		contextToAggregate.putAll(THREAD.get());
		for (final Default<?> d : additionalValues) {
			contextToAggregate.put(d, d.getValue());
		}

		final Default<?>[] defs = new Default[contextToAggregate.size()];
		int i = 0;
		for (final Map.Entry<Default<?>, Object> me : contextToAggregate.entrySet()) {
			defs[i++] = ((Default<Object>) me.getKey()).withOtherValue(me.getValue());
		}
		return defs;
	}

	public static String currentContextReport() {
		return Arrays.asList(bundleCurrentContext()).toString();
	}

	/**
	 * Takes the {@link #bundleCurrentContext(Default...) current context} and runs the block with it.
	 * Advantage: Context access is a bit faster.<br/>
	 * Disadvantage: changes to {@link Context.Scope scopes} other than {@link Context#THREAD} set <em>within</em> the
	 * block
	 * will not be seen <em>within</em> the block.<br7>
	 * May be used at a central entry point where are "global" context has been set.
	 *
	 * @param <T> result
	 * @param block to run != null
	 * @return result of the block
	 */
	public static <T> T runWithOptimizedContext(final StrictBlock<T> block) {
		return runWith(block, Context.bundleCurrentContext());
	}

	public static <T> StrictBlock<T> wrapInOptimizedContext(final StrictBlock<T> block) {
		return new StrictBlock<T>() {
			@Override
			public T execute() {
				return runWith(block, Context.bundleCurrentContext());
			}
		};
	}

	/**
	 * Wraps the given {@link Runnable} such that it is run with the given context.
	 *
	 * @param <T> type of result
	 * @param runnable to execute
	 * @param scope see {@link Scope}
	 * @param values to provide in context
	 * @return Runnable a wrapped call to {@link #runWith}
	 */
	public static <T> StrictBlock<T> wrapInContext(final StrictBlock<T> runnable, final Scope scope,
			final Default<?>... values) {
		return new StrictBlock<T>() {
			@Override
			public T execute() {
				return runWith(runnable, scope, values);
			}
		};
	}

	/**
	 * Poll the current number of contexts.
	 * This is meant as a monitoring method and may take some time/give inaccurate results.
	 *
	 * @return int
	 */
	public static int totalNumberOfContexts() {
		int size = globalValues.size();
		for (final Data clv : classLoaderValues.values()) {
			size += clv.size();
		}
		// ThreadGroup root = Thread.currentThread().getThreadGroup();
		// while(root.getParent()!=null)root = root.getParent();
		// Thread[] elems = new Thread[1000];
		// root.enumerate(elems, true);
		// for(Thread t : elems)
		// TODO how to count the size of *all* the ThreadLocals?
		size += threadLocalValues.get().size();
		size += inheritableThreadLocalValues.get().size();
		return size;
	}

	@Override
	public String toString() {
		return "context for " + this.defaulter.getType();
	}

	public Default<T> getOtherDefault(final T value) {
		return this.defaulter.withOtherValue(value);
	}
}
