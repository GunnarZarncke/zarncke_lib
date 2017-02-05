package de.zarncke.lib.sys.module;

import java.util.Collection;
import java.util.Map;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.i18n.Translations;
import de.zarncke.lib.io.store.Store;
import de.zarncke.lib.io.store.StoreUtil;
import de.zarncke.lib.sys.BinarySupplier;
import de.zarncke.lib.sys.Health;
import de.zarncke.lib.thread.InterruptibleThread;
import de.zarncke.lib.thread.ThreadUtil;
import de.zarncke.lib.util.Chars;
import de.zarncke.lib.util.Q;
import de.zarncke.lib.value.ObjectRef;
import de.zarncke.lib.value.Value;

/**
 * Checks that the Threads behave and provides info.
 *
 * @author Gunnar Zarncke
 */
public class ThreadModule extends AbstractModule implements BinarySupplier {

	private static final String KEY_STACKTRACES = "stacktraces";
	private final Value<Integer> designNumberOfThreads;

	public ThreadModule(final Value<Integer> designNumberOfThreads) {
		this.designNumberOfThreads = designNumberOfThreads;
	}
	public ThreadModule(final int designNumberOfThreads) {
		this.designNumberOfThreads = ObjectRef.of(Q.i(designNumberOfThreads));
	}

	@Override
	public Translations getName() {
		return new Translations("Threads");
	}

	@Override
	public String getMetaInformation() {
		ThreadGroup mainGroup = ThreadUtil.getRootThreadGroup();
		StringBuilder sb = new StringBuilder();
		appendInfo(mainGroup, sb, 0);
		return sb.toString();
	}

	private void appendInfo(final ThreadGroup group, final StringBuilder buffer, final int depth) {
		// FEATURE consider reading extended ThreadInfo from ThreadMXBean thbean = ManagementFactory.getThreadMXBean()
		buffer.append(Chars.repeat("  ", depth));
		buffer.append("Group '");
		buffer.append(group.getName());
		buffer.append("':");
		if (group.isDaemon()) {
			buffer.append(" DAEMON");
		}
		if (group.getMaxPriority() < Thread.MAX_PRIORITY) {
			buffer.append(" PRIO<");
			buffer.append(group.getMaxPriority());
		}
		buffer.append("\n");
		for (Thread thread : getThreads(group)) {
			appendInfo(thread, buffer, depth + 1);
		}
		for (ThreadGroup sgroup : getThreadGroups(group)) {
			appendInfo(sgroup, buffer, depth + 1);
		}
	}

	private void appendInfo(final Thread thread, final StringBuilder buffer, final int depth) {
		buffer.append(Chars.repeat("  ", depth));
		buffer.append(thread.getId());
		buffer.append(" '");
		buffer.append(thread.getName());
		buffer.append("': ");
		buffer.append(thread.getState());
		if (thread.isDaemon()) {
			buffer.append(",DAEMON");
		}
		if (!thread.isAlive()) {
			buffer.append(",DEAD");
		}
		if (thread.getPriority() != Thread.NORM_PRIORITY) {
			buffer.append(",PRIO=");
			buffer.append(thread.getPriority());
		}
		buffer.append("\n");
	}

	@Override
	protected void doRecovery() {
		InterruptibleThread.interruptAllInterruptibleThreads(getAllThreads(), true);
	}

	private Collection<Thread> getAllThreads() {
		return ThreadUtil.getChildThreads(ThreadUtil.getRootThreadGroup(), true);
	}
	private Collection<Thread> getThreads(final ThreadGroup group) {
		Thread[] threadArr = new Thread[group.activeCount() + 5];
		int count = group.enumerate(threadArr);
		return L.l(threadArr).subList(0, count);
	}

	private Collection<ThreadGroup> getThreadGroups(final ThreadGroup group) {
		ThreadGroup[] groupArr = new ThreadGroup[group.activeGroupCount() + 5];
		int count = group.enumerate(groupArr);
		return L.l(groupArr).subList(0, count);
	}

	@Override
	protected Health getHealthProtected() {
		Health health;
		Collection<Thread> threads = getAllThreads();
		int total = threads.size();
		int running = countRunning(threads);

		int designThreads = this.designNumberOfThreads.get().intValue();
		if (running > designThreads * 10) {
			health = Health.FAILURE;
		} else if (running > designThreads) {
			health = Health.ERRORS;
		} else if (total > designThreads) {
			health = Health.WARNINGS;
		} else if (running > designThreads * 9 / 10) {
			health = Health.WARNINGS;
		} else if (running > designThreads / 2) {
			health = Health.INFO;
		} else if (running <= 2) {
			health = Health.VIRGIN;
		} else {
			health = Health.CLEAN;
		}

		return health;
	}

	private int countRunning(final Collection<Thread> threads) {
		int running = 0;
		for (Thread thread : threads) {
			if (thread.isAlive() && thread.getState() == Thread.State.RUNNABLE) {
				running++;
			}
		}
		return running;
	}

	@Override
	public double getLoad() {
		int running = countRunning(getAllThreads());
		return (double) running / this.designNumberOfThreads.get().intValue();
	}

	@Override
	public Store getBinaryInformation(final Object key) {
		if (KEY_STACKTRACES.equals(key)) {
			Map<Thread, StackTraceElement[]> stacktraces = Thread.getAllStackTraces();
			StringBuilder sb = new StringBuilder();
			for(Map.Entry<Thread,StackTraceElement[]>me:stacktraces.entrySet()){
				appendInfo(me.getKey(), sb, 0);
				for(StackTraceElement ste:me.getValue()){
					sb.append("  at ").append(ste).append("\n");
				}
			}
			return StoreUtil.asStore(sb, null);
		}
		return null;
	}

}
