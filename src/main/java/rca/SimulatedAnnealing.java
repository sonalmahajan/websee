package rca;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.Point;

import util.Util;
import websee.HtmlElement;
import config.Constants;
import exceptions.NoMoreValuesToTryException;

public class SimulatedAnnealing 
{
	private Logger log;

	private HtmlElement element;
	private String visualProperty; // CSS -> style:<property_name>:<unit>, HTML attribute -> <attribute_name>
	private String[] testPagePathAndName;
	private String newSADir;
	private String oracleFullPath;
	private int visualPropertyValueWithoutUnits;
	private List<Point> originalDifferencePixels;
	
	private Map<String, Integer> cloneDPMap;
	
	private long startTimeProperty;
	
	public SimulatedAnnealing(HtmlElement element, String visualProperty, String oracleFullPath, String testFileFullPath, List<Point> originalDifferencePixels, double visualPropertyValueWithoutUnits, long startTimeProperty, Logger log)
	{
		this.element = element;
		this.visualProperty = visualProperty;
		this.oracleFullPath = oracleFullPath;
		this.originalDifferencePixels = originalDifferencePixels;
		this.visualPropertyValueWithoutUnits = (int)visualPropertyValueWithoutUnits;
		this.log = log;

		// create new directory
		this.testPagePathAndName = Util.getPathAndFileNameFromFullPath(testFileFullPath);
		this.newSADir = testPagePathAndName[0] + File.separatorChar + "SA";
		File gaDir = new File(newSADir);
		gaDir.mkdir();

		cloneDPMap = new HashMap<String, Integer>();
		
		this.startTimeProperty = startTimeProperty;
	}
	
	/**
	 * Goal of this simulated annealing is to minimize E
	 * @param low: lower end of the range of values
	 * @param high: higher end of the range of values
	 * @param T: starting temperature
	 * @return
	 * @throws IOException
	 */
	public Map<String, String> runSimulatedAnnealing(int low, int high, int T, boolean isRCANumericAnalysisRateOfChange) throws IOException
	{
		if(isRCANumericAnalysisRateOfChange)
		{
			return runSimulatedAnnealingWithRateOfChange(low, high, T);
		}
		
		Map<String, String>  returnMap = new HashMap<String, String>();
		boolean terminate = false;
		int fixValue = 0;
		boolean fixFound = false;
		
		log.info("    Starting temperature = " + T + ", state values range = [" + low + ", " + high + "]");
		
		Set<Integer> visited = new	 HashSet<Integer>();
		visited.add(visualPropertyValueWithoutUnits);

		int current = visualPropertyValueWithoutUnits;
		int currentEnergyValue;
		int next;
		int nextEnergyValue;
		int bestSolution = Integer.MAX_VALUE;
		int bestEnergyValue = Integer.MAX_VALUE;
		int diffPixels = 0;
		try
		{
			current = getRandomNext(low, high, visited);
			visited.add(current);
			
			while(T > 0 && !terminate)
			{
				if(Util.convertNanosecondsToSeconds(System.nanoTime() - startTimeProperty) > Constants.RCA_PER_PROPERTY_TIMEOUT_IN_MINS * 60)
				{
					log.info("");
					log.info("Analysis for property timed out");
					returnMap.put(Constants.RCA_FIX_NOT_FOUND, bestEnergyValue + "#" + bestSolution + (visualProperty.contains("style") ? "px" : ""));
					return returnMap;
				}
				currentEnergyValue = getEnergyValue(current);
				log.info("        T = " + T + " current (state, energy) = (" + current + ", " + currentEnergyValue + ")");
				
				next = getRandomNext(low, high, visited);
				visited.add(next);
				
				nextEnergyValue = getEnergyValue(next);
				log.info("               next (state, energy) = (" + next + ", " + nextEnergyValue + ")");
				int deltaE = nextEnergyValue - currentEnergyValue;
				if(deltaE < 0 || Math.random() <= Math.exp(-deltaE / T))
				{
					current = next;
					currentEnergyValue = nextEnergyValue;
				}
				if(currentEnergyValue < bestEnergyValue)
				{
					bestEnergyValue = currentEnergyValue;
					bestSolution = current;
				}
				T--;
				if(T == 0 || Util.isCurrentInAcceptableReductionThreshold(bestEnergyValue, originalDifferencePixels.size()))
				{
					//if(bestEnergyValue == 0)
					if(Util.isCurrentInAcceptableReductionThreshold(bestEnergyValue, originalDifferencePixels.size()))
					{
						fixFound = true;
					}
					terminate = true;
				}
			}
		}
		catch(NoMoreValuesToTryException e)
		{
			terminate = true;
		}
		
		if(bestSolution != Integer.MAX_VALUE)
		{
			fixValue = bestSolution;
		}
		else
		{
			fixValue = current;
		}
		
		String isFixFound = "root cause found!";
		if(bestEnergyValue == 0)
		{
			isFixFound = " => exact " + isFixFound;
		}
		else if(Util.isCurrentInAcceptableReductionThreshold(bestEnergyValue, originalDifferencePixels.size()))
		{
			isFixFound = " => acceptable " + isFixFound;
		}
		else
		{
			isFixFound = "";
		}
		log.info("        value = " + fixValue + (visualProperty.contains("style") ? "px" : "") + isFixFound);
		
		if (fixFound)
		{
			// problem solved
			returnMap.put(Constants.RCA_FIX_FOUND, bestEnergyValue + "#" + fixValue + (visualProperty.contains("style") ? "px" : ""));
			return returnMap;
		}
		String[] newName = Util.getFileNameAndExtension(testPagePathAndName[1], "_clone_" + fixValue);
		String cloneFileName = newName[0] + newName[1];
		diffPixels = cloneDPMap.get(cloneFileName);
		returnMap.put(Constants.RCA_FIX_NOT_FOUND, diffPixels + "#" + fixValue + (visualProperty.contains("style") ? "px" : ""));
		return returnMap;
	}
	
