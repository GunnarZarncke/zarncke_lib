package de.zarncke.lib.id;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Function;

import de.zarncke.lib.block.Running;
import de.zarncke.lib.coll.L;
import de.zarncke.lib.ctx.Context;
import de.zarncke.lib.err.GuardedTest;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.value.BiTypeMapping;
import de.zarncke.lib.value.Default;
import de.zarncke.lib.value.TypeNameMapping;

public class ResolvingTest extends GuardedTest {

	static class Tid extends Unique<Tid> {
		private static final long serialVersionUID = 1L;

		public Tid(final Gid<Tid> id) {
			super(id);
		}

	}

	public static final Function<Gid<Tid>, Tid> T_RESOLVER = new Function<Gid<Tid>, Tid>() {
		public Tid apply(final Gid<Tid> from) {
			return new Tid(from);
		}
	};

	public static final Function<Collection<? extends Gid<Tid>>, List<Tid>> T_LIST_RESOLVER = new Function<Collection<? extends Gid<Tid>>, List<Tid>>() {
		public List<Tid> apply(final Collection<? extends Gid<Tid>> from) {
			return Factory.getListByInteratingOverElements(from, Factory.forFunction(T_RESOLVER, Tid.class));
		}
	};

	@Test
	public void testFactory() {
		Factory f = new Factory();

		Assert.assertNull(f.get((Gid<?>) null));

		f.register(Tid.class, T_RESOLVER);

		Gid<Tid> id = Gid.ofUtf8("test", Tid.class);

		Tid tid = f.get(id);

		Assert.assertEquals(id, tid.getId());

		Gid<String> sid = Gid.ofUtf8("test", String.class);
		try {
			f.get(sid);
			Assert.fail("should be illegal");
		} catch (IllegalArgumentException e) {
			Warden.disregard(e);
		}
	}

	@Test
	public void testListFactory() {
		Factory f = new Factory();

		// f.register(Tid.class, T_RESOLVER);
		f.registerForList(Tid.class, T_LIST_RESOLVER);

		Gid<Tid> id = Gid.ofUtf8("test", Tid.class);
		Gid<Tid> id2 = Gid.ofUtf8("test2", Tid.class);

		// Tid tid = f.get(id);
		// Assert.assertEquals(id, tid.getId());

		List<Tid> tids = f.get(L.l(id, id2));
		Assert.assertEquals(2, tids.size());
		Assert.assertEquals(id, tids.get(0).getId());
		Assert.assertEquals(id2, tids.get(1).getId());

	}

	@Test
	public void testSimpleTypes() {
		Factory f = new Factory();

		Assert.assertNull(f.get((Gid<?>) null));

		f.register(Long.class, Resolving.LONG_RESOLVER);
		f.register(Integer.class, Resolving.INTEGER_RESOLVER);
		f.register(String.class, Resolving.STRING_RESOLVER);

		check(f, null);
		check(f, Integer.valueOf(-5));
		check(f, Integer.valueOf(5));
		check(f, Long.valueOf(1000000000L));
		check(f, "");
		check(f, "hello world");
		check(f, "hello\n,\täöüworld");

	}

	@Test
	public void testCollectionTypes() {
		final Factory f = new Factory();

		Context.runWith(new Running() {
			public void run() {
				f.register(Collection.class, Resolving.COLLECTION_RESOLVER);
				f.register(Tid.class, T_RESOLVER);

				Collection<Tid> tids = Arrays.asList(new Tid[] { new Tid(id(1)), new Tid(id(2)), new Tid(id(3)) });
				check(f, tids);

				f.register(Long.class, Resolving.LONG_RESOLVER);
				f.register(Integer.class, Resolving.INTEGER_RESOLVER);
				f.register(String.class, Resolving.STRING_RESOLVER);
				Collection<String> cs = Arrays.asList(new String[] { "Hello", "", "World", "!" });
				Collection<Long> ls = Arrays.asList(new Long[] { Long.valueOf(1), Long.valueOf(-1), Long.valueOf(1000) });
				check(f, cs);
				check(f, ls);
			}
		}, Default.of(f, Resolver.class));
	}

	@Test
	public void testClasses() {
		final Factory f = new Factory();
		f.register(Class.class, Resolving.CLASS_RESOLVER);

		check(f, null);
		check(f, String.class);
		check(f, Tid.class);
		check(f, Collection.class);

		BiTypeMapping tnm = new BiTypeMapping();
		tnm.assign(String.class, "S");
		Context.runWith(new Running() {
			public void run() {
				Gid<?> id = check(f, String.class);
				Assert.assertEquals("S", id.toUtf8String());
			}
		}, Default.of(tnm, TypeNameMapping.class));

		check(f, String.class);
	}

	private Gid<Tid> id(final int id) {
		return Gid.of(id, Tid.class);
	}

	private Gid<?> check(final Resolver r, final Object o) {
		Gid<?> id = Resolving.getId(o);
		Assert.assertEquals(o, r.get(id));
		return id;
	}

}
