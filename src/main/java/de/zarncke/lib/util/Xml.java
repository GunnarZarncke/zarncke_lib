package de.zarncke.lib.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.zarncke.lib.err.Warden;
import de.zarncke.lib.i18n.Locales;
import de.zarncke.lib.i18n.Translations;
import de.zarncke.lib.log.Log;

/**
 * Some helper methods to fetch content from an XML DOM.
 * 
 * @author Gunnar Zarncke
 * @deprecated use {@link de.zarncke.lib.xml.Xml} instead
 */
@Deprecated
public final class Xml {

	private Xml() {// helper
	}

	public static Translations parseTranslatedKeys(final Node node, final String nodeName) {
		Translations translated = new Translations();
		NodeList cnl = node.getChildNodes();
		for (int k = 0; k < cnl.getLength(); k++) {
			Node cn = cnl.item(k);
			if (cn.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			if (cn.getNodeName().equals(nodeName)) {
				String lang = getAttribute("lang", cn.getAttributes());
				String val = getTextContent(cn);
				if (lang != null && val != null) {
					Locale l3 = null;
					try {
						l3 = Locales.forIso3(lang, "", "");
					} catch (Exception e) {
						Warden.disregard(e);
						if (!Locales.LANGUAGE_CODE_UNDEFINED3.equals(lang)) {
							Log.LOG.get().report("ignoring illegal language (use as untranslated)" + e);
						}
					}
					translated.put(l3, val);
				}
			}
		}
		if (translated.getDefaultLocale() == null) {
			translated.setDefaultLocale(Locale.GERMAN);
		}
		return translated;
	}

	public static void handleTranslations(final Translations translation, final Node node) {
		NamedNodeMap cnatts = node.getAttributes();
		String lang = getAttribute("lang", cnatts);
		String txt = getTextContent(node);
		Locale l3 = null;
		try {
			l3 = Locales.forIso3(lang, "", "");
		} catch (Exception e) {
			if (lang.equals(Locales.LANGUAGE_CODE_UNDEFINED3)) {
				l3 = new Locale(Locales.LANGUAGE_CODE_UNDEFINED2,"","");
			} else {
				Warden.disregardAndReport(e);
			}
		}
		translation.put(l3, txt);

		Node dfltItem = cnatts.getNamedItem("dflt");
		if (dfltItem != null) {
			String dflt = getTextContent(dfltItem);
			if (dflt != null) {
				translation.setDefaultLocale(l3);
			}
		}
	}

	/**
	 * Returns the textual content of a node.
	 * Uses the text up to the first child element if child elements are present.
	 * Returns null if the body is empty
	 *
	 * @param node != null
	 * @return String or null if empty
	 */
	public static String getTextContent(final Node node) {
		if (node == null) {
			return null;
		}
		Node n = node.getFirstChild();
		return n == null ? null : n.getNodeValue();
	}

	/**
	 * Returns the first child node with matching tag name.
	 *
	 * @param type name of node tag
	 * @param parent to look in
	 * @return Node or null if none found
	 */
	public static Node getChildNode(final String type, final Node parent) {
		NodeList cnl = parent.getChildNodes();
		for (int k = 0; k < cnl.getLength(); k++) {
			Node cn = cnl.item(k);
			if (type.equals(cn.getNodeName())) {
				return cn;
			}
		}
		return null;
	}

	/**
	 * Returns the child nodes with matching tag name.
	 *
	 * @param type name of node tag
	 * @param parent to look in
	 * @return List of Node , may be empty
	 */
	public static List<Node> getChildNodes(final String type, final Node parent) {
		ArrayList<Node> matches = new ArrayList<Node>();
		NodeList cnl = parent.getChildNodes();
		for (int k = 0; k < cnl.getLength(); k++) {
			Node cn = cnl.item(k);
			if (type.equals(cn.getNodeName())) {
				matches.add(cn);
			}
		}
		return matches;
	}

	/**
	 * Get an Attribute from {@link NamedNodeMap}.
	 *
	 * @param attrName != null
	 * @param attributes NamedNodeMap != null
	 * @return value or null if not present
	 */
	public static String getAttribute(final String attrName, final NamedNodeMap attributes) {
		if (attributes == null) {
			return null;
		}
		Node item = attributes.getNamedItem(attrName);
		if (item == null) {
			return null;
		}
		return item.getNodeValue();
	}
}