	private Map<String, String> runSimulatedAnnealingWithRateOfChange(int low, int high, int T) throws IOException
	{
		Map<String, String>  returnMap = new HashMap<String, String>();
		boolean terminate = false;
		int fixValue = 0;
		boolean fixFound = false;
		int constantRateOfChangeCount = Integer.MIN_VALUE;
		int previousEnergyValue = 0;
		
		log.info("    Starting temperature = " + T + ", state values range = [" + low + ", " + high + "], difference pixels size = " + originalDifferencePixels.size());
		
		Set<Integer> visited = new HashSet<Integer>();
		visited.add(visualPropertyValueWithoutUnits);

		int current = visualPropertyValueWithoutUnits;
		int currentEnergyValue;
		int next;
		int nextEnergyValue;
		int bestSolution = Integer.MAX_VALUE;
		int bestEnergyValue = Integer.MAX_VALUE;
		int diffPixels = 0;
		try
		{
			current = getRandomNext(low, high, visited);
			visited.add(current);
			
			while(T > 0 && !terminate)
			{
				currentEnergyValue = getEnergyValue(current);
				log.info("        T = " + T + " current (state, energy) = (" + current + ", " + currentEnergyValue + ")");
				
				next = getRandomNext(low, high, visited);
				visited.add(next);
				
				nextEnergyValue = getEnergyValue(next);
				log.info("               next (state, energy) = (" + next + ", " + nextEnergyValue + ")");
				int deltaE = nextEnergyValue - currentEnergyValue;
				
				if(deltaE < 0 || Math.random() <= Math.exp(-deltaE / T))
				{
					current = next;
					currentEnergyValue = nextEnergyValue;
				}
				if(currentEnergyValue < bestEnergyValue)
				{
					bestEnergyValue = currentEnergyValue;
					bestSolution = current;
				}
				
				if(previousEnergyValue == currentEnergyValue)
				{
					constantRateOfChangeCount++;
				}
				else
				{
					previousEnergyValue = currentEnergyValue;
					constantRateOfChangeCount = 0;
				}
				
				T--;
				if(T == 0 && constantRateOfChangeCount < Constants.RCA_NUMERIC_ANALYSIS_RATE_OF_CHANGE_WINDOW_SIZE)
				{
					// reset temperature to allow more iterations
					T = Constants.SIMULATED_ANNEALING_STARTING_TEMPERATURE;
					terminate = false;
				}
				else if(constantRateOfChangeCount > Constants.RCA_NUMERIC_ANALYSIS_RATE_OF_CHANGE_WINDOW_SIZE)
				{
					// rate of change is constant, indicating that there is no convergence and this cannot be the correct faulty property
					log.info("Constant rate of change. Hence, terminating.");
					break;
				}
				if(T == 0 || Util.isCurrentInAcceptableReductionThreshold(bestEnergyValue, originalDifferencePixels.size()))
				{
					//if(bestEnergyValue == 0)
					if(Util.isCurrentInAcceptableReductionThreshold(bestEnergyValue, originalDifferencePixels.size()))
					{
						fixFound = true;
					}
					terminate = true;
				}
			}
		}
		catch(NoMoreValuesToTryException e)
		{
			terminate = true;
		}
		
		if(bestSolution != Integer.MAX_VALUE)
		{
			fixValue = bestSolution;
		}
		else
		{
			fixValue = current;
		}
		
		String isFixFound = "root cause found!";
		if(bestEnergyValue == 0)
		{
			isFixFound = " => exact " + isFixFound;
		}
		else if(Util.isCurrentInAcceptableReductionThreshold(bestEnergyValue, originalDifferencePixels.size()))
		{
			isFixFound = " => acceptable " + isFixFound;
		}
		else
		{
			isFixFound = "";
		}
		log.info("        value = " + fixValue + (visualProperty.contains("style") ? "px" : "") + isFixFound);
		
		if (fixFound)
		{
			// problem solved
			returnMap.put(Constants.RCA_FIX_FOUND, bestEnergyValue + "#" + fixValue + (visualProperty.contains("style") ? "px" : ""));
			return returnMap;
		}
		String[] newName = Util.getFileNameAndExtension(testPagePathAndName[1], "_clone_" + fixValue);
		String cloneFileName = newName[0] + newName[1];
		diffPixels = cloneDPMap.get(cloneFileName);
		returnMap.put(Constants.RCA_FIX_NOT_FOUND, diffPixels + "#" + fixValue + (visualProperty.contains("style") ? "px" : ""));
		return returnMap;
	}	
	
