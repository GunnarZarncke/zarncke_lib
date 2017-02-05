package de.zarncke.lib.www;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.err.ExceptionUtil;
import de.zarncke.lib.io.IOTools;
import de.zarncke.lib.log.Log;

/**
 * Http container which support WebDAV (basically). Currently doesn't work with Windows.
 *
 * @author Gunnar Zarncke
 */
public class WebService implements Container {
	private static final String MIMETYPE_HTML = "text/html";

	public interface Content {
		Result get(String path) throws Exception;

		void addName(Object it, Map m);

		Object getProp(String path);

		List<Object> children(Object base);

		String getPath(Object it);

		Date getCreationDate(Object it);

		Result putOrPost(String path, String ctIn, byte[] bin, boolean isPost);

		void stopAll();

		String getCursor(Object it);

		Object getMime(Object it);
	}

	public static class Result {
		public Object obj;

		public int code;

		public String type;
	}

	private static final String HEADER_CONTENT_TYPE = "Content-Type";

	private static class ErrorMethod implements HttpMethod {
		public void execute(final Request req, final Response resp) throws IOException {
			resp.set("DAV", "1");
			resp.setCode(501);
			byte[] ba = "unsupported operation".getBytes();
			OutputStream os = resp.getOutputStream(ba.length);
			os.write(ba);
			os.close();
		}
	}

	private class GetMethod implements HttpMethod {
		private final boolean isHead;

		public GetMethod(final boolean isHead) {
			this.isHead = isHead;
		}

		public void execute(final Request req, final Response resp) throws IOException {
			resp.set("DAV", "1");
			resp.setDate("Last-Modified", System.currentTimeMillis());

			String path = preprocessUri(req.getPath().getPath());

			int code;
			Object obj;
			String ct = "text/plain";
			try {
				Result res = WebService.this.content.get(path);
				code = res.code;
				obj = res.obj;
				if (res.code == 302) {
					resp.set("Location", obj.toString());
				}
				if (res.type != null) {
					ct = res.type;
				}
			} catch (Throwable t) { // NOPMD
				Log.LOG.get().report(t);
				obj = ExceptionUtil.getStackTrace(t).getBytes();
				code = 500;
			}
			byte[] ba;
			if (obj instanceof byte[]) {
				ba = (byte[]) obj;
			} else {
				ba = "no byte[] found".getBytes();
				code = 500;
			}

			resp.set("Content-Type", ct);
			resp.setCode(code);
			OutputStream os = resp.getOutputStream(ba.length);
			if (this.isHead) {
				resp.setContentLength(0);
			} else {
				os.write(ba);
			}
			os.close();
		}
	}

	private interface HttpMethod {
		public void execute(Request req, Response resp) throws IOException;
	}

	private class OptionsMethod implements HttpMethod {
		public void execute(final Request req, final Response resp) throws IOException {
			String path = req.getPath().getPath();
			log(path, "");

			// if ( "*".equals(path) )
			// {
			//
			// }
			// else
			// {
			resp.set("Allow", "COPY,DELETE,GET,HEAD,MKCOL,MOVE,OPTIONS,PROPFIND,PROPPATCH,PUT");
			resp.set("DAV", "1");
			// hack for M$:
			resp.set("MS-Author-Via", "DAV");
			// }

			resp.setCode(200);
			resp.setContentLength(0);
			resp.getOutputStream().close();
		}
	}

