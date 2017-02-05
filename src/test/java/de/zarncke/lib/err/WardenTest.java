package de.zarncke.lib.err;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.LinkedList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import de.zarncke.lib.block.ABlock;
import de.zarncke.lib.block.Running;
import de.zarncke.lib.coll.Elements;
import de.zarncke.lib.ctx.Context;
import de.zarncke.lib.log.Log;
import de.zarncke.lib.value.Default;

/**
 * Test Warden.
 */
public class WardenTest {

	private static final class CaptureLog implements Log {
		private final List<Throwable> caught = new LinkedList<Throwable>();

		public List<Throwable> getCaught() {
			return this.caught;
		}

		@Override
		public void report(final Throwable throwableToReport) {
			this.caught.add(throwableToReport);
		}

		@Override
		public void report(final CharSequence issue) {
			if (issue.toString().startsWith("disregarding")) {
				return;
			}
			Assert.fail(issue.toString());
		}

		@Override
		public void report(final Object... debugObject) {
			Assert.fail("unexpected " + Elements.toString(debugObject));
		}
	}

	@Test
	public void testWardenSpotted() {
		final CaptureLog cl = new CaptureLog();
		Context.runWith(new Running() {
			@Override
			public void run() {
				Exception e = new Exception();
				Warden.spot(e);
			}
		}, Default.of(cl, Log.class));
		// should now be reported
		Assert.assertEquals(cl.getCaught().size(), 1);
	}

	@Test
	public void testWardenGuarded() {
		final CaptureLog cl = new CaptureLog();
		Context.runWith(new Running() {
			@Override
			public void run() {
				List<Throwable> unhandled = Warden.guard(new Running() {
					@Override
					public void run() {
						Exception e = new Exception();
						Warden.spot(e);

						Assert.assertTrue(cl.getCaught().isEmpty());
					}
				});
				// should now be handled
				Assert.assertEquals(1, unhandled.size());

			}
		}, Default.of(cl, Log.class));
		// should now be reported
		Assert.assertEquals(1, cl.getCaught().size());
		Assert.assertTrue(cl.getCaught().get(0) + " must be UnhandledException",
				cl.getCaught().get(0) instanceof UnhandledException);
	}

	@Test
	public void testWardenDisregarded() {
		final CaptureLog cl = new CaptureLog();
		Context.runWith(new Running() {
			@Override
			public void run() {
				Warden.guard(new Running() {
					@Override
					public void run() {
						try {
							Exception e = new Exception();
							e = Warden.spot(e);
							// should not be reported
							Assert.assertTrue(cl.getCaught().isEmpty());
							throw e;
						} catch (Exception e) {
							Warden.disregard(e);
						}
					}
				});
			}
		}, Default.of(cl, Log.class));
		// should not be reported
		Assert.assertTrue(cl.getCaught().isEmpty());
	}

	@Test
	public void testWardenDisregardedAndFollowingReportedException() {
		final CaptureLog cl = new CaptureLog();
		final RuntimeException expectedException = new RuntimeException(); // NOPMD test
		Context.runWith(new Running() {
			@Override
			public void run() {
				Warden.guard(new Running() {
					@Override
					public void run() {
						try {
							Exception e = new Exception(); // NOPMD test
							e = Warden.spot(e);
							// should not be reported
							Assert.assertTrue(cl.getCaught().isEmpty());
							throw e;
						} catch (Exception e) {
							Warden.disregard(e);
						}

						// now throw a real exception to be reported
						expectedException.fillInStackTrace();
						throw Warden.spot(expectedException);
					}
				});
			}
		}, Default.of(cl, Log.class));

		Assert.assertEquals(2, cl.getCaught().size());
		Throwable actual = cl.getCaught().get(0);
		Assert.assertEquals(expectedException, actual);
		actual = cl.getCaught().get(1);
		Assert.assertEquals(UnhandledException.class, actual.getClass());
		Assert.assertEquals(expectedException, actual.getCause());
	}

	@Test
	public void testWardenWrappedDisregardedAndFollowingReportedException() {
		final CaptureLog cl = new CaptureLog();
		final RuntimeException expectedException = new RuntimeException(); // NOPMD test
		UncaughtExceptionHandler uncaughtHandler = Mockito.mock(UncaughtExceptionHandler.class);
		Context.runWith(Warden.guarded(ABlock.wrapInReportingBlock(new Running() {
			@Override
			public void run() {
				try {
					Exception e = new Exception(); // NOPMD test
					e = Warden.spot(e);
					// should not be reported
					Assert.assertTrue(cl.getCaught().isEmpty());
					throw e;
				} catch (Exception e) {
					Warden.disregard(e);
				}

				// now throw a real exception to be reported
				expectedException.fillInStackTrace();
				throw Warden.spot(expectedException);
			}
		}, null, uncaughtHandler)), Default.of(cl, Log.class));

		Mockito.verify(uncaughtHandler).uncaughtException(Matchers.any(Thread.class), Matchers.eq(expectedException));

		// only the latter should be reported
		Assert.assertEquals(1, cl.getCaught().size());
		final Throwable actual = cl.getCaught().get(0);
		Assert.assertEquals(UnhandledException.class, actual.getClass());
		Assert.assertEquals(expectedException, actual.getCause());
	}
}
