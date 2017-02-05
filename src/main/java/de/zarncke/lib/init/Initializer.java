package de.zarncke.lib.init;

import java.util.HashSet;
import java.util.Set;

public class Initializer {
	public class Interest<T> {
		Class<T> interfaceClass;

		T implementation;

		public Interest(final Class<T> interfaceClass) {
			this.interfaceClass = interfaceClass;
		}

		public T getImplementation() throws IllegalStateException {
			if (!isResolved()) {
				throw new IllegalStateException();
			}
			return this.implementation;
		}

		public boolean isResolved() {
			return this.implementation != null;
		}

		public void waitForResolution() throws InterruptedException {
			while (true) {
				synchronized (Initializer.this) {
					if (isResolved()) {
						return;
					}
					Initializer.this.wait();
				}
			}
		}

		@Override
		public String toString() {
			return "interest in " + this.interfaceClass;
		}
	}

	private final Set<Interest<?>> interests = new HashSet<Interest<?>>();

	private final Set<Class<?>> classes = new HashSet<Class<?>>();

	public <T> Interest<T> signalInterestInImplementation(final Class<T> interfaceClass) {
		Interest<T> interest = new Interest<T>(interfaceClass);
		this.interests.add(interest);
		return interest;
	}

	public void registerImplementation(final Class<?> implementationClass) {
		this.classes.add(implementationClass);
	}

	@SuppressWarnings("unchecked")
	public void completeInitialization() {
		for (Class<?> cl : this.classes) {
			for (Interest interest : this.interests) {
				if (interest.interfaceClass.isAssignableFrom(cl)) {
					if (interest.implementation == null) {
						interest.implementation = cl;
					} else {
						throw new IllegalStateException("Found multiple implementations " + cl + " and "
								+ interest.implementation + " for registered " + interest);
					}
				}
			}
		}

		for (Interest interest : this.interests) {
			if (interest.implementation == null) {
				throw new IllegalStateException("Found unresolved implementations " + interest.implementation
						+ " for registered " + interest);

			}
		}
		synchronized (this) {
			notifyAll();
		}
	}
}
