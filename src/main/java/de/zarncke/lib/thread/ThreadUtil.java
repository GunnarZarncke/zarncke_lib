package de.zarncke.lib.thread;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import de.zarncke.lib.coll.L;

/**
 * Tools for dealing with {@link Thread}s and {@link ThreadGroup}s.
 *
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
public final class ThreadUtil {
	private ThreadUtil() {
		// hidden constructor of helper class
	}

	/**
	 * Determine root thread group.
	 * Traverses from the current threads group upwards.
	 *
	 * @return {@link ThreadGroup}
	 */
	public static ThreadGroup getRootThreadGroup() {
		ThreadGroup mainGroup = Thread.currentThread().getThreadGroup();
		if (mainGroup.getParent() != null) {
			mainGroup = mainGroup.getParent();
		}
		return mainGroup;
	}

	/**
	 * Finds all threads with names matching a regex.
	 *
	 * @param parent to find children for
	 * @param nameRegEx to test
	 * @param recurse true: descendante, false: direct children
	 * @return matching threads
	 */
	@Nonnull
	public static Collection<Thread> getThreadByName(@Nonnull final ThreadGroup parent,
			@Nonnull final String nameRegEx, final boolean recurse) {
		Pattern pattern = Pattern.compile(nameRegEx);
		List<Thread> matches = L.l();
		for (Thread thread : getChildThreads(parent, recurse)) {
			if (pattern.matcher(thread.getName()).matches()) {
				matches.add(thread);
			}
		}
		return matches;
	}

	/**
	 * Finds all thread groups with names matching a regex.
	 *
	 * @param parent to find children for
	 * @param nameRegEx to test
	 * @param recurse true: descendante, false: direct children
	 * @return matching thread groups
	 */
	@Nonnull
	public static Collection<ThreadGroup> getThreadGroupByName(@Nonnull final ThreadGroup parent,
			@Nonnull final String nameRegEx, final boolean recurse) {
		Pattern pattern = Pattern.compile(nameRegEx);
		List<ThreadGroup> matches = L.l();
		for (ThreadGroup group : getChildThreadGroups(parent, recurse)) {
			if (pattern.matcher(group.getName()).matches()) {
				matches.add(group);
			}
		}
		return matches;
	}

	/**
	 * Failsafe method to get all child ThreadGroups.
	 *
	 * @param group to get thread groups for
	 * @param recurse true: get descendants recursively, false: only direct child thread groups
	 * @return Collection of {@link ThreadGroup}s in order returned by {@link ThreadGroup#enumerate},
	 * doesn't include the group itself
	 */
	@Nonnull
	public static Collection<ThreadGroup> getChildThreadGroups(@Nonnull final ThreadGroup group, final boolean recurse) {
		int expectedSize = group.activeGroupCount() + 1;
		while (true) {
			ThreadGroup[] threadArr = new ThreadGroup[expectedSize];
			int count = group.enumerate(threadArr, recurse);
			if (count < expectedSize) {
				return L.l(threadArr).subList(0, count);
			}
			expectedSize = expectedSize * 4 / 3 + 1;
		}
	}

	/**
	 * Failsafe method to get all child Threads.
	 *
	 * @param group to get threads for
	 * @param recurse true: get descendants recursively, false: only direct child threads
	 * @return Collection of Threads in order returned by {@link ThreadGroup#enumerate}
	 */
	@Nonnull
	public static Collection<Thread> getChildThreads(final @Nonnull ThreadGroup group, final boolean recurse) {
		int expectedSize = group.activeCount() + 1;
		while (true) {
			Thread[] threadArr = new Thread[expectedSize];
			int count = group.enumerate(threadArr, recurse);
			if (count < expectedSize) {
				return L.l(threadArr).subList(0, count);
			}
			expectedSize = expectedSize * 4 / 3 + 1;
		}
	}

}
