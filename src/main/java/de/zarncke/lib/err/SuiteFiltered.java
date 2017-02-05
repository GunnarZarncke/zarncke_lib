package de.zarncke.lib.err;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.List;

import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

import de.zarncke.lib.block.Running;
import de.zarncke.lib.coll.Elements;
import de.zarncke.lib.coll.L;
import de.zarncke.lib.ctx.Context;
import de.zarncke.lib.test.Tests;
import de.zarncke.lib.value.Default;

/**
 * Extends JUnit 4 {@link Suite} with filtering and decoration.
 * <ul>
 * <li>Test class lists can also be provided dynamically by {@link WithClassesFrom} (implementing
 * {@link TestClassProvider}.</li>
 * <li>Enumerated test classes are run by {@link Guarded} instead of JUnit4 default runner.</li>
 * <li>Nested suites are converted to {@link SuiteFiltered}.</li>
 * </ul>
 *
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
public class SuiteFiltered extends Suite {

	public static interface TestClassProvider {
		Collection<Class<?>> getTests() throws InitializationError;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@Inherited
	public @interface WithClassesFrom {
		Class<? extends TestClassProvider>[] value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@Inherited
	public @interface RunCategories {
		Class<?>[] value();
	}

	private final Default<?>[] context;

	public SuiteFiltered(final Class<?> klass, final RunnerBuilder builder) throws InitializationError {
		// super(builder, klass, getAnnotatedClasses(klass));
		super(klass, determineRunner(klass, builder));
		this.context = Guarded.getContext(klass);
	}

	private static List<Runner> determineRunner(final Class<?> klass, final RunnerBuilder builder)
			throws InitializationError {
		List<Runner> runners = builder.runners(klass, getAnnotatedClasses(klass));
		for (int i = 0; i < runners.size(); i++) {
			Runner r = runners.get(i);
			// replace created junit4 runners with Guarded runner
			if (r instanceof BlockJUnit4ClassRunner) {
				Class<?> childClass = ((BlockJUnit4ClassRunner) r).getTestClass().getJavaClass();
				runners.set(
						i,
						new Guarded(childClass, builder, mergeCategories(getCategories(klass),
								getCategories(childClass))));
			}
			if (r instanceof Suite) {
				Class<?> childClass = ((Suite) r).getTestClass().getJavaClass();
				// TODO filter nested suites by inner AND outer categories?
				runners.set(i, new SuiteFiltered(childClass, builder));
			}
		}
		return runners;
	}

	private static Class<?>[] mergeCategories(final Class<?>[] a, final Class<?>[] b) {
		if (a == null) {
			return b;
		}
		if (b == null) {
			return a;
		}
		return Elements.concat(a, b);
	}

	@Override
	public void run(final RunNotifier notifier) {
		if (this.context != null && this.context.length > 0) {
			Context.runWith(new Running() {
				@Override
				public void run() {
					superRun(notifier);
				}
			}, this.context);
		} else {
			superRun(notifier);
		}
	}

	private void superRun(final RunNotifier notifier) {
		super.run(notifier);
	}

	private static Class<?>[] getAnnotatedClasses(final Class<?> klass) throws InitializationError {
		Collection<Class<?>> tests = L.l();

		SuiteClasses annotation = klass.getAnnotation(SuiteClasses.class);
		if (annotation != null) {
			tests.addAll(L.l(annotation.value()));
		}

		WithClassesFrom classProvider = klass.getAnnotation(WithClassesFrom.class);
		if (classProvider != null) {
			Class<? extends TestClassProvider>[] classProviders = classProvider.value();
			for (Class<? extends TestClassProvider> providerClass : classProviders) {
				try {
					tests.addAll(providerClass.newInstance().getTests());
				} catch (InstantiationException e) {
					throw new InitializationError(e);
				} catch (IllegalAccessException e) {
					throw new InitializationError(e);
				}
			}
		}

		Collection<Class<?>> filtered = L.l();
		Class<?>[] categories = getCategories(klass);
		if (categories != null) {
			for (Class<?> test : tests) {
				if (Tests.hasClassMatchingCategory(test, categories)) {
					filtered.add(test);
				}
				// note: this only excludes classes anotated with the category, methods will be handled below
			}
			tests = filtered;
		}

		if (tests.isEmpty()) {
			throw new InitializationError(String.format("class '%s' must have a SuiteClasses or WithClassesFrom "
					+ "annotation and there must be any test left after filtering", klass.getName()));
		}

		return tests.toArray(new Class<?>[tests.size()]);
	}

	private static Class<?>[] getCategories(final Class<?> klass) {
		RunCategories categories = klass.getAnnotation(RunCategories.class);
		if (categories == null) {
			return null;
		}
		Class<?>[] cats = categories.value();
		return cats == null ? new Class<?>[0] : cats;
	}

}