	private int getRandomNext(int min, int max, Set<Integer> visited) throws NoMoreValuesToTryException
	{
		if((max - min + 1) == visited.size())
		{
			// no more values left to try
			throw new NoMoreValuesToTryException("All values in the given range [" + min + ", " + max + "] are tried");
		}
		
		int next;
		while(visited.contains(next = Util.getRandomNumber(min, max)))
		{}
		return next;
	}
	
	private int getEnergyValue(int stateValue) throws IOException
	{
		String cloneFileName;
		String cloneFile;

		// create test page clone
		String[] newName = Util.getFileNameAndExtension(testPagePathAndName[1], "_clone_" + stateValue);
		cloneFileName = newName[0] + newName[1];
		cloneFile = newSADir + File.separatorChar + cloneFileName;

		FileUtils.copyFile(new File(testPagePathAndName[0] + File.separatorChar + testPagePathAndName[1]), new File(cloneFile));
		Document document = Jsoup.parse(new File(cloneFile), null);
		Element e = Util.getElementFromXPathJava(element.getXpath(), document);

		// check if CSS property or HTML attribute to modify
		if (visualProperty.contains("style"))
		{
			// CSS property (style:<property_name>:<unit>)
			String css[] = visualProperty.split(":");
			String style = e.attr("style");
			style = style + "; " + css[1] + ":" + stateValue + css[2]; 
			e.attr(css[0], style);
		}
		else
		{
			// HTML attribute
			e.attr(visualProperty, "" + stateValue);
		}

		document.outputSettings().prettyPrint(false);
		//document.outputSettings().escapeMode(Entities.EscapeMode.extended);
		String html = document.html();		
		PrintWriter out = new PrintWriter(new File(cloneFile));
		out.print(html);
		out.close();

		// run detection to see if the problem is resolved
		int diffPixels = RootCauseAnalysis.fitnessFunction(oracleFullPath, cloneFile, originalDifferencePixels);
		cloneDPMap.put(cloneFileName, diffPixels);
		return diffPixels;
	}
	
