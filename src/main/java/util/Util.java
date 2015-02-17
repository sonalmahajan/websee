package util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.w3c.dom.Node;

import websee.ImageProcessing;
import websee.WebDriverSingleton;
import config.Constants;

public class Util
{
	/**
	 * Save rendered webpage
	 */
	public static File saveHTMLPage(String urlString, String path) throws IOException
	{
		// wget to save html page
		Runtime runtime = Runtime.getRuntime();
		Process p = runtime.exec("wget -p -k -E -nd -P " + path + " " + urlString);
		try
		{
			p.waitFor();
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}

		File savedHTML = new File(getSavedFilePath(path));
		return savedHTML;
	}

	public static void saveHTMLPageWindows(String urlString, String path, String fileName, boolean doNotOverwrite) throws IOException
	{
		File file = new File(path + File.separatorChar + fileName);
		
		// check if overwrite=false and file exists and is not empty. Do not overwrite if the file seems proper
		if(doNotOverwrite && file.exists() && file.length() > 0)
		{
			return;
		}
		
		Document doc = Jsoup.connect(urlString).get();
		
		//fix position to absolute
		//Element root = doc.select("html").first();
		//root.attr("style", "position:absolute");

		// check for relative urls (href, src, action, background, url)
		ArrayList<String> htmlRelativeUrlsTagsAttributes = Constants.getHtmlRelativeUrlsTagsAttributes();
		for (String attribute : htmlRelativeUrlsTagsAttributes)
		{
			for (Element element : doc.select("[" + attribute + "]"))
			{
				element.attr(attribute, element.absUrl(attribute));
			}
		}

		file.createNewFile();
		doc.outputSettings().prettyPrint(false);
		//doc.outputSettings().escapeMode(Entities.EscapeMode.extended);
		String html = doc.html();		
		PrintWriter out = new PrintWriter(file);
		out.print(html);
		out.close();
	}

	private static String getSavedFilePath(String path)
	{
		File target = new File(path);
		String fileName = Constants.MATCH_FILE;

		if (target != null && target.isDirectory())
		{
			for (File f : target.listFiles())
			{
				if (f.isFile())
				{
					Pattern p = Pattern.compile(fileName);
					Matcher m = p.matcher(f.getName());
					if (m.find())
						return f.getAbsolutePath();
				}
			}
		}
		return "";
	}

	/**
	 * Given an HTML element, retrieve its XPath
	 * 
	 * @param js
	 *            Selenium JavascriptExecutor object to execute javascript
	 * @param element
	 *            Selenium WebElement corresponding to the HTML element
	 * @return XPath of the given element
	 */
	public static String getElementXPath(JavascriptExecutor js, WebElement element)
	{
		return (String) js.executeScript(
		"var getElementXPath = function(element) {" + 
//			"if (element && element.id){" + 
//				"return '//*[@id=\"' + element.id + '\"]';" + 
//			"}" + 
//			"else {" + 
				"return getElementTreeXPath(element);" + 
//			"}" + 
		"};" +

		"var getElementTreeXPath = function(element) {" + 
			"var paths = [];" + 
			"for (; element && element.nodeType == 1; element = element.parentNode)  {" + 
				"var index = 0;" + 
//				"if (element && element.id) {" + 
//					"paths.splice(0, 0, '/*[@id=\"' + element.id + '\"]');" + 
//					"break;" + 
//				"}" +
				"for (var sibling = element.previousSibling; sibling; sibling = sibling.previousSibling) {" + 
					"if (sibling.nodeType == Node.DOCUMENT_TYPE_NODE) {" + 
						"continue;" + 
					"}" +
					"if (sibling.nodeName == element.nodeName) {" + 
						"++index;" + 
					"}" + 
				"}" +

				"var tagName = element.nodeName.toLowerCase();" + 
				"var pathIndex = (\"[\" + (index+1) + \"]\");" + 
				"paths.splice(0, 0, tagName + pathIndex);" + 
			"}" +
			"return paths.length ? \"/\" + paths.join(\"/\") : null;" + 
		"};" +
			
		"return getElementXPath(arguments[0]);", element);
	}

