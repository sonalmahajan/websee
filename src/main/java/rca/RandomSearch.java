package rca;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.openqa.selenium.By;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;

import util.Util;
import websee.HtmlElement;
import websee.WebSeeTool;
import config.Constants;

public class RandomSearch
{
	private HtmlElement element;
	private String visualProperty; // CSS -> style:<property_name>:<unit>, HTML attribute -> <attribute_name>
	private String[] testPagePathAndName;
	private String newRandomDir;
	private String oracleFullPath;
	private Logger log;
	
	private Set<String> visitedValues;
	
	private List<Point> pixelsInElement;

	public RandomSearch(HtmlElement element, String visualProperty, String testFileFullPath, String oracleFullPath)
	{
		this.element = element;
		this.visualProperty = visualProperty;
		this.oracleFullPath = oracleFullPath;
		this.visitedValues = new HashSet<String>();

		// create new directory
		this.testPagePathAndName = Util.getPathAndFileNameFromFullPath(testFileFullPath);
		this.newRandomDir = testPagePathAndName[0] + File.separatorChar + "RandomNumeric";
		File gaDir = new File(newRandomDir);
		gaDir.mkdir();
	}
	
	public RandomSearch(HtmlElement element, String visualProperty, String testFileFullPath, String oracleFullPath, Logger log)
	{
		this.element = element;
		this.visualProperty = visualProperty;
		this.oracleFullPath = oracleFullPath;
		this.log = log;
		this.visitedValues = new HashSet<String>();

		// create new directory
		this.testPagePathAndName = Util.getPathAndFileNameFromFullPath(testFileFullPath);
		this.newRandomDir = testPagePathAndName[0] + File.separatorChar + "RandomNumeric";
		File gaDir = new File(newRandomDir);
		gaDir.mkdir();
	}

	public void randomNumericSearch(long timeToRun, int low, int high) throws IOException
	{
		long startTime = System.nanoTime();
		
		do
		{
			int value = getRandomNumber(low, high);
			
			while(visitedValues.contains(String.valueOf(value)))
			{
				if(visitedValues.size() >= (Math.abs(high) + Math.abs(low)))
				{
					log.info("All integers tried, no solution found");
					System.out.println("All integers tried, no solution found");
					return;
				}
				value = getRandomNumber(low, high);
			}
			visitedValues.add(String.valueOf(value));
			
			// create test page clone
			String[] newName = Util.getFileNameAndExtension(testPagePathAndName[1], "_clone_" + value);
			String cloneFileName = newName[0] + newName[1];
			String cloneFile = newRandomDir + File.separatorChar + cloneFileName;
			
			FileUtils.copyFile(new File(testPagePathAndName[0] + File.separatorChar + testPagePathAndName[1]), new File(cloneFile));
			Document document = Jsoup.parse(new File(cloneFile), null);
			Element e = Util.getElementFromXPathJava(element.getXpath(), document);
	
			// check if CSS property or HTML attribute to modify
			if (visualProperty.contains("style"))
			{
				// CSS property (style:<property_name>:<unit>)
				String css[] = visualProperty.split(":");
				String style = e.attr("style");
				style = style + "; " + css[1] + ":" + value + css[2]; 
				e.attr(css[0], style);
			}
			else
			{
				// HTML attribute
				e.attr(visualProperty, "" + value);
			}
			
			document.outputSettings().prettyPrint(false);
			document.outputSettings().escapeMode(Entities.EscapeMode.extended);
			String html = document.html();		
			PrintWriter out = new PrintWriter(new File(cloneFile));
			out.print(html);
			out.close();
	
			// run detection to see if the problem is resolved
			WebSeeTool vit = new WebSeeTool(oracleFullPath, cloneFile);
			try
			{
				List<Point> diffPixels = vit.detection(false);
		
				if(diffPixels.size() == 0)
				{
					log.info("Solution found!");
					System.out.println("Solution found!!!");
					return;
				}
			}
			catch(Exception wde)
			{
				System.out.println("Large value leading to big canvas Selenium cannot capture. Hence, skipping");
			}
		} while(System.nanoTime() - startTime <= timeToRun);
	}
	
