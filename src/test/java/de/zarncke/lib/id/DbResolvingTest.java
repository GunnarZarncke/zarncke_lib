package de.zarncke.lib.id;

import java.io.Serializable;

import junit.framework.Assert;

import org.junit.Test;

import de.zarncke.lib.block.Running;
import de.zarncke.lib.ctx.Context;
import de.zarncke.lib.db.DbUsingTest;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.id.Ids.IdInjected;
import de.zarncke.lib.value.Default;

public class DbResolvingTest extends DbUsingTest {

	static class T1 implements Serializable, IdProposer {
		private static final long serialVersionUID = 1L;
		String str;
		int i;

		private T1(final String str, final int i) {
			super();
			this.str = str;
			this.i = i;
		}

		@Override
		public String toString() {
			return super.hashCode() + "->" + this.str + "," + this.i;
		}

		@Override
		public String getIdProposal() {
			return this.str;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + this.i;
			result = prime * result + (this.str == null ? 0 : this.str.hashCode());
			return result;
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			T1 other = (T1) obj;
			if (this.i != other.i) {
				return false;
			}
			if (this.str == null) {
				if (other.str != null) {
					return false;
				}
			} else if (!this.str.equals(other.str)) {
				return false;
			}
			return true;
		}

	}

	public static class T2 extends Unique<T2> {
		private static final long serialVersionUID = 1L;
		// only to enforce different serializations
		@SuppressWarnings("unused")
		private String val;

		public T2() {
			super(null);
		}

		public T2(final Gid<T2> id, final String val) {
			super(id);
			this.val = val;
		}
	}

	public static class T3 implements IdInjected<T3>, Serializable {
		private static final long serialVersionUID = 1L;
		private Gid<? extends T3> id;

		private final String data;

		protected String getData() {
			return this.data;
		}

		protected Gid<? extends T3> getId() {
			return this.id;
		}

		private T3(final String data) {
			this.data = data;
		}

		@Override
		public void setId(final Gid<? extends T3> id) {
			this.id = id;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (this.data == null ? 0 : this.data.hashCode());
			return result;
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			T3 other = (T3) obj;
			if (this.data == null) {
				if (other.data != null) {
					return false;
				}
			} else if (!this.data.equals(other.data)) {
				return false;
			}
			return true;
		}

	}

	@Override
	protected void setUpInTransaction() {
		super.setUpInTransaction();
		SerializingDbResolver.createSchema();
	}

	@Test
	public static void testDbResolverString() {
		final SerializingDbResolver<String> sdr = SerializingDbResolver.of(String.class);

		Factory factory = new Factory();
		factory.register(String.class, sdr);
		Context.runWith(new Running() {
			@Override
			public void run() {
				Assert.assertNull(sdr.apply(null));
				@SuppressWarnings("unchecked")
				Gid<String> hi = (Gid<String>) SerializingDbResolver.remember("hallo");
				SerializingDbResolver.dumpSchema();
				Assert.assertEquals("hallo", sdr.apply(hi));
				// may not match with other type
				try {
					@SuppressWarnings("unchecked" /*other id types should fail*/)
					String notExpected = sdr.apply(new Gid(hi.getIdAsBytes(), Long.class));
					Assert.assertNull(notExpected);
					fail("should raise error");
				} catch (IllegalArgumentException e) {
					Warden.disregard(e);
					// expected
				}
			}
		}, Default.of(factory, Resolver.class));
	}

	@Test
	public static void testDbResolverProposer() {
		final SerializingDbResolver<T1> sdr = SerializingDbResolver.of(T1.class);
		Factory factory = new Factory();
		factory.register(T1.class, sdr);
		Context.runWith(new Running() {
			@Override
			public void run() {
				Assert.assertNull(sdr.apply(null));

				T1 t1 = new T1("hallo", 1);
				T1 t2 = new T1("hallo", 2);

				Assert.assertNull(sdr.apply(Gid.ofUtf8("hallo", T1.class)));
				// note: this implicitly tests non-caching of null resolves

				@SuppressWarnings("unchecked" /*we know the type*/)
				Gid<T1> i1 = (Gid<T1>) SerializingDbResolver.remember(t1);
				Assert.assertEquals("hallo", i1.toUtf8String());
				Assert.assertEquals(t1, sdr.apply(i1));

				@SuppressWarnings("unchecked")
				Gid<T1> i2 = (Gid<T1>) SerializingDbResolver.remember(t2);
				Assert.assertEquals(t2, sdr.apply(i2));
			}
		}, Default.of(factory, Resolver.class));
	}


	@Test
	public static void testDbResolverHasId() {
		final SerializingDbResolver<T2> sdr = SerializingDbResolver.of(T2.class);
		Factory factory = new Factory();
		factory.register(T2.class, sdr);
		Context.runWith(new Running() {
			@Override
			public void run() {
				Assert.assertNull(sdr.apply(null));

				T2 t1 = new T2(Gid.ofUtf8("a", T2.class), "a");
				T2 t2 = new T2(Gid.ofUtf8("b", T2.class), "b");

				Assert.assertNull(sdr.apply(Gid.ofUtf8("a", T2.class)));

				@SuppressWarnings("unchecked" /*we know the type*/)
				Gid<T2> i1 = (Gid<T2>) SerializingDbResolver.remember(t1);
				Assert.assertEquals(t1.getId(), i1);
				Assert.assertEquals("a", i1.toUtf8String());
				Assert.assertEquals(t1, sdr.apply(i1));

				@SuppressWarnings("unchecked")
				Gid<T2> i2 = (Gid<T2>) SerializingDbResolver.remember(t2);
				Assert.assertEquals(t2.getId(), i2);
				Assert.assertEquals(t2, sdr.apply(i2));
			}
		}, Default.of(factory, Resolver.class));
	}

	@Test
	public static void testDbResolverIdInjected() {
		final SerializingDbResolver<T3> sdr = SerializingDbResolver.of(T3.class);
		Factory factory = new Factory();
		factory.register(T3.class, sdr);
		Context.runWith(new Running() {
			@Override
			public void run() {
				Assert.assertNull(sdr.apply(null));

				T3 t1 = new T3("a");
				T3 t2 = new T3("b");

				Assert.assertNull(sdr.apply(Gid.ofUtf8("a", T3.class)));

				@SuppressWarnings("unchecked" /*we know the type*/)
				Gid<T3> i1 = (Gid<T3>) SerializingDbResolver.remember(t1);
				Assert.assertNotNull(i1);
				Assert.assertEquals(t1.getId(), i1);
				Assert.assertEquals(t1, sdr.apply(i1));
				Assert.assertEquals("a", sdr.apply(i1).getData());

				@SuppressWarnings("unchecked")
				Gid<T3> i2 = (Gid<T3>) SerializingDbResolver.remember(t2);
				Assert.assertFalse("different ids", i1.equals(i2));
				Assert.assertEquals(t2.getId(), i2);
				Assert.assertEquals(t2, sdr.apply(i2));
				Assert.assertEquals("b", sdr.apply(i2).getData());

				Assert.assertEquals(t1, sdr.apply(i1));
			}
		}, Default.of(factory, Resolver.class));
	}


}