	/**
	 * Get filename name and extension separated. Suffix can be provided to be
	 * added to the name part.
	 * 
	 * @param fileName
	 *            filename to be split (e.g. abc.html)
	 * @param additionalSuffix
	 *            any suffix that should be added to the name part only of the
	 *            file (e.g. _test, 1)
	 * @return String array of size 2. Index 0 contains filename only and index
	 *         1 contains extension (e.g. arr[0] = abc_test1, arr[1] = .html)
	 */
	public static String[] getFileNameAndExtension(String fileName, String... additionalSuffix)
	{
		String[] fileNameArray = fileName.split(Constants.FILE_EXTENSION_REGEX);
		String[] returnFileNameArray = new String[2];

		returnFileNameArray[0] = fileNameArray[0];
		for (int i = 0; i < additionalSuffix.length; i++)
		{
			returnFileNameArray[0] = returnFileNameArray[0] + additionalSuffix[i];
		}

		String temp = "";
		for (int i = 1; i < fileNameArray.length; i++)
		{
			temp = temp + "." + fileNameArray[i];
		}
		returnFileNameArray[1] = temp;

		return returnFileNameArray;
	}

	public static String createDirectory(String path, String dirName)
	{
		String dirPath = path + File.separatorChar + dirName;
		File dir = new File(dirPath);
		if (!dir.exists())
		{
			dir.mkdir();
		}
		else
		{
			int count = 1;
			do
			{
				dir = new File(dirPath + "_" + count++);
			}
			while (dir.exists());
			dir.mkdir();
		}
		return dir.getAbsolutePath();
	}

	/**
	 * Take screenshot of the saved html page
	 */
	public static void getScreenshot(String savedHTMLFileName, String dirPath, String imageName, boolean doNotOverwrite) throws IOException
	{
		File imageFile = new File(dirPath + File.separatorChar + imageName);
		
		// check if overwrite=false and file exists and is not empty. Do not overwrite if the file seems proper
		if(doNotOverwrite && imageFile.exists() && imageFile.length() > 0)
		{
			return;
		}
		
		WebDriverSingleton instance = WebDriverSingleton.getInstance();
		WebDriver driver = instance.getDriver();
		
		File file = new File(dirPath + File.separatorChar + savedHTMLFileName);
		WebDriverSingleton.getInstance().loadPage(file.getAbsolutePath());
		try
		{
			File screenshotFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
			FileUtils.copyFile(screenshotFile, imageFile);
		}
		catch(WebDriverException wde)
		{
			System.out.println("File " + file.getAbsolutePath() + " has canvas too big for Selenium to capture image. Hence, skipping.");
			return;
		}
		
		// check if the screenshot is blank
		ImageProcessing ip = new ImageProcessing();
		if(ip.isImageBlank(imageFile.getAbsolutePath()))
		{
			// close the browser instance and get screenshot again
			WebDriverSingleton.closeDriver();
			getScreenshot(savedHTMLFileName, dirPath, imageName, false);
		}
		
		
		//WebDriverSingleton.closeDriver();
	}

	public static ArrayList<String[]> getHtmlFileNamesInDirectory(String dirPath)
	{
		ArrayList<String[]> fileNames = new ArrayList<String[]>();
		File target = new File(dirPath);
		String fileName = Constants.MATCH_FILE;

		if (target != null && target.isDirectory())
		{
			for (File f : target.listFiles())
			{
				if (f.isFile())
				{
					Pattern p = Pattern.compile(fileName);
					Matcher m = p.matcher(f.getName());
					if (m.find())
					{
						fileNames.add(getFileNameAndExtension(f.getName()));
					}
				}
			}
		}
		return fileNames;
	}

	public static String getXPathOfElementJava(Element element)
	{
		/*if(element != null && !element.id().isEmpty())
		{
			return "//*[@id=\"" + element.id() + "\"]";
		}
		else
		{*/
			return getElementTreeXPathJava(element);
		//}
	}
	
	private static String getElementTreeXPathJava(Element element)
	{
		ArrayList<String> paths = new ArrayList<String>();
		for(; element != null && !element.tagName().equals("#root") ; element = element.parent())
		{
			int index = 0;
			/*if(!element.id().isEmpty())
			{
				paths.add("/*[@id=\"" + element.id() + "\"]");
				break;
			}*/
			
			for(Element sibling = element.previousElementSibling() ; sibling != null && !sibling.tagName().equals("#root") ; sibling = sibling.previousElementSibling())
			{
				if(sibling.tagName().equals(element.tagName()))
				{
					++index;
				}
			}
			String tagName = element.tagName().toLowerCase();
			String pathIndex = "[" + (index + 1) + "]";
			paths.add(tagName + pathIndex);
		}
		
		String result = null;
		if(paths.size() > 0)
		{
			result = "/";
			for (int i = paths.size()-1 ; i > 0 ; i--)
			{
				result = result + paths.get(i) + "/";
			}
			result = result + paths.get(0);
		}
		
		return result;
	}
	
