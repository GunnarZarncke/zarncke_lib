package de.zarncke.lib.block;

import java.util.Collection;

import com.google.common.base.Supplier;

/**
 * Allows a component to indicate which {@link Task}s it wants to run.
 *
 * @author Gunnar Zarncke
 * @deprecated use {@link Supplier} instead
 */
@Deprecated
public interface TaskProvider {
	Collection<Task> getTasksToRun();
}
