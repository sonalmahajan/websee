package evalframework;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import util.Util;
import config.Constants;

public class SeedErrorsInitializer {
	private HashMap<String, HtmlElementEval> htmlElementsAttributes;
	private HashMap<String, String> htmlAttributesMap;
	private HashMap<String, HashMap<String, String>> cssAttributesMap;
	private HashMap<String, ArrayList<String>> helperValues;

	public void createSeedErrorsXml(File file, Node htmlFile,
			org.w3c.dom.Document doc) throws IOException,
			ParserConfigurationException, SAXException {
		Document document = Jsoup.parse(file, null);
		Elements elements = document.getAllElements();

		// initialization
		HtmlElementsAttributes htmlElementsAttributesObj = HtmlElementsAttributes
				.getInstance();
		htmlElementsAttributes = htmlElementsAttributesObj.getHtmlAttributes();

		HtmlAttributesMaster ham = HtmlAttributesMaster.getInstance();
		htmlAttributesMap = ham.getHtmlAttributes();

		CssAttributesMaster cam = CssAttributesMaster.getInstance();
		cssAttributesMap = cam.getCssAttributes();

		AttributeHelperValuesMaster ahvm = AttributeHelperValuesMaster
				.getInstance();
		helperValues = ahvm.getHelperValues();

		XmlProcessor xmlProcessor = XmlProcessor.getInstance();

		HashMap<String, ArrayList<Element>> htmlElementsInFile = new HashMap<String, ArrayList<Element>>();

		WebDriver driver = new FirefoxDriver();
		driver.manage().window().maximize();
//		driver.manage().window().setSize(new Dimension(800, 600));
		driver.get("file://" + file.getAbsolutePath());

		for (Element currentElement : elements) {
			// check if the current element has any visual attributes
			if (htmlElementsAttributes.containsKey(currentElement.tagName())) {
				// check if the element is not hidden
				try {
					WebElement e = driver.findElement(By.xpath(Util
							.getXPathOfElementJava(currentElement)));
					if (e.getLocation().x < 0 || e.getLocation().y < 0
							|| e.getSize().width <= 0
							|| e.getSize().height <= 0
							|| currentElement.text().length() <= 1) {
						continue;
					}
				} catch (NoSuchElementException e) {
					continue;
				}

				// add element to htmlElementsInFile
				ArrayList<Element> elementList;
				if (htmlElementsInFile.containsKey(currentElement.tagName())) {
					elementList = htmlElementsInFile.get(currentElement
							.tagName());
				} else {
					elementList = new ArrayList<Element>();
				}
				elementList.add(currentElement);
				htmlElementsInFile.put(currentElement.tagName(), elementList);
			}
		}
//		driver.close();
		driver.quit();

		// Process CSS Attributes
		for (String attribute : xmlProcessor.getCSSAttributes()) {
			if (cssAttributesMap.containsKey(attribute)) {
				Set<String> value = xmlProcessor
						.getCandidateElementTagsFromCSSAttribute(attribute);
				ArrayList<Element> candidate = new ArrayList<Element>();
				// System.out.println(attribute + ": (" + value.size() + ") ");
				for (String tag : value) {
					if (htmlElementsInFile.containsKey(tag)) {
						candidate.addAll(htmlElementsInFile.get(tag));
					}
				}
				if (!candidate.isEmpty()) {
					HashMap<String, String> propertyAttributes = cssAttributesMap
							.get(attribute);
					for (String propertyKey : propertyAttributes.keySet()) {
						Element candidateElement = pickARandomElement(candidate);
						processCssProperty(htmlFile, doc, candidateElement,
								propertyKey);
					}
				}
			}
		}

		// Process HTML Attributes
		for (String attribute : xmlProcessor.getHTMLAttributes()) {
			if (htmlAttributesMap.containsKey(attribute)) {
				Set<String> value = xmlProcessor
						.getCandidateElementTagsFromHTMLAttribute(attribute);
				ArrayList<Element> candidate = new ArrayList<Element>();
				// System.out.println(attribute + ": (" + value.size() + ") ");
				for (String tag : value) {
					if (htmlElementsInFile.containsKey(tag)) {
						candidate.addAll(htmlElementsInFile.get(tag));
					}
				}
				if (!candidate.isEmpty()) {
					Element candidateElement = pickARandomElement(candidate);
					processHtmlAttribute(htmlFile, doc, candidateElement,
							attribute);
				}
			}
		}
	}

