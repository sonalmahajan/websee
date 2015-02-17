package rca;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.Point;

import util.Util;
import websee.HtmlElement;
import config.Constants;

public class PredefinedValuesAnalysis
{
	private Logger log;
	
	// singleton pattern
	private static Map<String, List<String>> predefinedPropertyValuesMap;
	
	private String oracleFullPath;
	private String testFileFullPath;
	private HtmlElement element;
	private String visualProperty;
	private String visualPropertyValue;
	private List<Point> originalDifferencePixels;
	
	private boolean isCSS;
	
	private long startTimeProperty;

	public PredefinedValuesAnalysis(String oracleFullPath, String testFileFullPath, List<Point> originalDifferencePixels, HtmlElement element, String visualProperty, String visualPropertyValue, boolean isCSS, Logger log)
	{
		if(predefinedPropertyValuesMap == null || predefinedPropertyValuesMap.isEmpty())
		{
			predefinedPropertyValuesMap = new HashMap<String, List<String>>();
			LinkedProperties prop = (LinkedProperties) readPropertiesFile(new File(Constants.PREDEFINED_CSS_PROPERTIES_FILE_PATH));
			processProperties(prop);
		}
		
		this.oracleFullPath = oracleFullPath;
		this.testFileFullPath = testFileFullPath;
		this.originalDifferencePixels = originalDifferencePixels;
		this.element = element;
		this.visualProperty = visualProperty;
		this.visualPropertyValue = visualPropertyValue;
		this.isCSS = isCSS;
		this.log = log;
		
		this.startTimeProperty = System.nanoTime();
	}
	public static Map<String, List<String>> getPredefinedPropertyValuesMap()
	{
		return predefinedPropertyValuesMap;
	}

	public static void setPredefinedPropertyValuesMap(Map<String, List<String>> predefinedPropertyValuesMap)
	{
		PredefinedValuesAnalysis.predefinedPropertyValuesMap = predefinedPropertyValuesMap;
	}

	private Properties readPropertiesFile(File file)
	{
		// Read properties file.
		LinkedProperties properties = new LinkedProperties();
		try
		{
			properties.load(new FileInputStream(file));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return properties;
	}
	
	private void processProperties(LinkedProperties prop)
	{
		for(Object key : prop.keySet())
		{
			String propertyValue = prop.getProperty((String) key);
			predefinedPropertyValuesMap.put((String) key, Arrays.asList(propertyValue.split("\\|")));
		}
	}
	
	public Map<String, String> runPredefinedValues() throws IOException
	{
		Map<String, String>  returnMap = new HashMap<String, String>();
		
		List<String> values = predefinedPropertyValuesMap.get(visualProperty);
		
		int diffPixels = originalDifferencePixels.size();
		if(values != null)
		{
			for(String val : values)
			{
				if(Util.convertNanosecondsToSeconds(System.nanoTime() - startTimeProperty) > Constants.RCA_PER_PROPERTY_TIMEOUT_IN_MINS * 60)
				{
					log.info("");
					log.info("Analysis for property timed out");
					returnMap.put(Constants.RCA_FIX_NOT_FOUND, diffPixels + "#" + val);
					return returnMap;
				}
				
				if(val.equalsIgnoreCase(visualPropertyValue))
				{
					returnMap.put(Constants.RCA_FIX_NOT_FOUND, -1 + "#" + val);
					continue;
				}
				
				String logString = "    ";
				
				// seed val in the page
				Document doc = Jsoup.parse(new File(testFileFullPath), null);
				Element e = Util.getElementFromXPathJava(element.getXpath(), doc);
				if(isCSS)
				{
					String style = e.attr("style"); 
					style = style + "; " + visualProperty + ":" + val; 
					e.attr("style", style);
				}
				else
				{
					e.attr(visualProperty, val);
				}
				logString = logString + "value = " + val;
				
				doc.outputSettings().prettyPrint(false);
//				doc.outputSettings().escapeMode(Entities.EscapeMode.extended);
				String html = doc.html();		
				PrintWriter out = new PrintWriter(new File(testFileFullPath));
				out.print(html);
				out.close();

				// run detection to see if the problem is resolved
				diffPixels = RootCauseAnalysis.fitnessFunction(oracleFullPath, testFileFullPath, originalDifferencePixels);
				
				if (diffPixels == 0)
				{
					// problem solved
					logString = logString + " => exact root cause found!";
					log.info(logString);
					returnMap.put(Constants.RCA_FIX_FOUND, val);
					return returnMap;
				}
				log.info(logString);
				returnMap.put(Constants.RCA_FIX_NOT_FOUND, diffPixels + "#" + val);
			}
		}
		else
		{
			returnMap.put(Constants.RCA_FIX_NOT_FOUND, -1 + "#" + "INVALID PROPERTY");
			log.info("given property "+ visualProperty + " not in the valid list of properties");
		}
		return returnMap;
	}
}

/**
 * preserve order of the properties file
 */
class LinkedProperties extends Properties
{
	private static final long serialVersionUID = 1L;
	private final LinkedHashSet<Object> keys = new LinkedHashSet<Object>();

	public Enumeration<Object> keys()
	{
		return Collections.<Object> enumeration(keys);
	}

	@Override
	public Set<Object> keySet()
	{
		return new LinkedHashSet<Object>(Collections.list(keys()));
	}

	public Object put(Object key, Object value)
	{
		keys.add(key);
		return super.put(key, value);
	}
}