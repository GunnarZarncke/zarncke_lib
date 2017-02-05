package de.zarncke.lib.db;

import java.util.List;

import de.zarncke.lib.block.Running;
import de.zarncke.lib.coll.L;
import de.zarncke.lib.i18n.Translations;
import de.zarncke.lib.lang.CodeResponsible;
import de.zarncke.lib.lang.Piece;
import de.zarncke.lib.sys.Health;
import de.zarncke.lib.sys.module.Module;

/**
 * Provides Db load and shutdown support.
 * A load of 0 means 0 connections.
 * A load of 0.1 means 1 connection
 * A load of ~0.5 means 5 connections
 * A load of ~0.65 means 10 connections
 * A load of ~0.9 means 50 connections
 * A load near 1 means very many connections.
 *
 * @author Gunnar Zarncke
 */
public class DatabaseModule implements Module, CodeResponsible {
	private State state = State.UNINITIALIZED;

	@Override
	public Health getHealth() {
		// TODO send "ping" to Db and check answer
		return Health.VIRGIN;
	}

	@Override
	public double getLoad() {
		// TODO get load statistics from db (e.g. concurrent connections)
		int c = Db.openConnections;
		return c == 0 ? 0.0 : 1.0 - 1.0 / (c / 5.5 + 0.95);
	}

	@Override
	public State getState() {
		return this.state;
	}

	@Override
	public Translations getName() {
		return new Translations("Database " + Db.DB.get().getUrl());
	}

	@Override
	public List<Module> getSubModules() {
		return L.e();
	}

	@Override
	public void kill() {
		// Db.transactional(new Runnable() {
		// public void run() {
		// Db.utilize("SHUTDOWN IMMEDIATE", null);
		// }
		// });
	}

	@Override
	public void shutdown() {
		this.state = State.GOING_DOWN;
		if (Db.isInitialized()) {
			try {
				Db.transactional(new Running() {
					@Override
					public void run() {
						Db.utilize("SHUTDOWN", null);
					}
				});
				this.state = State.DOWN;
			} catch (Exception e) {
				this.state = State.STUCK;
			}
		} else {
			this.state = State.DOWN;
		}
	}

	@Override
	public void startOrRestart() {
		this.state = State.GOING_UP;
		Db.DB.get();
		// TODO send "ping" to Db and check answer
		if (Db.isInitialized()) {
			this.state = State.UP;
		} else {
			this.state = State.UNINITIALIZED;
		}
	}

	@Override
	public void tryRecovery() {
		this.state = State.RECOVERING;
		if (Db.isInitialized()) {
			Db.transactional(new Running() {
				@Override
				public void run() {
					Db.utilize("SHUTDOWN COMPACT", null);
				}
			});
		}
		this.state = State.UP;
	}

	@Override
	public String toString() {
		return "db " + Db.DB.get().getUrl();
	}

	@Override
	public String getMetaInformation() {
		return Db.DB.get().getUrl();
	}

	@Override
	public boolean isResponsibleFor(final Piece code) {
		if (code.getName().startsWith("de.zarncke.lib.db.") || code.getName().startsWith("hsqldb.")) {
			return true;
		}
		return false;
	}

}
