package de.zarncke.lib.io;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import de.zarncke.lib.err.GuardedTest;

public class AutoMarkStreamTest extends GuardedTest {
	public void testAutoMarking() throws Exception {
		InputStream ins = new ByteArrayInputStream("ibcdefgihjhijklmnopq".getBytes());

		AutoMarkInputStream amis = new AutoMarkInputStream(ins, new byte[][] { "cde".getBytes(), "ij".getBytes() });

		byte[] ba = new byte[3];
		amis.read(ba);
		assertEquals((byte) 'i', ba[0]);
		assertEquals((byte) 'b', ba[1]);
		assertEquals((byte) 'c', ba[2]);
		assertEquals(3, amis.getSinceLastMark());

		amis.read(ba);
		assertEquals((byte) 'd', ba[0]);
		assertEquals((byte) 'e', ba[1]);
		assertEquals((byte) 'f', ba[2]);
		assertEquals(1, amis.getSinceLastMark());

		// the pattern should have matched
		amis.reset();
		amis.read(ba);
		assertEquals((byte) 'f', ba[0]);
		assertEquals((byte) 'g', ba[1]);
		assertEquals((byte) 'i', ba[2]);
		assertEquals(3, amis.getSinceLastMark());

		amis.read(ba);
		assertEquals((byte) 'h', ba[0]);
		assertEquals((byte) 'j', ba[1]);
		assertEquals((byte) 'h', ba[2]);
		assertEquals(6, amis.getSinceLastMark());

		assertEquals('i', amis.read());
		assertEquals(7, amis.getSinceLastMark());
		assertEquals('j', amis.read());
		assertEquals(0, amis.getSinceLastMark());

		// should have matched
		amis.skip(4);
		amis.reset();
		assertEquals(0, amis.getSinceLastMark());

		amis.read(ba);
		assertEquals((byte) 'k', ba[0]);
		assertEquals((byte) 'l', ba[1]);
		assertEquals((byte) 'm', ba[2]);
		assertEquals(3, amis.getSinceLastMark());
	}
}
