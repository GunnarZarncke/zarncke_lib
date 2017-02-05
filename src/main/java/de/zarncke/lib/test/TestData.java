package de.zarncke.lib.test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.base.Supplier;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.lang.ClassTools;
import de.zarncke.lib.util.Q;
import de.zarncke.lib.util.Reflect;
import de.zarncke.lib.util.Reflect.Accessor;

public class TestData<T> {
	private final class RepeatValueGenerator<T> implements ValueGenerator {

		List<T> values;
		int idx = 0;

		public RepeatValueGenerator(final List<T> values) {
			this.values = values;
		}

		@Override
		public Object generateFor(final String name, final Type type) {
			return this.values.get(this.idx++ % this.values.size());
		}
	}

	private final class NullGenerator implements ValueGenerator {
		@Override
		public Object generateFor(final String name, final Type type) {
			return null;
		}
	}

	private final class NameGenerator implements ValueGenerator {
		@Override
		public Object generateFor(final String name, final Type type) {
			return name;
		}
	}

	interface Populator {
		void populate(Object o);
	}

	interface ValueGenerator {
		Object generateFor(String name, Type type);
	}

	static class StdPopulator implements Populator {

		private Accessor accessor;
		private Supplier<?> supplier;

		public StdPopulator(final Accessor<?, ?> accessor, final Supplier<?> supplier) {
			this.accessor = accessor;
			this.supplier = supplier;
		}

		@Override
		public void populate(final Object o) {
			this.accessor.set(o, this.supplier.get());
		}
	}

	static class TestDataLimiter<T> {

	}

	private class TestInfo<T> {
		private Class<T> clazz;
		private Type type;
		private Supplier<T> supplier;
		private List<Populator> populators = L.l();

		public TestInfo(final Type type) {
			this.clazz = type instanceof Class<?> ? (Class<T>) type : null;
			this.type = type;
		}

		T create() {
			if (this.supplier == null) {
				this.supplier = new Supplier<T>() {
					@Override
					public T get() {
						try {
							return TestInfo.this.clazz.newInstance();
						} catch (Exception e) {
							throw Warden.spot(new RuntimeException("failed to create instance", e));
						}
					}
				};
				for (Field field : ClassTools.getFields(this.clazz)) {
					final Type fieldType = field.getGenericType();
					Map<Pattern, ValueGenerator> rules = TestData.this.valueRules.get(field.getType());
					if (rules != null) {
						final String name = field.getName();
						for (Map.Entry<Pattern, ValueGenerator> me : rules.entrySet()) {
							if (me.getKey().matcher(name).matches()) {
								final ValueGenerator generator = me.getValue();
								Supplier<Object> supplier = new Supplier<Object>() {
									@Override
									public Object get() {
										return generator.generateFor(name, fieldType);
									}
								};
								field.setAccessible(true);
								Accessor<T, ?> accessor = new Reflect<T>(this.clazz).access(field, null);
								this.populators.add(new StdPopulator(accessor, supplier));
							}
						}
					}
				}
				for (Method m : ClassTools.getMethods(this.clazz)) {

				}
			}
			return this.supplier.get();
		}

		T populate(final T object) {
			for (Populator pop : this.populators) {
				pop.populate(object);
			}
			return object;
		}
	}

	private Class<T> clazz;
	private Type type;

	private Map<Type, TestInfo<?>> testInfos = new HashMap<Type, TestInfo<?>>();

	private Map<Type, Map<Pattern, ValueGenerator>> valueRules = new HashMap<Type, Map<Pattern, ValueGenerator>>();

	public TestData(final Type type) {
		this.clazz = type instanceof Class<?> ? (Class<T>) type : null;
		this.type = type;
		exploreTypes(type, this.testInfos);
	}

	private void exploreTypes(final Type type, final Map<Type, TestInfo<?>> testInfos) {
		if (type == null || testInfos.containsKey(type)) {
			return;
		}
		TestInfo<?> info = new TestInfo(type);
		testInfos.put(type, info);

		if (type instanceof ParameterizedType) {
			ParameterizedType parameterized = (ParameterizedType) type;
			exploreTypes(parameterized.getOwnerType(), testInfos);
			exploreTypes(parameterized.getRawType(), testInfos);
			for (Type arg : parameterized.getActualTypeArguments()) {
				exploreTypes(arg, testInfos);
			}
		} else if (type instanceof Class<?>) {
			Class<?> clazz = (Class<?>) type;
			exploreTypes(clazz.getSuperclass(), testInfos);
			for (Class<?> interf : clazz.getInterfaces()) {
				exploreTypes(interf, testInfos);
			}
			exploreTypes(clazz.getEnclosingClass(), testInfos);
			exploreTypes(clazz.getComponentType(), testInfos);
			exploreMethod(clazz.getEnclosingMethod(), testInfos);
			exploreConstructor(clazz.getEnclosingConstructor(), testInfos);
			exploreAnnotations(clazz.getAnnotations(), testInfos);
			for (Method method : clazz.getDeclaredMethods()) {
				exploreMethod(method, testInfos);
			}
			for (Constructor<?> constructor : clazz.getConstructors()) {
				exploreConstructor(constructor, testInfos);
			}
			for (Field field : clazz.getDeclaredFields()) {
				exploreTypes(field.getType(), testInfos);
				exploreAnnotations(field.getAnnotations(), testInfos);
			}
		}
	}

