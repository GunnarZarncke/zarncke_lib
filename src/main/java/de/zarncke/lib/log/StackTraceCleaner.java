package de.zarncke.lib.log;

import javax.annotation.Nonnull;

import de.zarncke.lib.ctx.Context;
import de.zarncke.lib.err.ExceptionUtil;
import de.zarncke.lib.util.Replacer;
import de.zarncke.lib.util.Replacer.Builder;
import de.zarncke.lib.value.Default;

/**
 * Provides methods for cleaning stack traces and making them more human readable.
 * Derived classes may add to the rules.
 * static methods are available to use the currently set {@link StackTraceCleaner}.
 *
 * @author Gunnar Zarncke
 * @clean 20.03.2012
 */
public class StackTraceCleaner {
	public static final Context<StackTraceCleaner> CTX = Context.of(Default.of(new StackTraceCleaner(),
			StackTraceCleaner.class));

	private static final String PATTERN_EXCEPTION_NOT_INTENDED_TO_BE_THROWN = //
	"reported by de.zarncke.lib.err.ExceptionNotIntendedToBeThrown\n";

	private final Replacer reportCleaner;

	protected StackTraceCleaner() {
		Builder builder = Replacer.multiline();
		addPatterns(builder); // NOPMD intended extensibility
		this.reportCleaner = builder.done();
	}

	/**
	 * Derived classes may add patterns here - note that the class is not fully initialized yet!
	 * The replacements are executed in the order they are added.
	 * Add complex patterns before the call to super.addPatterns().
	 * Basic patterns should be added afterwards.
	 *
	 * @param cleanerBuilder to add patterns to
	 */
	protected void addPatterns(final Replacer.Builder cleanerBuilder) {
		String contextPattern = "(?:"
				+ quoteStacktrace("        at de.zarncke.lib.block.ABlock$2.execute(ABlock.java:41)\n")
				+ "|"
				+ quoteStacktrace("        at de.zarncke.lib.ctx.Context.runWith(Context.java:230)\n")
				+ "|"
				+ quoteStacktrace("        at de.zarncke.lib.ctx.Context$7.execute(Context.java:279)\n")
				+ "|"
				+ quoteStacktrace("        at de.zarncke.lib.block.Running.execute(Running.java:10)\n")
				+ "|"
				+ quoteStacktrace("        at de.zarncke.lib.block.Running.execute(Running.java:8)\n")
				+ ")*"
				+ quoteStacktrace("        at de.zarncke.lib.ctx.Context.runWith(Context.java:230)\n"
						+ "        at de.zarncke.lib.ctx.Context.runWith(Context.java:216)\n");

		cleanerBuilder
				.replaceAllRegex("(?:" + quoteStacktrace("  at org.eclipse.jdt.internal.junit") + ".*\\r?\\n)+",
						"        at [Eclipse.JDT]\n")
				.replaceAllRegex(
						quoteStacktrace("  at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n"
								+ "	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)\n"
								+ "	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)\n"
								+ "	at java.lang.reflect.Method.invoke(Method.java:597)\n"
								+ "	at junit.framework.TestCase.runTest(TestCase.java:168)\n"

								+ "	at junit.framework.TestCase.runBare(TestCase.java:134)\n"
								+ "	at junit.framework.TestResult$1.protect(TestResult.java:110)\n"
								+ "	at junit.framework.TestResult.runProtected(TestResult.java:128)\n"
								+ "	at junit.framework.TestResult.run(TestResult.java:113)\n"
								+ "	at junit.framework.TestCase.run(TestCase.java:124)\n"
								+ "	at junit.framework.TestSuite.runTest(TestSuite.java:243)\n"
								+ "	at junit.framework.TestSuite.run(TestSuite.java:238)"), "        at [JUnit]")
				.replaceAllRegex(
						quoteStacktrace("	at junit.framework.TestCase.runBare(TestCase.java:134)\n"
								+ "	at junit.framework.TestResult$1.protect(TestResult.java:110)\n"
								+ "	at junit.framework.TestResult.runProtected(TestResult.java:128)\n"
								+ "	at junit.framework.TestResult.run(TestResult.java:113)\n"
								+ "	at junit.framework.TestCase.run(TestCase.java:124)\n"
								+ "	at junit.framework.TestSuite.runTest(TestSuite.java:243)\n"
								+ "	at junit.framework.TestSuite.run(TestSuite.java:238)"), "        at [JUnit call]")

				.replaceAllRegex(
						quoteStacktrace(PATTERN_EXCEPTION_NOT_INTENDED_TO_BE_THROWN
								+ "        at de.zarncke.lib.log.group.GroupingLog.report(GroupingLog.java:53)\n"
								+ "        at de.zarncke.lib.err.Warden.logFromWithinLogging(Warden.java:63)\n"
								+ "        at de.zarncke.lib.err.Warden.disregard(Warden.java:43)"),
						"reported by [disregardAndReport]")
				.replaceAllRegex(
						quoteStacktrace(PATTERN_EXCEPTION_NOT_INTENDED_TO_BE_THROWN
								+ "	at de.zarncke.lib.log.group.GroupingLog.report(GroupingLog.java:98)\n"
								+ "	at de.zarncke.lib.err.Warden.log(Warden.java:57)\n"
								+ "	at de.zarncke.lib.err.Warden.report(Warden.java:50)\n"
								+ "	at de.zarncke.lib.err.Warden.encounter(Warden.java:157)\n"
								+ "	at de.zarncke.lib.err.Warden.finish(Warden.java:145)\n"
								+ "	at de.zarncke.lib.err.Warden.guard(Warden.java:173)"), "reported by Warden.guard()")
				.replaceAllRegex(
						quoteStacktrace(PATTERN_EXCEPTION_NOT_INTENDED_TO_BE_THROWN
								+ "	at de.zarncke.lib.log.group.GroupingLog.report(GroupingLog.java:135)\n"
								+ " at de.zarncke.lib.err.Warden.log(Warden.java:57)\n"
								+ " at de.zarncke.lib.err.Warden.disregardAndReport(Warden.java:38)"),
						"reported by [Warden.disregardAndReport]")
				.replaceAllRegex(
						quoteStacktrace(PATTERN_EXCEPTION_NOT_INTENDED_TO_BE_THROWN
								+ "	at de.zarncke.lib.log.group.GroupingLog.report(GroupingLog.java:135)\n"
								+ " at de.zarncke.lib.err.Warden.log(Warden.java:57)\n"
								+ " at de.zarncke.lib.err.Warden.report(Warden.java:50)"),
						"reported by [Warden.report]")
				.replaceAllRegex(
						quoteStacktrace(PATTERN_EXCEPTION_NOT_INTENDED_TO_BE_THROWN
								+ "	at de.zarncke.lib.log.group.GroupingLog.report(GroupingLog.java:135)"),
						"reported by")
				.replaceAllRegex(
						contextPattern + quoteStacktrace("        at de.zarncke.lib.db.Db.transactional(Db.java:94)"),
						"        at [transactional]")
				.replaceAllRegex(contextPattern, "        at [Context*]\n")
				.replaceAllRegex(
						quoteStacktrace("        at de.zarncke.lib.block.Running.execute(Running.java:10)\n"
								+ "        at de.zarncke.lib.block.Running.execute(Running.java:8)\n"
								+ "        at de.zarncke.lib.err.Warden.guard(Warden.java:166)"),
						"        at [Guarded->Running]")
				.replaceAllRegex(
						"("
								+ quoteStacktrace("	at sun.reflect.")
								+ ".*\\r?\\n)*"
								+ quoteStacktrace("	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)"),
						"        at [Reflection*]")
				.replaceAllRegex(
						"(" + quoteStacktrace("	org.junit.") + ".*\\r?\\n|" + quoteStacktrace("	junit.framework.Test")
								+ ".*\\r?\\n)+", "        at [JUnit*]\n").done();
	}

