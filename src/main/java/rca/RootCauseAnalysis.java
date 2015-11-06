package rca;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jgap.InvalidConfigurationException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.xml.sax.SAXException;

import prioritization.Extract;
import prioritization.Feature;
import prioritization.Predict;
import prioritization.VisualProperty;
import util.Util;
import websee.HtmlAttributesParser;
import websee.HtmlElement;
import websee.WebDriverSingleton;
import websee.WebSeeTool;
import config.Constants;

public class RootCauseAnalysis
{
	Logger rootCauseResultsLog;
	Logger rootCauseDetailsLog;

	private String oracleFullPath;
	private String testPageFullPath;
	private String testScreenshotFullPath;
	private String differencePixelsFileFullPath;
	private HtmlElement element;
	private String[] testPagePathAndName;
	private String newDir;

	private boolean rootCauseFound;
	private int numberOfDifferencePixels;
	private int reducedNumberOfDifferencePixels;
	private String rootCauseProperty;
	private String fixValue;
	
	// categories
	private static List<String> colorCategory;
	private static List<String> numericCategory;
	private static List<String> numericPositiveNegativeCategory;
	private static List<String> predefinedCategory;
	private static List<String> predefinedNumericCategory;
	private static List<String> predefinedStringCategory;
	private static List<String> booleanCategory;

	private List<Point> originalDifferencePixels;

	private TreeMap<Integer, Map<String, String>> bestRootCause;
	
	private boolean isRCANumericAnalysisRateOfChange = true;

	private static boolean isPropertyPrioritizationOn;
	private static boolean isSearchSpaceSetByHeuristic;
	private static boolean isFitnessFunctionNew;
	private static boolean isSimmulatedAnnealingToBeUsed;
	private static String expectedValue; 
	
	public RootCauseAnalysis(HtmlElement element, String oracleFullPath, String testPageFullPath, Logger rootCauseResultsLog, Logger rootCauseDetailsLog)
	{
		this(element, oracleFullPath, testPageFullPath, rootCauseResultsLog, rootCauseDetailsLog, true);
	}
	
