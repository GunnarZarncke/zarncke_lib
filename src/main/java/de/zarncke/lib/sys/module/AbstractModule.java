package de.zarncke.lib.sys.module;

import java.util.List;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.sys.Health;
import de.zarncke.lib.time.Times;
import de.zarncke.lib.util.Chars;

/**
 * Basic implementation of a module.
 * Handles basic state (leaves actual action open).
 * Aggregates state of sub modules (if any).
 *
 * @author Gunnar Zarncke
 */
public abstract class AbstractModule implements Module {

	protected State state = State.UNINITIALIZED;

	private Health lastHealth;
	private long lastCheckMillis;
	private String lastMessage = null;

	protected long getMinHealthRecheckMillis() {
		return Times.MILLIS_PER_SECOND;
	}

	/**
	 * Derided classes should implement {@link #getHealthProtected()}
	 */
	@Override
	public Health getHealth() {
		long now = System.currentTimeMillis();
		if (now < this.lastCheckMillis + getMinHealthRecheckMillis()) {
			return this.lastHealth;
		}
		Health health;
		try {
			health = getHealthProtected();
		} catch (Throwable t) { // NOPMD generic
			Warden.disregardAndReport(t);
			health = Health.ERRORS;
		}
		int deaths = 0;

		List<Module> modules = getSubModules();
		for (Module m : modules) {
			try {
				Health mHealth;
				mHealth = m.getHealth();

				if (mHealth.ordinal() > health.ordinal()) {
					if (mHealth == Health.DEAD) {
						health = Health.FAILURE;
						deaths++;
					}
				}
			} catch (Throwable t) { // NOPMD generic
				Warden.disregardAndReport(t);
				health = Health.ERRORS;
				deaths++;
			}
		}
		if (deaths > 0 && deaths == modules.size()) {
			health = Health.DEAD;
		}
		this.lastHealth = health;
		return health;

	}

	protected Health getHealthProtected() {
		return Health.VIRGIN;
	}

	@Override
	public double getLoad() {
		return Double.NaN;
	}

	@Override
	public State getState() {
		return this.state;
	}

	@Override
	public List<Module> getSubModules() {
		return L.e();
	}

	@Override
	public void kill() {
		// do nothing by default
	}

	@Override
	public final void shutdown() {
		this.state = State.GOING_DOWN;
		try {
			doShutdown();
			this.state = State.DOWN;
		} catch (Exception e) {
			Warden.disregardAndReport(e);
			this.state = State.STUCK;
		}
	}

	protected void doShutdown() {
		// for derived to supply
	}

	@Override
	public void startOrRestart() {
		if (this.state == State.UP) {
			this.state = State.GOING_DOWN;
			try {
				doShutdown();
			} catch (Exception e) {
				Warden.disregardAndReport(e);
			}
		}
		this.state = State.GOING_UP;
		try {
			doStartup();
			this.state = State.UP;
		} catch (Exception e) {
			Warden.disregardAndReport(e);
			this.state = State.STUCK;
		}
	}

	protected void doStartup() {
		// for derived to implement
	}

	@Override
	public void tryRecovery() {
		this.state = State.RECOVERING;
		try {
			doRecovery();
			this.state = State.UP;
		} catch (Exception e) {
			Warden.disregardAndReport(e);
			this.state = State.STUCK;
		}
	}

	protected void doRecovery() {
		// for derived to supply
	}

	@Override
	public String toString() {
		return getName().getDefault();
	}

	@Override
	public String getMetaInformation() {
		if (getSubModules().isEmpty()) {
			return this.lastMessage;
		}
		StringBuilder sb = new StringBuilder();
		for (Module m : getSubModules()) {
			sb.append(m.getName().getDefault()).append(":\n").append(m.getMetaInformation()).append("\n");
		}
		if (!Chars.isEmpty(this.lastMessage)) {
			sb.append("Health Problems:\n");
			sb.append(this.lastMessage);
		}
		return sb.toString();
	}

	public static Health max(final Health a, final Health b) {
		if (a == null) {
			return b;
		}
		if (b == null) {
			return a;
		}
		return a.ordinal() > b.ordinal() ? a : b;
	}

	protected String getLastMessage() {
		return this.lastMessage;
	}

	protected void setLastMessage(final String lastMessage) {
		this.lastMessage = lastMessage;
	}
}