	private class PropMethod implements HttpMethod {
		public void execute(final Request req, final Response resp) throws IOException {
			resp.set("DAV", "1");

			String path = preprocessUri(req.getPath().getPath());
			// TODO: this is a workaround for WebDAV collection handling!
			if (path.endsWith("/")) {
				path = path.substring(0, path.length() - 1);
			}

			byte[] ba;

			try {
				Object base;
				try {
					base = WebService.this.content.getProp(path);
					log(path, base);
				} catch (Throwable t) { // NOPMD
					Log.LOG.get().report(t);
					throw new StatusCodeExit(500, ExceptionUtil.getStackTrace(t));
				}

				String dep = req.getValue("Depth");
				int d;
				if (dep == null || "infinity".equals(dep)) {
					d = -1;
				} else {
					try {
						d = Integer.parseInt(dep);
					} catch (NumberFormatException nfe) {
						throw new StatusCodeExit(400, "Depth invalid");
					}
				}

				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				dbf.setValidating(false);
				dbf.setNamespaceAware(true);

				DocumentBuilder db = dbf.newDocumentBuilder();
				db.setErrorHandler(new ErrorHandler() {

					public void error(final SAXParseException exception) throws SAXException {
						exception.printStackTrace(System.out);
					}

					public void fatalError(final SAXParseException exception) throws SAXException {
						exception.printStackTrace(System.out);
						throw exception;
					}

					public void warning(final SAXParseException exception) throws SAXException {
						exception.printStackTrace(System.out);
					}
				});
				InputStream ins = req.getInputStream();
				byte[] iba = IOTools.getAllBytes(ins);
				if (iba.length == 0) {
					makeMultiStatus(WebService.this.baseUri, L.l(base), resp);
					return;
					// throw new StatusCodeExit(400, "missing xml body");
				}
				System.out.println(new String(iba));
				Document doc = db.parse(new ByteArrayInputStream(iba));// ins);

				Element rootElement = doc.getDocumentElement();
				if (!DAV_NAMESPACE.equals(rootElement.getNamespaceURI()) || !"propfind".equals(rootElement.getLocalName())) {
					throw new StatusCodeExit(400, "missing root element DAV:propfind");
				}

				NodeList nl = rootElement.getChildNodes();
				for (int i = 0; i < nl.getLength(); i++) {
					Node n = nl.item(i);
					if (n.getNodeType() == Node.ELEMENT_NODE) {
						if (DAV_NAMESPACE.equals(n.getNamespaceURI())) {
							String nn = n.getLocalName();
							if ("prop".equals(nn)) {
								Collection rprops = new HashSet(8);
								NodeList nl2 = n.getChildNodes();
								for (int i2 = 0; i2 < nl2.getLength(); i2++) {
									Node n2 = nl2.item(i2);
									if (n2.getNodeType() == Node.ELEMENT_NODE) {
										rprops.add(n2.getNamespaceURI() + n2.getLocalName());
									}
								}

								makeMultiStatus(WebService.this.baseUri, findThemByDepth(base, d), resp);

								return;
							}
							if ("allprop".equals(nn)) {
								// Collection rprops = getProps(base).keySet();

								makeMultiStatus(WebService.this.baseUri, findThemByDepth(base, d), resp);
								return;
							}
							if ("propname".equals(nn)) {
								// Set s = getProps(base).keySet();

								makeMultiStatus(WebService.this.baseUri, Collections.EMPTY_LIST, resp);
								return;
							}
						}
					}
				}

				resp.setCode(500);
				ba = "unsupported operation".getBytes();
			} catch (ParserConfigurationException e) {
				resp.setCode(500);
				String err = "parser config excep " + ExceptionUtil.getStackTrace(e);
				ba = err.getBytes();
			} catch (SAXException e) {
				resp.setCode(422);
				String err = "parser excep " + ExceptionUtil.getStackTrace(e);
				ba = err.getBytes();
			} catch (StatusCodeExit e) {
				resp.setCode(e.getCode());
				String err = e.getMessage();
				ba = err.getBytes();
			}

			log(path, resp.getCode() + "->" + new String(ba));

			OutputStream os = resp.getOutputStream(ba.length);
			os.write(ba);
			os.close();
		}

	}

	private class PropPatchMethod implements HttpMethod {
		public void execute(final Request req, final Response resp) throws IOException {
			String path = req.getPath().getPath();
			log(path, "");

			resp.setCode(403);
			resp.setContentLength(0);
			resp.getOutputStream().close();
		}
	}

	private class PutMethod implements HttpMethod {

		private final boolean isPost;

