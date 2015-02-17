package evalframework;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.openqa.selenium.Point;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import util.Util;
import websee.WebSeeTool;
import config.Constants;

public class SeedErrors
{
	private LinkedHashMap<String, ArrayList<HashMap<String, String>>> seededElementsSummary;
	
	public LinkedHashMap<String, ArrayList<HashMap<String, String>>> getSeededElementsSummary()
	{
		return seededElementsSummary;
	}

	public void setSeededElementsSummary(LinkedHashMap<String, ArrayList<HashMap<String, String>>> seededElementsSummary)
	{
		this.seededElementsSummary = seededElementsSummary;
	}

	public SeedErrors()
	{
		seededElementsSummary = new LinkedHashMap<String, ArrayList<HashMap<String, String>>>();
	}
	
	public ArrayList<HtmlFile> parseXMLFile(String xmlFileName, String dir) throws ParserConfigurationException, SAXException, IOException
	{
		ReadXMLFile rxf = new ReadXMLFile(xmlFileName, dir);
		ArrayList<HtmlFile> lOriginalFiles = rxf.parseXML();
		return lOriginalFiles;
	}
	
	/**
	 * create new file as a copy of original html file with errors seeded in it from the xml file
	 * 
	 * @param originalHtmlFile object of class HtmlFile containing information of the html file to be copied with changes to be made
	 */
	public void createSeededHtmlFile(HtmlFile originalHtmlFile, boolean doNotOverwrite) throws IOException
	{
		String originalFileName = originalHtmlFile.getFileName();
		String originalFilePath = originalHtmlFile.getPath();
		
		File in = new File(originalFilePath + File.separatorChar + originalFileName);
		
		String newFilesPath = originalFilePath + File.separatorChar + Constants.NEW_FILES_DIRECTORY;
		if(!doNotOverwrite || !new File(newFilesPath).exists())
		{
			// create new
			newFilesPath = Util.createDirectory(originalFilePath, Constants.NEW_FILES_DIRECTORY);
		}
		
		int count = 1;
		for(ChangeElements changeElement : originalHtmlFile.getChangeElements())
		{
			Document document = Jsoup.parse(in, null);

			String newFilePath = newFilesPath + File.separatorChar + (Constants.NEW_FILE_DIRECTORY + count);
			if(!doNotOverwrite || !new File(newFilePath).exists())
			{
				//create new directory
				newFilePath = Util.createDirectory(newFilesPath, (Constants.NEW_FILE_DIRECTORY + count));
			}
			count++;
			
			String[] newFileNameArray = Util.getFileNameAndExtension(Constants.NEW_FILE_NAME);
			String newFileName = newFileNameArray[0] + newFileNameArray[1];
			
			//copy html file contents in the new html file with changes specified in seed errors xml file
			ArrayList<HashMap<String, String>> htmlElementsSummary = new ArrayList<HashMap<String,String>>();
			for(HtmlTag htmlElement : changeElement.getElements())
			{
				htmlElementsSummary.add(summarizeSeededElementInformation(htmlElement));
				processElement(htmlElement, in, document);
			}
			
			//copy document to new output html file
			File outFile = new File(newFilePath + File.separatorChar + newFileName);
			if(doNotOverwrite && outFile.exists() && outFile.length() > 0)
			{
				continue;
			}
			outFile.createNewFile();
			
			document.outputSettings().prettyPrint(false);
			String html = document.html();		
			PrintWriter out = new PrintWriter(outFile);
			out.print(html);
			out.close();

			WebSeeTool vit = new WebSeeTool(originalFilePath + "/oracle.png", outFile.getAbsolutePath());
			List<Point> diffPixels = vit.detection(false);
			if (diffPixels != null && diffPixels.size() == 0)
			{
				FileUtils.deleteDirectory(new File(newFilePath));
				count--;
			} else {
				new File(newFilePath + "/diff_oracle_test.png").delete();
				new File(newFilePath + "/diff_oracle_test.txt").delete();
				new File(newFilePath + "/filtered_diff_oracle_test.txt").delete();
				new File(newFilePath + "/test.png").delete();
				seededElementsSummary.put(newFilePath, htmlElementsSummary);
			}
		}
	}
	
