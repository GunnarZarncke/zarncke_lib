package de.zarncke.lib.xml;

import javax.xml.stream.XMLStreamException;

import de.zarncke.lib.err.GuardedTest;

public class XhtmlTest extends GuardedTest {
	public void testXhtml() throws XMLStreamException {
		assertEquals("<?xml version=\"1.0\" ?><html xmlns=\"http://www.w3.org/1999/xhtml\">"
				+ "<head><title>My page</title>" //
				+ "</head>"
				+ "<body>" //
				+ "<h1>head</h1>" + "Hello World" //
				+ "<br></br>" //
				+ "<a href=\"http://zarncke.de/\">lib</a>" //
				+ "</body></html>", //
				new Xhtml().title("My page").h1("head")
				.text("Hello World").br().a("http://zarncke.de/", "lib").asString());
	}

	public void testXhtmlPretty() throws XMLStreamException {
		assertEquals(
				"<?xml version=\"1.0\" ?><html xmlns=\"http://www.w3.org/1999/xhtml\">\n"
						+ "<head><title>My page</title>\n" //
						+ "</head>\n" + "<body>\n" //
						+ "<h1>head</h1>" + "Hello World" //
						+ "<br></br>\n" //
						+ "<a href=\"http://zarncke.de/\">lib</a>\n" //
						+ "</body></html>", //
				new Xhtml().title("My page").setPretty().h1("head").text("Hello World").br()
						.a("http://zarncke.de/", "lib").asString());
	}

}
