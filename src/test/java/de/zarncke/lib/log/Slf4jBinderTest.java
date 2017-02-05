package de.zarncke.lib.log;

import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.zarncke.lib.err.GuardedTest4;

public class Slf4jBinderTest extends GuardedTest4 {
	@Test
	public void testBilder() throws Exception {
		Logger l = LoggerFactory.getLogger(Slf4jBinderTest.class);
		l.warn("test"); // this slf4j call should be routed to Log

		List<Object> res = ((BufferedLog) getBufferLog()).removeAndReturn();
		assertEquals(1, res.size());
		assertEquals("test", res.get(0));
	}
}