	/**
	 * May be used by implementations to convert a verbatim stacktrace into a useful regex.
	 *
	 * @param stackTraceFragment to escape
	 * @return a regex matching that stacktrace
	 */
	public static String quoteStacktrace(final String stackTraceFragment) {
		return stackTraceFragment.replaceAll("\n\\s+", "\\\\r?\\\\n\\\\s+").replaceAll("\\n$", "\\\\r?\\\\n")
				.replaceAll("^\\s+", "^\\\\s+").replaceAll("\\.", "\\\\.").replaceAll("\\$[0-9]*", ".[0-9]*")
				.replaceAll(":[0-9]*\\)", ":[0-9]*\\)").replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)");
	}

	public String makeHumanReadableReport(final CharSequence textContainingStackTraces) {
		return this.reportCleaner.apply(textContainingStackTraces).toString();
	}

	/**
	 * Cleans the {@link Report#getFullReport()}.
	 *
	 * @param report to clean
	 * @return clean String
	 */
	@Nonnull
	public static String makeHumanReadableReport(@Nonnull final Report report) {
		return CTX.get().makeHumanReadableReport(report.getFullReport());
	}

	/**
	 * Cleans the stacktrace.
	 *
	 * @param throwable to clean
	 * @return clean String
	 */
	@Nonnull
	public static String makeHumanReadableReport(@Nonnull final Throwable throwable) {
		return CTX.get().makeHumanReadableReport(ExceptionUtil.getStackTrace(throwable));
	}

	@Override
	public String toString() {
		return this.reportCleaner.toString();
	}
}
