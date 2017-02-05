package de.zarncke.lib.lang.reflect;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.annotation.Nonnull;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.lang.ClassTools;
import de.zarncke.lib.value.Typed;
import de.zarncke.lib.value.Value;

/**
 * A Reflected Object with convenience methods.
 * For easy reflection use.
 *
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
@SuppressWarnings("rawtypes" /* generic reflection using many raw types */)
public class Reflect implements Value<Object>, Typed {
	private Object ref;
	private final Class clazz;

	public Reflect(@Nonnull final Object ref) {
		this(ref, ref.getClass());
	}

	public Reflect(final Object ref, final Class clazz) {
		this.ref = ref;
		this.clazz = clazz;
	}

	@Override
	public Object get() {
		return this.ref;
	}

	public void set(final Object ref) {
		this.ref = ref;
	}

	@Override
	public Class<?> getType() {
		return this.clazz;
	}

	@Override
	public String toString() {
		return "Reflected " + this.ref;
	}

	public static Reflect create(final String className, final Object... parameters) {
		return createClass(findClass(className), parameters);
	}

	private static Reflect createClass(final Class clazz, final Object... parameters) {
		Object[] objs = new Object[parameters.length];
		Class[] classes = new Class[parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			Object param = parameters[i];
			while (param instanceof Reflect) {
				param = ((Reflect) param).get();
			}
			objs[i] = param;
			classes[i] = param == null ? Object.class : param.getClass();
		}
		Constructor method;
		try {
			method = ClassTools.getBestConstructor(clazz, classes);
		} catch (NoSuchMethodException e) {
			throw Warden.spot(new IllegalArgumentException(clazz.getName() + "() not found", e));
		}
		Object res;
		try {
			res = method.newInstance(objs);
		} catch (IllegalAccessException e) {
			throw Warden.spot(new IllegalArgumentException(clazz.getName() + "() is not accessible", e));

		} catch (InstantiationException e) {
			throw Warden.spot(new IllegalArgumentException(clazz.getName() + "() is not instantiable", e));
		} catch (InvocationTargetException e) {
			Throwable t = e.getCause();
			if (t instanceof RuntimeException) {
				throw Warden.spot((RuntimeException) t);
			}
			if (t instanceof Error) {
				throw Warden.spot((Error) t);
			}
			throw Warden.spot(new RuntimeException(
					"reflection yielded checked exception which is wrapped as unchecked", e)); // NOPMD generic reflect
		} catch (IllegalArgumentException e) {
			// Warden.disregard(e);
			throw Warden.spot(new RuntimeException("", e));
		}
		return new Reflect(res, clazz);
	}

	public static Reflect get(final String className, final String fieldName) {
		return getClass(findClass(className), null, fieldName);
	}

	private static Reflect getObject(Object object, final String fieldName) {
		if (object instanceof Reflect) {
			object = ((Reflect) object).get();
		}
		if (object == null) {
			throw Warden.spot(new IllegalArgumentException("cannot determine class of null"));
		}
		return getClass(object.getClass(), object, fieldName);
	}

	public Reflect get(final String fieldName) {
		return getClass(getType(), get(), fieldName);
	}

	private static Reflect getClass(final Class clazz, final Object self, final String fieldName) {
		Field field;
		try {
			field = clazz.getField(fieldName);
		} catch (NoSuchFieldException e) {
			throw Warden.spot(new IllegalArgumentException(clazz.getName() + "." + fieldName + " not found", e));
		}
		Object res;
		try {
			res = field.get(self);
		} catch (IllegalAccessException e) {
			throw Warden
					.spot(new IllegalArgumentException(clazz.getName() + "." + fieldName + " is not accessible", e));
		}
		return new Reflect(res, res == null ? field.getType() : res.getClass());
	}

	public static Reflect array(final String className, final Object... values) {
		Class clazz = findClass(className);
		Object array = Array.newInstance(clazz, values.length);
		if (values.length > 0 && values[0] instanceof Reflect) {
			for (int i = 0; i < values.length; i++) {
				Array.set(array, i, ((Reflect) values[i]).get());
			}
		} else {
			System.arraycopy(values, 0, array, 0, values.length);
		}
		return new Reflect(array);
	}

	public static Reflect call(final String className, final String methodName, final Object... parameters) {
		return callClass(findClass(className), null, methodName, parameters);
	}

	public Reflect call(final String methodName, final Object... parameters) {
		return callClass(getType(), get(), methodName, parameters);
	}

	private static Reflect callObject(Object object, final String methodName, final Object... parameters) {
		if (object instanceof Reflect) {
			object = ((Reflect) object).get();
		}
		if (object == null) {
			throw Warden.spot(new IllegalArgumentException("cannot determine class of null"));
		}
		return callClass(object.getClass(), object, methodName, parameters);
	}

	private static Class findClass(final String className) {
		Class clazz;
		try {
			clazz = Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw Warden.spot(new IllegalArgumentException(className + " not found", e));
		}
		return clazz;
	}

	private static Reflect callClass(final Class clazz, final Object self, final String methodName,
			final Object... parameters) {
		Object[] objs = new Object[parameters.length];
		Class[] classes = new Class[parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			Object param = parameters[i];
			while (param instanceof Reflect) {
				param = ((Reflect) param).get();
			}
			objs[i] = param;
			classes[i] = param == null ? Object.class : param.getClass();
		}
		Method method;
		try {
			method = ClassTools.getBestMethod(clazz, methodName, self == null, classes);
		} catch (NoSuchMethodException e) {
			throw Warden.spot(new IllegalArgumentException(clazz.getName() + "." + methodName + L.l(classes)
					+ " not found", e));
		}
		Object res;
		try {
			res = method.invoke(self, objs);
		} catch (IllegalAccessException e) {
			throw Warden.spot(new IllegalArgumentException(clazz.getName() + "." + methodName + L.l(classes)
					+ " is not accessible", e));

		} catch (InvocationTargetException e) {
			Throwable t = e.getCause();
			if (t instanceof RuntimeException) {
				throw Warden.spot((RuntimeException) t);
			}
			if (t instanceof Error) {
				throw Warden.spot((Error) t);
			}
			throw Warden.spot(new RuntimeException(
					"reflection yielded checked exception which is wrapped as unchecked", e)); // NOPMD generic reflect
		}
		// return new Reflect(res, res == null ? method.getReturnType() : res.getClass());
		return new Reflect(res, method.getReturnType());
	}

}
