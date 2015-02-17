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

public class CssAttributesMaster extends DefaultHandler
{
	// singleton pattern
	private static final CssAttributesMaster instance = new CssAttributesMaster();
	
	private File file;
	private HashMap<String, HashMap<String, String>> cssAttributes;
	private String property;
	private HashMap<String, String> propertyAttributes;
	private String key;
	private String value;

	private CssAttributesMaster()
	{
		this.file = new File(EvaluationFrameworkConstants.cssAttributesFilePath);
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
	
	public static CssAttributesMaster getInstance()
	{
		return instance;
	}
	
	public HashMap<String, HashMap<String, String>> getCssAttributes()
	{
		return cssAttributes;
	}

	public void setCssAttributes(HashMap<String, HashMap<String, String>> cssAttributes)
	{
		this.cssAttributes = cssAttributes;
	}
	
	public void parseXML() throws ParserConfigurationException, SAXException, IOException
	{
		SAXParserFactory factory = SAXParserFactory.newInstance();
	    SAXParser parser = factory.newSAXParser();
	    
	    cssAttributes = new HashMap<String, HashMap<String, String>>();
	    parser.parse(file, this);
	}
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
	{
		value = "";
		if(qName.equalsIgnoreCase("property"))
		{
			propertyAttributes = new HashMap<String, String>();
			property = attributes.getValue("name");
		}
		else if(qName.equalsIgnoreCase("attribute"))
		{
			key = attributes.getValue("name");
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException
	{
		if(qName.equalsIgnoreCase("attribute"))
		{
			propertyAttributes.put(key, value);
		}
		else if(qName.equalsIgnoreCase("property"))
		{
			cssAttributes.put(property, propertyAttributes);
		}
	}
	
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException
	{
		value = value + new String(ch, start, length);
	}

	@Override
	public String toString()
	{
		return "CssAttributesMaster [file=" + file + ", cssAttributes=" + cssAttributes + ", property=" + property + ", propertyAttributes=" + propertyAttributes + ", key=" + key + ", value=" + value + "]";
	}
}
