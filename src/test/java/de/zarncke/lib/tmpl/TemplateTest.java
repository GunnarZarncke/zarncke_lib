package de.zarncke.lib.tmpl;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import junit.framework.TestCase;

import de.zarncke.lib.io.IOTools;

public class TemplateTest extends TestCase {
	public void testTrivial() {
		assertRender(new Template(""), "");
		assertRender(new Template("simple"), "simple");
		assertRender(new Template("\n"), "\n");
		assertRender(new Template("hello\nworld\n"), "hello\nworld\n");
	}

	public void testShell() {
		Template t = new Template("hello ${X}end\n", Template.SHELL_PATTERN);
		assertRender(t, "hello end\n");
		t.clear();
		assertRender(t, "hello end\n");
		t.put("X", "world ");
		assertRender(t, "hello world end\n");
	}

	public void testShell2() {
		Template t = new Template("hello ${X}and ${X}end\n", Template.SHELL_PATTERN);
		assertRender(t, "hello and end\n");
		t.clear();
		assertRender(t, "hello and end\n");
		t.put("X", "world ");
		assertRender(t, "hello world and world end\n");
	}

	public void testSimple() {
		Template t = new Template("hello\n#ifdef X\npart 1\n#else\npart 2\n#endif /*X*/\nend\n");
		assertRender(t, "hello\npart 1\n#else\npart 2\nend\n");
		t.clear();
		assertRender(t, "hello\nend\n");
		t.put("X", "world\n");
		assertRender(t, "hello\nworld\nend\n");
	}

	public void testCopy() {
		Template t = new Template("hello\n#ifdef X\npart 1\n#endif /*X*/\nend\n");
		t.clear();
		assertRender(t, "hello\nend\n");
		t.put("X", "world\n");
		assertRender(t, "hello\nworld\nend\n");

		Template tcp = new Template(t);
		assertRender(tcp, "hello\nworld\nend\n");
		tcp.put("X", "other\n");
		assertRender(tcp, "hello\nother\nend\n");
		assertRender(t, "hello\nworld\nend\n");
	}

	public void test4inArow() {
		Template t4 = new Template("#ifdef 1\n1\n#endif\n#ifdef 2\n2\n#endif\n#ifdef 3\n3\n#endif\n#ifdef 4\n4\n#endif\n");
		assertRender(t4, "1\n2\n3\n4\n");
		t4.clear();
		assertRender(t4, "");
		t4.put("2", "two\n");
		t4.put("4", "four\n");
		t4.put("1", "one\n");
		t4.put("3", "three\n");
		assertRender(t4, "one\ntwo\nthree\nfour\n");
	}

	public void testFactory() throws IOException {
		TemplateFactory tf = new TemplateFactory();

		File f = File.createTempFile("test", ".template");
		f.deleteOnExit();
		IOTools.dump("hello", f);
		Template t = tf.forFile(f);

		assertRender(t, "hello");
	}

	private void assertRender(final Template template, final String expected) {
		StringWriter sw = new StringWriter();
		try {
			template.renderTo(sw);
		} catch (IOException e) {
			throw new RuntimeException("failed", e);
		}
		assertEquals(expected, sw.toString());
	}

}