	public static Element getElementFromXPathJava(String xPath, Document doc) throws IOException
	{
		String xPathArray[] = xPath.split("/");
		ArrayList<String> xPathList = new ArrayList<String>();
		
		for (int i = 0; i < xPathArray.length; i++)
		{
			if(!xPathArray[i].isEmpty())
			{
				xPathList.add(xPathArray[i]);
			}
		}

		Element foundElement = null;
		Elements elements;
		int startIndex = 0;
		
		String id = getElementId(xPathList.get(0));
		if(id != null && !id.isEmpty())
		{
			foundElement = doc.getElementById(id);
			if(foundElement == null)
				return null;
			elements = foundElement.children();
			startIndex = 1;
		}
		else
		{
			elements = doc.select(xPathList.get(0).replaceFirst(Constants.REGEX_FOR_GETTING_INDEX, ""));
		}
		for (int i = startIndex ; i < xPathList.size() ; i++)
		{
			String xPathFragment = xPathList.get(i);
			int index = getSiblingIndex(xPathFragment);
			boolean found = false;
			
			//strip off sibling index in square brackets
			xPathFragment = xPathFragment.replaceFirst(Constants.REGEX_FOR_GETTING_INDEX, "");
			
			for (Element element : elements)
			{
				if(found == false && xPathFragment.equalsIgnoreCase(element.tagName()))
				{
					//check if sibling index present
					if(index > 1)
					{
						int siblingCount = 0;
						for(Element siblingElement = element.firstElementSibling() ; siblingElement != null ; siblingElement = siblingElement.nextElementSibling())
						{
							if((siblingElement.tagName().equalsIgnoreCase(xPathFragment)))
							{
								siblingCount++;
								if(index == siblingCount)
								{
									foundElement = siblingElement;
									found = true;
									break;
								}
							}
						}
						//invalid element (sibling index does not exist)
						if(found == false)
							return null;
					}
					else
					{
						foundElement = element;
						found = true;
					}
					break;
				}
			}
			
			//element not found
			if(found == false)
			{
				return null;
			}
			
			elements = foundElement.children();
		}
		return foundElement;
	}
	
	private static int getSiblingIndex(String xPathElement)
	{
		String value = getValueFromRegex(Constants.REGEX_FOR_GETTING_INDEX, xPathElement);
		if(value == null)
			return -1;
		return Integer.parseInt(value);
	}
	
	private static String getElementId(String xPathElement)
	{
		return getValueFromRegex(Constants.REGEX_FOR_GETTING_ID, xPathElement);
	}
	
	public static String getValueFromRegex(String regex, String str)
	{
		Pattern p = Pattern.compile(regex, Pattern.DOTALL);
		Matcher m = p.matcher(str);
		if (m.find())
		{
			return m.group(1);		
		}
		return null;		
	}

	public static WebElement getElementFromCoordinates(JavascriptExecutor js, double x, double y)
	{
		//convert absolute co-ordinate values to relative with respect to the viewport
		//handle elements like embed which are javascript functions instead of DOM elements. In this case, return parent
		
		String javscriptGetElementFromCoordinates = "var getElementFromCoordinates = function (x, y) " +
													"{" +
														"var scrollx = window.scrollX;" +
														"var scrolly = window.scrollY;" +
														"var newx = x + scrollx;" +
														"var newy = y + scrolly;" +
														"window.scrollTo(newx, newy);" +
														"scrollx = window.scrollX;" +
														"scrolly = window.scrollY;" +
														"var element = document.elementFromPoint((newx-scrollx), (newy-scrolly));" +
														"while(typeof element === 'function')" +
														"{" +
															"element = element.parentElement;" +
														"}" +
														"window.scrollTo(0, 0);" +
														"return element;" +											
													"};" +
													"return getElementFromCoordinates(arguments[0], arguments[1]);";
		return (WebElement) js.executeScript(javscriptGetElementFromCoordinates, x, y);
	}
	