	private HashMap<String, String> summarizeSeededElementInformation(HtmlTag htmlElement)
	{
		//store summary in hashmap
		HashMap<String, String> summary = new HashMap<String, String>();
		summary.put("xpath", htmlElement.getXpath());
		summary.put("elementChangeType", htmlElement.getChangeType().toString());
		summary.put("newElements", htmlElement.getNewElements().toString());
		return summary;
	}
	
	private void processElement(HtmlTag htmlElementToProcess, File htmlFile, Document document) throws IOException
	{
		Element element = Util.getElementFromXPathJava(htmlElementToProcess.getXpath(), document);
		if(element == null)
			return;
		
		switch (htmlElementToProcess.getChangeType())
		{
			case REPLACE:
				replaceAction(element, htmlElementToProcess.getNewElements());
				break;
				
			case ADD_BEFORE_ELEMENT:
				addAction(element, htmlElementToProcess.getNewElements(), htmlElementToProcess.getChangeType());
				break;
				
			case ADD_AFTER_ELEMENT:
				addAction(element, htmlElementToProcess.getNewElements(), htmlElementToProcess.getChangeType());
				break;
				
			case ADD_AS_CHILD:
				addChildAction(element, htmlElementToProcess.getNewElements());
				break;
				
			case APPEND_TEXT_IN_ELEMENT:
				element.appendText(htmlElementToProcess.getNewElements().get(0).getText());
				break;
				
			case REMOVE:
				element.remove();
				break;
		}
	}
	
	private void replaceAction(Element element, ArrayList<NewElement> newElements)
	{
		//replace change type should have only one newElement tag
		NewElement newElement = newElements.get(0);
		
		//replace tag
		if(newElement.getTag() != null && !newElement.getTag().isEmpty())
		{
			element.tagName(newElement.getTag());
		}
		
		//replace text in tag
		if(newElement.getText() != null && !newElement.getText().isEmpty())
		{
			element.text(newElement.getText());
		}
		
		//replace/set attributes in tag
		if(newElement.getAttributes().size() > 0)
		{
			for(String attribute : newElement.getAttributes().keySet())
			{
				Attribute attributeObject = newElement.getAttributes().get(attribute);
				if(attributeObject.getChangeType() != null && attributeObject.getChangeType().equals(ChangeType.REMOVE_ATTRIBUTE))
				{
					attributeObject.setOriginalValue(element.attr(attribute));
					element.removeAttr(attribute);
				}
				else
				{
					attributeObject.setOriginalValue(element.attr(attribute));
					element.attr(attribute, attributeObject.getValue());
				}
			}
		}
	}
	
	private void addAction(Element element, ArrayList<NewElement> newElements, ChangeType changeType)
	{
		for (NewElement newElement : newElements)
		{
			Tag newTag = Tag.valueOf(newElement.getTag());
			Element elementToAdd = new Element(newTag, "");
			
			//add text and attributes to the new element
			if(newElement.getText() != null)
			{
				elementToAdd.text(newElement.getText());
			}
			if(newElement.getAttributes().size() > 0)
			{
				for(String attribute : newElement.getAttributes().keySet())
				{
					Attribute attributeObject = newElement.getAttributes().get(attribute);
					elementToAdd.attr(attribute, attributeObject.getValue());
				}
			}
			
			if(changeType == ChangeType.ADD_BEFORE_ELEMENT)
			{
				element.before(elementToAdd);
			}
			else
			{
				element.after(elementToAdd);
			}
		}
	}
	
	private void addChildAction(Element element, ArrayList<NewElement> newElements)
	{
		for (NewElement newElement : newElements)
		{
			Element childElement = element.appendElement(newElement.getTag());
			
			//add text and attributes to the new element
			if(newElement.getText() != null)
			{
				childElement.text(newElement.getText());
			}
			if(newElement.getAttributes().size() > 0)
			{
				for(String attribute : newElement.getAttributes().keySet())
				{
					Attribute attributeObject = newElement.getAttributes().get(attribute);
					childElement.attr(attribute, attributeObject.getValue());
				}
			}
		}
	}
	
