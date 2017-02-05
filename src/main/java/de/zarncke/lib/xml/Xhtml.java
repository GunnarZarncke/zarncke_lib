package de.zarncke.lib.xml;

import java.io.StringWriter;
import java.io.Writer;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import de.zarncke.lib.err.Warden;

/**
 * Simple DSL for creating xhtml.
 * requires Java 6 javax.xml.stream.XMLOutputFactory.
 *
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
public class Xhtml {

	private final Writer writer;
	private XMLStreamWriter xml;
	private String title;
	private boolean pretty = false;

	public Xhtml() {
		this(new StringWriter());
	}

	public Xhtml(final Writer writer) {
		this.writer = writer;
	}

	private void init() throws XMLStreamException {
		if (this.xml == null) {

			XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
			this.xml = outputFactory.createXMLStreamWriter(this.writer);

			this.xml.writeStartDocument();
			this.xml.writeStartElement("html");
			this.xml.writeDefaultNamespace("http://www.w3.org/1999/xhtml");
			if (this.pretty) {
				this.xml.writeCharacters("\n");
			}
			this.xml.writeStartElement("head");
			if (this.title != null) {
				this.xml.writeStartElement("title");
				this.xml.writeCharacters(this.title);
				this.xml.writeEndElement();
				if (this.pretty) {
					this.xml.writeCharacters("\n");
				}
			}
			this.xml.writeEndElement();
			if (this.pretty) {
				this.xml.writeCharacters("\n");
			}
			this.xml.writeStartElement("body");
			if (this.pretty) {
				this.xml.writeCharacters("\n");
			}
		}
	}

	public Xhtml newline() throws XMLStreamException {
		return text("\n");
	}

	public Xhtml title(final String atitle) {
		this.title = atitle;
		return this;
	}

	public Xhtml text(final String text) throws XMLStreamException {
		init();
		this.xml.writeCharacters(text);
		return this;
	}

	public Xhtml format(final String format, final Object... objects) throws XMLStreamException {
		return text(String.format(format, objects));
	}

	public Xhtml a(final String href, final String text) throws XMLStreamException {
		init();
		this.xml.writeStartElement("a");
		this.xml.writeAttribute("href", href);
		this.xml.writeCharacters(text);
		this.xml.writeEndElement();
		return this;
	}

	public Xhtml comment(final String comment) throws XMLStreamException {
		init();
		this.xml.writeComment(comment);
		return this;
	}

	public Xhtml br() throws XMLStreamException {
		init();
		this.xml.writeStartElement("br");
		this.xml.writeEndElement();
		if (this.pretty) {
			this.xml.writeCharacters("\n");
		}
		return this;
	}

	public Xhtml h1(final String text) throws XMLStreamException {
		return tag("h1", text);
	}

	public Xhtml b(final String text) throws XMLStreamException {
		return tag("b", text);
	}

	public Xhtml tag(final String tag, final String text) throws XMLStreamException {
		init();
		this.xml.writeStartElement(tag);
		this.xml.writeCharacters(text);
		this.xml.writeEndElement();
		return this;
	}

	public void done() throws XMLStreamException {
		if (this.pretty) {
			this.xml.writeCharacters("\n");
		}
		this.xml.writeEndElement();
		this.xml.writeEndElement();
		this.xml.writeEndDocument();
		this.xml.flush();
		this.xml.close();
		this.xml = null;
	}

	public String asString() throws XMLStreamException {
		done();
		if (this.writer instanceof StringWriter) {
			return ((StringWriter) this.writer).getBuffer().toString();
		}
		throw Warden.spot(new IllegalStateException("asString can only be called when initialized with StringWriter"));
	}

	public boolean isPretty() {
		return this.pretty;
	}

	public Xhtml setPretty(final boolean pretty) {
		this.pretty = pretty;
		return this;
	}

	public Xhtml setPretty() {
		return setPretty(true);
	}
}
