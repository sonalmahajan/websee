package rca;

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jgap.InvalidConfigurationException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.Point;

import util.Util;
import websee.HtmlElement;
import websee.ImageProcessing;
import config.Constants;

public class NumericAnalysis
{
	private Logger log;
	
	private String oracleFullPath;
	private String testFileFullPath;
	private String testScreenshotFullPath;
	private List<Point> originalDifferencePixels;
	private HtmlElement element;
	private String visualProperty;
	private String visualPropertyValue;
	private boolean isCSS;
	private boolean areNegativeValuesAllowed;
	private String cssUnit;
	
	private boolean fixFound;
	private int reducedDiffPixels;
	
	private long startTimeProperty;
	
	private static int horizontalTranslationValue = -1;
	private static int verticalTranslationValue = -1;

	public static void resetTranslationValues()
	{
		horizontalTranslationValue = -1;
		verticalTranslationValue = -1;
	}
	
	public NumericAnalysis(String oracleFullPath, String testFileFullPath, String testScreenshotFullPath, List<Point> originalDifferencePixels, HtmlElement element, String visualProperty, String visualPropertyValue, boolean isCSS, boolean areNegativeValuesAllowed, Logger log)
	{
		this.oracleFullPath = oracleFullPath;
		this.testFileFullPath = testFileFullPath;
		this.testScreenshotFullPath = testScreenshotFullPath;
		this.originalDifferencePixels = originalDifferencePixels;
		this.element = element;
		this.visualProperty = visualProperty;
		this.visualPropertyValue = visualPropertyValue;
		this.log = log;
		this.isCSS = isCSS;
		this.areNegativeValuesAllowed = areNegativeValuesAllowed;
		
		if(isCSS)
		{
			cssUnit = "px";
		}
		else
		{
			cssUnit = "";
		}
		
		this.fixFound = false;
		
		this.startTimeProperty = System.nanoTime();
	}
	