		public PutMethod(final boolean isPost) {
			this.isPost = isPost;
		}

		public void execute(final Request req, final Response resp) throws IOException {
			// dav req headers:
			// If = "If" ":" ( 1*No-tag-list | 1*Tagged-list)
			// Depth = "Depth" ":" ("0" | "1" | "infinity")
			// COPY+MOVE:
			// Destination = "Destination" ":" absoluteURI
			// Overwrite = "Overwrite" ":" ("T" | "F")

			// responses:
			// Status-URI = "Status-URI" ":" *(Status-Code Coded-URL)

			resp.set("DAV", "1");
			resp.setDate("Last-Modified", System.currentTimeMillis());

			String path = preprocessUri(req.getPath().getPath());

			String ctIn = req.getValue(HEADER_CONTENT_TYPE);

			InputStream ins = req.getInputStream();
			byte[] bin = IOTools.getAllBytes(ins);

			System.out.println("PUT " + new String(bin));

			int code = 404;
			String ct = MIMETYPE_HTML;
			Object obj;
			try {
				Result res = WebService.this.content.putOrPost(path, ctIn, bin, this.isPost);
				obj = res.obj;
				code = res.code;
				if (res.type != null) {
					ct = res.type;
				}
			} catch (Throwable t) { // NOPMD
				Log.LOG.get().report(t);
				obj = ExceptionUtil.getStackTrace(t).getBytes();
				code = 500;
			}
			byte[] ba;
			if (obj instanceof byte[]) {
				ba = (byte[]) obj;
			} else {
				code = 500;
				ba = "error".getBytes();
			}

			resp.set(HEADER_CONTENT_TYPE, ct);
			resp.setCode(code);
			OutputStream os = resp.getOutputStream(ba.length);
			os.write(ba);
			os.close();
		}
	}

	private static class StatusCodeExit extends Exception {
		private final int code;

		public StatusCodeExit(final int code, final String msg) {
			super(msg);
			this.code = code;
		}

		public int getCode() {
			return this.code;
		}

		@Override
		public String toString() {
			return this.code + " " + super.toString();
		}
	}

	private static final String DAV_NAMESPACE = "DAV:";

	private static final String DAV_PREFIX = "D";

	private static final int INFINITE_DEPTH = 6;

	// Map of String (propname) to propvalue
	private Map getProps(final Object it, final Document doc, final boolean asCollection) {
		Map m = new HashMap(12);

		this.content.addName(it, m);

		String path = this.content.getPath(it);
		int p = path.lastIndexOf("/");
		if (p >= 0) {
			path = path.substring(p + 1);
		}
		if (asCollection) {
			path = ".." + path;
		}

		Date date = this.content.getCreationDate(it);

		m.put("DAV:creationdate", date);
		m.put("DAV:displayname", path);
		String len = "0";
		m.put("DAV:getcontentlength", len);
		m.put("DAV:getcontenttype", this.content.getMime(it));
		m.put("DAV:getetag", "xyz");
		m.put("DAV:getlastmodified", date);
		if (asCollection) {
			Element el = doc.createElementNS(DAV_NAMESPACE, "collection");
			el.setPrefix(DAV_PREFIX);
			m.put("DAV:resourcetype", el);
		} else {
			m.put("DAV:resourcetype", "");
		}
		m.put("DAV:supportedlock", "");
		m.put("DAV:lockdiscovery", "");
		m.put("DAV:source", "");
		/*
		 * <D:link> <F:projfiles>Makefile </F:projfiles> <D:src>http://foo.bar/program </D:src>
		 * <D:dst>http://foo.bar/src/makefile </D:dst> </D:link>
		 */
		m.put("DAV:executable", "false");
		return m;
	}

	private final String baseUri;

	private final HttpMethod defaultMethod = new ErrorMethod();

	private final Map methods = new HashMap();

	Content content;

	private final File docRoot;

