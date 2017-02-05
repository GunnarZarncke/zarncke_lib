package de.zarncke.lib.log;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;

import de.zarncke.lib.block.Running;
import de.zarncke.lib.ctx.Context;
import de.zarncke.lib.err.GuardedTest;
import de.zarncke.lib.log.group.GroupingLog;
import de.zarncke.lib.log.group.ReportListener;

public class GroupingLogTest extends GuardedTest {

	// TODO add tests to check truncating, limiting and sampling

	@Test
	public void testContext() {
		final ReportListener rl = mock(ReportListener.class);

		final Log rlog = new GroupingLog(rl, 1, 10).setContextUsed(true);

		Context.runWith(new Running() {
			@Override
			public void run() {
				rlog.report("test");
				verify(rl).notifyOfReport(argThat(matchReport("Running.execute:a-test-context", true)));
			}
		}, GroupingLog.LOG_CONTEXT.getOtherDefault("a-test-context"));
	}

	@Test
	public void testNoGrouping() {
		ReportListener rl = mock(ReportListener.class);

		Log rlog = new GroupingLog(rl, 1, 10);

		rlog.report("test");
		rlog.report("test2");
		verify(rl).notifyOfReport(argThat(matchReport("1/1* test", false)));
		verify(rl).notifyOfReport(argThat(matchReport("1/1* test2", false)));
	}


	@Test
	public void testGrouping() {
		ReportListener rl = mock(ReportListener.class);

		Log rlog = new GroupingLog(rl, 4, 10);

		rlog.report("test");
		rlog.report("test2");
		rlog.report("test3");
		rlog.report("test4");
		rlog.report("test5");
		verify(rl).notifyOfReport(argThat(matchReport("1/1* test", false)));
		verify(rl).notifyOfReport(argThat(matchReport("4/4* test2", false)));
	}

	private BaseMatcher<Report> matchReport(final String expectedText, final boolean full) {
		return new BaseMatcher<Report>() {
			@Override
			public boolean matches(final Object arg0) {
				Report rep = (Report) arg0;
				if (full) {
					return rep.getFullReport().toString().contains(expectedText);
				}
				return rep.getSummary().equals(expectedText);
			}

			@Override
			public void describeTo(final Description d) {
				d.appendText("match report " + expectedText);
			}
		};
	}
}
