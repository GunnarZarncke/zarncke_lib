package de.zarncke.lib.data;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import de.zarncke.lib.err.CantHappenException;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.value.Target;
import de.zarncke.lib.value.Value;

/**
 * Tools for dealing with getters, setters and properties in general.
 *
 * @author Gunnar Zarncke
 */
public final class Props {
	private Props() {
		// helper
	}
	public static <B, V> Value<V> specialize(final Getter<B, V> getter, final B base) {
		return new Value<V>() {
			@Override
			public V get() {
				return getter.get(base);
			}

			@Override
			public String toString() {
				return getter.toString();
			}
		};
	}

	public static <B, V> Target<V> specialize(final Setter<B, V> setter, final B base) {
		return new Target<V>() {
			@Override
			public void set(final V value) {
				setter.set(base, value);
			}

			@Override
			public String toString() {
				return setter.toString();
			}
		};
	}

	public static <B, V> Getter<B, V> getter(final Class<B> type, final Class<V> valueType, final String propertyName)
			throws NoSuchMethodException {
		final Method method = type.getMethod(propertyName, (Class[]) null);
		if (!valueType.isAssignableFrom(method.getReturnType())) {
			throw Warden.spot(new IllegalArgumentException("method " + method + " doesn't return " + valueType));
		}
		return new Getter<B, V>() {
			@Override
			public V get(final B base) {
				try {
					return (V) method.invoke(base, (Object[]) null);
				} catch (IllegalArgumentException e) {
					throw Warden.spot(new CantHappenException("We checked at construction! Generics violation?", e));
				} catch (IllegalAccessException e) {
					throw Warden.spot(new SecurityException("We checked at construction!", e));
				} catch (InvocationTargetException e) {
					if (e.getTargetException() instanceof RuntimeException) {
						throw Warden.spot((RuntimeException) e.getTargetException());
					}
					throw Warden.spot(new RuntimeException("checked exception throw by " + method, e));
				}
			}

			@Override
			public String toString() {
				return method.toString();
			}
		};
	}

	public static <B, V> Setter<B, V> setter(final Class<B> type, final Class<V> valueType, final String propertyName)
			throws NoSuchMethodException {
		final Method method = type.getMethod(propertyName, type);
		return new Setter<B, V>() {
			@Override
			public void set(final B base, final V value) {
				try {
					method.invoke(base, value);
				} catch (IllegalArgumentException e) {
					throw Warden.spot(new CantHappenException("We checked at construction! Generics violation?", e));
				} catch (IllegalAccessException e) {
					throw Warden.spot(new SecurityException("We checked at construction!", e));
				} catch (InvocationTargetException e) {
					if (e.getTargetException() instanceof RuntimeException) {
						throw Warden.spot((RuntimeException) e.getTargetException());
					}
					throw Warden.spot(new RuntimeException("checked exception throw by " + method, e));
				}
			}

			@Override
			public String toString() {
				return method.toString();
			}
		};
	}

	public <B, I, V> Getter<B, V> chain(final Getter<B, I> iGetter, final Getter<I, V> vGetter) {
		return new Getter<B, V>() {
			@Override
			public V get(final B base) {
				I inter = iGetter.get(base);
				if (inter == null) {
					return null;
				}
				return vGetter.get(inter);
			}

			@Override
			public String toString() {
				return iGetter + "." + vGetter;
			}
		};
	}

	public <B, I, V> Setter<B, V> chain(final Getter<B, I> iGetter, final Setter<I, V> vSetter) {
		return new Setter<B, V>() {
			@Override
			public void set(final B base, final V value) {
				I inter = iGetter.get(base);
				if (inter == null) {
					throw Warden.spot(new NullPointerException("got null for " + iGetter + " on " + base));
				}
				vSetter.set(inter, value);
			}

			@Override
			public String toString() {
				return iGetter + "." + vSetter;
			}
		};
	}
}
