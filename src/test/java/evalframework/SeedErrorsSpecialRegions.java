package evalframework;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import util.Util;
import websee.ExclusionRegion;
import websee.SpecialRegion;
import websee.TextRegion;
import config.Constants;

public class SeedErrorsSpecialRegions extends DefaultHandler
{
	public SeedErrorsSpecialRegions() 
	{
		
	}
	
	private File file;
	private String oraclePath;
	private boolean appFound = false;
	private String value;
	private WebDriver driver;
	private List<TextRegion> textRegions;
	private List<ExclusionRegion> exclusionRegions;
	private Rectangle rectangle;
	private Set<String> styleProperties;
	private Set<String> xpaths;
	private String xpathTemp;

	public void parseXML() throws ParserConfigurationException, SAXException, IOException
	{
		SAXParserFactory factory = SAXParserFactory.newInstance();
	    SAXParser parser = factory.newSAXParser();
	    
	    parser.parse(file, this);
	}
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
	{
		value = "";
		if(qName.equalsIgnoreCase("app"))
		{
			if(oraclePath.contains(attributes.getValue("name")))
			{
				appFound = true;
				driver = new FirefoxDriver();
				driver.manage().window().maximize();
//				driver.manage().window().setSize(new Dimension(800, 600));
				driver.get("file://" + oraclePath + File.separatorChar + Constants.ORIGINAL_FILENAME);
				
				xpaths = new HashSet<String>();
				textRegions = new ArrayList<TextRegion>();
				exclusionRegions = new ArrayList<ExclusionRegion>();
			}
		}
		else if(appFound && qName.equalsIgnoreCase("styleProperties"))
		{
			styleProperties = new HashSet<String>();
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException
	{
		if(appFound && qName.equalsIgnoreCase("app"))
		{
			appFound = false;
//			driver.close();
			driver.quit();
		}
		else if(appFound && qName.equalsIgnoreCase("xpath"))
		{
			xpathTemp = value;
			
			rectangle = new Rectangle();
			WebElement e = driver.findElement(By.xpath(value));
			rectangle.x = e.getLocation().x;
			rectangle.y = e.getLocation().y;
			rectangle.width = e.getSize().width;
			rectangle.height = e.getSize().height;
		}
		else if(appFound && qName.equalsIgnoreCase("styleProperty"))
		{
			styleProperties.add(value);
		}
		else if(appFound && qName.equalsIgnoreCase("textRegion"))
		{
			xpaths.add(xpathTemp);
			
			TextRegion tr = new TextRegion();
			tr.setRectangle(rectangle);
			tr.setStyleProperties(styleProperties);
			textRegions.add(tr);
		}
		else if(appFound && qName.equalsIgnoreCase("exclusionRegion"))
		{
			ExclusionRegion er = new ExclusionRegion();
			er.setRectangle(rectangle);
			exclusionRegions.add(er);
		}
	}
	
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException
	{
		value = value + new String(ch, start, length);
	}
	
	public LinkedHashMap<String, ArrayList<HashMap<String, String>>> processRegions(String fullFilePath, String oraclePath, LinkedHashMap<String, ArrayList<HashMap<String, String>>> seededElementsSummary) throws IOException
	{
		this.file = new File(fullFilePath);
		this.oraclePath = oraclePath;
		
		try
		{
			parseXML();
		}
		catch (ParserConfigurationException e2)
		{
			e2.printStackTrace();
		}
		catch (SAXException e2)
		{
			e2.printStackTrace();
		}
		
		// get tests dir
		for (File urlDir : new File(oraclePath).listFiles())
		{
			if (urlDir.isDirectory() && urlDir.getName().equals(Constants.NEW_FILES_DIRECTORY))
			{
				String testFilesPath = urlDir.getAbsolutePath();
				
				List<SpecialRegion> specialRegionsWithoutTextRegions = new ArrayList<SpecialRegion>(exclusionRegions);
				
				// iterate in tests dir
				int count = 0;
				for(File testDir : new File(testFilesPath).listFiles())
				{
					if(testDir.isDirectory())
					{
						count++;
						String testFilePath = testDir.getAbsolutePath();
						createSpecialRegionsXmlFile(testFilePath, specialRegionsWithoutTextRegions);
					}
				}
				
				// text region extra processing
				List<SpecialRegion> specialRegionsWithTextRegions = new ArrayList<SpecialRegion>(exclusionRegions);
				specialRegionsWithTextRegions.addAll(textRegions);

				// create new test case for text regions
				String newDirPath = Util.createDirectory(testFilesPath, Constants.NEW_FILE_DIRECTORY + (count+1));
				File newFile = new File(newDirPath + File.separatorChar + Constants.NEW_FILE_NAME);
				FileUtils.copyFile(new File(oraclePath + File.separatorChar + Constants.ORIGINAL_FILENAME), newFile);			
				
				// add entry in seed_errors.xml
				File oracleParentFilePath = new File(oraclePath).getParentFile();
				String seedErrorsXmlFilePath = oracleParentFilePath.getAbsolutePath() + File.separatorChar + Constants.SEED_ERRORS_XML_FILENAME;
				DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder docBuilder = null;
				try
				{
					docBuilder = docFactory.newDocumentBuilder();
				}
				catch (ParserConfigurationException e1)
				{
					e1.printStackTrace();
				}
				org.w3c.dom.Document doc = null;
				try
				{
					doc = docBuilder.parse(new File(seedErrorsXmlFilePath));
				}
				catch (SAXException e1)
				{
					e1.printStackTrace();
				}
				org.w3c.dom.NodeList nodes = doc.getElementsByTagName("htmlFile");
				org.w3c.dom.Node htmlFile = nodes.item(0);
				for(String xpath : xpaths)
				{
					org.w3c.dom.Element ce = (org.w3c.dom.Element) htmlFile.appendChild(doc.createElement("changeElements"));
					org.w3c.dom.Element he = (org.w3c.dom.Element) ce.appendChild(doc.createElement("htmlElement"));
					org.w3c.dom.Element xp = (org.w3c.dom.Element) he.appendChild(doc.createElement("xpath"));
					xp.setTextContent(xpath);
					org.w3c.dom.Element ct = (org.w3c.dom.Element) he.appendChild(doc.createElement("change"));
					ct.setAttribute("type", ChangeType.TEXT_REGION.name());
					
					ArrayList<HashMap<String, String>> htmlElementsSummary = new ArrayList<HashMap<String,String>>();
					HashMap<String, String> summary = new HashMap<String, String>();
					summary.put("xpath", xpath);
					summary.put("elementChangeType", ChangeType.TEXT_REGION.name());
					summary.put("newElements", "");
					htmlElementsSummary.add(summary);
					seededElementsSummary.put(newDirPath, htmlElementsSummary);
				}
				
				// change text content of text region in test.html
/*				Document document = Jsoup.parse(newFile, null);
				for(String xpath : xpaths)
				{
					Element e = Util.getElementFromXPathJava(xpath, document);
					String text = e.ownText();
					text = StringUtils.reverseDelimited(StringUtils.reverse(text), ' ');
					e.text(text);
				}
				String html = document.html();
				FileUtils.writeStringToFile(newFile, html, Util.getHtmlPageCharset(document));
*/				
				createSpecialRegionsXmlFile(newDirPath, specialRegionsWithTextRegions);
			}
		}
		return seededElementsSummary;
	}
	
	private void createSpecialRegionsXmlFile(String path, List<SpecialRegion> specialRegions) throws IOException
	{
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = null;
		try
		{
			docBuilder = docFactory.newDocumentBuilder();
		}
		catch (ParserConfigurationException e1)
		{
			e1.printStackTrace();
		}
		org.w3c.dom.Document doc = docBuilder.newDocument();
		
		org.w3c.dom.Element rootElement = doc.createElement("specialRegions");
		org.w3c.dom.Element sr = (org.w3c.dom.Element) doc.appendChild(rootElement);
		
		for (SpecialRegion specialRegion : specialRegions)
		{
			org.w3c.dom.Node srType = null;
			
			if(specialRegion instanceof ExclusionRegion)
			{
				srType = sr.appendChild(doc.createElement("exclusionRegion"));
			}
			if(specialRegion instanceof TextRegion)
			{
				TextRegion tr = (TextRegion) specialRegion;
			
				srType = sr.appendChild(doc.createElement("textRegion"));
				org.w3c.dom.Node styleProps = srType.appendChild(doc.createElement("styleProperties"));
				
				for(String prop : tr.getStyleProperties())
				{
					org.w3c.dom.Node styleProp = styleProps.appendChild(doc.createElement("styleProperty"));
					styleProp.setTextContent(prop);
				}
			}
			
			org.w3c.dom.Node rect = srType.appendChild(doc.createElement("rectangle"));
			org.w3c.dom.Node left = rect.appendChild(doc.createElement("left"));
			left.setTextContent(String.valueOf(specialRegion.getRectangle().x));
			org.w3c.dom.Node top = rect.appendChild(doc.createElement("top"));
			top.setTextContent(String.valueOf(specialRegion.getRectangle().y));
			org.w3c.dom.Node width = rect.appendChild(doc.createElement("width"));
			width.setTextContent(String.valueOf(specialRegion.getRectangle().width));
			org.w3c.dom.Node height = rect.appendChild(doc.createElement("height"));
			height.setTextContent(String.valueOf(specialRegion.getRectangle().height));
		}
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = null;
		try
		{
			transformer = transformerFactory.newTransformer();
		}
		catch (TransformerConfigurationException e)
		{
			e.printStackTrace();
		}
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
		DOMSource source = new DOMSource(doc);
		
		File xmlFile = new File(path + File.separatorChar + Constants.SPECIAL_REGIONS_XML_FILENAME);
		StreamResult result = new StreamResult(xmlFile.toURI().getPath());
		try
		{
			transformer.transform(source, result);
		}
		catch (TransformerException e)
		{
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws IOException
	{
		String specialRegionsXmlFilePath = "evaluationframework/com/evaluation/framework/java/special_regions_initializer.xml";
		SeedErrorsSpecialRegions sesr = new SeedErrorsSpecialRegions();
		LinkedHashMap<String, ArrayList<HashMap<String, String>>> seededElementsSummary = new LinkedHashMap<String, ArrayList<HashMap<String, String>>>();
		sesr.processRegions(specialRegionsXmlFilePath, "C:\\USC\\visual_checking\\evaluation\\test_run_1\\www.gmail.com", seededElementsSummary);
	}
}