	public void runSeedErrors(String xmlFileName, String dir, boolean doNotOverwrite)
	{
		System.out.println("seeded html file creation started");
		ArrayList<HtmlFile> lOriginalFiles = null;
		try
		{
			lOriginalFiles = parseXMLFile(xmlFileName, dir);
		}
		catch (ParserConfigurationException e)
		{
			e.printStackTrace();
		}
		catch (SAXException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		for (HtmlFile originalFile : lOriginalFiles)
		{
			try
			{
				createSeededHtmlFile(originalFile, doNotOverwrite);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		System.out.println("seeded html file creation completed");
	}
}

/**
 * class used to parse lines.xml file using SAX parser
 */
class ReadXMLFile extends DefaultHandler
{
	private File file;
	private ArrayList<ChangeElements> lChangeElements;
	private ChangeElements changeElementsTemp;
	private ArrayList<HtmlTag> lElement;
	private HtmlTag elementTemp;
	private ArrayList<NewElement> lNewElements;
	private NewElement newElementTemp;
	private String tempValue;
	private ArrayList<HtmlFile> lHtmlFiles;
	private HtmlFile htmlFileTemp;
	private String attributeName;
	private Attribute attribute;
	private HashMap<String, Attribute> attributes;
	
	public ReadXMLFile(String fileName, String path)
	{
		this.file = new File(path + File.separator + fileName);
	}
	
	public ArrayList<HtmlFile> parseXML() throws ParserConfigurationException, SAXException, IOException
	{
		SAXParserFactory factory = SAXParserFactory.newInstance();
	    SAXParser parser = factory.newSAXParser();
	    
	    lHtmlFiles = new ArrayList<HtmlFile>();
	    parser.parse(file, this);
//	    printChangeElements();
		return lHtmlFiles;
	}
	
	public void printChangeElements()
	{
		for (ChangeElements changeElement : lChangeElements)
		{
			System.out.println(changeElement);
		}
	}
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
	{
		tempValue = "";
		if(qName.equalsIgnoreCase("htmlFile"))
		{
			htmlFileTemp = new HtmlFile();
			lChangeElements = new ArrayList<ChangeElements>();
			htmlFileTemp.setFileName(attributes.getValue("name"));
			htmlFileTemp.setPath(attributes.getValue("path"));
		}
		else if(qName.equalsIgnoreCase("changeElements"))
		{
			changeElementsTemp = new ChangeElements();
			lElement = new ArrayList<HtmlTag>();
		}
		else if(qName.equalsIgnoreCase("htmlElement"))
		{
			elementTemp = new HtmlTag();
			lNewElements = new ArrayList<NewElement>();
		}
		else if(qName.equalsIgnoreCase("change"))
		{
			elementTemp.setChangeType(ChangeType.valueOf(attributes.getValue("type")));
		}
		else if(qName.equalsIgnoreCase("newElement"))
		{
			newElementTemp = new NewElement();			
		}
		else if(qName.equalsIgnoreCase("attributes"))
		{
			this.attributes = new HashMap<String, Attribute>();
		}
		else if(qName.equalsIgnoreCase("attribute"))
		{
			attributeName = attributes.getValue("name");
			attribute = new Attribute();
			attribute.setName(attributeName);
			attribute.setChangeType(attributes.getValue("type"));
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException
	{
		if(qName.equalsIgnoreCase("attribute"))
		{
			attribute.setValue(tempValue);
			attributes.put(attributeName, attribute);
		}
		else if(qName.equalsIgnoreCase("tag"))
		{
			newElementTemp.setTag(tempValue);
		}
		else if(qName.equalsIgnoreCase("text"))
		{
			newElementTemp.setText(tempValue);
		}
		else if(qName.equalsIgnoreCase("newElement"))
		{
			newElementTemp.setAttributes(attributes);
			lNewElements.add(newElementTemp);
		}
		else if(qName.equalsIgnoreCase("xpath"))
		{
			elementTemp.setXpath(tempValue);
		}
		else if(qName.equalsIgnoreCase("htmlElement"))
		{
			elementTemp.setNewElements(lNewElements);
			lElement.add(elementTemp);
		}
		else if(qName.equalsIgnoreCase("changeElements"))
		{
			changeElementsTemp.setElements(lElement);
			lChangeElements.add(changeElementsTemp);
		}
		else if(qName.equalsIgnoreCase("htmlFile"))
		{
			htmlFileTemp.setChangeElements(lChangeElements);
			lHtmlFiles.add(htmlFileTemp);
		}
	}
	
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException
	{
		tempValue = tempValue + new String(ch, start, length);
	}
}

class Attribute
{
	private String name;
	private String value;
	private String changeType;
	private String originalValue;
	
	public String getOriginalValue() {
		return originalValue;
	}
	public void setOriginalValue(String originalValue) {
		this.originalValue = originalValue;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public String getValue()
	{
		return value;
	}
	public void setValue(String value)
	{
		this.value = value;
	}
	public String getChangeType()
	{
		return changeType;
	}
	public void setChangeType(String changeType)
	{
		this.changeType = changeType;
	}
	@Override
	public String toString()
	{
		return "Attribute [name=" + name + ", value=" + value + ", originalValue=" + originalValue + ", changeType=" + changeType + "]";
	}
}

/**
 * represents newElement tag in the lines.xml file
 */
class NewElement
{
	private String tag;
	private String text;
	private HashMap<String, Attribute> attributes = new HashMap<String, Attribute>();
	private String original;
	
	public String getOriginal() {
		return original;
	}
	public void setOriginal(String original) {
		this.original = original;
	}
	public String getTag()
	{
		return tag;
	}
	public void setTag(String tag)
	{
		this.tag = tag;
	}
	public String getText()
	{
		return text;
	}
	public void setText(String text)
	{
		this.text = text;
	}
	public HashMap<String, Attribute> getAttributes()
	{
		return attributes;
	}
	public void setAttributes(HashMap<String, Attribute> attributes2)
	{
		this.attributes = attributes2;
	}
	@Override
	public String toString()
	{
		return "NewElement [tag=" + tag + ", text=" + text + ", attributes=" + attributes + "]";
	}
}

/**
 * represents element tag in the lines.xml file
 */
class HtmlTag
{
	private String xpath;
	private ChangeType changeType;
	private ArrayList<NewElement> newElements;
	
	public String getXpath()
	{
		return xpath;
	}
	public void setXpath(String xpath)
	{
		this.xpath = xpath;
	}
	public ChangeType getChangeType()
	{
		return changeType;
	}
	public void setChangeType(ChangeType changeType)
	{
		this.changeType = changeType;
	}
	public ArrayList<NewElement> getNewElements()
	{
		return newElements;
	}
	public void setNewElements(ArrayList<NewElement> newElements)
	{
		this.newElements = newElements;
	}
	@Override
	public String toString()
	{
		return "HtmlElement [xpath=" + xpath + ", changeType=" + changeType + ", newElements=" + newElements + "]";
	}
}

/**
 * represents changeElements tag in the lines.xml file
 */
class ChangeElements
{
	private ArrayList<HtmlTag> htmlElements;

	public ArrayList<HtmlTag> getElements()
	{
		return htmlElements;
	}

	public void setElements(ArrayList<HtmlTag> htmlElements)
	{
		this.htmlElements = htmlElements;
	}
	@Override
	public String toString()
	{
		return "ChangeElements [htmlElements=" + htmlElements + "]";
	}
}

/**
 * represents htmlFile tag in the lines.xml file
 */
class HtmlFile
{
	private String fileName;
	private String path;
	private ArrayList<ChangeElements> changeElements;
	
	public String getFileName()
	{
		return fileName;
	}
	public void setFileName(String fileName)
	{
		this.fileName = fileName;
	}
	public String getPath()
	{
		return path;
	}
	public void setPath(String path)
	{
		this.path = path;
	}
	public ArrayList<ChangeElements> getChangeElements()
	{
		return changeElements;
	}

	public void setChangeElements(ArrayList<ChangeElements> changeElements)
	{
		this.changeElements = changeElements;
	}
	@Override
	public String toString()
	{
		return "HtmlFile [fileName=" + fileName + ", path=" + path + ", changeElements=" + changeElements + "]";
	}
}