	public WebService(final String baseUri, final String docRoot, final Content content) {
		this.docRoot = new File(docRoot);
		this.baseUri = baseUri;
		this.content = content;

		this.methods.put("GET", new GetMethod(false));
		this.methods.put("PUT", new PutMethod(false));
		this.methods.put("POST", new PutMethod(true));
		this.methods.put("HEAD", new GetMethod(true));
		this.methods.put("OPTIONS", new OptionsMethod());
		this.methods.put("PROPFIND", new PropMethod());
		this.methods.put("PROPPATCH", new PropPatchMethod());
		this.methods.put("LOCK", new ErrorMethod());
		this.methods.put("UNLOCK", new ErrorMethod());
		this.methods.put("COPY", new ErrorMethod());
		this.methods.put("MOVE", new ErrorMethod());
		this.methods.put("MKCOL", new ErrorMethod());
		this.methods.put("DELETE", new ErrorMethod());
	}

	private Collection findThemByDepth(final Object base, final int d) {
		Collection c = new LinkedList();
		findThemByDepth(base, d < 0 ? INFINITE_DEPTH : d, c);
		return c;
	}

	private void findThemByDepth(final Object base, final int d, final Collection accu) {
		accu.add(base);
		if (d > 0) {
			for (Object child : this.content.children(base)) {
				findThemByDepth(child, d - 1, accu);
			}
		}
	}

	private void log(final String path, final Object res) {
		System.out.println(path + "->" + res);
	}

	// prefix = url part before path
	// Collection of It
	private void makeMultiStatus(final String baseUri, final Collection coll, final Response resp) throws IOException {
		resp.setCode(207);
		resp.set("Content-Type", "text/xml; charset=\"utf-8\"");
		OutputStream outs = resp.getOutputStream();
		Document doc;
		try {
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

			Element multistatus = doc.createElementNS(DAV_NAMESPACE, "multistatus");
			multistatus.setPrefix(DAV_PREFIX);
			// System.out.println(multistatus2.getPrefix()+" "+
			// multistatus2.getNamespaceURI()+" " +multistatus2.getLocalName()+"
			// " +multistatus2.getTagName());
			for (Iterator iter = coll.iterator(); iter.hasNext();) {
				Object element = iter.next();

				multistatus.appendChild(makeResponseXml(element, baseUri, doc, df, true));
				multistatus.appendChild(makeResponseXml(element, baseUri, doc, df, false));

				Element responsedesc = doc.createElementNS(DAV_NAMESPACE, "responsedescription");
				responsedesc.setPrefix(DAV_PREFIX);
				responsedesc.appendChild(doc.createTextNode("welldone"));
				multistatus.appendChild(responsedesc);
			}

			doc.appendChild(multistatus);
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			DOMSource source = new DOMSource(doc);
			// StreamResult result = new StreamResult(outs);
			ByteArrayOutputStream baus = new ByteArrayOutputStream();
			StreamResult result = new StreamResult(baus);
			transformer.transform(source, result);

			System.out.println(new String(baus.toByteArray()));
			outs.write(baus.toByteArray());
		} catch (RuntimeException ioe) {
			ioe.printStackTrace(System.out);
			throw ioe;
		} catch (Exception e) {
			e.printStackTrace();
			throw (IOException) new IOException("parser").initCause(e);
		} finally {
			outs.close();
		}
	}

	private Element makePropXml(final Document doc, final DateFormat df, final Map m) {
		Element prop = doc.createElementNS(DAV_NAMESPACE, "prop");
		prop.setPrefix(DAV_PREFIX);

		for (Iterator iterator = m.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry me = (Map.Entry) iterator.next();
			Object v = me.getValue();

			String name = (String) me.getKey();
			int p = name.indexOf(":");
			Element ee;
			if (p >= 0) {
				ee = doc.createElementNS(name.substring(0, p + 1), name.substring(p + 1));
				ee.setPrefix(DAV_PREFIX);
			} else {
				ee = doc.createElement(name);
			}
			if (v != null) {
				String txt;
				if (v instanceof Date) {
					txt = df.format(v);
					ee.appendChild(doc.createTextNode(txt));
				} else if (v instanceof Node) {
					ee.appendChild((Node) v);
				} else {
					txt = v.toString();
					ee.appendChild(doc.createTextNode(txt));
				}
			}
			prop.appendChild(ee);
		}
		return prop;
	}