	private int getRandomNumber(int low, int high)
	{
		Random generator = new Random(); 
		if(low == Integer.MIN_VALUE && high == Integer.MAX_VALUE)
		{
			return generator.nextInt();
		}
		return generator.nextInt(high - low + 1) + low;
	}
	
	public void randomColorSearch(String oracleAverageColor, List<Point> diffPixelsInElement, long timeToRun, int low, int high) throws IOException
	{
		pixelsInElement = diffPixelsInElement;
		
		long startTime = System.nanoTime();
		
		do
		{
			String value = Util.getHexFromDecimal(getRandomNumber(low, high));
			
			while(visitedValues.contains(value))
			{
				if(visitedValues.size() >= (Math.abs(high) + Math.abs(low)))
				{
					log.info("All colors tried, no solution found");
					System.out.println("All colors tried, no solution found");
					return;
				}
				value = Util.getHexFromDecimal(getRandomNumber(low, high));
			}
			visitedValues.add(value);

			// create test page clone
			String[] newName = Util.getFileNameAndExtension(testPagePathAndName[1], "_clone_" + value.replace("#", ""));
			String cloneFileName = newName[0] + newName[1];
			String cloneFile = newRandomDir + File.separatorChar + cloneFileName;
			
			FileUtils.copyFile(new File(testPagePathAndName[0] + File.separatorChar + testPagePathAndName[1]), new File(cloneFile));
			Document document = Jsoup.parse(new File(cloneFile), null);
			Element e = Util.getElementFromXPathJava(element.getXpath(), document);
	
			// check if CSS property or HTML attribute to modify
			if (visualProperty.contains("style"))
			{
				// CSS property (style:<property_name>:<unit>)
				String css[] = visualProperty.split(":");
				String style = e.attr("style");
				style = style + "; " + css[1] + ":" + value; 
				e.attr(css[0], style);
			}
			document.outputSettings().prettyPrint(false);
			document.outputSettings().escapeMode(Entities.EscapeMode.extended);
			String html = document.html();		
			PrintWriter out = new PrintWriter(new File(cloneFile));
			out.print(html);
			out.close();
	
			// take screenshot of test page
			Util.getScreenshot(cloneFileName, newRandomDir, newName[0] + Constants.SCREENSHOT_FILE_EXTENSION, true);

			// compute average color from test page
			String testAverageColor = computeAverageColor(newRandomDir + File.separatorChar + newName[0] + Constants.SCREENSHOT_FILE_EXTENSION);
			double distance = getColorDistance(Color.decode(oracleAverageColor), Color.decode(testAverageColor));
			
			WebSeeTool vit = new WebSeeTool(oracleFullPath, cloneFile);
			List<Point> diffPixels = vit.detection(true);
			
			if((distance + diffPixels.size()) == 0.0)
			{
				log.info("Solution found!");
				System.out.println("Solution found!!!");
				return;
			}
		} while((System.nanoTime() - startTime) <= timeToRun);
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
		if(pixelsInElement == null || pixelsInElement.size() == 0)
		{
			pixelsInElement = new ArrayList<Point>();
			
			WebSeeTool vit = new WebSeeTool(oracleFullPath, testPagePathAndName[0] + File.separatorChar + testPagePathAndName[1]);
			List<Point> diffPixels = vit.detection(false);
		
			// get difference pixels contained within element's boundaries
			FirefoxDriver d = new FirefoxDriver();
			d.manage().window().maximize();
			d.get("file://" + testPagePathAndName[0] + File.separatorChar + testPagePathAndName[1]);
			WebElement el = d.findElement(By.xpath(element.getXpath()));
			int x = el.getLocation().x;
			int y = el.getLocation().y;
			int width = el.getSize().width;
			int height = el.getSize().height;
			d.quit();
	
			for(Point p : diffPixels)
			{
				if(Util.isPointInRectangle(p.x, p.y, x, y, width, height, true))
				{
					pixelsInElement.add(p);
				}
			}
		}
		
		// get colors for all difference pixels within the element boundaries
		Map<String, Integer> colors = new HashMap<String, Integer>();
		for (Point p : pixelsInElement)
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
	
	public static void main(String[] args)
	{
		RandomSearch rs = new RandomSearch(null, "", "", "");
		System.out.println(rs.getColorDistance(Color.decode("#FFA000"), Color.decode("#333300")));
	}
}