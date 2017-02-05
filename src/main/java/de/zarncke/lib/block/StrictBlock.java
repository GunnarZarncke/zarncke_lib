package de.zarncke.lib.block;

public interface StrictBlock<T> extends Block<T> {
	T execute();
}