	private Element makeResponseXml(final Object element, final String baseUri, final Document doc, final DateFormat df,
			final boolean asCollection) {
		Element response = doc.createElementNS(DAV_NAMESPACE, "response");
		response.setPrefix(DAV_PREFIX);
		{
			Element href = doc.createElementNS(DAV_NAMESPACE, "href");
			href.setPrefix(DAV_PREFIX);
			String cursor = this.content.getCursor(element);
			String xcursor = cursor;
			if (asCollection) {
				int p = cursor.lastIndexOf("/");
				if (p > 0) {
					xcursor = cursor.substring(0, p + 1) + ".." + cursor.substring(p + 1);
				}
			}
			href.appendChild(doc.createTextNode(baseUri + xcursor));
			response.appendChild(href);

			Element propstat = doc.createElementNS(DAV_NAMESPACE, "propstat");
			propstat.setPrefix(DAV_PREFIX);
			{
				Map m = getProps(element, doc, asCollection);
				propstat.appendChild(makePropXml(doc, df, m));

				Element st = doc.createElementNS(DAV_NAMESPACE, "status");
				st.setPrefix(DAV_PREFIX);
				st.appendChild(doc.createTextNode("HTTP/1.1 200 OK"));
				propstat.appendChild(st);
			}
			response.appendChild(propstat);
		}
		return response;
	}

	private String preprocessUri(String path) {
		if (path.startsWith("/")) {
			path = path.substring(1);
		}

		// all "..xxx" files refer actually to their "xxx" counterpart (due to
		// webdav collection limitations)
		path = path.replaceAll("\\.\\.", "");
		return path;
	}

	public void handle(final Request req, final Response resp) {
		resp.set("Server", "ZarnckeServer/0.001 (Simple/2.6)");
		resp.setDate("Date", System.currentTimeMillis());

		/*
		 * Cache-Control ; Section 14.9
		 */
		HttpMethod meth = (HttpMethod) this.methods.get(req.getMethod());
		if (meth == null) {
			meth = this.defaultMethod;
		}

		// TODO: remove: this is test/dbeug only
		Map reqHeaders = new HashMap();
		reqHeaders.put("Accept", req.getValue("Accept"));
		reqHeaders.put("Accept-Charset", req.getValue("Accept-Charset"));
		reqHeaders.put("Accept-Encoding", req.getValue("Accept-Encoding"));
		reqHeaders.put("Accept-Language", req.getValue("Accept-Language"));
		reqHeaders.put("Authorization", req.getValue("Authorization"));
		reqHeaders.put("Expect", req.getValue("Expect"));
		reqHeaders.put("From", req.getValue("From"));
		reqHeaders.put("Host", req.getValue("Host"));
		reqHeaders.put("If-Match", req.getValue("If-Match"));
		reqHeaders.put("If-Modified-Since", req.getValue("If-Modified-Since"));
		reqHeaders.put("If-None-Match", req.getValue("If-None-Match"));
		reqHeaders.put("If-Range", req.getValue("If-Range"));
		reqHeaders.put("If-Unmodified-Since", req.getValue("If-Unmodified-Since"));
		reqHeaders.put("Range", req.getValue("Range"));
		reqHeaders.put("Referer", req.getValue("Referer"));
		reqHeaders.put("User-Agent", req.getValue("User-Agent"));

		reqHeaders.put("DAV", req.getValue("DAV"));
		reqHeaders.put("If", req.getValue("If"));

		System.out.println(meth + " " + reqHeaders);

		try {
			meth.execute(req, resp);
		} catch (IOException e) {
			// TODO suitable error handling!
			e.printStackTrace();
			throw new RuntimeException("what now?", e);
		}
	}

	public void stop() {
		this.content.stopAll();
	}
}