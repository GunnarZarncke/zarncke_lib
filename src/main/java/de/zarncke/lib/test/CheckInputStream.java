/**
 * 
 */
package de.zarncke.lib.test;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Assert;

public class CheckInputStream extends InputStream {
	private final InputStream is1;
	private final InputStream is2;
	int pos = 0;

	public CheckInputStream(final InputStream is1, final InputStream is2) {
		this.is1 = is1;
		this.is2 = is2;
	}

	@Override
	public int read(final byte[] b, final int off, final int len) throws IOException {
		int r1 = this.is1.read(b, off, len);
		byte[] bb = new byte[len];
		int r2 = this.is2.read(bb);
		Assert.assertTrue("invalid " + r1 + " at pos" + this.pos, r1 <= len);
		Assert.assertTrue("invalid " + r2 + " at pos" + this.pos, r2 <= len);
		for (int i = 0; i < r1 && i < r2; i++) {
			Assert.assertEquals("differ at" + this.pos, b[off + i], bb[i]);
			this.pos++;
		}
		for (int i = r2; i < r1; i++) {
			int b2 = this.is1.read();
			Assert.assertEquals("differ at" + this.pos, b[off + i], (byte) b2);
			this.pos++;
		}
		for (int i = r1; i < r2; i++) {
			int b2 = this.is2.read();
			Assert.assertEquals("differ at" + this.pos, b[off + i], (byte) b2);
			this.pos++;
		}
		return r1;
	}

	@Override
	public int read() throws IOException {
		int a = this.is2.read();
		int b = this.is1.read();
		Assert.assertEquals("differ at" + this.pos, a, b);
		this.pos++;
		return a;
	}
}