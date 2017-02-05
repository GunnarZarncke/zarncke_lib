package de.zarncke.lib.block;

public abstract class Running implements StrictBlock<Void>, java.lang.Runnable {
	public final Void execute() {
		run();
		return null;
	}
}
