package de.zarncke.lib.init;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class is part of the Boot test which tests that a (main) class can be loaded from a (nested) archive.
 * Do not call. The main method only works in conjunction with {@link BootTest}.
 *
 * @author Gunnar Zarncke
 */
public class BootDummy {

	/**
	 * This main method is called by the booting process. It is part of the test! Do not call.
	 *
	 * @param args
	 * @throws IOException
	 */
	public static void main(final String[] args) throws IOException {
		InputStream ins = BootDummy.class.getResourceAsStream("test.resource");
		if (ins == null) {
			throw new IllegalArgumentException("expected to be able to load resource from the declared Class-Path.");
		}
		// tests that the content matches
		String expected = "Hello World";
		byte[] ba = new byte[expected.length()];
		ins.read(ba);
		if (!new String(ba, "ASCII").equals(expected)) {
			throw new IllegalStateException("content of resource doesn't match");
		}
	}
}