	public static String getHtmlPageCharset(Document document)
	{
		String charsetName = Constants.DEFAULT_CHARSET;
		Element meta = document.select("meta[http-equiv=content-type], meta[charset]").first();
        if (meta != null) 
        {
        	String foundCharset = meta.hasAttr("http-equiv") ? getValueFromRegex(Constants.CHARSET_REGEX, meta.attr("content")) : meta.attr("charset");
        	if (foundCharset != null && foundCharset.length() != 0) 
        	{
                charsetName = foundCharset;
            }
        }
        return charsetName;
	}
	
	public static String getNextAvailableFileName(String dir, String baseFileName)
	{
		File file = null;
		int count = -1;
		String[] fileNameArray = getFileNameAndExtension(baseFileName);
		String newFileName = "";
		do
		{
			count++;
			newFileName = fileNameArray[0];
			if(count > 0)
			{
				newFileName = newFileName + "_" + count;
			}
			file = new File(dir + File.separatorChar + newFileName + fileNameArray[1]);
			
		} while(file != null && file.exists());
		
		return newFileName + fileNameArray[1];
	}
	
	public static int getTotalNumberOfHtmlElementsInPage(String fileNameWithPath) throws IOException
	{
		Document document = Jsoup.parse(new File(fileNameWithPath), null);
		return document.getAllElements().size();
	}
	
	public static List<Integer> getNumbersFromString(String string)
	{
		List<Integer> numbers = new ArrayList<Integer>();
		Pattern p = Pattern.compile("-?\\d+");
		Matcher m = p.matcher(string);
		while (m.find()) 
		{
			numbers.add(Integer.valueOf(m.group()));
		}
		return numbers;
	}
	
	public static Double getDecimalNumberFromString(String string)
	{
		Pattern p = Pattern.compile("-?\\d+\\.?\\d*");
		Matcher m = p.matcher(string);
		if (m.find()) 
		{
			return Double.valueOf(m.group());
		}
		return null;
	}

	public static String[] getPathAndFileNameFromFullPath(String fullPath)
	{
		String[] result = new String[2];
		
		File file = new File(fullPath);
		if(file.exists() && file.isFile())
		{
			result[0] = file.getParent();
			result[1] = file.getName();
		}
		return result;
	}
	
	public static double getWeightedMean(List<Double> weights, List<Double> values)
	{
		double numerator = 0.0;
		double denominator = 0.0;
		
		for (int i = 0; i < weights.size(); i++)
		{
			numerator = numerator + (weights.get(i) * values.get(i));
			denominator = denominator + weights.get(i);
		}
		
		return numerator/denominator;
	}
	
	public static int getDistance(String expected, String actual)
	{
		String expectedArray[] = expected.split("/");
		String actualArray[] = actual.split("/");
		int expectedLength = expectedArray.length - 1;
		int actualLength = actualArray.length - 1;
		int distance;
		
		int matchingCount = 0;
		for(int i = 1 ; i < expectedArray.length && i < actualArray.length ; i++)
		{
			if(expectedArray[i].equals(actualArray[i]))
			{
				matchingCount++;
			}
			else
			{
				break;
			}
		}
		
		distance = (actualLength - matchingCount) + (expectedLength - matchingCount);
		
		return distance;
	}
	
	public static boolean isPointInRectangle(int x, int y, int left, int top, int width, int height, boolean isBorderIncluded)
	{
		if(isBorderIncluded)
		{
			if(x >= left && y >= top && x <= (left + width) && y <= (top + height))
				return true;
		}
		else
		{
			if(x > left && y > top && x < (left + width) && y < (top + height))
				return true;
		}
		return false;
	}
	
	public static double convertNanosecondsToSeconds(long time)
	{
		return (double) time/1000000000.0;
	}
	
	public static Logger getNewLogger(String filePathWithName, String loggerName) throws IOException
	{
		Logger log = org.apache.log4j.Logger.getLogger(loggerName);
		PatternLayout layout = new PatternLayout(Layout.LINE_SEP + "%m");
	    FileAppender appender = new FileAppender(layout, filePathWithName, true);    
	    log.addAppender(appender);
	    return log;
	}