	private void exploreConstructor(final Constructor<?> constructor, final Map<Type, TestInfo<?>> testInfos) {
		if (constructor == null) {
			return;
		}
		exploreTypes(constructor.getDeclaringClass(), testInfos);
		for (Type paramType : constructor.getGenericParameterTypes()) {
			exploreTypes(paramType, testInfos);
		}
		exploreAnnotations(constructor.getAnnotations(), testInfos);
		for (Annotation[] annots : constructor.getParameterAnnotations()) {
			exploreAnnotations(annots, testInfos);
		}
	}

	private void exploreMethod(final Method method, final Map<Type, TestInfo<?>> testInfos) {
		if (method == null) {
			return;
		}
		exploreTypes(method.getGenericReturnType(), testInfos);
		for (Type paramType : method.getGenericParameterTypes()) {
			exploreTypes(paramType, testInfos);
		}
		for (Type paramType : method.getGenericExceptionTypes()) {
			exploreTypes(paramType, testInfos);
		}
		exploreAnnotations(method.getAnnotations(), testInfos);
		for (Annotation[] annots : method.getParameterAnnotations()) {
			exploreAnnotations(annots, testInfos);
		}
	}

	private void exploreAnnotations(final Annotation[] annotations, final Map<Type, TestInfo<?>> testInfos) {
		for (Annotation a : annotations) {
			exploreTypes(a.annotationType(), testInfos);
		}
	}

	public static <T> TestData<T> in(final Class<T> type) {
		return new TestData<T>(type);
	}

	public T create() {
		T object = createInstance();

		return object;
	}

	private T createInstance() {
		TestInfo<T> info = getInfo(this.clazz);
		T res = info.create();
		info.populate(res);
		return res;
	}

	private TestInfo<T> getInfo(final Type type) {
		return (TestInfo<T>) this.testInfos.get(type);
	}

	public TestData<T> set(final String element, final Object value) {
		getInfo(this.clazz).populators.add(new Populator() {
			@Override
			public void populate(final Object o) {
				try {
					TestData.this.clazz.getField(element).set(o, value);
				} catch (Exception e) {
					throw Warden.spot(new RuntimeException("cannot set " + element + " in " + o, e));
				}
			}
		});
		return this;
	}

	private ValueGenerator findRule(final String name, final Class<?> type) {
		Map<Pattern, ValueGenerator> rules = this.valueRules.get(type);
		for (Map.Entry<Pattern, ValueGenerator> me : rules.entrySet()) {
			if (me.getKey().matcher(name).matches()) {
				return me.getValue();
			}
		}
		return new NullGenerator();
	}

	TestData<T> addRule(final String pattern, final Object value) {
		addRule(pattern, value.getClass(), new ValueGenerator() {
			@Override
			public Object generateFor(final String name, final Type type) {
				return value;
			}
		});
		return this;
	}

	TestData<T> addRule(final String pattern, final Class<?> type, final ValueGenerator generator) {
		for (Class<?> impl : ClassTools.getAllImplementedInterfaces(type, true)) {
			Map<Pattern, ValueGenerator> rules = this.valueRules.get(impl);
			if (rules == null) {
				rules = new LinkedHashMap<Pattern, TestData.ValueGenerator>();
				this.valueRules.put(impl, rules);
			}
			rules.put(Pattern.compile(pattern), generator);
		}
		return this;
	}

	TestData<T> addDefaultRules() {
		addRule("(?i).*firstname.*", String.class,
				new RepeatValueGenerator<String>(L.l("Anton", "Berta", "Charly", "Dan", "Eleanora")));
		addRule("(?i).*lastname.*", String.class,
				new RepeatValueGenerator<String>(L.l("Anderson", "Bauer", "Chan", "Decker", "Ender")));
		addRule("(?i).*name.*", String.class,
				new RepeatValueGenerator<String>(L.l("Anton", "Berta", "Charly", "Dan", "Eleanora")));
		addRule("(?:)count", Integer.class, new ValueGenerator() {
			int index = 0;

			@Override
			public Object generateFor(final String name, final Type type) {
				return Q.i(this.index++);
			}
		});
		addRule(".*", String.class, new NameGenerator());
		addRule(".*", List.class, new ValueGenerator() {
			@Override
			public Object generateFor(final String name, final Type type) {
				if(type instanceof Class<?>) {
					return L.l();
				}
				if(!(type instanceof ParameterizedType)) {
					return null;
				}
				Type elementType = ((ParameterizedType)type).getActualTypeArguments()[0];
				List<Object> res = L.l();
				for (int i = 0; i < 10; i++) {
					res.add(getInfo(elementType).create());
				}
				return res;
			}});
		return this;
	}

}
