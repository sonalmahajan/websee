package evalframework;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class HtmlElementsAttributes extends DefaultHandler
{
	// singleton pattern
	private static final HtmlElementsAttributes instance = new HtmlElementsAttributes();
	
	private File file;
	private HashMap<String, HtmlElementEval> htmlElementsAttributes;
	private String tagName;
	private HtmlElementEval htmlElement;
	private String value;

	private HtmlElementsAttributes()
	{
		this.file = new File(EvaluationFrameworkConstants.htmlElementsAttributesFilePath);
		try
		{
			parseXML();
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
	}
	
	public static HtmlElementsAttributes getInstance()
	{
		return instance;
	}

	public HashMap<String, HtmlElementEval> getHtmlAttributes()
	{
		return htmlElementsAttributes;
	}

	public void setHtmlAttributes(HashMap<String, HtmlElementEval> htmlAttributes)
	{
		this.htmlElementsAttributes = htmlAttributes;
	}

	public void parseXML() throws ParserConfigurationException, SAXException, IOException
	{
		SAXParserFactory factory = SAXParserFactory.newInstance();
	    SAXParser parser = factory.newSAXParser();
	    
	    htmlElementsAttributes = new HashMap<String, HtmlElementEval>();
	    parser.parse(file, this);
	}
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
	{
		value = "";
		if(qName.equalsIgnoreCase("htmlElement"))
		{
			tagName = attributes.getValue("tagName");
			htmlElement = new HtmlElementEval();
			htmlElement.setTagName(tagName);
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException
	{
		if(qName.equalsIgnoreCase("htmlAttributes"))
		{
			htmlElement.setHtmlAttributes(getArrayListFromString(value));
		}
		else if(qName.equalsIgnoreCase("cssAttributes"))
		{
			htmlElement.setCssAttributes(getArrayListFromString(value));
		}
		else if(qName.equalsIgnoreCase("requiredElements"))
		{
			htmlElement.setRequiredElements(getArrayListFromString(value));
		}
		else if(qName.equalsIgnoreCase("htmlElement"))
		{
			htmlElementsAttributes.put(tagName, htmlElement);
		}
	}
	
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException
	{
		value = value + new String(ch, start, length);
	}
	
	private ArrayList<String> getArrayListFromString(String string)
	{
		ArrayList<String> arrayList = new ArrayList<String>();
		
		if(string != null && !string.isEmpty())
		{
			String[] array = string.split(",");
			for (int i = 0; i < array.length; i++)
			{
				arrayList.add(array[i]);
			}
		}
		return arrayList;
	}
}