package de.zarncke.lib.id;

import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;

import de.zarncke.lib.test.ContextUsingTest;
import de.zarncke.lib.time.Times;
import de.zarncke.lib.util.Chars;
import de.zarncke.lib.value.Default;

public class IdTest extends ContextUsingTest {
	@Test
	public void testSimpleIds() {
		Assert.assertEquals("", Gid.ofUtf8("", String.class).toUtf8String());

		Gid<String> id = Gid.ofUtf8("hallo1", String.class);
		Assert.assertEquals(String.class, id.getType());
		Assert.assertEquals("hallo1", id.toUtf8String());
		Assert.assertEquals("aGFsbG8x", id.toBase64String());
		Assert.assertEquals("68616c6c6f31", id.toHexString());

		Gid<String> id2 = Gid.ofUtf8("A", String.class);
		Assert.assertEquals(Integer.valueOf(65), id2.toInteger());
		Assert.assertEquals(Long.valueOf(65), id2.toLong());
	}

	@Test
	public void testCombinedIds() {
		Gid<IdTest> id0 = Ids.makeId(IdTest.class);

		Assert.assertEquals(IdTest.class, id0.getType());
		Assert.assertEquals(0, id0.getIdAsBytes().length);

		Gid<String> id1 = Gid.ofUtf8("hallo", String.class);
		Gid<Integer> id2 = Gid.of(123, Integer.class);
		Gid<Random> id3 = Gid.ofUtf8("test", Random.class);
		Gid<Long> id4 = Gid.ofUtf8(Chars.repeat("12", 100).toString(), Long.class);
		Gid<IdTest> idc = Ids.makeId(IdTest.class, id1, id2, id3, id4);

		Assert.assertEquals(id1, Ids.getPartId(idc, 0, String.class));
		Assert.assertEquals(id2, Ids.getPartId(idc, 1, Integer.class));
		Assert.assertEquals(id3, Ids.getPartId(idc, 2, Random.class));
		Assert.assertEquals(id4, Ids.getPartId(idc, 3, Long.class));
	}

	@Test
	public void testPartIds() {
		testTwoLengthConcat(0, 80);
		for (int i = 0; i < 200; i++) {
			for (int j = 0; j < 200; j++) {
				testTwoLengthConcat(i, j);
			}
		}
	}

	private void testTwoLengthConcat(final int i, final int j) {
		Gid<IdTest> id1 = Gid.ofUtf8(Chars.repeat("-", i).toString(), IdTest.class);
		Gid<IdTest> id2 = Gid.ofUtf8(Chars.repeat("o", j).toString(), IdTest.class);

		byte[] appended = Ids.appendIds(id1.getIdAsBytes(), id2.getIdAsBytes());
		Gid<IdTest> composite = Gid.of(appended, IdTest.class);

		Assert.assertEquals(i + "," + j + ":", id1, Ids.getPartId(composite, 0, IdTest.class));
		Assert.assertEquals(i + "," + j + ":", id2, Ids.getPartId(composite, 1, IdTest.class));

		Assert.assertEquals(i + "/" + j, composite,
				Resolving.fromExternalForm(Resolving.toExternalForm(composite), IdTest.class));
	}

	@Override
	protected Default<?>[] getContextsToApply() {
		Factory factory = new Factory();
		Resolving.registerResolvers(factory);
		return Default.single(factory, Resolver.class);
	}

	@Override
	protected long getMaximumTestMillis() {
		return 10 * Times.MILLIS_PER_SECOND;
	}
}
