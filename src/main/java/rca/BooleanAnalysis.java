package rca;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.Point;

import util.Util;
import websee.HtmlElement;
import config.Constants;

public class BooleanAnalysis
{
	private Logger log;
	
	private String oracleFullPath;
	private String testFileFullPath;
	private List<Point> originalDifferencePixels;
	private HtmlElement element;
	private String visualAttribute;
	
	private long startTimeProperty;
	
	public BooleanAnalysis(String oracleFullPath, String testFileFullPath, List<Point> originalDifferencePixels, HtmlElement element, String visualAttribute, Logger log)
	{
		this.oracleFullPath = oracleFullPath;
		this.testFileFullPath = testFileFullPath;
		this.originalDifferencePixels = originalDifferencePixels;
		this.element = element;
		this.visualAttribute = visualAttribute;
		this.log = log;
		
		this.startTimeProperty = System.nanoTime();
	}
	
	public Map<String, String> runBooleanAnalysis() throws IOException
	{
		Map<String, String>  returnMap = new HashMap<String, String>();
		
		String logString = "    ";
		
		// remove html attribute from the element in test page and see if it solves the problem
		Document doc = Jsoup.parse(new File(testFileFullPath), null);
		Element e = Util.getElementFromXPathJava(element.getXpath(), doc);
		e.removeAttr(visualAttribute);
		logString = logString + "value = remove";
		doc.outputSettings().prettyPrint(false);
		//doc.outputSettings().escapeMode(Entities.EscapeMode.extended);
		String html = doc.html();		
		PrintWriter out = new PrintWriter(new File(testFileFullPath));
		out.print(html);
		out.close();

		// run detection to see if the problem is resolved
		int diffPixels = RootCauseAnalysis.fitnessFunction(oracleFullPath, testFileFullPath, originalDifferencePixels);
		
		if(Util.convertNanosecondsToSeconds(System.nanoTime() - startTimeProperty) > Constants.RCA_PER_PROPERTY_TIMEOUT_IN_MINS * 60)
		{
			log.info("");
			log.info("Analysis for property timed out");
			returnMap.put(Constants.RCA_FIX_NOT_FOUND, diffPixels + "#" + "remove " + visualAttribute);
			return returnMap;
		}
		
		if (diffPixels == 0)
		{
			// problem solved
			logString = logString + " => exact root cause found!";
			log.info(logString);
			returnMap.put(Constants.RCA_FIX_FOUND, "remove " + visualAttribute);
			return returnMap;
		}
		log.info(logString);
		returnMap.put(Constants.RCA_FIX_NOT_FOUND, diffPixels + "#" + "remove " + visualAttribute);
		return returnMap;
	}
}
