package de.zarncke.lib.math;

import java.io.UnsupportedEncodingException;

import junit.framework.TestCase;

import de.zarncke.lib.err.CantHappenException;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.region.Binary;
import de.zarncke.lib.region.Region;
import de.zarncke.lib.region.RegionUtil;

public class CompactSetTest extends TestCase {
	class Str implements Binary {
		private final String str;

		public Str(final String str) {
			super();
			this.str = str;
		}

		@Override
		public Region encode() {
			try {
				return RegionUtil.asRegion(this.str.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw Warden.spot(new CantHappenException("", e));
			}
		}
	}


	public void testSimple() {
		CompactSet<Str> cs = new CompactSet<Str>(new Binary.Decoder<Str>() {
			@Override
			public Str decode(final Region data) {
				try {
					return new Str(new String(data.toByteArray(), "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					throw Warden.spot(new CantHappenException("UTF-8?", e));
				}
			}
		});
}
}
