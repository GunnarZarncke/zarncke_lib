package de.zarncke.lib.block;

import de.zarncke.lib.block.Task.Progress;

/**
 * A {@link Block} which encapsulates a unit of work.
 * May or may not be called more than once.
 * Yields an indication of (its) progress within the larger operation.
 *
 * @author Gunnar Zarncke
 */
public interface Task extends Block<Progress> {
	/**
	 * @deprecated use {@link de.zarncke.lib.progress.Progress} instead.
	 */
	@Deprecated
	interface Progress extends de.zarncke.lib.progress.Progress {
		// present for backward compatibility
	}

	/**
	 * @deprecated use {@link de.zarncke.lib.progress.AbstractProgress} instead.
	 */
	@Deprecated
	abstract class AbstractProgress extends de.zarncke.lib.progress.AbstractProgress implements Progress {
		// present for backward compatibility
	}

	/**
	 * Execute one instance of this Task.
	 * Should return immediately after; should not sleep itself.
	 * If needed it may test time and decide to do nothing.
	 *
	 * @return Progress information for some monitoring agent.
	 */
	@Override
	Progress execute() throws InterruptedException;

	/**
	 * Higher numeric values means higher priority.
	 * You may assume that this priority matches some OS priority (if possible).
	 *
	 * @return 0 is default priority
	 */
	int getPriority();
}
