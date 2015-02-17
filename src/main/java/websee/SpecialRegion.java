package websee;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SpecialRegion extends DefaultHandler
{
	public SpecialRegion()
	{}
	
	public SpecialRegion(String fullFilePath) 
	{
		this.file = new File(fullFilePath);
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
	
	private File file;
	private Set<SpecialRegion> specialRegions;
	private String value;
	private Rectangle rectangle;
	private TextRegion textRegionTemp;
	private ExclusionRegion exclusionRegionTemp;
	private Set<String> styleProperties;

    public Set<SpecialRegion> getSpecialRegions()
	{
		return specialRegions;
	}

	public void setSpecialRegions(Set<SpecialRegion> specialRegions)
	{
		this.specialRegions = specialRegions;
	}

	public Rectangle getRectangle()
	{
		return rectangle;
	}

	public void setRectangle(Rectangle rectangle)
	{
		this.rectangle = rectangle;
	}
	
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
		if(qName.equalsIgnoreCase("specialRegions"))
		{
			specialRegions = new HashSet<SpecialRegion>();			
		}
		else if(qName.equalsIgnoreCase("textRegion"))
		{
			textRegionTemp = new TextRegion();
		}
		else if(qName.equalsIgnoreCase("exclusionRegion"))
		{
			exclusionRegionTemp = new ExclusionRegion();
		}
		else if(qName.equalsIgnoreCase("rectangle"))
		{
			rectangle = new Rectangle();
		}
		else if(qName.equalsIgnoreCase("styleProperties"))
		{
			styleProperties = new HashSet<String>();
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException
	{
		if(qName.equalsIgnoreCase("left"))
		{
			rectangle.x = Integer.parseInt(value);
		}
		else if(qName.equalsIgnoreCase("top"))
		{
			rectangle.y = Integer.parseInt(value);
		}
		else if(qName.equalsIgnoreCase("width"))
		{
			rectangle.width = Integer.parseInt(value);
		}
		else if(qName.equalsIgnoreCase("height"))
		{
			rectangle.height = Integer.parseInt(value);
		}
		else if(qName.equalsIgnoreCase("styleProperty"))
		{
			styleProperties.add(value);
		}
		else if(qName.equalsIgnoreCase("textRegion"))
		{
			textRegionTemp.setStyleProperties(styleProperties);
			textRegionTemp.setRectangle(rectangle);
			specialRegions.add(textRegionTemp);
		}
		else if(qName.equalsIgnoreCase("exclusionRegion"))
		{
			exclusionRegionTemp.setRectangle(rectangle);
			specialRegions.add(exclusionRegionTemp);
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
		return "SpecialRegion [specialRegions=" + specialRegions + "]";
	}
}