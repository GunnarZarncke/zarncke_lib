package de.zarncke.lib.time;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import de.zarncke.lib.ctx.Context;
import de.zarncke.lib.value.Default;

public class JavaClock implements Clock {
	private static Context<Clock> defaultClock = Context.of(Default.of(new JavaClock(), Clock.class));

	public static Clock getTheClock() {
		return defaultClock.get();
	}

	private volatile long lastMs = Integer.MIN_VALUE;
	private volatile DateTime lastTime = null;

	public long getCurrentTimeMillis() {
		return System.currentTimeMillis();
	}

	@Override
	public DateTime getCurrentDateTime() {
		long newMs = getCurrentTimeMillis();
		if (this.lastTime == null || newMs != this.lastMs) {
			this.lastTime = new DateTime(newMs, DateTimeZone.UTC);
			this.lastMs = newMs;
		}
		return this.lastTime;
	}

	@Override
	public String toString() {
		return getCurrentDateTime().toString();
	}

}
