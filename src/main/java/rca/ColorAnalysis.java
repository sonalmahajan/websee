package rca;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.imageio.ImageIO;
import javax.xml.xpath.XPathExpressionException;

import org.apache.log4j.Logger;
import org.jgap.InvalidConfigurationException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.By;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.xml.sax.SAXException;

import util.Util;
import websee.CSSParser;
import websee.HtmlElement;
import websee.WebDriverSingleton;
import config.Constants;

public class ColorAnalysis
{
	private Logger log;
	
	private String oracleFullPath;
	private String testFileFullPath;
	private List<Point> originalDifferencePixels;
	private HtmlElement element;
	private String visualProperty;
	private String visualPropertyValue;
	
	private long startTimeProperty;

	private BufferedImage oracleImage;
	
	public ColorAnalysis(String oracleFullPath, String testFileFullPath, List<Point> originalDifferencePixels, HtmlElement element, String visualProperty, String visualPropertyValue, Logger log)
	{
		this.oracleFullPath = oracleFullPath;
		this.testFileFullPath = testFileFullPath;
		this.originalDifferencePixels = originalDifferencePixels;
		this.element = element;
		this.visualProperty = visualProperty;
		this.visualPropertyValue = visualPropertyValue;
		
		this.log = log;
		
		this.startTimeProperty = System.nanoTime();
		
		try 
		{
			this.oracleImage = getBufferedImage(this.oracleFullPath);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	public Map<String, String> runColorAnalysisByColorDistance() throws IOException, InvalidConfigurationException
	{
		if(!RootCauseAnalysis.isSearchSpaceSetByHeuristic())
		{
			int low = Util.getDecimalFromHex("#000000");
			int high = Util.getDecimalFromHex("#FFFFFF");

			/*String expectedValue = RootCauseAnalysis.getExpectedValue();
			int decVal = Util.getDecimalFromHex(expectedValue);
			if(decVal == -1)
			{
				expectedValue = "#000000";
				decVal = Util.getDecimalFromHex(expectedValue);
			}			
			int low = decVal - Constants.RCA_SBST_SEARCH_SPACE_SIZE;
			if(low < Util.getDecimalFromHex("#000000"))
			{
				low = Util.getDecimalFromHex("#000000");
			}
			int high = decVal + Constants.RCA_SBST_SEARCH_SPACE_SIZE;
			if(high > Util.getDecimalFromHex("#FFFFFF"))
			{
				high = Util.getDecimalFromHex("#FFFFFF");
			}*/
			
			if(RootCauseAnalysis.isSimmulatedAnnealingToBeUsed())
			{
				SimulatedAnnealing sa = new SimulatedAnnealing(element, "style:" + visualProperty + ":", oracleFullPath, testFileFullPath, originalDifferencePixels, Util.getDecimalNumberFromString(visualPropertyValue), startTimeProperty, log);
				return sa.runSimulatedAnnealingForColor(low, high, Constants.SIMULATED_ANNEALING_STARTING_TEMPERATURE);
			}
			GAFitnessFunction ga = new GAFitnessFunction(element, visualProperty, oracleFullPath, testFileFullPath, originalDifferencePixels, Util.getDecimalNumberFromString(visualPropertyValue), log);
			ga.setOracleColor(ga.computeAverageColor(oracleFullPath));
			
			return ga.runGAForColorAnalysis(low, high);
		}
		
		// get all colors in the element
		SortedSet<Map.Entry<String, Integer>> sortedColorset = getAllColorsInElement();

		// method 2: run color distance
		return runByColorDistance(sortedColorset);
	}

	private Map<String, String> runByColorDistance(SortedSet<Map.Entry<String, Integer>> sortedColorset) throws InvalidConfigurationException, IOException
	{
		Map<String, String>  returnMap = new HashMap<String, String>();

		log.info("    Color set = " + sortedColorset);
		
		if(sortedColorset.isEmpty())
		{
			returnMap.put(Constants.RCA_FIX_NOT_FOUND, originalDifferencePixels.size() + "#" + visualPropertyValue);
			return returnMap;
		}
		
		int ret = 0;
		if(Util.convertNanosecondsToSeconds(System.nanoTime() - startTimeProperty) > Constants.RCA_PER_PROPERTY_TIMEOUT_IN_MINS * 60)
		{
			log.info("");
			log.info("Analysis for property timed out");
			returnMap.put(Constants.RCA_FIX_NOT_FOUND, ret + "#" + sortedColorset.first().getKey());
			return returnMap;
		}
		
		if(sortedColorset.size() == 1)
		{
			ret = runDetection(sortedColorset.first().getKey());
			if(ret == 0)
			{
				returnMap.put(Constants.RCA_FIX_FOUND, sortedColorset.first().getKey());
			}
			else
			{
				returnMap.put(Constants.RCA_FIX_NOT_FOUND, ret + "#" + sortedColorset.first().getKey());
			}
			return returnMap;
		}
		
		Document doc = Jsoup.parse(new File(testFileFullPath), null);
		Element e = Util.getElementFromXPathJava(element.getXpath(), doc);
		String referenceCSSProperty = "background-color";
		String referenceColor = "#FFFFFF";
		
		CSSParser cssParser = new CSSParser(testFileFullPath);
		try
		{
			cssParser.parseCSSWithInheritance();
			String xpath = element.getXpath();
			Element curr = e;

			boolean bgColorFound = false;
			if(visualProperty.equalsIgnoreCase("background-color"))
			{
				if(e.hasText())
				{
					log.info("    text color (foreground) used as reference color");
					referenceCSSProperty = "color";
					referenceColor = "#000000";
				}
				else if(e.children().size() > 0)
				{
					log.info("    child color (foreground) used as reference color");
					
					WebDriverSingleton instance = WebDriverSingleton.getInstance();
					instance.loadPage(testFileFullPath);
					WebDriver driver = instance.getDriver();
					
					for(Element child : e.children())
					{
						if(bgColorFound)
						{
							break;
						}
						
						WebElement we = driver.findElement(By.xpath(Util.getXPathOfElementJava(child)));
						
						// check if the child is visible
						if(we.getSize().width > 0 && we.getSize().height > 0)
						{
							int lx = we.getLocation().x+1;
							int ty = we.getLocation().y+1;
							int rx = we.getLocation().x + we.getSize().width;
							int by = we.getLocation().y + we.getSize().height;
							//driver.quit();
							
							Map<String, Integer> colors = new HashMap<String, Integer>();
							for (int i = lx; i < rx; i++)
							{
								for (int j = ty; j < by; j++)
								{
									String c = getPixelColorInHex(oracleImage, i, j);
									Integer cnt = 0;
									if (colors.get(c) != null)
									{
										cnt = colors.get(c);
									}
									colors.put(c, ++cnt);
								}
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
							referenceColor = sortedset.first().getKey();
							bgColorFound = true;
						}
					}
					if(!bgColorFound)
					{
						//driver.quit();
					}
				}
				else if(e.parent() == null || curr.parent().tagName().equalsIgnoreCase("#root"))
				{
					bgColorFound = true;
				}
				else
				{
					log.info("    parent background color used as reference color");
					curr = e.parent();
					xpath = Util.getXPathOfElementJava(curr);
				}
			}
			
			// find background color or text color by traversing up the DOM tree if not mentioned for the required element
			while(!bgColorFound)
			{
				Map<String, String> css = cssParser.getCSSPropertiesForElement(xpath);
				if (css.containsKey(referenceCSSProperty) && !css.get(referenceCSSProperty).equalsIgnoreCase("transparent"))
				{
					referenceColor = css.get(referenceCSSProperty);
					bgColorFound = true;
				}
				else if(curr.parent() == null || curr.parent().tagName().equalsIgnoreCase("#root"))
				{
					bgColorFound = true;
				}
				else
				{
					curr = curr.parent();
					xpath = Util.getXPathOfElementJava(curr);
				}
			}
		}
		catch (SAXException e1)
		{
			e1.printStackTrace();
		}
		catch (XPathExpressionException e1)
		{
			e1.printStackTrace();
		}
		
		Map<String, Double> distanceMap = new HashMap<String, Double>();
		for (Map.Entry<String, Integer> entry : sortedColorset)
		{
			double dist = getColorDistance(Color.decode(entry.getKey()), Color.decode(referenceColor));
			distanceMap.put(entry.getKey(), dist);
		}
		
		List<Entry<String, Double>> sortedMap = entriesSortedByValues(distanceMap);
		log.info("    Sorted color set = " + sortedMap);
		for (Entry<String, Double> entry : sortedMap)
		{
			if(Util.convertNanosecondsToSeconds(System.nanoTime() - startTimeProperty) > Constants.RCA_PER_PROPERTY_TIMEOUT_IN_MINS * 60)
			{
				log.info("");
				log.info("Analysis for property timed out");
				returnMap.put(Constants.RCA_FIX_NOT_FOUND,  ret + "#" + sortedMap.get(0).getKey());
				return returnMap;
			}

			String originalColor = entry.getKey();
			if (!visualPropertyValue.equalsIgnoreCase(originalColor))
			{
				ret = runDetection(originalColor);
				if(ret == 0)
				{
					returnMap.put(Constants.RCA_FIX_FOUND, originalColor);
					return returnMap;
				}
			}
		}
		returnMap.put(Constants.RCA_FIX_NOT_FOUND, ret + "#" + sortedMap.get(0).getKey());
		return returnMap;
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
	
	private <K, V extends Comparable<? super V>> List<Entry<K, V>> entriesSortedByValues(Map<K, V> map)
	{

		List<Entry<K, V>> sortedEntries = new ArrayList<Entry<K, V>>(map.entrySet());

		Collections.sort(sortedEntries, new Comparator<Entry<K, V>>()
		{
			public int compare(Entry<K, V> e1, Entry<K, V> e2)
			{
				return e2.getValue().compareTo(e1.getValue());
			}
		});

		return sortedEntries;
	}
	
	/*public Map<String, String> runColorAnalysisByDarkestColor() throws IOException, InvalidConfigurationException
	{
		// get all colors in the element
		SortedSet<Map.Entry<String, Integer>> sortedColorset = getAllColorsInElement();

		// method 2: run color distance
		return runByDarkestColor(sortedColorset);
	}*/
	
	/*private Map<String, String> runByDarkestColor(SortedSet<Map.Entry<String, Integer>> sortedColorset) throws InvalidConfigurationException, IOException
	{
		Map<String, String>  returnMap = new HashMap<String, String>();
		
		String darkestColor = sortedColorset.first().getKey();
		String lightestColor = sortedColorset.first().getKey();
		for (Map.Entry<String, Integer> entry : sortedColorset)
		{
			if(entry.getKey().compareToIgnoreCase(darkestColor) < 0)
			{
				darkestColor = entry.getKey();
			}
			if(entry.getKey().compareToIgnoreCase(lightestColor) > 0)
			{
				lightestColor = entry.getKey();
			}
		}
		
		if (!visualPropertyValue.equalsIgnoreCase(darkestColor))
		{
			int ret = runDetection(darkestColor);
			if(ret == 0)
			{
				returnMap.put(Constants.RCA_FIX_FOUND, darkestColor);
				return returnMap;
			}
			else
			{
				returnMap.put(Constants.RCA_FIX_NOT_FOUND, ret + "#" + darkestColor);
			}
		}
		else
		{
			returnMap.put(Constants.RCA_FIX_NOT_FOUND, -1 + "#" + darkestColor);
		}
		if (!visualPropertyValue.equalsIgnoreCase(lightestColor))
		{
			int ret = runDetection(lightestColor);
			if(ret == 0)
			{
				returnMap.put(Constants.RCA_FIX_FOUND, lightestColor);
				return returnMap;
			}
			else
			{
				returnMap.put(Constants.RCA_FIX_NOT_FOUND, ret + "#" + lightestColor);
			}
		}
		else
		{
			returnMap.put(Constants.RCA_FIX_NOT_FOUND, -1 + "#" + lightestColor);
		}
		return returnMap;
	}
	
	public String runColorAnalysisByColorHistogram() throws IOException, InvalidConfigurationException
	{
		// get all colors in the element
		SortedSet<Map.Entry<String, Integer>> sortedColorset = getAllColorsInElement();

		// method 2: run genetic algorithm
		return runByColorHistogram(sortedColorset);
	}
	
	private String runByColorHistogram(SortedSet<Map.Entry<String, Integer>> sortedColorset) throws InvalidConfigurationException
	{
		List<String> colors = new ArrayList<String>();
		for (Map.Entry<String, Integer> entry : sortedColorset)
		{
			colors.add(entry.getKey());
		}
		
		GAFitnessFunction ga = new GAFitnessFunction(element, visualProperty, oracleFullPath, testFileFullPath, visualPropertyValue, log);
		return ga.runGAForColorAnalysis(colors);
	}
	
	public Map<String, String> runColorAnalysisByFrequencyCount() throws IOException
	{
		// get all colors in the element
		SortedSet<Map.Entry<String, Integer>> sortedColorset = getAllColorsInElement();
		System.out.println("sorted colors map: " + sortedColorset);
		// method 1: try all colors with descending frequency count
		return runByFrequencyMethod(sortedColorset);
	}
	
	private Map<String, String> runByFrequencyMethod(SortedSet<Map.Entry<String, Integer>> sortedColorset) throws IOException
	{
		Map<String, String>  returnMap = new HashMap<String, String>();
		
		// try all colors to see if that solves the problem
		for (Map.Entry<String, Integer> entry : sortedColorset)
		{
			String oracleColor = entry.getKey();

			// check if there is a mismatch in oracle and test colors
			if (!visualPropertyValue.equalsIgnoreCase(oracleColor))
			{
				int ret = runDetection(oracleColor);
				if(ret == 0)
				{
					returnMap.put(Constants.RCA_FIX_FOUND, oracleColor);
					return returnMap;
				}
				else
				{
					returnMap.put(Constants.RCA_FIX_NOT_FOUND, ret + "#" + oracleColor);
				}
			}
			else
			{
				returnMap.put(Constants.RCA_FIX_NOT_FOUND, -1 + "#" + oracleColor);
			}
		}
		return returnMap;
	}*/
	
	private SortedSet<Map.Entry<String, Integer>> getAllColorsInElement() throws NumberFormatException, IOException
	{
		// get difference pixels contained within element's boundaries
		int x = element.getX();
		int y = element.getY();
		int width = element.getWidth();
		int height = element.getHeight();
		int incrementSize = 0;
		int maxTries = 50;
		
		List<Point> pixelsInElement = new ArrayList<Point>();
		
		do
		{
			x = x - incrementSize;
			y = y - incrementSize;
			width = width + incrementSize;
			height = height + incrementSize;
			for(Point pixel : originalDifferencePixels)
			{			
				if(Util.isPointInRectangle(pixel.x, pixel.y, x, y, width, height, true))
				{
					pixelsInElement.add(new Point(pixel.x, pixel.y));
				}
			}
			incrementSize++;
		} while(pixelsInElement.isEmpty() && incrementSize < maxTries);
		
		if(pixelsInElement.isEmpty())
		{
			
		}
		
		// get colors for all difference pixels within the element boundaries
		Map<String, Integer> colors = new HashMap<String, Integer>();
		for(int i = 0; i < pixelsInElement.size(); i = i + Constants.RCA_COLOR_ANALYSIS_PIXEL_SUB_SAMPLING_RATE)
		{
			Point p = pixelsInElement.get(i);
			if(p.x >= 0 && p.y >= 0 && p.x < oracleImage.getWidth() && p.y < oracleImage.getHeight())
			{
				String c = getPixelColorInHex(oracleImage, p.x, p.y);
				Integer cnt = 0;
				if (colors.get(c) != null)
				{
					cnt = colors.get(c);
				}
				colors.put(c, ++cnt);
			}
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

	private BufferedImage getBufferedImage(String imagePath) throws IOException
	{
		File file = new File(imagePath);
		BufferedImage image = ImageIO.read(file);
		return image;
	}
	
	private String getPixelColorInHex(BufferedImage image, int x, int y)
	{		
		int clr = image.getRGB(x, y);
		int red = (clr >> 16) & 0xff;
		int green = (clr >> 8) & 0xff;
		int blue = (clr) & 0xff;
		
		String hex = Util.getHexFromRGB(red, green, blue);
		return hex;
	}
	
	private int runDetection(String color) throws IOException
	{
		Document doc = Jsoup.parse(new File(testFileFullPath), null);
		Element e = Util.getElementFromXPathJava(element.getXpath(), doc);
		
		String logString = "    ";

		// apply oracle color to the element in test page and see if it solves the problem
		String style = e.attr("style"); 
		style = style + "; " +visualProperty + ":" + color; 
		e.attr("style", style);
		logString = logString + "value = " + color;
		doc.outputSettings().prettyPrint(false);
		//doc.outputSettings().escapeMode(Entities.EscapeMode.extended);
		String html = doc.html();
		
		PrintWriter out = new PrintWriter(new File(testFileFullPath));
		out.print(html);
		out.close();

		// run detection to see if the problem is resolved
		int diffPixels = RootCauseAnalysis.fitnessFunction(oracleFullPath, testFileFullPath, originalDifferencePixels);
		
		if (diffPixels == 0)
		{
			// problem solved
			logString = logString + " => exact root cause found!";
			log.info(logString);
			return 0;
		}
		log.info(logString);
		return diffPixels;
	}
}