	public static org.w3c.dom.Element getW3CElementFromXPathJava(String xPath, org.w3c.dom.Document doc) throws IOException
	{
		String xPathArray[] = xPath.split("/");
		ArrayList<String> xPathList = new ArrayList<String>();
		
		for (int i = 0; i < xPathArray.length; i++)
		{
			if(!xPathArray[i].isEmpty())
			{
				xPathList.add(xPathArray[i]);
			}
		}

		org.w3c.dom.Element foundElement = null;
		org.w3c.dom.NodeList elements;
		int startIndex = 0;
		
		String id = getElementId(xPathList.get(0));
		if(id != null && !id.isEmpty())
		{
			foundElement = doc.getElementById(id);
			if(foundElement == null)
				return null;
			elements = foundElement.getChildNodes();
			startIndex = 1;
		}
		else
		{
			elements = doc.getElementsByTagName(xPathList.get(0).replaceFirst(Constants.REGEX_FOR_GETTING_INDEX, ""));
		}
		for (int i = startIndex ; i < xPathList.size() ; i++)
		{
			String xPathFragment = xPathList.get(i);
			int index = getSiblingIndex(xPathFragment);
			boolean found = false;
			
			//strip off sibling index in square brackets
			xPathFragment = xPathFragment.replaceFirst(Constants.REGEX_FOR_GETTING_INDEX, "");
			
			for (int j = 0; j < elements.getLength(); j++)
			{
				if(elements.item(j).getNodeType() != Node.ELEMENT_NODE)
				{
					continue;
				}
				
				org.w3c.dom.Element element = (org.w3c.dom.Element) elements.item(j);

				if(found == false && xPathFragment.equalsIgnoreCase(element.getTagName()))
				{
					//check if sibling index present
					if(index > 1)
					{
						int siblingCount = 0;
						
						for(org.w3c.dom.Node siblingNode = element.getParentNode().getFirstChild() ; siblingNode != null ; siblingNode = siblingNode.getNextSibling())
						{
							if(siblingNode.getNodeType() != Node.ELEMENT_NODE)
							{
								continue;
							}
							
							org.w3c.dom.Element siblingElement = (org.w3c.dom.Element)siblingNode;
							if((siblingElement.getTagName().equalsIgnoreCase(xPathFragment)))
							{
								siblingCount++;
								if(index == siblingCount)
								{
									foundElement = siblingElement;
									found = true;
									break;
								}
							}
						}
						//invalid element (sibling index does not exist)
						if(found == false)
							return null;
					}
					else
					{
						foundElement = element;
						found = true;
					}
					break;
				}
			}
			
			//element not found
			if(found == false)
			{
				return null;
			}
			
			elements = foundElement.getChildNodes();
		}
		return foundElement;
	}
	
	public static int getDecimalFromHex(String hex)
	{
		try
		{
			return Integer.parseInt(hex.replace("#", ""), 16);
		}
		catch(Exception e)
		{
			return -1;
		}
	}
	
	public static String getHexFromDecimal(int dec)
	{
		//return "#" + Integer.toHexString(dec);
		return String.format("#%06X", (0xFFFFFF & dec));
	}
	
	public static String getHexFromRGB(int red, int green, int blue)
	{
		return String.format("#%02x%02x%02x", red, green, blue);
	}

	public static int getRandomNumber(int low, int high)
	{
		Random generator = new Random(); 
		if(low == Integer.MIN_VALUE && high == Integer.MAX_VALUE)
		{
			return generator.nextInt();
		}
		return generator.nextInt(high - low + 1) + low;
	}

	public static void drawRectangleOnImage(String imageFileName, String path, int x, int y, int w, int h) throws IOException
	{
		File imageFile = new File(path + File.separatorChar + imageFileName);
        BufferedImage img = ImageIO.read(imageFile);

        Graphics2D graph = img.createGraphics();
        graph.setColor(Color.BLACK);
        graph.drawRect(x, y, w, h);
        graph.dispose();

        ImageIO.write(img, Constants.IMAGE_EXTENSION, new File(path + File.separatorChar + imageFileName));
	}

	public static double getNormalizedValue(double min, double max, double val)
	{
		return ((val - min) /(max - min));
	}
	
	public static boolean isCurrentInAcceptableReductionThreshold(double current, double original)
	{
		return ((original - current)/original) * 100.0 >= Constants.RCA_NUMERIC_ANALYSIS_REDUCTION_IN_DIFFERENCE_PIXELS_THRESHOLD_PERCENTAGE;
	}
	
	public static void main(String[] args) throws IOException
	{
		System.out.println(Util.getTotalNumberOfHtmlElementsInPage("C:\\USC\\visual_checking\\evaluation\\test\\Student1\\test7\\test.html"));
	}
}