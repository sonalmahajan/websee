package evalframework;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class HtmlAttributesMaster extends DefaultHandler
{
	// singleton pattern
	private static final HtmlAttributesMaster instance = new HtmlAttributesMaster();
	
	private File file;
	private HashMap<String, String> htmlAttributes;
	private String key;
	private String value;

	private HtmlAttributesMaster()
	{
		this.file = new File(EvaluationFrameworkConstants.htmlAttributesFilePath);
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
	
	public static HtmlAttributesMaster getInstance()
	{
		return instance;
	}
	
	public HashMap<String, String> getHtmlAttributes()
	{
		return htmlAttributes;
	}

	public void setHtmlAttributes(HashMap<String, String> htmlAttributes)
	{
		this.htmlAttributes = htmlAttributes;
	}

	public void parseXML() throws ParserConfigurationException, SAXException, IOException
	{
		SAXParserFactory factory = SAXParserFactory.newInstance();
	    SAXParser parser = factory.newSAXParser();
	    
	    htmlAttributes = new HashMap<String, String>();
	    parser.parse(file, this);
	}
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
	{
		value = "";
		if(qName.equalsIgnoreCase("attribute"))
		{
			key = attributes.getValue("name");
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException
	{
		if(qName.equalsIgnoreCase("attribute"))
		{
			htmlAttributes.put(key, value);
		}
	}
	
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException
	{
		value = value + new String(ch, start, length);
	}
}
