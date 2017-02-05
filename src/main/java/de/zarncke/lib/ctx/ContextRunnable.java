package de.zarncke.lib.ctx;

import de.zarncke.lib.block.Running;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.value.Default;

/**
 * A {@link de.zarncke.lib.block.Block} which runs its body in a {@link Context}.
 * 
 * @author Gunnar Zarncke
 */
public abstract class ContextRunnable extends Running {

	private final Default<?>[] values;

	public ContextRunnable(final Default<?>... values) {
		this.values = values;
	}

	public final void run() {
		Warden.guard(Context.wrapInContext(new Running() {
					public void run() {
						runGuardedWithContext();
					}
		}, Context.INHERITED, ContextRunnable.this.values));
	}

	public abstract void runGuardedWithContext();

}