	public RootCauseAnalysis(HtmlElement element, String oracleFullPath, String testPageFullPath, Logger rootCauseResultsLog, Logger rootCauseDetailsLog, boolean isRCANumericAnalysisRateOfChange)
	{
		this.element = element;
		this.oracleFullPath = oracleFullPath;
		this.testPageFullPath = testPageFullPath;
		this.rootCauseResultsLog = rootCauseResultsLog;
		this.rootCauseDetailsLog = rootCauseDetailsLog;

		// create new directory
		this.testPagePathAndName = Util.getPathAndFileNameFromFullPath(testPageFullPath);
		this.newDir = testPagePathAndName[0] + File.separatorChar + "RCA";
		File dir = new File(newDir);
		dir.mkdir();

		// initialize the category lists only once
		if (colorCategory == null)
		{
			colorCategory = new ArrayList<String>();
			numericCategory = new ArrayList<String>();
			numericPositiveNegativeCategory = new ArrayList<String>();
			predefinedCategory = new ArrayList<String>();
			predefinedNumericCategory = new ArrayList<String>();
			predefinedStringCategory = new ArrayList<String>();
			booleanCategory = new ArrayList<String>();
		}
		readPropertiesFile();

		rootCauseFound = false;
		WebSeeTool vit = new WebSeeTool(oracleFullPath, testPageFullPath);
		originalDifferencePixels = vit.getDifferencePixels(oracleFullPath, testPageFullPath);
		numberOfDifferencePixels = originalDifferencePixels.size();
		reducedNumberOfDifferencePixels = numberOfDifferencePixels;

		testScreenshotFullPath = testPagePathAndName[0] + File.separatorChar + Util.getFileNameAndExtension(testPagePathAndName[1])[0] + Constants.SCREENSHOT_FILE_EXTENSION;
		if (!new File(testScreenshotFullPath).exists())
		{
			try
			{
				Util.getScreenshot(testPageFullPath, testPagePathAndName[0], Util.getFileNameAndExtension(testPagePathAndName[1])[0] + Constants.SCREENSHOT_FILE_EXTENSION, null, true);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		try
		{
			FileUtils.copyFileToDirectory(new File(testScreenshotFullPath), new File(newDir));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		testScreenshotFullPath = newDir + File.separatorChar + Util.getFileNameAndExtension(testPagePathAndName[1])[0] + Constants.SCREENSHOT_FILE_EXTENSION;
		
		bestRootCause = new TreeMap<Integer, Map<String, String>>();
		
		this.isRCANumericAnalysisRateOfChange = isRCANumericAnalysisRateOfChange;
	}

	public void resetCategoryLists()
	{
		colorCategory = null;
		numericCategory = null;
		numericPositiveNegativeCategory = null;
		predefinedCategory = null;
		predefinedNumericCategory = null;
		predefinedStringCategory = null;
		booleanCategory = null;
	}
	
	public void setConfig(boolean isPropertyPrioritizationOn, boolean isSearchSpaceSetByHeuristic, boolean isFitnessFunctionNew, boolean isSimmulatedAnnealingToBeUsed, String expectedValue)
	{
		RootCauseAnalysis.isPropertyPrioritizationOn = isPropertyPrioritizationOn;
		RootCauseAnalysis.isSearchSpaceSetByHeuristic = isSearchSpaceSetByHeuristic;
		RootCauseAnalysis.isFitnessFunctionNew = isFitnessFunctionNew;
		RootCauseAnalysis.isSimmulatedAnnealingToBeUsed = isSimmulatedAnnealingToBeUsed;
		RootCauseAnalysis.expectedValue = expectedValue;
	}
	
	public static boolean isPropertyPrioritizationOn()
	{
		return isPropertyPrioritizationOn;
	}

	public static boolean isSearchSpaceSetByHeuristic()
	{
		return isSearchSpaceSetByHeuristic;
	}

	public static boolean isFitnessFunctionNew()
	{
		return isFitnessFunctionNew;
	}

	public static boolean isSimmulatedAnnealingToBeUsed()
	{
		return isSimmulatedAnnealingToBeUsed;
	}

	public static String getExpectedValue()
	{
		return expectedValue;
	}
	public static void setPropertyPrioritizationOn(boolean isPropertyPrioritizationOn)
	{
		RootCauseAnalysis.isPropertyPrioritizationOn = isPropertyPrioritizationOn;
	}

	public static void setSearchSpaceSetByHeuristic(boolean isSearchSpaceSetByHeuristic)
	{
		RootCauseAnalysis.isSearchSpaceSetByHeuristic = isSearchSpaceSetByHeuristic;
	}

	public static void setFitnessFunctionNew(boolean isFitnessFunctionNew)
	{
		RootCauseAnalysis.isFitnessFunctionNew = isFitnessFunctionNew;
	}

	public static void setSimmulatedAnnealingToBeUsed(boolean isSimmulatedAnnealingToBeUsed)
	{
		RootCauseAnalysis.isSimmulatedAnnealingToBeUsed = isSimmulatedAnnealingToBeUsed;
	}

	public static void setExpectedValue(String expectedValue)
	{
		RootCauseAnalysis.expectedValue = expectedValue;
	}

	public List<Point> getOriginalDifferencePixels()
	{
		return originalDifferencePixels;
	}

	public void setOriginalDifferencePixels(List<Point> originalDifferencePixels)
	{
		this.originalDifferencePixels = originalDifferencePixels;
	}

	public int getReducedNumberOfDifferencePixels()
	{
		return reducedNumberOfDifferencePixels;
	}

	public String getRootCauseProperty()
	{
		return rootCauseProperty;
	}

	public String getFixValue()
	{
		return fixValue;
	}

	private int getNumberOfDifferencePixels()
	{
		BufferedReader reader;
		int lines = 0;
		try
		{
			reader = new BufferedReader(new FileReader(differencePixelsFileFullPath));
			while (reader.readLine() != null)
			{
				lines++;
			}
			reader.close();
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return lines;
	}

	private void readPropertiesFile()
	{
		// populate the category lists only once
		if (!colorCategory.isEmpty())
		{
			return;
		}

		File file = new File(Constants.RCA_PROPERTIES_CATEGORIZATION_FILE_PATH);
		LinkedProperties properties = new LinkedProperties();
		try
		{
			properties.load(new FileInputStream(file));
			for (Object key : properties.keySet())
			{
				String propertyValue = properties.getProperty((String) key);
				if (propertyValue.equalsIgnoreCase(Constants.RCA_COLOR_CATEGORY))
				{
					colorCategory.add((String) key);
				}
				else if (propertyValue.equalsIgnoreCase(Constants.RCA_NUMERIC_POSITIVE_ONLY_CATEGORY) || propertyValue.equalsIgnoreCase(Constants.RCA_NUMERIC_POSITIVE_NEGATIVE_CATEGORY))
				{
					numericCategory.add((String) key);
					if (propertyValue.equalsIgnoreCase(Constants.RCA_NUMERIC_POSITIVE_NEGATIVE_CATEGORY))
					{
						numericPositiveNegativeCategory.add((String) key);
					}
				}
				else if (propertyValue.equalsIgnoreCase(Constants.RCA_PREDEFINED_CATEGORY))
				{
					predefinedCategory.add((String) key);
				}
				else if (propertyValue.equalsIgnoreCase(Constants.RCA_PREDEFINED_NUMERIC_POSITIVE_ONLY_CATEGORY) || propertyValue.equalsIgnoreCase(Constants.RCA_PREDEFINED_NUMERIC_POSITIVE_NEGATIVE_CATEGORY))
				{
					predefinedNumericCategory.add((String) key);
					if (propertyValue.equalsIgnoreCase(Constants.RCA_PREDEFINED_NUMERIC_POSITIVE_NEGATIVE_CATEGORY))
					{
						numericPositiveNegativeCategory.add((String) key);
					}
				}
				else if (propertyValue.equalsIgnoreCase(Constants.RCA_PREDEFINED_STRING_CATEGORY))
				{
					predefinedStringCategory.add((String) key);
				}
				else if (propertyValue.equalsIgnoreCase(Constants.RCA_BOOLEAN_CATEGORY))
				{
					booleanCategory.add((String) key);
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void runHtmlAnalysis(String attr, String logString) throws IOException, InvalidConfigurationException
	{
		rootCauseDetailsLog.info("");

		// create clone of test file to work on
		String[] newName = Util.getFileNameAndExtension(testPagePathAndName[1], "_clone_" + attr);
		String cloneFileName = newName[0] + newName[1];
		String cloneTestPageFullPath = newDir + File.separatorChar + cloneFileName;
		FileUtils.copyFile(new File(testPageFullPath), new File(cloneTestPageFullPath));

		if (booleanCategory.contains(attr))
		{
			logString = logString + " (boolean category)";
			rootCauseDetailsLog.info(logString);

			Long startTime = System.nanoTime();

			// process for boolean
			BooleanAnalysis ba = new BooleanAnalysis(oracleFullPath, cloneTestPageFullPath, originalDifferencePixels, element, attr, rootCauseDetailsLog);
			Map<String, String> returnValue = ba.runBooleanAnalysis();

			Long endTime = System.nanoTime();
			rootCauseDetailsLog.info("Time = " + Util.convertNanosecondsToSeconds(endTime - startTime) + " sec");

			if (returnValue.containsKey(Constants.RCA_FIX_FOUND))
			{
				// problem found
				rootCauseFound = true;
				reducedNumberOfDifferencePixels = 0;
				rootCauseProperty = attr;
				fixValue = returnValue.get(Constants.RCA_FIX_FOUND);
				rootCauseResultsLog.info("Problem in " + element.getXpath() + " in attribute " + attr + 
						". New value should be remove" + returnValue.get(Constants.RCA_FIX_FOUND) + " => exact root cause found!" +
						" Number of difference pixels reduced from " + originalDifferencePixels.size() + " to " + reducedNumberOfDifferencePixels);
			}
			else
			{
				// check if the returned number of difference pixels are
				// less than the one already present in
				// reducedNumberOfDifferencePixels
				String ret[] = returnValue.get(Constants.RCA_FIX_NOT_FOUND).split("#");

				// indicates value same as property value was applied
				if (Integer.parseInt(ret[0]) == -1)
				{
					ret[0] = numberOfDifferencePixels + "";
				}

				if (Integer.parseInt(ret[0]) <= reducedNumberOfDifferencePixels)
				{
					// candidate root cause
					reducedNumberOfDifferencePixels = Integer.parseInt(ret[0]);
					rootCauseProperty = attr;
					fixValue = ret[1];
				}
			}
		}
		else if (numericCategory.contains(attr))
		{
			logString = logString + " (numeric category)";
			rootCauseDetailsLog.info(logString);

			Long startTime = System.nanoTime();

			// process for numeric values by genetic algorithm
			NumericAnalysis na = new NumericAnalysis(oracleFullPath, cloneTestPageFullPath, testScreenshotFullPath, originalDifferencePixels, element, attr, element.getHtmlAttributes().get(attr), false,
					numericPositiveNegativeCategory.contains(attr), rootCauseDetailsLog);
			Map<String, String> returnValue = na.runNumericAnalysis(isRCANumericAnalysisRateOfChange);

			Long endTime = System.nanoTime();
			rootCauseDetailsLog.info("Time = " + Util.convertNanosecondsToSeconds(endTime - startTime) + " sec");

			if (returnValue.containsKey(Constants.RCA_FIX_FOUND))
			{
				// problem found
				String ret[] = returnValue.get(Constants.RCA_FIX_FOUND).split("#");
				
				rootCauseFound = true;
				reducedNumberOfDifferencePixels = Integer.valueOf(ret[0]);
				rootCauseProperty = attr;
				fixValue = ret[1];
				String fixString = " root cause found!";
				if(reducedNumberOfDifferencePixels == 0)
				{
					fixString = " => exact" + fixString;
				}
				else
				{
					fixString = " => acceptable" + fixString;
				}
				rootCauseResultsLog.info("Problem in " + element.getXpath() + " in attribute " + attr + 
						". New value should be " + fixValue + fixString +
						" Number of difference pixels reduced from " + originalDifferencePixels.size() + " to " + reducedNumberOfDifferencePixels);
			}
			else
			{
				// check if the returned number of difference pixels are
				// less than the one already present in
				// reducedNumberOfDifferencePixels
				String ret[] = returnValue.get(Constants.RCA_FIX_NOT_FOUND).split("#");

				// indicates value same as property value was applied
				if (Integer.parseInt(ret[0]) == -1)
				{
					ret[0] = numberOfDifferencePixels + "";
				}

				if (Integer.parseInt(ret[0]) <= reducedNumberOfDifferencePixels)
				{
					// candidate root cause
					reducedNumberOfDifferencePixels = Integer.parseInt(ret[0]);
					rootCauseProperty = attr;
					fixValue = ret[1];
				}
			}
		}
		else if (predefinedCategory.contains(attr))
		{
			logString = logString + " (predefined category)";
			rootCauseDetailsLog.info(logString);

			Long startTime = System.nanoTime();

			// process for predefined values by bruteforce
			PredefinedValuesAnalysis pva = new PredefinedValuesAnalysis(oracleFullPath, cloneTestPageFullPath, originalDifferencePixels, element, attr, element.getHtmlAttributes().get(attr), false, rootCauseDetailsLog);
			Map<String, String> returnValue = pva.runPredefinedValues();

			Long endTime = System.nanoTime();
			rootCauseDetailsLog.info("Time = " + Util.convertNanosecondsToSeconds(endTime - startTime) + " sec");

			if (returnValue.containsKey(Constants.RCA_FIX_FOUND))
			{
				// problem found
				rootCauseFound = true;
				reducedNumberOfDifferencePixels = 0;
				rootCauseProperty = attr;
				fixValue = returnValue.get(Constants.RCA_FIX_FOUND);
				rootCauseResultsLog.info("Problem in " + element.getXpath() + " in attribute " + attr + 
						". New value should be " + returnValue.get(Constants.RCA_FIX_FOUND) + " => exact root cause found!" + 
						" Number of difference pixels reduced from " + originalDifferencePixels.size() + " to " + reducedNumberOfDifferencePixels);
			}
			else
			{
				// check if the returned number of difference pixels are
				// less than the one already present in
				// reducedNumberOfDifferencePixels
				String ret[] = returnValue.get(Constants.RCA_FIX_NOT_FOUND).split("#");

				// indicates value same as property value was applied
				if (Integer.parseInt(ret[0]) == -1)
				{
					ret[0] = numberOfDifferencePixels + "";
				}

				if (Integer.parseInt(ret[0]) <= reducedNumberOfDifferencePixels)
				{
					// candidate root cause
					reducedNumberOfDifferencePixels = Integer.parseInt(ret[0]);
					rootCauseProperty = attr;
					fixValue = ret[1];
				}
			}
		}
		
		Map<String, String> propValueMap;
		if(bestRootCause.get(reducedNumberOfDifferencePixels) != null)
		{
			propValueMap = bestRootCause.get(reducedNumberOfDifferencePixels);
		}
		else
		{
			propValueMap = new HashMap<String, String>();
		}
		propValueMap.put(rootCauseProperty, fixValue);
		bestRootCause.put(reducedNumberOfDifferencePixels, propValueMap);
	}

	private void runCssAnalysis(String property, String logString) throws IOException, InvalidConfigurationException
	{
		rootCauseDetailsLog.info("");

		// create clone of test file to work on
		String[] newName = Util.getFileNameAndExtension(testPagePathAndName[1], "_clone_" + property);
		String cloneFileName = newName[0] + newName[1];
		String cloneTestPageFullPath = newDir + File.separatorChar + cloneFileName;
		FileUtils.copyFile(new File(testPageFullPath), new File(cloneTestPageFullPath));

		if (colorCategory.contains(property))
		{
			logString = logString + " (color category)";
			rootCauseDetailsLog.info(logString);

			Long startTime = System.nanoTime();

			// process for color
			startTime = System.nanoTime();

			ColorAnalysis ca = new ColorAnalysis(oracleFullPath, cloneTestPageFullPath, originalDifferencePixels, element, property, element.getCssProperties().get(property), rootCauseDetailsLog);

			rootCauseDetailsLog.info("");
			rootCauseDetailsLog.info("Method 3: color distance");
			Map<String, String> returnValue = ca.runColorAnalysisByColorDistance();

			Long endTime = System.nanoTime();
			rootCauseDetailsLog.info("Time = " + Util.convertNanosecondsToSeconds(endTime - startTime) + " sec");

			if (returnValue.containsKey(Constants.RCA_FIX_FOUND))
			{
				// problem found
				rootCauseFound = true;
				reducedNumberOfDifferencePixels = 0;
				rootCauseProperty = property;
				fixValue = returnValue.get(Constants.RCA_FIX_FOUND);
				rootCauseResultsLog.info("Problem in " + element.getXpath() + " in attribute " + property + 
						". New value should be " + returnValue.get(Constants.RCA_FIX_FOUND) + " => exact root cause found!" + 
						" Number of difference pixels reduced from " + originalDifferencePixels.size() + " to " + reducedNumberOfDifferencePixels);
			}
			else if (returnValue.containsKey(Constants.RCA_FIX_NOT_FOUND))
			{
				// check if the returned number of difference pixels are less
				// than the one already present in
				// reducedNumberOfDifferencePixels
				String ret[] = returnValue.get(Constants.RCA_FIX_NOT_FOUND).split("#");

				// indicates value same as property value was applied
				if (Integer.parseInt(ret[0]) == -1)
				{
					ret[0] = numberOfDifferencePixels + "";
				}

				if (Integer.parseInt(ret[0]) <= reducedNumberOfDifferencePixels)
				{
					// candidate root cause
					reducedNumberOfDifferencePixels = Integer.parseInt(ret[0]);
					rootCauseProperty = property;
					fixValue = returnValue.get(Constants.RCA_FIX_NOT_FOUND);
				}
			}
		}
		else if (numericCategory.contains(property))
		{
			logString = logString + " (numeric category)";
			rootCauseDetailsLog.info(logString);

			Long startTime = System.nanoTime();

			// process for numeric values by genetic algorithm
			NumericAnalysis na = new NumericAnalysis(oracleFullPath, cloneTestPageFullPath, testScreenshotFullPath, originalDifferencePixels, element, property, element.getCssProperties().get(property), true,
					numericPositiveNegativeCategory.contains(property), rootCauseDetailsLog);
			Map<String, String> returnValue = na.runNumericAnalysis(isRCANumericAnalysisRateOfChange);

			Long endTime = System.nanoTime();
			rootCauseDetailsLog.info("Time = " + Util.convertNanosecondsToSeconds(endTime - startTime) + " sec");

			if (returnValue.containsKey(Constants.RCA_FIX_FOUND))
			{
				// problem found
				String ret[] = returnValue.get(Constants.RCA_FIX_FOUND).split("#");
				
				rootCauseFound = true;
				reducedNumberOfDifferencePixels = Integer.valueOf(ret[0]);
				rootCauseProperty = property;
				fixValue = ret[1];
				String fixString = " root cause found!";
				if(reducedNumberOfDifferencePixels == 0)
				{
					fixString = " => exact" + fixString;
				}
				else
				{
					fixString = " => acceptable" + fixString;
				}
				rootCauseResultsLog.info("Problem in " + element.getXpath() + " in attribute " + property + 
						". New value should be " + fixValue + fixString + 
						" Number of difference pixels reduced from " + originalDifferencePixels.size() + " to " + reducedNumberOfDifferencePixels);
			}
			else
			{
				// check if the returned number of difference pixels are less
				// than the one already present in
				// reducedNumberOfDifferencePixels
				String ret[] = returnValue.get(Constants.RCA_FIX_NOT_FOUND).split("#");

				// indicates value same as property value was applied
				if (Integer.parseInt(ret[0]) == -1)
				{
					ret[0] = numberOfDifferencePixels + "";
				}

				if (Integer.parseInt(ret[0]) <= reducedNumberOfDifferencePixels)
				{
					// candidate root cause
					reducedNumberOfDifferencePixels = Integer.parseInt(ret[0]);
					rootCauseProperty = property;
					fixValue = ret[1];
				}
			}
		}
		else if (predefinedCategory.contains(property))
		{
			logString = logString + " (predefined category)";
			rootCauseDetailsLog.info(logString);

			Long startTime = System.nanoTime();

			// process for predefined values by bruteforce
			PredefinedValuesAnalysis pva = new PredefinedValuesAnalysis(oracleFullPath, cloneTestPageFullPath, originalDifferencePixels, element, property, element.getCssProperties().get(property), true, rootCauseDetailsLog);
			Map<String, String> returnValue = pva.runPredefinedValues();

			Long endTime = System.nanoTime();
			rootCauseDetailsLog.info("Time = " + Util.convertNanosecondsToSeconds(endTime - startTime) + " sec");

			if (returnValue.containsKey(Constants.RCA_FIX_FOUND))
			{
				// problem found
				rootCauseFound = true;
				reducedNumberOfDifferencePixels = 0;
				rootCauseProperty = property;
				fixValue = returnValue.get(Constants.RCA_FIX_FOUND);
				rootCauseResultsLog.info("Problem in " + element.getXpath() + " in attribute " + property + 
						". New value should be " + returnValue.get(Constants.RCA_FIX_FOUND) + " => exact root cause found!" + 
						" Number of difference pixels reduced from " + originalDifferencePixels.size() + " to " + reducedNumberOfDifferencePixels);
			}
			else
			{
				// check if the returned number of difference pixels are less
				// than the one already present in
				// reducedNumberOfDifferencePixels
				String ret[] = returnValue.get(Constants.RCA_FIX_NOT_FOUND).split("#");

				// indicates value same as property value was applied
				if (Integer.parseInt(ret[0]) == -1)
				{
					ret[0] = numberOfDifferencePixels + "";
				}

				if (Integer.parseInt(ret[0]) <= reducedNumberOfDifferencePixels)
				{
					// candidate root cause
					reducedNumberOfDifferencePixels = Integer.parseInt(ret[0]);
					rootCauseProperty = property;
					fixValue = ret[1];
				}
			}
		}
		else if (predefinedNumericCategory.contains(property))
		{
			logString = logString + " (predefined+numeric category)";
			rootCauseDetailsLog.info(logString);

			Long startTime = System.nanoTime();

			// step 1: process for predefined values by bruteforce
			PredefinedValuesAnalysis pva = new PredefinedValuesAnalysis(oracleFullPath, cloneTestPageFullPath, originalDifferencePixels, element, property, element.getCssProperties().get(property), true, rootCauseDetailsLog);
			Map<String, String> returnValue = pva.runPredefinedValues();

			if (returnValue.containsKey(Constants.RCA_FIX_FOUND))
			{
				// problem found
				rootCauseFound = true;
				reducedNumberOfDifferencePixels = 0;
				rootCauseProperty = property;
				fixValue = returnValue.get(Constants.RCA_FIX_FOUND);
				rootCauseResultsLog.info("Problem in " + element.getXpath() + " in attribute " + property + 
						". New value should be " + returnValue.get(Constants.RCA_FIX_FOUND) + " => exact root cause found!" + 
						" Number of difference pixels reduced from " + originalDifferencePixels.size() + " to " + reducedNumberOfDifferencePixels);
			}
			else
			{
				// check if the returned number of difference pixels are less
				// than the one already present in
				// reducedNumberOfDifferencePixels
				String ret[] = returnValue.get(Constants.RCA_FIX_NOT_FOUND).split("#");

				// indicates value same as property value was applied
				if (Integer.parseInt(ret[0]) == -1)
				{
					ret[0] = numberOfDifferencePixels + "";
				}

				if (Integer.parseInt(ret[0]) <= reducedNumberOfDifferencePixels)
				{
					// candidate root cause
					reducedNumberOfDifferencePixels = Integer.parseInt(ret[0]);
					rootCauseProperty = property;
					fixValue = ret[1];
				}
			}

			// step 2: process for numeric values by genetic algorithm
			if(!rootCauseFound)
			{
				NumericAnalysis na = new NumericAnalysis(oracleFullPath, cloneTestPageFullPath, testScreenshotFullPath, originalDifferencePixels, element, property, element.getCssProperties().get(property), true,
						numericPositiveNegativeCategory.contains(property), rootCauseDetailsLog);
				returnValue = na.runNumericAnalysis(isRCANumericAnalysisRateOfChange);
				if (returnValue.containsKey(Constants.RCA_FIX_FOUND))
				{
					String ret[] = returnValue.get(Constants.RCA_FIX_FOUND).split("#");

					// problem found
					rootCauseFound = true;
					reducedNumberOfDifferencePixels = Integer.valueOf(ret[0]);
					rootCauseProperty = property;
					fixValue = ret[1];
					String fixString = " root cause found!";
					if(reducedNumberOfDifferencePixels == 0)
					{
						fixString = " => exact" + fixString;
					}
					else
					{
						fixString = " => acceptable" + fixString;
					}
					rootCauseResultsLog.info("Problem in " + element.getXpath() + " in attribute " + property + 
							". New value should be " + fixValue + fixString + 
							" Number of difference pixels reduced from " + originalDifferencePixels.size() + " to " + reducedNumberOfDifferencePixels);
				}
				else
				{
					// check if the returned number of difference pixels are less
					// than the one already present in
					// reducedNumberOfDifferencePixels
					String ret[] = returnValue.get(Constants.RCA_FIX_NOT_FOUND).split("#");
	
					// indicates value same as property value was applied
					if (Integer.parseInt(ret[0]) == -1)
					{
						ret[0] = numberOfDifferencePixels + "";
					}
	
					if (Integer.parseInt(ret[0]) <= reducedNumberOfDifferencePixels)
					{
						// candidate root cause
						reducedNumberOfDifferencePixels = Integer.parseInt(ret[0]);
						rootCauseProperty = property;
						fixValue = ret[1];
					}
				}
			}

			Long endTime = System.nanoTime();
			rootCauseDetailsLog.info("Time = " + Util.convertNanosecondsToSeconds(endTime - startTime) + " sec");
		}
		else if (predefinedStringCategory.contains(property))
		{
			logString = logString + " (predefined+string category)";
			rootCauseDetailsLog.info(logString);

			Long startTime = System.nanoTime();

			// process for predefined values by bruteforce
			PredefinedValuesAnalysis pva = new PredefinedValuesAnalysis(oracleFullPath, cloneTestPageFullPath, originalDifferencePixels, element, property, element.getCssProperties().get(property), true, rootCauseDetailsLog);
			Map<String, String> returnValue = pva.runPredefinedValues();

			Long endTime = System.nanoTime();
			rootCauseDetailsLog.info("Time = " + Util.convertNanosecondsToSeconds(endTime - startTime) + " sec");

			if (returnValue.containsKey(Constants.RCA_FIX_FOUND))
			{
				// problem found
				rootCauseFound = true;
				reducedNumberOfDifferencePixels = 0;
				rootCauseProperty = property;
				fixValue = returnValue.get(Constants.RCA_FIX_FOUND);
				rootCauseResultsLog.info("Problem in " + element.getXpath() + " in attribute " + property + 
						". New value should be " + returnValue.get(Constants.RCA_FIX_FOUND) + " => exact root cause found!" + 
						" Number of difference pixels reduced from " + originalDifferencePixels.size() + " to " + reducedNumberOfDifferencePixels);
			}
			else
			{
				// check if the returned number of difference pixels are less
				// than the one already present in reducedNumberOfDifferencePixels
				String ret[] = returnValue.get(Constants.RCA_FIX_NOT_FOUND).split("#");

				// indicates value same as property value was applied
				if (Integer.parseInt(ret[0]) == -1)
				{
					ret[0] = numberOfDifferencePixels + "";
				}
				if (Integer.parseInt(ret[0]) <= reducedNumberOfDifferencePixels)
				{
					// candidate root cause
					rootCauseProperty = property;
					fixValue = ret[1];
				}
			}
			rootCauseResultsLog.info("Potentially problem in " + element.getXpath() + " in property " + property + ". The exact value is not known because it is a string.");
		}
		
		Map<String, String> propValueMap;
		if(bestRootCause.get(reducedNumberOfDifferencePixels) != null)
		{
			propValueMap = bestRootCause.get(reducedNumberOfDifferencePixels);
		}
		else
		{
			propValueMap = new HashMap<String, String>();
		}
		propValueMap.put(rootCauseProperty, fixValue);
		bestRootCause.put(reducedNumberOfDifferencePixels, propValueMap);
	}

	private List<VisualProperty> getPropertiesListWithoutPrioritization()
	{
		Long startTime = System.nanoTime();
		List<VisualProperty> unprioritizedVisualPropertiesList = new ArrayList<VisualProperty>();
		
		for(String prop : element.getHtmlAttributes().keySet())
		{
			if (booleanCategory.contains(prop) || numericCategory.contains(prop) || predefinedCategory.contains(prop)){
				unprioritizedVisualPropertiesList.add(new VisualProperty("html", prop));
			}
		}
		for(String prop : element.getCssProperties().keySet())
		{
			if (colorCategory.contains(prop) || numericCategory.contains(prop) || predefinedCategory.contains(prop) || predefinedNumericCategory.contains(prop) || predefinedStringCategory.contains(prop)){
				unprioritizedVisualPropertiesList.add(new VisualProperty("css", prop));
			}
		}
		
		rootCauseDetailsLog.info("");
		rootCauseDetailsLog.info("Unprioritized visual properties: " + unprioritizedVisualPropertiesList);
		Long endTime = System.nanoTime();
		rootCauseDetailsLog.info("Prioritization Time = " + Util.convertNanosecondsToSeconds(endTime - startTime) + " sec");
		
		return unprioritizedVisualPropertiesList;
	}
	
	private List<VisualProperty> getPropertiesListWithPrioritization()
	{
		Long startTime = System.nanoTime();
		String modelfile = "src/test/resources/mix.all";
		Predict p = new Predict(modelfile);
		Extract e = new Extract(oracleFullPath, testPageFullPath, testScreenshotFullPath, element.getXpath());
		Feature f = e.extract();
		TreeMap<VisualProperty, Double> score = p.predict(f);
		List<VisualProperty> prioritizedVisualPropertiesList = p.scoreToRank(score, element);
		
		for(String prop : element.getHtmlAttributes().keySet())
		{
			VisualProperty vp = new VisualProperty("html", prop);
			if (!prioritizedVisualPropertiesList.contains(vp) && (booleanCategory.contains(prop) || numericCategory.contains(prop) || predefinedCategory.contains(prop))){
				prioritizedVisualPropertiesList.add(vp);
			}
		}
		for(String prop : element.getCssProperties().keySet())
		{
			VisualProperty vp = new VisualProperty("css", prop);
			if (!prioritizedVisualPropertiesList.contains(vp) && (colorCategory.contains(prop) || numericCategory.contains(prop) || predefinedCategory.contains(prop) || predefinedNumericCategory.contains(prop) || predefinedStringCategory.contains(prop))){
				prioritizedVisualPropertiesList.add(vp);
			}
		}
		
		rootCauseDetailsLog.info("");
		rootCauseDetailsLog.info("Prioritized visual properties: " + prioritizedVisualPropertiesList);
		Long endTime = System.nanoTime();
		rootCauseDetailsLog.info("Prioritization Time = " + Util.convertNanosecondsToSeconds(endTime - startTime) + " sec");
		
		return prioritizedVisualPropertiesList;
	}
	
	private void runInitPrint()
	{
		rootCauseResultsLog.info(element.getXpath());
		rootCauseDetailsLog.info(element.getXpath());
		rootCauseDetailsLog.info("");

		rootCauseDetailsLog.info("CSS properties: " + element.getCssProperties());
		rootCauseDetailsLog.info("HTML attributes: " + element.getHtmlAttributes());		
	}
	
	public void runRootCauseAnalysis() throws IOException, InvalidConfigurationException
	{
		if(!isPropertyPrioritizationOn)
		{
			runRootCauseAnalysisWithoutPrioritization();
			return;
		}
		
		runInitPrint();
		
		// get prioritized list of visual properties
		List<VisualProperty> prioritizedVisualPropertiesList = getPropertiesListWithPrioritization();
		run(prioritizedVisualPropertiesList);
	}
	
	public void runRootCauseAnalysisWithoutPrioritization() throws IOException, InvalidConfigurationException
	{
		runInitPrint();
		
		List<VisualProperty> unprioritizedVisualPropertiesList = getPropertiesListWithoutPrioritization();
		run(unprioritizedVisualPropertiesList);
	}
	
	private void run(List<VisualProperty> visualPropertiesList) throws IOException, InvalidConfigurationException
	{
		Long startTime = System.nanoTime();

		int count = 1;
		for (VisualProperty vp : visualPropertiesList)
		{
			if(Util.convertNanosecondsToSeconds(System.nanoTime() - startTime) > Constants.RCA_PER_ELEMENT_TIMEOUT_IN_MINS * 60)
			{
				rootCauseDetailsLog.info("");
				rootCauseDetailsLog.info("RCA element timed out. Remaining properties cannot be checked.");
				break;
			}
			
			String logString = (count++) + " of " + element.getCssProperties().size() + ". " + vp.getName();
			if (vp.getType().equalsIgnoreCase("css"))
			{
				logString = logString + " -> CSS";
				runCssAnalysis(vp.getName(), logString);
			}
			else
			{
				logString = logString + " -> HTML";
				runHtmlAnalysis(vp.getName(), logString);
			}
			if (rootCauseFound)
			{
				break;
			}
		}
		Long endTime = System.nanoTime();

		if (!rootCauseFound)
		{
			rootCauseResultsLog.info("The exact fix for this element was not found.");
			rootCauseResultsLog.info("The best root cause was found to be ");
			if(!bestRootCause.isEmpty())
			{
				Map<String, String> rootCausePropValueMap = bestRootCause.get(bestRootCause.firstKey());
				for(String prop : rootCausePropValueMap.keySet())
				{
					rootCauseResultsLog.info("    " + prop + " wih value " + rootCausePropValueMap.get(prop)); 
				}
			}
			else
			{
				rootCauseResultsLog.info("    none");
			}
			rootCauseResultsLog.info("Number of difference pixels reduced from " + numberOfDifferencePixels + " to " + reducedNumberOfDifferencePixels);
		}

		double totaltime = Util.convertNanosecondsToSeconds(endTime - startTime);
		rootCauseDetailsLog.info("");
		rootCauseDetailsLog.info("Total Time = " + totaltime + " sec");
		rootCauseResultsLog.info("");

		rootCauseResultsLog.info("Total Time = " + totaltime + " sec");

		rootCauseDetailsLog.info("-------------------------------------------------------------------------------------------------");
	}

	public static int fitnessFunction(String oracleFullPath, String modifiedTestWebPageFullPath, List<Point> originalDifferencePixels) throws IOException
	{
		if(!isFitnessFunctionNew)
		{
			return fitnessFunctionOld(oracleFullPath, modifiedTestWebPageFullPath);
		}
		
		// take modified test page screenshot
		String[] modifiedTestWebPagePathAndName = Util.getPathAndFileNameFromFullPath(modifiedTestWebPageFullPath);
		String screenshotName = Util.getFileNameAndExtension(modifiedTestWebPagePathAndName[1])[0] + Constants.SCREENSHOT_FILE_EXTENSION;
		String modifiedTestWebPageScreenshotFullPath = modifiedTestWebPagePathAndName[0] + File.separatorChar + screenshotName;
		Util.getScreenshot(modifiedTestWebPagePathAndName[1], modifiedTestWebPagePathAndName[0], screenshotName, null, false);
		
		int reducedNumberOfPixels = originalDifferencePixels.size();

		// first check: only for difference pixels, see if there is no
		// difference
		Mat oracleMat = Highgui.imread(oracleFullPath);
		Mat modifiedTestMat = Highgui.imread(modifiedTestWebPageScreenshotFullPath);
		for (Point pixel : originalDifferencePixels)
		{
			double[] oracleRGBPixelValue = oracleMat.get(pixel.y, pixel.x);
			double[] modifiedTestRGBPixelValue = modifiedTestMat.get(pixel.y, pixel.x);

			if (oracleRGBPixelValue == null && modifiedTestRGBPixelValue == null)
			{
				// handling for different sized images: both are null implies
				// that the difference pixel is no longer present
				reducedNumberOfPixels--;
			}
			else if (oracleRGBPixelValue != null && modifiedTestRGBPixelValue != null)
			{
				boolean isSame = true;
				for (int i = 0; i < oracleRGBPixelValue.length; i++)
				{
					if (oracleRGBPixelValue[i] != modifiedTestRGBPixelValue[i])
					{
						isSame = false;
						break;
					}
				}
				if (isSame)
				{
					reducedNumberOfPixels--;
					if (reducedNumberOfPixels == 0)
					{
						break;
					}
				}
			}
		}

		// second check: perform a full image comparison
/*		if (reducedNumberOfPixels == 0)
		{
			VisualInvariantsTool vit = new VisualInvariantsTool(oracleFullPath, modifiedTestWebPageFullPath);
			List<Point> newDifferencePixels = vit.detection(false);
			reducedNumberOfPixels = newDifferencePixels.size();
		}
*/
		return reducedNumberOfPixels;
	}

	private static int fitnessFunctionOld(String oracleFullPath, String modifiedTestWebPageFullPath) throws IOException
	{
		WebSeeTool vit = new WebSeeTool(oracleFullPath, modifiedTestWebPageFullPath);
		List<Point> newDifferencePixels = vit.detection(false);
		return newDifferencePixels.size();
	}
	
	public static void main(String[] args) throws IOException, InvalidConfigurationException, SAXException, XPathExpressionException
	{
		//String basePath = "C:\\USC\\Research\\WebSee\\evaluation\\test";
		// String basePath = "C:\\USC\\visual_checking\\evaluation\\richa";
		String basePath = "C:\\USC\\visual_checking\\evaluation\\test";

		Document doc = Jsoup.parse(new File(basePath + File.separatorChar + "oracle.html"), null);
		doc.outputSettings().prettyPrint(false);
		String html1 = doc.html();
		PrintWriter out = new PrintWriter(new File(basePath + File.separatorChar + "oracle.html"));		
		out.print(html1);
		out.close();
		doc = Jsoup.parse(new File(basePath + File.separatorChar + "oracle.html"), null);
		doc.outputSettings().prettyPrint(false);
		html1 = doc.html();
		out = new PrintWriter(new File(basePath + File.separatorChar + "oracle.html"));		
		out.print(html1);
		out.close();

		doc = Jsoup.parse(new File(basePath + File.separatorChar + "test.html"), null);
		doc.outputSettings().prettyPrint(false);
		html1 = doc.html();
		out = new PrintWriter(new File(basePath + File.separatorChar + "test.html"));
		out.print(html1);
		out.close();
		doc = Jsoup.parse(new File(basePath + File.separatorChar + "test.html"), null);
		doc.outputSettings().prettyPrint(false);
		html1 = doc.html();
		out = new PrintWriter(new File(basePath + File.separatorChar + "test.html"));
		out.print(html1);
		out.close();
		
		String oracleFullPath = basePath + File.separatorChar + "oracle.png";
		String testPageFullPath = basePath + File.separatorChar + "test.html";
		String differencePixelsFileFullPath = basePath + File.separatorChar + "filtered_diff_oracle_test.txt";

		Util.getScreenshot("oracle.html", basePath, "oracle.png", null, true);
		/*
		 * Color c = Color.decode("#000000");
		 * System.out.println("#000000: rgb = (" + c.getRed() + ", " +
		 * c.getGreen() + ", " + c.getBlue() + ")"); double Y =
		 * 0.2126*c.getRed() + 0.7152*c.getGreen() + 0.0722*c.getBlue();
		 * System.out.println(Y < 128 ? "closer to black" : "closer to white");
		 */

		// prepare element
		HtmlElement element = new HtmlElement();
		// String xpath =
		// "/html[1]/body[1]/div[1]/div[2]/div[2]/div[2]/div[1]/h4[1]";
		// String xpath = "/html[1]/body[1]/div[2]/div[2]";
		// String xpath = ".//*[@id='e86']";
		String xpath = "/html[1]/body[1]/div[1]/div[2]/div[2]/form[1]/input[12]";

		WebDriverSingleton instance = WebDriverSingleton.getInstance();
		instance.loadPage(testPageFullPath);
		WebDriver d = instance.getDriver();
		WebElement el = d.findElement(By.xpath(xpath));
		element.setX(el.getLocation().x);
		element.setY(el.getLocation().y);
		element.setWidth(el.getSize().width);
		element.setHeight(el.getSize().height);

		xpath = Util.getElementXPath((JavascriptExecutor) d, el);
		element.setXpath(xpath);

		Map<String, String> html = new HashMap<String, String>();

		// --- METHOD 1 ---
		HtmlAttributesParser hap = new HtmlAttributesParser(testPageFullPath);
		//html = hap.getHTMLAttributesForElement(xpath);

		// --- METHOD 2 ---
		element.setHtmlAttributes(html);

		Map<String, String> css = new HashMap<String, String>();

		// --- METHOD 1 ---
		/*CSSParser cssParser = new CSSParser(testPageFullPath);
		cssParser.parseCSS();
		css = cssParser.getCSSPropertiesForElement(xpath);*/

		// --- METHOD 2 ---
		/*
		 * css.put("border-style", "none"); css.put("border-width", "0.0px");
		 * css.put("font-size", "15.0px"); css.put("font-weight", "normal");
		 * css.put("line-height", "22.0px"); css.put("margin-bottom", "0.46em");
		 * css.put("margin-left", "0.0px"); css.put("margin-right", "0.0px");
		 * css.put("margin-top", "15.0px"); css.put("width", "270.0px");
		 * css.put("direction", "rtl"); css.put("font-size", "16px");
		 * css.put("background-image",
		 * "url('file://C:/Dev/VisualInvariants/evaluationframework/test_image.gif')"
		 * ); css.put("padding", "10.0px");
		 */

		
		css.put("width", "350px");
		element.setCssProperties(css);

		System.out.println(element);

		Logger resultsLog = Util.getNewLogger(basePath + File.separatorChar + "RCA_results.txt", "RCA_results");
		Logger detailsLog = Util.getNewLogger(basePath + File.separatorChar + "RCA_details.txt", "RCA_details");

		RootCauseAnalysis rca = new RootCauseAnalysis(element, oracleFullPath, testPageFullPath, resultsLog, detailsLog, false);
		//rca.runRootCauseAnalysisWithoutPrioritization();
		rca.setConfig(true, true, true, false, "");
		rca.runRootCauseAnalysis();

		WebDriverSingleton.closeDriver();
	}
}
