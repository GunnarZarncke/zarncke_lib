package de.zarncke.lib.ctx;

import org.junit.Assert;
import org.junit.Test;

import de.zarncke.lib.block.ABlock;
import de.zarncke.lib.block.Running;
import de.zarncke.lib.coll.L;
import de.zarncke.lib.err.ErrorCollector;
import de.zarncke.lib.value.Default;

// note: may not inherit from GuardedTest* because that sets its own contexts
public class ContextTest {
	class T {
		int v;

		public T(final int i) {
			this.v = i;
		}
	}

	class S extends T {
		public S(final int i) {
			super(i);
		}
	}

	@Test
	public void testGlobalContext() throws ErrorCollector {
		final ErrorCollector ec = new ErrorCollector();

		String qualifier = ContextTest.class.getName() + ".test";
		final Context<String> gctx = Context.of(Default.of("test", qualifier));
		Assert.assertEquals(gctx.get(), "test");
		Context.runWith(new Running() {
			@Override
			public void run() {
				try {
					Assert.assertEquals(gctx.get(), "hallo");
				} catch (Exception e) {
					ec.add(e);
				}
			}
		}, Context.GLOBAL, Default.of("hallo", qualifier));
		ec.throwIfAny();

		Assert.assertEquals(gctx.get(), "test");
	}

	@Test
	public void testThreadContexts() throws InterruptedException, ErrorCollector {
		final ErrorCollector ec = new ErrorCollector();

		final Default<T> t1 = Default.of(new T(1), T.class);
		final Context<T> tctx = Context.of(t1);
		final Default<S> s11 = Default.of(new S(11), S.class);
		final Context<S> sctx = Context.of(s11);
		Assert.assertEquals(tctx.get().v, 1);
		Assert.assertEquals(sctx.get().v, 11);

		final Thread[] subThread = new Thread[1];

		// call with t=2 (inheritable) and s=22 (not inheritable)
		Context.runWith(new Running() {
			@Override
			public void run() {
				try {
					Assert.assertEquals(tctx.get().v, 2);
					Assert.assertEquals(sctx.get().v, 11); // still

					Context.runWith(new Running() {
						@Override
						public void run() {
							Assert.assertEquals(tctx.get().v, 2);
							Assert.assertEquals(sctx.get().v, 22);

							Default<?>[] curr = Context.bundleCurrentContext();
							Assert.assertEquals("is:" + L.l(curr), 2, curr.length);
							Assert.assertEquals(t1, curr[0]);
							Assert.assertEquals(s11, curr[1]);

							// create it here - it should inherit tctx
							subThread[0] = createSubThread(ec, tctx, sctx);
							subThread[0].start();
						}
					}, Context.THREAD, Default.of(new S(22), S.class));
				} catch (Throwable e) { // NOPMD
					ec.add(e);
					if (e instanceof Error) {
						throw (Error) e;
					}
				}
			}
		}, Context.INHERITED, Default.of(new T(2), T.class));
		ec.throwIfAny();

		Assert.assertEquals(tctx.get().v, 1);
		Assert.assertEquals(sctx.get().v, 11);
		subThread[0].join();
		ec.throwIfAny();
		Assert.assertEquals(tctx.get().v, 1);
		Assert.assertEquals(sctx.get().v, 11);
	}

	private Thread createSubThread(final ErrorCollector ec, final Context<T> tctx, final Context<S> sctx) {
		// this thread runs second so these assertions are checked at the earliest after start() below
		return new Thread() {
			@Override
			public void run() {
				try {
					Assert.assertEquals(tctx.get().v, 2); // inherited 2
					Assert.assertEquals(sctx.get().v, 11); // unchanged

					Context.runWith(new Running() {
						@Override
						public void run() {
							Assert.assertEquals(tctx.get().v, 3);
							Assert.assertEquals(sctx.get().v, 11); // unchanged still
						}
					}, Default.of(new T(3), T.class));

					Assert.assertEquals(tctx.get().v, 2);
				} catch (Throwable e) { // NOPMD
					ec.add(e);
					if (e instanceof Error) {
						throw (Error) e;
					}
				}
			}
		};
	}

	/**
	 * A clsas loader which does nothing but provides another scope.
	 */
	private static class DummyClassLoader extends ClassLoader {
		private DummyClassLoader(final ClassLoader parent) {
			super(parent);
		}
	}

	@Test
	public void testContextWithClassLoading() throws ErrorCollector {
		final ErrorCollector ec = new ErrorCollector();

		final Context<T> tctx = Context.of(Default.of(new T(11), T.class));
		final Context<S> sctx = Context.of(Default.of(new S(111), S.class));
		Assert.assertEquals(tctx.get().v, 11);
		Assert.assertEquals(sctx.get().v, 111);

		Context.runWith(new Running() {
			@Override
			public void run() {
				try {
					Assert.assertEquals(tctx.get().v, 12);
					Assert.assertEquals(sctx.get().v, 111);

					Thread t = new Thread(ABlock.toRunnable(Context.wrapInContext(new Running() {
						@Override
						public void run() {
							try {
								Assert.assertEquals(tctx.get().v, 11); // outer class loader value
								Assert.assertEquals(sctx.get().v, 112); // still
							} catch (Throwable e) { // NOPMD
								ec.add(e);
								if (e instanceof Error) {
									throw (Error) e;
								}
							}
						}
					}, Context.CLASS_LOADER, Default.of(new S(112), S.class))));

					t.setContextClassLoader(new DummyClassLoader(ContextTest.class.getClassLoader()));
					t.start();
					t.join();
					Assert.assertEquals(tctx.get().v, 12); //
					Assert.assertEquals(sctx.get().v, 111); //

				} catch (Throwable e) { // NOPMD
					ec.add(e);
					if (e instanceof Error) {
						throw (Error) e;
					}
				}
			}
		}, Context.CLASS_LOADER, Default.of(new T(12), T.class));

		ec.throwIfAny();

		Assert.assertEquals(tctx.get().v, 11);
		Assert.assertEquals(sctx.get().v, 111);

	}

	@Test
	public void testContextScopeOverriding() throws ErrorCollector {
		final ErrorCollector ec = new ErrorCollector();

		final String qualifier = ContextTest.class.getName() + ".test";
		final Context<String> gctx = Context.of(Default.of("test", qualifier));
		Assert.assertEquals(gctx.get(), "test");
		Context.runWith(new Running() {
			@Override
			public void run() {
				Context.runWith(new Running() {
					@Override
					public void run() {
						Assert.assertEquals(gctx.get(), "inner");
					}
				}, Context.THREAD, Default.of("inner", qualifier));

				Assert.assertEquals(gctx.get(), "hallo");
			}
		}, Context.GLOBAL, Default.of("hallo", qualifier));
		ec.throwIfAny();

		Assert.assertEquals(gctx.get(), "test");
	}

}