	public Map<String, String> runNumericAnalysis(boolean isRCANumericAnalysisRateOfChange) throws IOException, InvalidConfigurationException
	{
		Map<String, String>  returnMap = new HashMap<String, String>();

		// check for a pure string value which can come from predefined values
		if(isCSS && Util.getDecimalNumberFromString(visualPropertyValue) == null)
		{
			visualPropertyValue = "0.0px";
		}
		
		// step 1: perform translation heuristic
		double val = -1;
		if(RootCauseAnalysis.isSearchSpaceSetByHeuristic())
		{
			val = translationHeuristic();
		}
		
		if(Util.convertNanosecondsToSeconds(System.nanoTime() - startTimeProperty) > Constants.RCA_PER_PROPERTY_TIMEOUT_IN_MINS * 60)
		{
			log.info("");
			log.info("Analysis for property timed out");
			returnMap.put(Constants.RCA_FIX_NOT_FOUND, 1 + "#" + (Util.getDecimalNumberFromString(visualPropertyValue) + val) + cssUnit);
			return returnMap;
		}
		
		if(fixFound)
		{
			returnMap.put(Constants.RCA_FIX_FOUND, reducedDiffPixels + "#" + (Util.getDecimalNumberFromString(visualPropertyValue) + val) + cssUnit);
			return returnMap;
		}
		
		// no translation
		if(val == 0)
		{
			returnMap.put(Constants.RCA_FIX_NOT_FOUND, -1 + "#" + (Util.getDecimalNumberFromString(visualPropertyValue) + val) + cssUnit);
			return returnMap;		
		}

		// step 2: run search algorithm
		double rangeLow =  0;
		double rangeHigh = 1;

		if(areNegativeValuesAllowed)
		{
			//rangeLow =  -1.0 * Math.abs(Util.getDecimalNumberFromString(visualPropertyValue) - val);
			rangeLow =  Util.getDecimalNumberFromString(visualPropertyValue) - val;
		}
		//double rangeHigh =  Math.abs(Util.getDecimalNumberFromString(visualPropertyValue) + val);
		rangeHigh =  Util.getDecimalNumberFromString(visualPropertyValue) + val;

		if(!RootCauseAnalysis.isSearchSpaceSetByHeuristic())
		{
			ImageProcessing ip = new ImageProcessing();
			String[] oraclePAndN = Util.getPathAndFileNameFromFullPath(oracleFullPath);
			Rectangle rect = ip.getImageSize(oraclePAndN[0], oraclePAndN[1]);
			
			val = Math.max(rect.height, rect.width);
			
			if(areNegativeValuesAllowed)
			{
				rangeLow =  -1.0 * val;
			}
			else
			{
				rangeLow = 0;
			}
			rangeHigh =  val;
			
			//rangeLow = Short.MIN_VALUE;
			//rangeHigh = Short.MAX_VALUE;
			
			GAFitnessFunction ga = new GAFitnessFunction(element, (isCSS ? "style:"+visualProperty+":"+cssUnit : visualProperty), oracleFullPath, testFileFullPath, originalDifferencePixels, Util.getDecimalNumberFromString(visualPropertyValue), log);
			return ga.runGAForNumericAnalysisForSBST((int)rangeLow, (int)rangeHigh);
			
			/*Double expectedValue = Util.getDecimalNumberFromString(RootCauseAnalysis.getExpectedValue());
			if(expectedValue == null)
			{
				expectedValue = 0.0;
			}
			rangeLow = expectedValue - Constants.RCA_SBST_SEARCH_SPACE_SIZE;
			if(!areNegativeValuesAllowed && rangeLow < 0)
			{
				rangeLow = 0;
			}
			rangeHigh = expectedValue + Constants.RCA_SBST_SEARCH_SPACE_SIZE;*/
		}
		
		if(rangeHigh < rangeLow)
		{
			// swap
			double temp = rangeLow;
			rangeLow = rangeHigh;
			rangeHigh = temp;
		}
		
		if(RootCauseAnalysis.isSimmulatedAnnealingToBeUsed())
		{
			SimulatedAnnealing sa = new SimulatedAnnealing(element, (isCSS ? "style:"+visualProperty+":"+cssUnit : visualProperty), oracleFullPath, testFileFullPath, originalDifferencePixels, Util.getDecimalNumberFromString(visualPropertyValue), startTimeProperty ,log);
			return sa.runSimulatedAnnealing((int)rangeLow, (int)rangeHigh, Constants.SIMULATED_ANNEALING_STARTING_TEMPERATURE, isRCANumericAnalysisRateOfChange);			
		}
		else
		{
			GAFitnessFunction ga = new GAFitnessFunction(element, (isCSS ? "style:"+visualProperty+":"+cssUnit : visualProperty), oracleFullPath, testFileFullPath, originalDifferencePixels, Util.getDecimalNumberFromString(visualPropertyValue), log);
			return ga.runGAForNumericAnalysis((int)rangeLow, (int)rangeHigh);
		}
	}

