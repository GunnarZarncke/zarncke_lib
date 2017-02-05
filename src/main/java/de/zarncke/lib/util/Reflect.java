package de.zarncke.lib.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.zarncke.lib.err.Warden;

/**
 * Reflects properties of a type T.
 *
 * @author Gunnar Zarncke
 * @param <T> to reflect
 */
public class Reflect<T> {
	public static interface Invoker<B> {
		void on(B base);
	}

	public static interface Accessor<B, E> {
		E get(B base);

		void set(B base, E element);
	}

	private final Class<T> interfaceToReflect;

	public Reflect(final Class<T> interfaceToReflect) {
		this.interfaceToReflect = interfaceToReflect;
	}

	public static <T> Reflect<T> reflect(final Class<T> classToReflect) {
		if (!classToReflect.isInterface()) {
			throw Warden.spot(new IllegalArgumentException(classToReflect + " must be interface"));
		}
		return new Reflect<T>(classToReflect);
	}

	private Invoker<T> invoker(final String name) {
		try {
			final Method m = this.interfaceToReflect.getMethod(name);
			return new Invoker<T>() {
				@Override
				public void on(final T base) {
					try {
						m.invoke(m);
					} catch (Exception e) {
						throw Warden.spot(new RuntimeException("cannot invoke " + m, e));
					}
				}
			};
		} catch (Exception e1) {
			throw Warden.spot(new IllegalArgumentException("cannot find " + name + " in " + this.interfaceToReflect));
		}
	}

	/**
	 * @param name of field to access
	 * @param fieldType to be returned by field (for type checking)
	 * @return Accessor
	 */
	public <F> Accessor<T, F> access(final String name, final Class<F> fieldType) {
		try {
			final Field f = this.interfaceToReflect.getField(name);
			if (!fieldType.isAssignableFrom(f.getType())) {
				throw Warden.spot(new IllegalArgumentException("field " + f + " is not of type " + fieldType));
			}
			return (Accessor<T, F>) access(f, fieldType);
		} catch (Exception e1) {
			throw Warden.spot(new IllegalArgumentException("cannot find " + name + " in " + this.interfaceToReflect));
		}
	}

	/**
	 * @param field to use for access
	 * @param type just for type checking
	 * @return Accessor
	 */
	public <F> Accessor<T, ?> access(final Field field, final Class<F> type) {
		final String name = field.getName();
		return new Accessor<T, F>() {
			@Override
			public F get(final T base) {
				try {
					return (F) field.get(base);
				} catch (Exception e) {
					throw Warden.spot(new RuntimeException("cannot read " + name, e));
				}
			}

			@Override
			public void set(final T base, final F element) {
				try {
					field.set(base, element);
				} catch (Exception e) {
					throw Warden.spot(new RuntimeException("cannot write " + name, e));
				}
			}
		};
	}


	public static <A, B, C> Accessor<A, C> chain(final Accessor<A, B> aToB, final Accessor<B, C> bToC) {
		return new Accessor<A, C>() {
			@Override
			public C get(final A a) {
				B b = aToB.get(a);
				if (b == null) {
					return null;
				}
				return bToC.get(b);
			}

			@Override
			public void set(final A a, final C c) {
				B b = aToB.get(a);
				if (b == null) {
					throw Warden.spot(new NullPointerException("trying to access " + bToC + " at " + aToB + " on " + a
							+ " found null value"));
				}
				bToC.set(b, c);
			}
		};
	}

	public static <A, B> Invoker<A> chain(final Accessor<A, B> aToB, final Invoker<B> bCall) {
		return new Invoker<A>() {
			@Override
			public void on(final A a) {
				B b = aToB.get(a);
				if (b == null) {
					throw Warden.spot(new NullPointerException("trying to access " + bCall + " at " + aToB + " on " + a
							+ " found null value"));
				}
				bCall.on(b);
			}
		};
	}

}
