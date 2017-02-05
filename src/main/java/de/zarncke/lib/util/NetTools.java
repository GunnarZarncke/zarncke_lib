package de.zarncke.lib.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.coll.Pair;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.io.StreamWithName;

/**
 * Communication with a internet services e.g. a web-server.
 */
public final class NetTools {

	// TODO make pass-in parameter
	private static final int MAX_HEAD_REQUEST_TIMEOUT_MS = 10000;

	public static final String LOCALHOST = "localhost";
	public static final String HOSTNAME_ENV_VARIABLE = "env.HOSTNAME";

	private NetTools() {
		// helper
	}

	private static final int MAX_REDIRECTS = 8;
	private static final String POST_ENCODING = "ISO-8859-1";

	/**
	 * Opens an InputStream from an URL and returns it and its name - either the URL or the last Location header in case
	 * of redirects.
	 *
	 * @param connection != null
	 * @return StreamWithName which also contains last modification date and header gields as meta information,
	 * the stream may be null in case the last request/redirect failed
	 * @throws IOException if unreadable
	 */
	public static StreamWithName getInputStreamAndName(final URLConnection connection) throws IOException {
		URLConnection currentConn = connection;
		boolean redir;
		int redirects = 0;
		String location = currentConn.getURL().getPath();
		InputStream in = null;
		do {
			if (currentConn instanceof HttpURLConnection) {
				((HttpURLConnection) currentConn).setInstanceFollowRedirects(false);
			}
			currentConn.setConnectTimeout(MAX_HEAD_REQUEST_TIMEOUT_MS);
			currentConn.setReadTimeout(MAX_HEAD_REQUEST_TIMEOUT_MS);

			// We want to open the input stream before getting headers
			// because getHeaderField() et al swallow IOExceptions.
			in = currentConn.getInputStream();
			redir = false;
			if (currentConn instanceof HttpURLConnection) {
				HttpURLConnection http = (HttpURLConnection) currentConn;
				int stat = http.getResponseCode();
				if (stat >= 300 && stat <= 307 && stat != 306 && stat != 304) {
					URL base = http.getURL();
					String loc = http.getHeaderField("Location");
					URL target = null;
					if (loc == null) {
						throw Warden.spot(new IOException("redirect to " + location + " producted response " + stat
								+ " without Location header."));
					}
					target = new URL(base, loc);

					http.disconnect();
					checkRedirects(redirects, target);
					redir = true;
					location = target.getPath();
					currentConn = target.openConnection();
					redirects++;
				}
			}
		} while (redir);
		return new StreamWithName(in, location, currentConn.getHeaderFields()).setTime(new DateTime(currentConn
				.getLastModified(), DateTimeZone.UTC));
	}

	/**
	 * Opens an URLConnections and returns effective name and time - either the URL or the last Location header in case
	 * of redirects.
	 *
	 * @param connection != null
	 * @return StreamWithName
	 * @throws IOException if unreadable or status code != 200 or 204
	 */
	public static Pair<String, Long> getEffectiveName(final URLConnection connection) throws IOException {
		String location = connection.getURL().getPath();
		if (!(connection instanceof HttpURLConnection)) {
			return Pair.pair(location, Q.l(connection.getLastModified()));
		}

		HttpURLConnection currentConn = (HttpURLConnection) connection;
		boolean redir;
		int redirects = 0;
		do {
			currentConn.setInstanceFollowRedirects(false);
			currentConn.setRequestMethod("HEAD");
			currentConn.setConnectTimeout(MAX_HEAD_REQUEST_TIMEOUT_MS);
			currentConn.setReadTimeout(MAX_HEAD_REQUEST_TIMEOUT_MS);
			redir = false;
			int stat = currentConn.getResponseCode();
			if (stat >= 300 && stat <= 307 && stat != 306 && stat != 304) {
				URL base = currentConn.getURL();
				String loc = currentConn.getHeaderField("Location");
				URL target = null;
				if (loc == null) {
					throw Warden.spot(new IOException("redirect to " + location + " produced response " + stat
							+ " without Location header."));
				}
				target = new URL(base, loc);

				currentConn.disconnect();
				checkRedirects(redirects, target);
				redir = true;
				location = target.getPath();
				currentConn = (HttpURLConnection) target.openConnection();
				redirects++;
			} else if (stat != 200 && stat != 204) {
				throw Warden.spot(new IOException("HEAD request to " + location + " produced response " + stat + "."));
			}
		} while (redir);
		return Pair.pair(location, Q.l(currentConn.getLastModified()));
	}