	private <T> T pickARandomElement(ArrayList<T> list) {
		int ran = new Random().nextInt(list.size());
		return list.get(ran);
	}

	private void processHtmlAttribute(Node htmlFile, org.w3c.dom.Document doc,
			Element currentElement, String htmlAttribute) {
		Node changeElements = htmlFile.appendChild(doc
				.createElement("changeElements"));
		String htmlAttributeValue = htmlAttributesMap.get(htmlAttribute);
		String elementAttributeValue = currentElement.attr(htmlAttribute);

		// TODO: what is this?
		if (htmlAttribute.contains("_")) {
			htmlAttribute = htmlAttribute.split("_")[0];
		}

		// check for boolean attribute
		if (htmlAttributeValue.isEmpty()) {
			// check if attribute present
			if (currentElement.hasAttr(htmlAttribute)) {
				// remove the attribute
				createNewEntryInXml(currentElement, changeElements, doc,
						ChangeType.REPLACE.name(),
						ChangeType.REMOVE_ATTRIBUTE.name(), htmlAttribute,
						htmlAttributeValue);
			} else {
				// add new entry for boolean attribute
				createNewEntryInXml(currentElement, changeElements, doc,
						ChangeType.REPLACE.name(), "", htmlAttribute, "");
			}
		}
		// regular attribute with key-value pair
		else {
			// get actual values from helper values
			ArrayList<String> values = helperValues.get(htmlAttributeValue);

			// check if the attribute is present
			if (!elementAttributeValue.isEmpty()) {
				// check value of attribute
				for (String value : values) {
					if (!elementAttributeValue.equalsIgnoreCase(value)) {
						// new attribute value
						elementAttributeValue = value;
						break;
					}
				}
			} else {
				elementAttributeValue = values.get(0);
			}
			createNewEntryInXml(currentElement, changeElements, doc,
					ChangeType.REPLACE.name(), "", htmlAttribute,
					elementAttributeValue);
		}
	}

	private void processCssProperty(Node htmlFile, org.w3c.dom.Document doc,
			Element currentElement, String helperValueKey) {
		Node changeElements = htmlFile.appendChild(doc
				.createElement("changeElements"));
		String helperValueKeyArray[] = helperValueKey.split(",");
		String attr = currentElement.attr("style");

		for (int i = 0; i < helperValueKeyArray.length; i++) {
			helperValueKey = helperValueKeyArray[i];
			if (helperValueKey.contains("=")) {
				String specificHelperValueKeyArray[] = helperValueKey
						.split("=");
				attr = attr + specificHelperValueKeyArray[0] + ":"
						+ specificHelperValueKeyArray[1] + ";";
				continue;
			}

			// get actual values from helper values
			ArrayList<String> values = helperValues.get(helperValueKey);

			// write values to xml
			// check if there exists style attribute in the
			// current html element
			if (!attr.isEmpty()) {
				// replace css attribute value if it exists
				if (attr.contains(helperValueKey)) {
					String elementAttributeValue = "";
					Pattern p = Pattern.compile(".*" + helperValueKey
							+ "[\\s]*:[\\s]*(.*?);|\\s*|\".*", Pattern.DOTALL);
					Matcher m = p.matcher(attr);
					int start = -1;
					int end = -1;
					if (m.find()) {
						elementAttributeValue = m.group(1);
						start = m.start(1);
						end = m.end(1);
					}
					for (String value : values) {
						if (elementAttributeValue != null
								&& !elementAttributeValue
										.equalsIgnoreCase(value)) {
							// new attribute value
							elementAttributeValue = value;
							break;
						}
					}
					// replace with new value
					if (elementAttributeValue != null)
						attr = attr.substring(0, start) + elementAttributeValue
								+ attr.substring(end);
					else
						attr = attr + helperValueKey + ":" + values.get(0)
								+ ";";
				} else {
					// append css attribute to style
					attr = attr + helperValueKey + ":" + values.get(0) + ";";
				}
			} else {
				// add style attribute with css attributes
				if (values == null) {
					System.out.println("NULL ---- helperValueKey: "
							+ helperValueKey + "#values: " + values);
				}
				if (values.size() == 0) {
					System.out.println("EMPTY ---- helperValueKey: "
							+ helperValueKey + "#values: " + values);
				}

				attr = attr + helperValueKey + ":" + values.get(0) + ";";
			}
		}

		// create new entry in xml
		createNewEntryInXml(currentElement, changeElements, doc,
				ChangeType.REPLACE.name(), "", "style", attr);
	}