	private double translationHeuristic() throws IOException
	{
		ImageProcessing ip = new ImageProcessing();
		String[] oraclePAndN = Util.getPathAndFileNameFromFullPath(oracleFullPath);
		String[] testPAndN = Util.getPathAndFileNameFromFullPath(testScreenshotFullPath);
		
		String oracleMinusTest = Util.getFileNameAndExtension(oraclePAndN[1])[0] + "_minus_" + Util.getFileNameAndExtension(testPAndN[1])[0] + ".txt";
		String testMinusOracle = Util.getFileNameAndExtension(testPAndN[1])[0] + "_minus_" + Util.getFileNameAndExtension(oraclePAndN[1])[0] + ".txt";
		
		ip.compareImages(oraclePAndN[0], oraclePAndN[1], testPAndN[0], testPAndN[1], oracleMinusTest, Constants.IMAGEMAGICK_COMPARE_MINUS_SRC, true);
		ip.compareImages(oraclePAndN[0], oraclePAndN[1], testPAndN[0], testPAndN[1], testMinusOracle, Constants.IMAGEMAGICK_COMPARE_MINUS_DST, true);

		BufferedReader oracleFile = new BufferedReader(new FileReader(testPAndN[0] + File.separatorChar + oracleMinusTest));
		BufferedReader testFile = new BufferedReader(new FileReader(testPAndN[0] + File.separatorChar + testMinusOracle));
		String line;
		String[] strPixel;
		List<Integer> oracleX = new ArrayList<Integer>();
		List<Integer> oracleY = new ArrayList<Integer>();
		List<Integer> testX = new ArrayList<Integer>();
		List<Integer> testY = new ArrayList<Integer>();

		if(verticalTranslationValue == -1 && horizontalTranslationValue == -1)
		{
			Rectangle rectTest = ip.getImageSize(testPAndN[0], testPAndN[1]);
			Rectangle rectOracle = ip.getImageSize(oraclePAndN[0], oraclePAndN[1]);
			
			int testDPCount = 0;
			while ((line = testFile.readLine()) != null)
			{
				if (!line.contains("black") && !line.contains("enumeration"))
				{
					
					strPixel = line.split(":")[0].split(",");
					int x = Integer.parseInt(strPixel[0]);
					int y = Integer.parseInt(strPixel[1]);
					
					//if(Util.isPointInRectangle(x, y, element.getX(), element.getY(), element.getWidth(), element.getHeight(), true))
					if(Util.isPointInRectangle(x, y, 0, 0, rectTest.width, rectTest.height, true))
					{
						testX.add(x);
						testY.add(y);
						testDPCount++;
					}
				}
			}
			testFile.close();
			
			int count = 0;
			while ((line = oracleFile.readLine()) != null)
			{
				if (!line.contains("black") && !line.contains("enumeration"))
				{
					if(count >= testDPCount)
					{
						break;
					}
					strPixel = line.split(":")[0].split(",");
					int x = Integer.parseInt(strPixel[0]);
					int y = Integer.parseInt(strPixel[1]);
					if(Util.isPointInRectangle(x, y, 0, 0, rectOracle.width, rectOracle.height, true))
					{
						oracleX.add(x);
						oracleY.add(y);
						count++;
					}
				}
			}
			oracleFile.close();
	
			if(oracleX.size() == 0 || oracleY.size() == 0 || testX.size() == 0 || testY.size() == 0)
			{
				log.info("No translation heuristic could be applied because one of the four lists are 0");
				log.info("oracleX.size() = " + oracleX.size());
				log.info("oracleY.size() = " + oracleY.size());
				log.info("testX.size() = " + testX.size());
				log.info("testY.size() = " + testY.size());
				return 0;
			}
			
			// compute average horizontal translation
			horizontalTranslationValue = 0;
			int maxHori = Integer.MIN_VALUE;
			int maxVer = Integer.MIN_VALUE;
			
			for(int i = 0; i < oracleX.size() && i < testX.size(); i += Constants.NUMERIC_ANALYSIS_TRANSLATION_SUB_SAMPLING_VALUE)
			{
				horizontalTranslationValue = horizontalTranslationValue + oracleX.get(i) - testX.get(i);
				/*if(oracleX.get(i) - testX.get(i) > maxHori)
				{
					maxHori = oracleX.get(i) - testX.get(i);
				}*/
			}
			horizontalTranslationValue = (int) Math.ceil((double)horizontalTranslationValue / testX.size());
			//horizontalTranslationValue = maxHori;

			// compute average vertical translation
			verticalTranslationValue = 0;
			for(int i = 0; i < oracleY.size() && i < testY.size(); i += Constants.NUMERIC_ANALYSIS_TRANSLATION_SUB_SAMPLING_VALUE)
			{
				verticalTranslationValue = verticalTranslationValue + oracleY.get(i) - testY.get(i);
				/*if(oracleY.get(i) - testY.get(i) > maxVer)
				{
					maxVer = oracleY.get(i) - testY.get(i);
				}*/
			}
			verticalTranslationValue = (int) Math.ceil((double)verticalTranslationValue / testY.size());
			//verticalTranslationValue = maxVer;
		}
		
		Document doc = Jsoup.parse(new File(testFileFullPath), null);
		Element e = Util.getElementFromXPathJava(element.getXpath(), doc);
		double newValue = 0;
		String newValueString = "";
		double retValue = 0;
		
		String logString = "    ";
		if(visualProperty.toLowerCase().contains("left") || visualProperty.toLowerCase().contains("right") || visualProperty.toLowerCase().contains("width") || visualProperty.toLowerCase().contains("indent"))
		{
			int horizontalTranslationValueTmp = horizontalTranslationValue;
			
			// handle width specially
			if(visualProperty.toLowerCase().equals("width"))
			{
				horizontalTranslationValueTmp = horizontalTranslationValueTmp * -1;
			}
			
			// apply only horizontal translation
			newValue = Util.getDecimalNumberFromString(visualPropertyValue) + horizontalTranslationValueTmp;
			newValueString = newValue + cssUnit;
			retValue = horizontalTranslationValueTmp;
			logString = logString + "value (HT left top) = ";
			applyTranslationToPageAndCheckWithDetection(doc, e, newValueString, logString);
		}
		else if(visualProperty.toLowerCase().contains("top") || visualProperty.toLowerCase().contains("bottom") || visualProperty.toLowerCase().contains("height"))
		{
			int verticalTranslationValueTmp = verticalTranslationValue;
			
			// handle height specially
			if(visualProperty.toLowerCase().equals("height"))
			{
				verticalTranslationValueTmp = verticalTranslationValueTmp * -1;
			}

			// apply only vertical translation
			newValue = Util.getDecimalNumberFromString(visualPropertyValue) + verticalTranslationValueTmp;
			newValueString = newValue + cssUnit;
			retValue = verticalTranslationValueTmp;
			logString = logString + "value (VT left top) = ";
			applyTranslationToPageAndCheckWithDetection(doc, e, newValueString, logString);
		}
		else
		{
			// apply both translations
			// 1. horizontal translation
			newValue = Util.getDecimalNumberFromString(visualPropertyValue) + horizontalTranslationValue;
			newValueString = newValue + cssUnit;
			retValue = horizontalTranslationValue;
			logString = "    ";
			logString = logString + "value (HT left top) = ";
			applyTranslationToPageAndCheckWithDetection(doc, e, newValueString, logString);
			
			if(!fixFound)
			{
				logString = "    ";
				// 2. vertical translation
				newValue = Util.getDecimalNumberFromString(visualPropertyValue) + verticalTranslationValue;
				newValueString = newValue + cssUnit;
				retValue = verticalTranslationValue;
				logString = logString + "value (VT left top) = ";
				applyTranslationToPageAndCheckWithDetection(doc, e, newValueString, logString);
			}
		}

		System.out.println("average horizontal translation = " + horizontalTranslationValue);
		System.out.println("average vertical translation = " + verticalTranslationValue);
		//System.out.println("max horizontal translation = " + horizontalTranslationValue);
		//System.out.println("max vertical translation = " + verticalTranslationValue);
		
		// if problem not solved, return translation values
		if(!fixFound)
		{
			// take max of all translations
			retValue = Math.max(Math.abs(horizontalTranslationValue), Math.abs(verticalTranslationValue));
		}
		return retValue;
	}
	