	private static void checkRedirects(final int redirects, final URL target) {
		// Redirection should be allowed only for HTTP and HTTPS
		// and should be limited to 8 redirections at most.
		if (!(target.getProtocol().equals("http") || target.getProtocol().equals("https"))
				|| redirects >= MAX_REDIRECTS) {
			throw Warden.spot(new SecurityException("illegal URL redirect"));
		}
	}

	/**
	 * Local hostname.
	 *
	 * @return $HOSTNAME
	 */
	public static String getHostName() {
		String host = Misc.ENVIRONMENT.get().get(HOSTNAME_ENV_VARIABLE);
		if (host == null) {
			try {
				InetAddress addr = InetAddress.getLocalHost();
				host = addr.getHostName();
			} catch (UnknownHostException e) {
				host = LOCALHOST;
			}
		}
		return host;
	}

	public static Collection<InetAddress> getHostIpAdresses() {
		InetAddress addr;
		try {
			addr = InetAddress.getLocalHost();
		} catch (UnknownHostException e1) {
			return L.e();
		}
		try {
			String host = addr.getHostName();
			return Arrays.asList(InetAddress.getAllByName(host));
		} catch (UnknownHostException e) {
			return L.l(addr);
		}
	}

	/**
	 * This method performs a post to the specified URL and delivers its reply.
	 *
	 * @param urlString A String representing the URL you want to post to.
	 * @param post A Reader representing the content you want to post.
	 * @return A Reader representing the reply after your post.
	 * @throws MalformedURLException if so
	 * @throws IOException on connect
	 */
	public static Reader doPost(final String urlString, final Reader post) throws MalformedURLException, IOException {
		Reader result = null;
		URLConnection conn = null;
		OutputStream urlOutputStream = null;
		int tempChar = 0;

		nonNullUrl(urlString);
		if (post == null) {
			throw new IllegalArgumentException("Writer to post is null.");
		}

		try {
			// Establishing connection
			URL url = new URL(urlString);
			conn = url.openConnection();
			/*
			 * if (conn instanceof HttpsURLConnectionImpl)
			 * {
			 * System.out.println("conn "+conn);
			 * HttpsURLConnectionImpl huc = (HttpsURLConnectionImpl) conn;
			 * javax.net.ssl.HostnameVerifier hv = new javax.net.ssl.HostnameVerifier()
			 * {
			 * public boolean verify(String arg0, SSLSession arg1)
			 * {
			 * System.out.println(arg0);
			 * return true;
			 * }
			 * public boolean verify(String arg0, String arg1)
			 * {
			 * System.out.println(arg0 + "\n" + arg1);
			 * return true;
			 * }
			 * };
			 * huc.setHostnameVerifier(hv);
			 * }
			 */

			// Preparing post
			conn.setDoOutput(true);
			urlOutputStream = conn.getOutputStream();

			// Posting
			while ((tempChar = post.read()) >= 0) {
				urlOutputStream.write(tempChar);
			}

			// Reading reply
			result = new InputStreamReader(conn.getInputStream());
		} catch (MalformedURLException e) {
			throw (MalformedURLException) new MalformedURLException("URL \"" + urlString + "\" is malformed.")
					.initCause(e);
		} catch (IOException e) {
			throw (IOException) new IOException("Error while posting to \"" + urlString + "\".").initCause(e);
		}

		return result;
	}

