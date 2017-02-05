package de.zarncke.lib.id;

import java.util.Collection;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import de.zarncke.lib.block.Running;
import de.zarncke.lib.ctx.Context;
import de.zarncke.lib.err.GuardedTest;
import de.zarncke.lib.region.Region;
import de.zarncke.lib.util.Chars;
import de.zarncke.lib.util.ObjectTool;
import de.zarncke.lib.value.Default;

public class IdSerializableTest extends GuardedTest {

	private static final class Id1Resolver implements Resolver {
		private final Ided singleId;

		private Id1Resolver(final Ided id) {
			this.singleId = id;
		}

		public <T> T get(final Gid<T> id) {
			if (!id.equals(this.singleId.getId())) {
				return null;
			}
			@SuppressWarnings("unchecked")
			T cast = (T) this.singleId;
			return cast;
		}

		@Override
		public <T> List<T> get(final Collection<? extends Gid<T>> ids) {
			return Factory.getListByInteratingOverElements(ids, this);
		}
	}

	private static class Ided extends IdSerializable<Ided> {
		private static final long serialVersionUID = 1L;
		{
			setId(Gid.of(1, Ided.class));
		}
		transient String test;
	}

	@Test
	public void testIdSerializing() {
		final Ided i1 = new Ided();
		Resolver res = new Id1Resolver(i1);

		final String str = Chars.repeat("hallo", 1000).toString();
		i1.test = str;
		Context.runWith(new Running() {
			public void run() {
				Region ser = ObjectTool.serialize(i1);
				Assert.assertTrue(
						"we expected significantly less size then " + ser.length() + " because only the id is stored",
						ser.length() < 300);

				Ided i2 = (Ided) ObjectTool.deserialize(ser);
				Assert.assertSame("we expect the same object because the resolver returns it", i1, i2);
				Assert.assertEquals(str, i2.test);
			}
		}, Default.of(res, Resolver.class));
	}
}
