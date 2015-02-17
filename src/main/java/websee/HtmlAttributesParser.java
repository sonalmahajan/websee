package websee;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import util.Util;

public class HtmlAttributesParser
{
	private Document document;
	
	public HtmlAttributesParser(String htmlFileFullPath)
	{
		try
		{
			this.document = Jsoup.parse(new File(htmlFileFullPath), null);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public Map<String, String> getHTMLAttributesForElement(String xpath) throws IOException
	{
		Map<String, String> htmlAttributes = new HashMap<String, String>();
		Element e = Util.getElementFromXPathJava(xpath, document);
		if(e != null)
		{
			for(Attribute attribute : e.attributes())
			{
				// ignore id, name, class, style, etc. attributes, since we want only explicitly specified visual html attributes
				if(!attribute.getKey().equalsIgnoreCase("id") && !attribute.getKey().equalsIgnoreCase("name") && 
						!attribute.getKey().equalsIgnoreCase("class") && !attribute.getKey().equalsIgnoreCase("style")
						&& !attribute.getKey().equalsIgnoreCase("src") && !attribute.getKey().equalsIgnoreCase("alt")
						&& !attribute.getKey().equalsIgnoreCase("href"))
				{
					htmlAttributes.put(attribute.getKey(), attribute.getValue());
				}
			}
		}
		return htmlAttributes;
	}
}
