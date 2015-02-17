package websee;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import org.apache.xerces.parsers.DOMParser;
import org.cyberneko.html.HTMLConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import util.Util;
import cz.vutbr.web.css.CSSFactory;
import cz.vutbr.web.css.NodeData;
import cz.vutbr.web.domassign.StyleMap;

/**
 * jStyleParser used for CSS parsing
 * @author sonal
 *
 */

public class CSSParser
{
	private String fileFullPath;
	private InputStream is;
    private Document doc;
    private String charset;
    private StyleMap styleMap;

    public CSSParser(String fileFullPath)
    {
    	this.fileFullPath = fileFullPath;
    }

    public InputStream getInputStream()
    {
        return is;
    }

    private void parse() throws SAXException, IOException
    {
        this.is = new FileInputStream(fileFullPath);
        DOMParser parser = new DOMParser(new HTMLConfiguration());
        parser.setProperty("http://cyberneko.org/html/properties/names/elems", "lower");
        if (charset != null)
            parser.setProperty("http://cyberneko.org/html/properties/default-encoding", charset);
        parser.parse(new org.xml.sax.InputSource(is));
        doc = parser.getDocument();
    }

    public void parseCSS() throws SAXException, IOException
    {
    	parse();
    	
    	// don't include inherited properties
    	styleMap = CSSFactory.assignDOM(doc, createBaseFromFilename(fileFullPath), "screen", false);
    }
    
    private static URL createBaseFromFilename(String filename) {
		try {
			File f = new File(filename);
			return f.toURI().toURL();
		} catch (MalformedURLException e) {
			return null;
		}
	}
    
    public void parseCSSWithInheritance() throws SAXException, IOException
    {
    	parse();
    	
    	// include inherited properties
    	styleMap = CSSFactory.assignDOM(doc, createBaseFromFilename(fileFullPath), "screen", true);
    }
    
    public Map<String, String> getCSSPropertiesForElement(String xpathExpression) throws XPathExpressionException, IOException
    {
    	Map<String, String> cssProperties = new HashMap<String, String>();
    	
//    	XPath xPath = XPathFactory.newInstance().newXPath();
//    	NodeList nodes = (NodeList)xPath.evaluate(xpathExpression, doc.getDocumentElement(), XPathConstants.NODESET);

    	Element e = Util.getW3CElementFromXPathJava(xpathExpression, doc);
    	
    	if(e != null)
    	{
    		NodeData data = styleMap.get(e);
    		
    		// process data
    		if(data != null)
    		{
	    		String[] rules = data.toString().split(";");
	    		
		    	for (int i = 0; i < rules.length-1; i++)
				{
					String[] rule = rules[i].split(":\\s");
					if(rule.length == 2)
					{
						String prop = rule[0].trim();
						String val = rule[1].trim();
						if(!val.isEmpty())
						{
							cssProperties.put(prop, val);
						}
					}
				}
		    	
		    	// get shorthand css properties
		    	ShorthandCSSProperties shorthandCSSProperties = ShorthandCSSProperties.getInstance();
		    	Map<String, List<String>> shorthandProperties = shorthandCSSProperties.getShorthandPropertyMap();
		    	
		    	// check if individual rules can be combined to a short hand notation
	    		for(String key : shorthandProperties.keySet())
	    		{
	    			boolean fitForSubstitution = true;
	    			
	    			// read value of first property and match with remaining
	    			String propertyValue = cssProperties.get(shorthandProperties.get(key).get(0));
	    			for(String val : shorthandProperties.get(key))
	    			{
	    				if(propertyValue == null || !cssProperties.containsKey(val) || !propertyValue.equalsIgnoreCase(cssProperties.get(val)))
	    				{
	    					fitForSubstitution = false;
	    				}
	    			}
	    			
	    			if(fitForSubstitution)
	    			{
	    				// replace individual properties with shorthand property
	    				for(String val : shorthandProperties.get(key))
	    				{
	    					cssProperties.remove(val);
	    				}
	    				cssProperties.put(key, propertyValue);
	    			}
	    		}
    		}
    	}
    	return cssProperties;
    }
    
    public static void main(String[] args) throws SAXException, IOException, XPathExpressionException
	{
		CSSParser cp = new CSSParser("/Users/sonal/USC/rca_ml/xpert_runs/www.gtk.org/test1/www.gtk.org/index.html");
		cp.parseCSS();
		System.out.println(cp.getCSSPropertiesForElement("/HTML[1]/BODY[1]/MAIN[1]".toLowerCase()).keySet());
	}
}