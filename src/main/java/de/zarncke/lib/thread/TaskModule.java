package de.zarncke.lib.thread;

import java.util.Collection;
import java.util.Map;

import de.zarncke.lib.block.Task;
import de.zarncke.lib.coll.Elements;
import de.zarncke.lib.coll.L;
import de.zarncke.lib.data.HasSelfInfo;
import de.zarncke.lib.i18n.Translations;
import de.zarncke.lib.lang.CodeResponsible;
import de.zarncke.lib.lang.Piece;
import de.zarncke.lib.log.Log;
import de.zarncke.lib.log.group.GroupingLog;
import de.zarncke.lib.sys.Health;
import de.zarncke.lib.sys.module.AbstractModule;
import de.zarncke.lib.sys.module.Module;

/**
 * A {@link Module} for monitoring the {@link Log logging system} with special support for {@link GroupingLog}.
 *
 * @author Gunnar Zarncke
 */
public class TaskModule extends AbstractModule implements CodeResponsible {

	private final double plannedTasks;

	public TaskModule(final double plannedTasks) {
		this.plannedTasks = plannedTasks;
	}

	@Override
	public Health getHealth() {
		return Health.VIRGIN;
	}

	private Collection<TaskThread> getTaskThreads() {
		return L.copy(Elements.checkedIterable(ThreadUtil.getChildThreads(ThreadUtil.getRootThreadGroup(), true),
				TaskThread.class).iterator());
	}

	@Override
	public double getLoad() {
		return getTaskThreads().size() / this.plannedTasks;
	}

	@Override
	public State getState() {
		return this.state;
	}

	@Override
	public Translations getName() {
		return new Translations("Tasks");
	}

	@Override
	protected void doShutdown() {
		for (TaskThread tt : getTaskThreads()) {
			tt.shutdown();
		}
	}

	@Override
	protected void doRecovery() {
		for (TaskThread tt : getTaskThreads()) {
			tt.interrupt();
		}
	}

	@Override
	public String toString() {
		return "tasks";
	}

	@Override
	public String getMetaInformation() {
		StringBuilder sb = new StringBuilder();
		for (TaskThread tt : getTaskThreads()) {
			sb.append(tt.getName()).append(":");
			Task current = tt.getCurrentTask();
			if (current != null) {
				sb.append("  current:\n");
				printTask(current, sb);
				sb.append("  all:\n");
			}
			for (Task task : tt.getTasks()) {
				printTask(task, sb);
			}
		}
		return sb.toString();
	}

	public void printTask(final Task task, final StringBuilder buffer) {
		buffer.append("  ").append(task.getClass().getSimpleName()).append(":");
		if (task instanceof HasSelfInfo) {
			buffer.append("\n");
			for (Map.Entry<String, ?> me : ((HasSelfInfo) task).getSelfInfo().entrySet()) {
				buffer.append("    ").append(me.getKey()).append(":").append(me.getValue()).append("\n");
			}
		} else {
			buffer.append(task.toString());
		}
		buffer.append("\n");
	}

	@Override
	public boolean isResponsibleFor(final Piece code) {
		if (code.getName().startsWith("de.zarncke.lib.thread.")) {
			return true;
		}
		return false;
	}
}