	/**
	 * This method performs a post to the specified URL and delivers its reply.
	 *
	 * @param urlString A String representing the URL you want to post to.
	 * @param post A String representing the content you want to post.
	 * @return A String representing the reply after your post.
	 * @throws java.net.MalformedURLException if so
	 * @throws java.io.IOException of post
	 */
	public static String doPost(final String urlString, final String post) throws MalformedURLException, IOException {
		String result = null;
		URLConnection conn = null;
		OutputStream urlOutputStream = null;
		InputStream urlInputStream = null;
		StringWriter content = new StringWriter();
		int tempChar = 0;
		int tempByte = 0;

		nonNullUrl(urlString);
		if (post == null) {
			throw new IllegalArgumentException("String to post is null.");
		}

		try {
			StringReader postReader = new StringReader(post);

			// Establishing connection
			URL url = new URL(urlString);
			conn = url.openConnection();

			// Preparing post
			conn.setDoOutput(true);
			urlOutputStream = conn.getOutputStream();

			// Posting
			while ((tempChar = postReader.read()) >= 0) {
				urlOutputStream.write(tempChar);
			}

			// Reading reply
			urlInputStream = conn.getInputStream();
			while ((tempByte = urlInputStream.read()) >= 0) {
				content.write(tempByte);
			}

			result = content.toString();
		} catch (MalformedURLException e) {
			throw (MalformedURLException) new MalformedURLException("URL \"" + urlString + "\" is malformed.")
					.initCause(e);
		} catch (IOException e) {
			throw new IOException("Error while posting to \"" + urlString + "\".", e);
		}

		return result;
	}

	private static void nonNullUrl(final String urlString) {
		if (urlString == null) {
			throw new IllegalArgumentException("URL to post to is null.");
		}
	}

	/**
	 * This method delivers a reply of the specified URL.
	 *
	 * @param urlString A String representing the URL from which you want to get a reply.
	 * @return A Reader representing the reply from the URL.
	 * @throws java.net.MalformedURLException
	 * @throws java.io.IOException
	 * @see java.io.Reader
	 */
	public static Reader doGetReader(final String urlString) throws MalformedURLException, IOException {
		Reader result = null;

		// Checking argument
		if (urlString == null) {
			throw new IllegalArgumentException("URL to get from is null.");
		}

		try {
			// Establishing connection and reading from it
			URL url = new URL(urlString);
			result = new InputStreamReader(url.openStream());
		} catch (MalformedURLException e) {
			throw (MalformedURLException) new MalformedURLException("URL \"" + urlString + "\" is malformed.")
					.initCause(e);
		} catch (IOException e) {
			throw new IOException("Error while opening stream from \"" + urlString + "\".", e);
		}
		return result;
	}

	/**
	 * This method delivers a reply of the specified URL.
	 *
	 * @param urlString A String representing the URL from which you want to get a reply.
	 * @return A String representing the reply from the URL.
	 * @throws java.net.MalformedURLException URL
	 * @throws java.io.IOException network
	 */
	public static String doGetString(final String urlString) throws MalformedURLException, IOException {
		String result = null;
		InputStream urlInputStream = null;
		StringWriter content = new StringWriter();
		int tempByte = 0;

		// Checking argument
		if (urlString == null) {
			throw new IllegalArgumentException("URL to get from is null.");
		}

		try {
			// Establishing connection
			URL url = new URL(urlString);
			urlInputStream = url.openStream();

			// Reading from it
			while ((tempByte = urlInputStream.read()) >= 0) {
				content.write(tempByte);
			}

			// Converting output
			result = content.toString();
		} catch (MalformedURLException e) {
			throw (MalformedURLException) new MalformedURLException("URL \"" + urlString + "\" is malformed.")
					.initCause(e);
		} catch (IOException e) {
			throw new IOException("Error while reading stream from \"" + urlString + "\".", e);
		}

		return result;
	}

	/**
	 * This method provides the WebClient as command-line tool.
	 * Its usage will be explained when you enter "-help" as parameter for the WebClient in command-line mode.
	 *
	 * @param url to post to
	 * @param data to post as form parameters
	 * @return Reader for the response; the caller has the duty to close it
	 * @throws Exception on any errors
	 */
	public static Reader doPost(final String url, final Map<?, ?> data) throws Exception {
		StringBuffer content = new StringBuffer();
		for (Map.Entry<?, ?> element : data.entrySet()) {
			content.append(element.getKey()).append("=")
					.append(URLEncoder.encode((String) element.getValue(), POST_ENCODING));
			content.append("&");
		}
		content.setLength(content.length() - 1);
		Reader reader = new StringReader(content.toString());

		return doPost(url, reader);
	}
}