	public Map<String, String> runSimulatedAnnealingForColor(int low, int high, int T)
	{
		Map<String, String>  returnMap = new HashMap<String, String>();
		boolean terminate = false;
		int fixValue = 0;
		boolean fixFound = false;
		
		log.info("    Starting temperature = " + T + ", state values range = [" + low + ", " + high + "]");
		
		Set<Integer> visited = new HashSet<Integer>();
		visited.add(visualPropertyValueWithoutUnits);

		int current = visualPropertyValueWithoutUnits;
		int currentEnergyValue;
		int next;
		int nextEnergyValue;
		int bestSolution = Integer.MAX_VALUE;
		int bestEnergyValue = Integer.MAX_VALUE;
		int diffPixels = 0;
		
		String oracleAverageColor = "#FFFFFF";
		try
		{
			oracleAverageColor = computeAverageColor(oracleFullPath);
		}
		catch (NumberFormatException e)
		{
			e.printStackTrace();
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
		}
		
		try
		{
			current = getRandomNext(low, high, visited);
			visited.add(current);
			
			while(T > 0 && !terminate)
			{
				if(Util.convertNanosecondsToSeconds(System.nanoTime() - startTimeProperty) > Constants.RCA_PER_PROPERTY_TIMEOUT_IN_MINS * 60)
				{
					log.info("");
					log.info("Analysis for property timed out");
					returnMap.put(Constants.RCA_FIX_NOT_FOUND, bestEnergyValue + "#" + bestSolution + (visualProperty.contains("style") ? "px" : ""));
					return returnMap;
				}
				currentEnergyValue = (int) getEnergyByColorDistance(current, oracleAverageColor);
				log.info("        T = " + T + " current (state, energy) = (" + current + ", " + currentEnergyValue + ")");
				
				next = getRandomNext(low, high, visited);
				visited.add(next);
				
				nextEnergyValue = (int) getEnergyByColorDistance(next, oracleAverageColor);
				log.info("               next (state, energy) = (" + next + ", " + nextEnergyValue + ")");
				int deltaE = nextEnergyValue - currentEnergyValue;
				if(deltaE < 0 || Math.random() <= Math.exp(-deltaE / T))
				{
					current = next;
					currentEnergyValue = nextEnergyValue;
				}
				if(currentEnergyValue < bestEnergyValue)
				{
					bestEnergyValue = currentEnergyValue;
					bestSolution = current;
				}
				T--;
				if(T == 0)
				{
					if(bestEnergyValue == 0)
					{
						fixFound = true;
					}
					terminate = true;
				}
			}
		}
		catch(NoMoreValuesToTryException e)
		{
			terminate = true;
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		if(bestSolution != Integer.MAX_VALUE)
		{
			fixValue = bestSolution;
		}
		else
		{
			fixValue = current;
		}
		
		String isFixFound = (fixFound) ? " => exact root cause found!" : "";
		log.info("        value = " + fixValue + (visualProperty.contains("style") ? "px" : "") + isFixFound);
		
		if (fixFound)
		{
			// problem solved
			returnMap.put(Constants.RCA_FIX_FOUND, fixValue + (visualProperty.contains("style") ? "px" : ""));
			return returnMap;
		}
		String[] newName = Util.getFileNameAndExtension(testPagePathAndName[1], "_clone_" + fixValue);
		String cloneFileName = newName[0] + newName[1];
		diffPixels = cloneDPMap.get(cloneFileName);
		returnMap.put(Constants.RCA_FIX_NOT_FOUND, diffPixels + "#" + fixValue + (visualProperty.contains("style") ? "px" : ""));
		return returnMap;

	}
	
	public String computeAverageColor(String imageWithFullPath) throws NumberFormatException, IOException
	{
		SortedSet<Map.Entry<String, Integer>> sortedColorset = getAllColorsInElement(imageWithFullPath);

		// get average color
		int totalDecColors = 0;
		for (Map.Entry<String, Integer> entry : sortedColorset)
		{
			totalDecColors = totalDecColors + Util.getDecimalFromHex(entry.getKey());
		}
		int averageColor = totalDecColors/sortedColorset.size();
		String averageColorHex = Util.getHexFromDecimal(averageColor);
		//String averageColorHex = sortedColorset.first().getKey();
		
		return averageColorHex;
	}

	private double getColorDistance(Color c1, Color c2)
	{
	    double rmean = ( c1.getRed() + c2.getRed() )/2;
	    int r = c1.getRed() - c2.getRed();
	    int g = c1.getGreen() - c2.getGreen();
	    int b = c1.getBlue() - c2.getBlue();
	    double weightR = 2 + rmean/256;
	    double weightG = 4.0;
	    double weightB = 2 + (255-rmean)/256;
	    return Math.sqrt(weightR*r*r + weightG*g*g + weightB*b*b);
	} 
	
	private SortedSet<Map.Entry<String, Integer>> getAllColorsInElement(String imageWithFullPath) throws NumberFormatException, IOException
	{
		// get colors for all difference pixels
		Map<String, Integer> colors = new HashMap<String, Integer>();
		for (Point p : originalDifferencePixels)
		{
			String c = getPixelColorInHex(imageWithFullPath, p.x, p.y);
			Integer cnt = 0;
			if (colors.get(c) != null)
			{
				cnt = colors.get(c);
			}
			colors.put(c, ++cnt);
		}

		// sort the colors map in descending order of frequency count
		SortedSet<Map.Entry<String, Integer>> sortedset = new TreeSet<Map.Entry<String, Integer>>(new Comparator<Map.Entry<String, Integer>>()
		{
			public int compare(Map.Entry<String, Integer> e1, Map.Entry<String, Integer> e2)
			{
				return e2.getValue().compareTo(e1.getValue());
			}
		});
		sortedset.addAll(colors.entrySet());

		return sortedset;
	}

	private String getPixelColorInHex(String imagePath, int x, int y) throws IOException
	{
		File file = new File(imagePath);
		BufferedImage image = ImageIO.read(file);
		
		int clr = image.getRGB(x, y);
		int red = (clr >> 16) & 0xff;
		int green = (clr >> 8) & 0xff;
		int blue = (clr) & 0xff;
		
		String hex = String.format("#%02x%02x%02x", red, green, blue);
		return hex;
	}

	private double getEnergyByColorDistance(int colorValue, String oracleAverageColor) throws IOException
	{
		String cloneFileName;
		String cloneFile;

		// create test page clone
		String[] newName = Util.getFileNameAndExtension(testPagePathAndName[1], "_clone_" + Util.getHexFromDecimal(colorValue).replace("#", ""));
		cloneFileName = newName[0] + newName[1];
		cloneFile = newSADir + File.separatorChar + cloneFileName;

		FileUtils.copyFile(new File(testPagePathAndName[0] + File.separatorChar + testPagePathAndName[1]), new File(cloneFile));
		Document document = Jsoup.parse(new File(cloneFile), null);
		Element e = Util.getElementFromXPathJava(element.getXpath(), document);

		// check if CSS property or HTML attribute to modify
		if (visualProperty.contains("style"))
		{
			// CSS property (style:<property_name>:<unit>)
			String css[] = visualProperty.split(":");
			String style = e.attr("style");
			style = style + "; " + css[1] + ":" + Util.getHexFromDecimal(colorValue); 
			e.attr(css[0], style);
		}
		else
		{
			// HTML attribute
			e.attr(visualProperty, "" + colorValue);
		}

		document.outputSettings().prettyPrint(false);
		//document.outputSettings().escapeMode(Entities.EscapeMode.extended);
		String html = document.html();		
		PrintWriter out = new PrintWriter(new File(cloneFile));
		out.print(html);
		out.close();

		// take screenshot of test page
		Util.getScreenshot(cloneFileName, newSADir, newName[0] + Constants.SCREENSHOT_FILE_EXTENSION, null, true);
		
		// compute average color from test page
		String testAverageColor = computeAverageColor(newSADir + File.separatorChar + newName[0] + Constants.SCREENSHOT_FILE_EXTENSION);
		double distance = getColorDistance(Color.decode(oracleAverageColor), Color.decode(testAverageColor));
		
		// run detection to check if the two images are same
		int diffPixels = (int) (distance + RootCauseAnalysis.fitnessFunction(oracleFullPath, cloneFile, originalDifferencePixels));
		cloneDPMap.put(cloneFileName, diffPixels);
		return diffPixels;		
	}
}