	private void applyTranslationToPageAndCheckWithDetection(Document doc, Element e, String newValueString, String logString) throws IOException
	{
		if(newValueString.equalsIgnoreCase(visualPropertyValue))
		{
			return;
		}
		
		if(isCSS)
		{
			String style = e.attr("style");
			style = style + "; " + visualProperty + ":" + newValueString;
			e.attr("style", style);
		}
		else
		{
			e.attr(visualProperty, newValueString);
		}
		logString = logString + newValueString;
		
		doc.outputSettings().prettyPrint(false);
		//doc.outputSettings().escapeMode(Entities.EscapeMode.extended);
		String html = doc.html();		
		PrintWriter out = new PrintWriter(new File(testFileFullPath));
		out.print(html);
		out.close();
		
		// run detection to see if the problem is resolved
		reducedDiffPixels = RootCauseAnalysis.fitnessFunction(oracleFullPath, testFileFullPath, originalDifferencePixels);
		
		if (reducedDiffPixels == 0)
		{
			// problem solved
			logString = logString + " => exact root cause found!";
			fixFound = true;
		}
		else if (Util.isCurrentInAcceptableReductionThreshold(reducedDiffPixels, originalDifferencePixels.size()))
		{
			// problem solved
			logString = logString + " => acceptable root cause found!";
			fixFound = true;
		}
		log.info(logString);
	}
}
