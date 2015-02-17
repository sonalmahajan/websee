package evalframework;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XmlProcessor {

	private static final XmlProcessor instance = new XmlProcessor();

	private Hashtable<String, TreeSet<String>> cssTable;

	private Hashtable<String, TreeSet<String>> htmlTable;

	public Set<String> getCSSAttributes() {
		return cssTable.keySet();
	}

	public Set<String> getCandidateElementTagsFromCSSAttribute(String attribute) {
		return cssTable.get(attribute);
	}

	public Set<String> getHTMLAttributes() {
		return htmlTable.keySet();
	}

	public Set<String> getCandidateElementTagsFromHTMLAttribute(String attribute) {
		return htmlTable.get(attribute);
	}

	public static XmlProcessor getInstance() {
		return instance;
	}

	private XmlProcessor() {
		loadXml();
	}

	private Hashtable<String, TreeSet<String>> initTable(NodeList nodeList) {
		Hashtable<String, TreeSet<String>> table = new Hashtable<String, TreeSet<String>>();
		TreeSet<String> set = new TreeSet<String>();
		for (int i = 0; i < nodeList.getLength(); i++) {

			Node node = nodeList.item(i);
			Element element = (Element) node.getParentNode();
			String tag = element.getAttribute("tagName");
			for (String s : node.getTextContent().split(",")) {
				if (s.length() > 0) {
					set.add(s);
					TreeSet<String> htmlelementsset;
					if (table.containsKey(s)) {
						htmlelementsset = table.get(s);
					} else {
						htmlelementsset = new TreeSet<String>();
					}
					htmlelementsset.add(tag);
					table.put(s, htmlelementsset);
				}
			}
		}
//		for (String key : table.keySet()) {
//			TreeSet<String> value = table.get(key);
//			Iterator<String> it = value.iterator();
//			System.out.print(key + ": ");
//			while (it.hasNext()) {
//				System.out.print(it.next() + " ");
//			}
//			System.out.println();
//		}
		return table;
	}

	public Document loadXml() {
		File file = new File(
				EvaluationFrameworkConstants.completeHtmlElementsFilePath);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(file);
			cssTable = initTable(doc.getElementsByTagName("cssAttributes"));
			htmlTable = initTable(doc.getElementsByTagName("htmlAttributes"));
			return doc;
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