	private void createNewEntryInXml(Element currentElement,
			Node changeElements, org.w3c.dom.Document doc, String changeType,
			String attributeChangeType, String attributeKey,
			String attributeValue) {
		Node htmlElementInXml = changeElements.appendChild(doc
				.createElement("htmlElement"));
		Node xpath = htmlElementInXml.appendChild(doc.createElement("xpath"));
		xpath.setTextContent(Util.getXPathOfElementJava(currentElement));
		org.w3c.dom.Element changeElement = doc.createElement("change");
		changeElement.setAttribute("type", changeType);
		htmlElementInXml.appendChild(changeElement);
		if (!changeType.equalsIgnoreCase(ChangeType.REMOVE.name())) {
			Node newElements = htmlElementInXml.appendChild(doc
					.createElement("newElements"));
			Node newElement = newElements.appendChild(doc
					.createElement("newElement"));
			if (changeType
					.equalsIgnoreCase(ChangeType.ADD_AFTER_ELEMENT.name())) {
				Node tag = newElement.appendChild(doc.createElement("tag"));
				tag.setTextContent(currentElement.tagName());
				Node text = newElement.appendChild(doc.createElement("text"));
				text.setTextContent("dummy element");
			}
			Node attributes = newElement.appendChild(doc
					.createElement("attributes"));
			org.w3c.dom.Element attributeElement = doc
					.createElement("attribute");
			attributeElement.setAttribute("name", attributeKey);
			if (attributeChangeType != null && !attributeChangeType.isEmpty()) {
				attributeElement.setAttribute("type", attributeChangeType);
			}
			attributeElement.setTextContent(attributeValue);
			attributes.appendChild(attributeElement);
		}
	}

	public void runSeedErrorsInitializer(String xmlFileName, String dir,
			String originalHtmlFileName, String originalHtmlFilePath,
			boolean doNotAppend) {
		File xmlFile = new File(dir + File.separatorChar + xmlFileName);

		// check if overwrite=false and file exists and is not empty. Do not
		// overwrite if the file seems proper
		if (doNotAppend && xmlFile.exists() && xmlFile.length() > 0) {
			return;
		}

		DocumentBuilderFactory docFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder docBuilder = null;
		try {
			docBuilder = docFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		}

		org.w3c.dom.Document doc = null;

		if (xmlFile.exists()) {
			try {
				doc = docBuilder.parse(new File(dir + File.separatorChar
						+ xmlFileName));
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			doc = docBuilder.newDocument();
		}

		org.w3c.dom.Element root = doc.getDocumentElement();

		if (root == null) {
			org.w3c.dom.Element rootElement = doc.createElement("root");
			root = (org.w3c.dom.Element) doc.appendChild(rootElement);
		}

		org.w3c.dom.Element htmlFileObject = doc.createElement("htmlFile");
		htmlFileObject.setAttribute("name", originalHtmlFileName);
		htmlFileObject.setAttribute("path", originalHtmlFilePath);
		org.w3c.dom.Node htmlFile = root.appendChild(htmlFileObject);

		try {
			createSeedErrorsXml(new File(originalHtmlFilePath
					+ File.separatorChar + originalHtmlFileName), htmlFile, doc);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}

		System.out.println("seed errors xml created");
		TransformerFactory transformerFactory = TransformerFactory
				.newInstance();
		Transformer transformer = null;
		try {
			transformer = transformerFactory.newTransformer();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		}
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, new File(
				Constants.SEED_ERRORS_DTD_FILENAME_WITH_PATH).getName());
		transformer.setOutputProperty(
				"{http://xml.apache.org/xslt}indent-amount", "4");
		DOMSource source = new DOMSource(doc);

		StreamResult result = new StreamResult(xmlFile.toURI().getPath());
		try {
			transformer.transform(source, result);
		} catch (TransformerException e) {
			e.printStackTrace();
		}

		// copy seed errors dtd file
		File dtdFile = new File(Constants.SEED_ERRORS_DTD_FILENAME_WITH_PATH);
		String basePath = new File(originalHtmlFilePath).getParent();
		if (!new File(basePath + File.separatorChar + dtdFile.getName())
				.exists()) {
			try {
				// FileUtils.copyFileToDirectory(dtdFile, new File(basePath));
				FileUtils.copyFileToDirectory(dtdFile, new File(dir));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
