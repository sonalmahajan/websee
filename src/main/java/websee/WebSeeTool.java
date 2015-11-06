package websee;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jgap.InvalidConfigurationException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.xml.sax.SAXException;

import rca.NumericAnalysis;
import rca.RootCauseAnalysis;
import rca.RunS1Test;
import util.Util;
import clustering.DifferencePixelsClustering;
import config.Constants;

public class WebSeeTool
{
	private static Logger logger = Logger.getLogger(WebSeeTool.class);

	private boolean R_TREE_FLAG = true;

	private String referenceImagePath;
	private String comparisonImagePath;
	private String referenceImageName;
	private String comparisonImageName;
	private String differenceText;
	private WebDriver driver;
	private File savedHtmlFile;
	private HtmlDomTree rTree;
	private Set<SpecialRegion> specialRegions;
	private File filteredDiffFile;
	private List<Point> differencePixels;
	private String differenceImageFullPath;

	private boolean isRCATimeoutInMins;
	private boolean isRCAElementsCutoff;
	private boolean isRCANumericAnalysisRateOfChange;

	private long startTimeThisTestCase;
	private long endTimeThisTestCase;

	private Map<Integer, List<Node<HtmlElement>>> clusterElementsMap;
	private boolean isClusteringToBeUsed = true;
	
	private String specialRegionsFullPath;

	private RunS1Test statsObj;
	
	private Map<String, Double> timeMapInSec;

	public WebSeeTool(String oracleFullPath, String testFileFullPath, RunS1Test statsObj)
	{
		this(oracleFullPath, testFileFullPath);
		this.statsObj = statsObj;
	}

	public WebSeeTool(String oracleFullPath, String testFileFullPath, String specialRegionsFullPath)
	{
		timeMapInSec = new HashMap<String, Double>();
		
		rTree = null;
		this.specialRegionsFullPath = specialRegionsFullPath;
		specialRegions = new HashSet<SpecialRegion>();

		String[] oraclePathAndName = Util.getPathAndFileNameFromFullPath(oracleFullPath);
		this.referenceImagePath = oraclePathAndName[0];
		this.referenceImageName = oraclePathAndName[1];

		String[] testFilePathAndName = Util.getPathAndFileNameFromFullPath(testFileFullPath);
		this.comparisonImagePath = testFilePathAndName[0];
		comparisonImageName = Util.getFileNameAndExtension(testFilePathAndName[1])[0] + Constants.SCREENSHOT_FILE_EXTENSION;

		this.savedHtmlFile = new File(testFileFullPath);

		// compute difference text name (diff_oracle_test.txt)
		String tempDiffName[] = Util.getFileNameAndExtension(Constants.COMPARE_IMAGES_DIFFERENCE_TEXT_FILENAME, "_" + Util.getFileNameAndExtension(referenceImageName)[0] + "_" + Util.getFileNameAndExtension(testFilePathAndName[1])[0]);
		differenceText = tempDiffName[0] + tempDiffName[1];

		differencePixels = new ArrayList<Point>();
		clusterElementsMap = new HashMap<Integer, List<Node<HtmlElement>>>();
	}
	
	public WebSeeTool(String oracleFullPath, String testFileFullPath)
	{
		timeMapInSec = new HashMap<String, Double>();
		
		rTree = null;
		specialRegions = new HashSet<SpecialRegion>();

		String[] oraclePathAndName = Util.getPathAndFileNameFromFullPath(oracleFullPath);
		this.referenceImagePath = oraclePathAndName[0];
		this.referenceImageName = oraclePathAndName[1];

		String[] testFilePathAndName = Util.getPathAndFileNameFromFullPath(testFileFullPath);
		this.comparisonImagePath = testFilePathAndName[0];
		comparisonImageName = Util.getFileNameAndExtension(testFilePathAndName[1])[0] + Constants.SCREENSHOT_FILE_EXTENSION;

		this.savedHtmlFile = new File(testFileFullPath);

		// compute difference text name (diff_oracle_test.txt)
		String tempDiffName[] = Util.getFileNameAndExtension(Constants.COMPARE_IMAGES_DIFFERENCE_TEXT_FILENAME, "_" + Util.getFileNameAndExtension(referenceImageName)[0] + "_" + Util.getFileNameAndExtension(testFilePathAndName[1])[0]);
		differenceText = tempDiffName[0] + tempDiffName[1];

		differencePixels = new ArrayList<Point>();
		clusterElementsMap = new HashMap<Integer, List<Node<HtmlElement>>>();
	}

	public void setConfig(boolean isRCATimeoutInMins, boolean isRCAElementsCutoff, boolean isRCANumericAnalysisRateOfChange, boolean isPropertyPrioritizationOn, boolean isSearchSpaceSetByHeuristic, boolean isFitnessFunctionNew,
			boolean isSimmulatedAnnealingToBeUsed, String expectedValue)
	{
		this.isRCATimeoutInMins = isRCATimeoutInMins;
		this.isRCAElementsCutoff = isRCAElementsCutoff;
		this.isRCANumericAnalysisRateOfChange = isRCANumericAnalysisRateOfChange;

		RootCauseAnalysis.setPropertyPrioritizationOn(isPropertyPrioritizationOn);
		RootCauseAnalysis.setFitnessFunctionNew(isFitnessFunctionNew);
		RootCauseAnalysis.setSearchSpaceSetByHeuristic(isSearchSpaceSetByHeuristic);
		RootCauseAnalysis.setSimmulatedAnnealingToBeUsed(isSimmulatedAnnealingToBeUsed);
		RootCauseAnalysis.setExpectedValue(expectedValue);
	}

	public boolean isR_TREE_FLAG()
	{
		return R_TREE_FLAG;
	}

	public void setR_TREE_FLAG(boolean r_TREE_FLAG)
	{
		R_TREE_FLAG = r_TREE_FLAG;
	}

	public List<Point> getDifferencePixels(String oracleFullPath, String testFileFullPath)
	{
		if (differencePixels == null || differencePixels.isEmpty())
		{
			WebSeeTool vit = new WebSeeTool(oracleFullPath, testFileFullPath);
			try
			{
				differencePixels = vit.detection(true);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		return differencePixels;
	}

	public String getReferenceImagePath()
	{
		return referenceImagePath;
	}

	public void setReferenceImagePath(String referenceImagePath)
	{
		this.referenceImagePath = referenceImagePath;
	}

	public String getComparisonImagePath()
	{
		return comparisonImagePath;
	}

	public void setComparisonImagePath(String comparisonImagePath)
	{
		this.comparisonImagePath = comparisonImagePath;
	}

	public String getReferenceImageName()
	{
		return referenceImageName;
	}

	public void setReferenceImageName(String referenceImageName)
	{
		this.referenceImageName = referenceImageName;
	}

	public String getComparisonImageName()
	{
		return comparisonImageName;
	}

	public void setComparisonImageName(String comparisonImageName)
	{
		this.comparisonImageName = comparisonImageName;
	}

	public String getDifferenceText()
	{
		return differenceText;
	}

	public void setDifferenceText(String differenceText)
	{
		this.differenceText = differenceText;
	}

	public File getSavedHtmlFile()
	{
		return savedHtmlFile;
	}

	public void setSavedHtmlFile(File savedHtmlFile)
	{
		this.savedHtmlFile = savedHtmlFile;
	}

	public HtmlDomTree getrTree()
	{
		return rTree;
	}

	public void setrTree(HtmlDomTree rTree)
	{
		this.rTree = rTree;
	}

	public Set<SpecialRegion> getSpecialRegions()
	{
		return specialRegions;
	}

	public void setSpecialRegions(Set<SpecialRegion> specialRegions)
	{
		this.specialRegions = specialRegions;
	}

	public void setDriver(WebDriver driver)
	{
		this.driver = driver;
	}

	public WebDriver getDriver()
	{
		return driver;
	}

	/**
	 * @return the clusterElementsMap
	 */
	public Map<Integer, List<Node<HtmlElement>>> getClusterElementsMap()
	{
		return clusterElementsMap;
	}

	public boolean isClusteringToBeUsed()
	{
		return isClusteringToBeUsed;
	}

	public void setClusteringToBeUsed(boolean isClusteringToBeUsed)
	{
		this.isClusteringToBeUsed = isClusteringToBeUsed;
	}

	/**
	 * Extract only difference pixel values from the text file
	 */
	public List<Point> extract(boolean doNotOverwrite) throws IOException
	{
		List<Point> lDifferencePixels = new ArrayList<Point>();

		File diffFile = new File(comparisonImagePath + File.separatorChar + differenceText);
		if (!diffFile.exists())
		{
			return lDifferencePixels;
		}

		filteredDiffFile = new File(comparisonImagePath + File.separatorChar + Constants.FILTERED_COMPARE_IMAGES_DIFFERENCE_TEXT_FILENAME + differenceText);
		if (doNotOverwrite && filteredDiffFile.exists())
		{
			// read filtered diff file into list
			BufferedReader in = new BufferedReader(new FileReader(filteredDiffFile));
			String line;
			String pointArray[];
			while ((line = in.readLine()) != null)
			{
				pointArray = line.split(",");
				int x = Integer.parseInt(pointArray[0]);
				int y = Integer.parseInt(pointArray[1]);
				lDifferencePixels.add(new Point(x, y));
			}
			in.close();
			return lDifferencePixels;
		}

		BufferedReader inputStream = new BufferedReader(new FileReader(diffFile));
		PrintWriter outputStream = new PrintWriter(new FileWriter(filteredDiffFile));

		// Pixel representation in text file
		// x,y: ... black -> no difference
		// x,y: ... other color -> difference
		String line;
		String[] strPixel;
		while ((line = inputStream.readLine()) != null)
		{
			if (!line.contains("black") && !line.contains("enumeration"))
			{
				strPixel = line.split(":")[0].split(",");
				int x = Integer.parseInt(strPixel[0]);
				int y = Integer.parseInt(strPixel[1]);

				// filtration 2: check if the pixel is not in any special region
				if (!isPixelInSpecialRegions(x, y))
				{
					lDifferencePixels.add(new Point(x, y));

					// copy this pixel line to be stored in filtered diff file
					outputStream.println(x + "," + y);
				}
			}
		}
		inputStream.close();
		outputStream.close();

		return lDifferencePixels;
	}

	private boolean isPixelInSpecialRegions(int x, int y)
	{
		for (SpecialRegion sr : specialRegions)
		{
			Rectangle rect = sr.getRectangle();
			if (Util.isPointInRectangle(x, y, rect.x, rect.y, rect.width, rect.height, true))
			{
				return true;
			}
		}
		return false;
	}

	public Set<Node<HtmlElement>> getErrorElements(List<Point> lDifferencePixels, String reportFileName, boolean doNotOverwrite) throws IOException
	{
		Set<Node<HtmlElement>> errorElements = new HashSet<Node<HtmlElement>>();
		if (doNotOverwrite && new File(reportFileName).exists() && new File(reportFileName).length() > 0)
		{
			return errorElements;
		}

		logger.info("---- GETERRORELEMENTS START ----");

		ImageProcessing ip = new ImageProcessing();
		Rectangle oracleSize = ip.getImageSize(referenceImagePath, referenceImageName);

		Map<String, Node<HtmlElement>> errorElementsMap = new HashMap<String, Node<HtmlElement>>();
		for (Point point : lDifferencePixels)
		{
			if (point == null)
				continue;

			List<Node<HtmlElement>> rTreeNodeAtPointList = new ArrayList<Node<HtmlElement>>();

			// R-TREE WAY
			if (R_TREE_FLAG)
			{
				// Node<HtmlElement> rTreeNodeAtPoint =
				// rTree.searchHtmlDomTreeByPoint(point.x, point.y);
				rTreeNodeAtPointList = rTree.searchRTreeByPoint(point.x, point.y);
			}
			else
			{
				// JAVASCRIPT WAY
				try
				{
					JavascriptExecutor js = (JavascriptExecutor) getDriver();

					WebElement webElement = Util.getElementFromCoordinates(js, point.x, point.y);
					Node<HtmlElement> e = new Node<HtmlElement>(null, new HtmlElement(js, webElement));

					List<WebElement> webElementChildren = webElement.findElements(By.xpath("*"));
					List<Node<HtmlElement>> c = new ArrayList<Node<HtmlElement>>();
					for (WebElement we : webElementChildren)
					{
						c.add(new Node<HtmlElement>(null, new HtmlElement(js, we)));
					}
					e.setChildren(c);

					rTreeNodeAtPointList.add(e);
				}
				catch (UnreachableBrowserException e)
				{
					System.out.println("unreachable browser exception reached");
					WebDriverSingleton.restartDriver();
					setDriver(WebDriverSingleton.getInstance().getDriver());
					WebDriverSingleton.getInstance().loadPage(savedHtmlFile.getAbsolutePath());
				}
			}

			for (Node<HtmlElement> rTreeNodeAtPoint : rTreeNodeAtPointList)
			{
				if (rTreeNodeAtPoint == null)
					continue;

				HtmlElement elementAtPoint = rTreeNodeAtPoint.getData();
				int locationX = elementAtPoint.getX();
				int locationY = elementAtPoint.getY();
				int dimensionW = elementAtPoint.getWidth();
				int dimensionH = elementAtPoint.getHeight();

				int numberOfDifferencePixelsInElementAtPoint = 0;
				if (errorElementsMap.get(elementAtPoint.getXpath()) != null)
				{
					numberOfDifferencePixelsInElementAtPoint = errorElementsMap.get(elementAtPoint.getXpath()).getData().getNumberOfDifferencePixels();
				}
				// if the pixel is outside the comparison dimensions, i.e., test
				// image > oracle image -> don't count
				if (Util.isPointInRectangle(point.x, point.y, oracleSize.x, oracleSize.y, oracleSize.width, oracleSize.height, false))
				{
					numberOfDifferencePixelsInElementAtPoint++;
				}

				errorElementsMap.put(elementAtPoint.getXpath(), rTreeNodeAtPoint);

				logger.info("element " + elementAtPoint.getXpath() + " found at " + point.x + ", " + point.y + " with rectangle [" + elementAtPoint.getX() + ", " + elementAtPoint.getY() + ", " + elementAtPoint.getWidth() + ", "
						+ elementAtPoint.getHeight() + "]");

				// short circuiting
				logger.info("lDifferencePixels before: " + lDifferencePixels.size());
				List<Node<HtmlElement>> children = rTreeNodeAtPoint.getChildren();
				List<Node<HtmlElement>> siblings = rTreeNodeAtPoint.getNodeSiblings();

				int index = lDifferencePixels.indexOf(point);
				for (int i = index + 1; i < lDifferencePixels.size(); i++)
				{
					Point p = lDifferencePixels.get(i);
					if (p == null)
						continue;

					if (p.x >= locationX && p.y >= locationY && p.x <= (locationX + dimensionW) && p.y <= (locationY + dimensionH))
					{
						// if the pixel is outside the comparison
						// dimensions, i.e., test image > oracle image ->
						// don't count
						if (Util.isPointInRectangle(point.x, point.y, oracleSize.x, oracleSize.y, oracleSize.width, oracleSize.height, false))
						{
							numberOfDifferencePixelsInElementAtPoint++;
						}
						
						boolean isPureCurrentElementPoint = true;

						// check if the point is not shared by any children
						if (children != null)
						{
							for (Node<HtmlElement> childNode : children)
							{
								HtmlElement child = childNode.getData();
								if (Util.isPointInRectangle(p.x, p.y, child.getX(), child.getY(), child.getWidth(), child.getHeight(), true))
								{
									isPureCurrentElementPoint = false;
									break;
								}
							}
						}

						// check if the point is not shared by any sibling
						if (siblings != null)
						{
							for (Node<HtmlElement> siblingNode : siblings)
							{
								HtmlElement sibling = siblingNode.getData();
								if (Util.isPointInRectangle(p.x, p.y, sibling.getX(), sibling.getY(), sibling.getWidth(), sibling.getHeight(), true))
								{
									isPureCurrentElementPoint = false;
									break;
								}
							}
						}

						if (isPureCurrentElementPoint == true)
						{
							lDifferencePixels.set(i, null);

							// if the pixel is outside the comparison
							// dimensions, i.e., test image > oracle image ->
							// don't count
							/*if (Util.isPointInRectangle(point.x, point.y, oracleSize.x, oracleSize.y, oracleSize.width, oracleSize.height, false))
							{
								numberOfDifferencePixelsInElementAtPoint++;
							}*/
						}
					}
					else
					{
						// logger.info("getErrorElements",
						// "no match: prune (not matching remaining)");
						// break;
					}
				}
				logger.info("lDifferencePixels after: " + lDifferencePixels.size());
				// logger.info("getErrorElements",
				// "lDifferencePixels size after short circuiting = " +
				// lDifferencePixels.size());
				elementAtPoint.setNumberOfDifferencePixels(numberOfDifferencePixelsInElementAtPoint);
				rTreeNodeAtPoint.setData(elementAtPoint);

				if (R_TREE_FLAG)
				{
					rTree.searchHtmlDomTreeByNode(rTreeNodeAtPoint).setData(elementAtPoint);
				}
			}
		}

		// extract error elements from map
		for (Node<HtmlElement> element : errorElementsMap.values())
		{
			if (element.getData().getHeight() > 0 && element.getData().getWidth() > 0)
			{
				errorElements.add(element);
			}
		}

		return errorElements;
	}

	public void writeReportFile(HashMap<WebElement, WebElement> sErrorElements, JavascriptExecutor js, String reportFileName, boolean doNotOverwrite) throws IOException
	{
		File out = new File(comparisonImagePath + File.separatorChar + reportFileName);

		if (doNotOverwrite && out.exists() && out.length() > 0)
		{
			return;
		}

		PrintWriter outWriter = new PrintWriter(new FileWriter(out));
		for (WebElement webElement : sErrorElements.keySet())
		{
			if (sErrorElements.get(webElement) != null)
			{
				outWriter.println(Util.getElementXPath(js, webElement) + " (" + Constants.DYNAMIC_ELEMENT + ": " + Util.getElementXPath(js, sErrorElements.get(webElement)) + ")");
			}
			else
			{
				outWriter.println(Util.getElementXPath(js, webElement));
			}
		}
		outWriter.close();
	}

	public void writeReportFile(List<Node<HtmlElement>> errorElements, PrintWriter outWriter) throws IOException
	{
		int count = 1;
		for (Node<HtmlElement> node : errorElements)
		{
			//outWriter.println(count + Constants.RESULT_FILE_PRIORITY_VALUE_SEPARATOR + node.getData().getXpath() + " -> " + node.getData().printInformation());
			outWriter.println(count + Constants.RESULT_FILE_PRIORITY_VALUE_SEPARATOR + node.getData().getXpath());
			count++;
		}
	}

	public void markResultOnSavedHtml(List<Node<HtmlElement>> errorElements, File savedHTML) throws IOException
	{
		// copy the html file
		String savedHTMLFileName = savedHTML.getName();
		String newFileName = Util.getNextAvailableFileName(comparisonImagePath, Constants.PREFIX_FOR_RESULT_HTML + savedHTMLFileName);
		File markedFile = new File(comparisonImagePath + File.separatorChar + newFileName);

		markedFile.createNewFile();

		Document document = Jsoup.parse(savedHTML, null);

		for (Node<HtmlElement> node : errorElements)
		{
			Element element = Util.getElementFromXPathJava(node.getData().getXpath(), document);
			if (element != null)
			{
				String styleValue = element.attr("style");
				element.attr("style", styleValue.isEmpty() ? Constants.HTML_ELEMENTS_JAVASCRIPT_MARKER : styleValue + ";" + Constants.HTML_ELEMENTS_JAVASCRIPT_MARKER);
			}
			else
			{
				System.out.println("\nELEMENT IS NULL" + node.getData().getXpath() + "\n");
			}
		}
		document.outputSettings().prettyPrint(false);
		// document.outputSettings().escapeMode(Entities.EscapeMode.extended);
		String html = document.html();
		PrintWriter out = new PrintWriter(markedFile);
		out.print(html);
		out.close();
	}

	public Map<String, String> getErrorElementsXpathsFromFile(String dir, String fileName) throws IOException
	{
		File reportFile = new File(dir + File.separatorChar + fileName);
		Map<String, String> errorElements = new LinkedHashMap<String, String>();

		if (reportFile.exists())
		{
			BufferedReader in = new BufferedReader(new FileReader(reportFile));
			String line;

			while ((line = in.readLine()) != null)
			{
				// check and avoid for time required line
				if (!line.isEmpty() && !line.contains(Constants.RESULT_FILE_TIME_REQUIRED_LINE))
				{
					String entry[] = line.split(Constants.RESULT_FILE_PRIORITY_VALUE_SEPARATOR)[1].split(" -> ");
					errorElements.put(entry[0], entry[1]);
				}
			}
			in.close();
		}
		return errorElements;
	}

	public void runVisualInvariantsTool(String oracleImageName, String oracleImagePath, String testWebPagePath, String testHTMLFileName, String reportFileName, String specialRegionsFileFullPath, Logger log, boolean doNotOverwrite)
			throws IOException, SAXException, InvalidConfigurationException
	{
		runWebSeeCore(oracleImageName, oracleImagePath, testWebPagePath, testHTMLFileName, reportFileName, specialRegionsFileFullPath, log, doNotOverwrite);

		// System.out.println("Phase 5: Root cause analysis");
		long startTime = System.nanoTime();
		for (Integer cid : clusterElementsMap.keySet())
		{
			rootCauseAnalysis(clusterElementsMap.get(cid), startTime);
			long endTime = System.nanoTime() - startTime;
			if (log != null)
			{
				DecimalFormat decimal = new DecimalFormat("0.00");
				log.info("Phase 5: " + decimal.format(Util.convertNanosecondsToSeconds(endTime)) + "s");
			}
		}
		postProcessing(reportFileName);
	}

	public List<Point> detection(boolean doNotOverwrite) throws IOException
	{
		Util.getScreenshot(savedHtmlFile.getName(), comparisonImagePath, comparisonImageName, referenceImagePath + File.separatorChar + referenceImageName, doNotOverwrite);

		Detection detection = new Detection();
		differenceImageFullPath = comparisonImagePath + File.separatorChar + Constants.COMPARE_IMAGES_DIFFERENCE_IMAGENAME;
		List<Point> lDifferencePixelsInitial = detection.detectionByPerceptualImageDifferencing(referenceImagePath + File.separatorChar + referenceImageName, comparisonImagePath + File.separatorChar + comparisonImageName, differenceImageFullPath);
//		List<Point> lDifferencePixelsInitial = detection.detectionByPixelByPixelImageDifferencing(referenceImagePath + File.separatorChar + referenceImageName, comparisonImagePath + File.separatorChar + comparisonImageName);
		// filtration: check if the pixel is not in any special region
		List<Point> lDifferencePixels = new ArrayList<Point>();
		for (Point p : lDifferencePixelsInitial)
		{
			if (!isPixelInSpecialRegions(p.x, p.y))
			{
				lDifferencePixels.add(new Point(p.x, p.y));
			}
		}
		System.out.println("Filtered difference pixels after special regions = " + lDifferencePixels.size());

		// sample difference pixels if the size is too large
		if(lDifferencePixels.size() > 1000)
		{
			List<Point> lDifferencePixelsTemp = new ArrayList<Point>();
			for(int i = 0; i < lDifferencePixels.size(); i += 10)
			{
				lDifferencePixelsTemp.add(lDifferencePixels.get(i));
			}
			lDifferencePixels = lDifferencePixelsTemp;
		}
		
		// write filtered difference pixels
		FileUtils.copyFile(new File(differenceImageFullPath), new File(comparisonImagePath + File.separatorChar + "diff_filtered.png"));
		BufferedImage bi = ImageIO.read(new File(comparisonImagePath + File.separatorChar + Constants.DIFFERENCE_IMAGE_FILTERED_WITH_SPECIAL_REGIONS_IMAGENAME));

		Color myWhite = new Color(255, 255, 255); // Color white
		int rgb = myWhite.getRGB();
		Color myBlack = new Color(0, 0, 0); // Color black
		int rgb1 = myBlack.getRGB();

		for (int r = 0; r < bi.getWidth(); r++)
		{
			for (int c = 0; c < bi.getHeight(); c++)
			{
				bi.setRGB(r, c, rgb1);
			}
		}
		for (Point p : lDifferencePixels)
		{
			bi.setRGB(p.x, p.y, rgb);
		}
		ImageIO.write(bi, "png", new File(comparisonImagePath + File.separatorChar + Constants.DIFFERENCE_IMAGE_FILTERED_WITH_SPECIAL_REGIONS_IMAGENAME));

		System.out.println("Filtered difference pixels with a factor of 10 = " + lDifferencePixels.size());
		
		return lDifferencePixels;
	}

	public void buildRTree() throws SAXException, IOException
	{
		if (rTree == null)
		{
			WebDriverSingleton instance = WebDriverSingleton.getInstance();
			setDriver(instance.getDriver());
			instance.loadPage(savedHtmlFile.getAbsolutePath());
		}

		// build r-tree
		if (rTree == null)
		{
			this.rTree = new HtmlDomTree(getDriver(), savedHtmlFile.getAbsolutePath());
			this.rTree.buildHtmlDomTree();
		}
	}

	public Set<Node<HtmlElement>> localization(List<Point> lDifferencePixels, String reportFileName, boolean doNotOverwrite) throws IOException, SAXException
	{
		List<Point> cloneDifferencePixels = new ArrayList<Point>();
		for (Point pixel : lDifferencePixels)
		{
			cloneDifferencePixels.add(pixel);
		}

		Set<Node<HtmlElement>> errorElements = getErrorElements(cloneDifferencePixels, reportFileName, doNotOverwrite);
		Set<Node<HtmlElement>> refinedErrorElements = refineLocalization(errorElements);
		errorElements = refinedErrorElements;
		return errorElements;
	}

	private Set<Node<HtmlElement>> refineLocalization(Set<Node<HtmlElement>> errorElements)
	{
		Set<Node<HtmlElement>> previousRefinedErrorElements = new HashSet<Node<HtmlElement>>();
		Set<Node<HtmlElement>> refinedErrorElements = new HashSet<Node<HtmlElement>>(errorElements);

		// work list algorithm
		do
		{
			previousRefinedErrorElements = refinedErrorElements;
			refinedErrorElements = new HashSet<Node<HtmlElement>>();
			for (Node<HtmlElement> errorNode : previousRefinedErrorElements)
			{
				List<Node<HtmlElement>> errorNodeSiblings = errorNode.getNodeSiblings();

				// check if all the siblings are in the error elements result
				// set but not the parent
				boolean isAllSiblingsInError = true;
				if (errorNodeSiblings != null)
				{
					for (Node<HtmlElement> siblingNode : errorNodeSiblings)
					{
						if (!errorElements.contains(siblingNode))
						{
							isAllSiblingsInError = false;
							break;
						}
					}
					if (isAllSiblingsInError)
					{
						// add parent to the result set if all the siblings are
						// in the result but not the parent
						// add the parent even if the current element is its
						// only child
						if (!errorElements.contains(errorNode.getParent()))
						{
							refinedErrorElements.add(errorNode.getParent());
						}
					}
				}

				List<Node<HtmlElement>> errorNodeChildren = errorNode.getChildren();
				// if the error element has only one child, add child
				if(errorNodeChildren != null && errorNodeChildren.size() == 1)
				{
					refinedErrorElements.add(errorNodeChildren.get(0));
				}
				
				// special case of <ul>
				// if current element has <ul> as a child node, then add the
				// child to the result set
				if (errorNodeChildren != null && errorNodeChildren.size() > 0)
				{
					for (Node<HtmlElement> childNode : errorNodeChildren)
					{
						if (childNode.getData().getTagName().equalsIgnoreCase("ul") || childNode.getData().getTagName().equalsIgnoreCase("ol"))
						{
							if (!errorElements.contains(childNode))
							{
								refinedErrorElements.add(childNode);
							}
						}
					}
				}

				// special case of hidden
				// check if one sibling is hidden and other siblings after that
				// are in error, then add the hidden sibling to the result set
				if (errorNodeSiblings != null && errorNodeSiblings.size() > 0)
				{
					boolean isHidden = false;
					Node<HtmlElement> hiddenSibling = null;
					boolean isAllRemainingSiblingsInError = true;
					for (Node<HtmlElement> siblingNode : errorNodeSiblings)
					{
						if (siblingNode.getData().getHeight() <= 0 || siblingNode.getData().getWidth() <= 0)
						{
							hiddenSibling = siblingNode;
							isHidden = true;
							continue;
						}
						if (isHidden)
						{
							if (errorElements.contains(siblingNode))
							{
								isAllRemainingSiblingsInError = true;
							}
							else
							{
								isAllRemainingSiblingsInError = false;
								break;
							}
						}
					}

					if (isAllRemainingSiblingsInError && isHidden && !errorElements.contains(hiddenSibling))
					{
						refinedErrorElements.add(hiddenSibling);
					}
				}
				// check if the element has hidden children, then add hidden
				// children to the result set
				if (errorNodeChildren != null && errorNodeChildren.size() > 0)
				{
					for (Node<HtmlElement> childNode : errorNodeChildren)
					{
						if ((childNode.getData().getHeight() <= 0 || childNode.getData().getWidth() <= 0) && !errorElements.contains(childNode))
						{
							refinedErrorElements.add(childNode);
						}
					}
				}

				// special case of <option> and <select>
				// if current tag is option, add all siblings
				// if current tag is select, add all children (option tags)
				if (errorNode.getData().getTagName().equalsIgnoreCase("option"))
				{
					if (errorNodeSiblings != null && errorNodeSiblings.size() > 0)
					{
						for (Node<HtmlElement> siblingNode : errorNodeSiblings)
						{
							if (!errorElements.contains(siblingNode))
							{
								refinedErrorElements.add(siblingNode);
							}
						}
					}
				}
				if (errorNode.getData().getTagName().equalsIgnoreCase("select"))
				{
					if (errorNodeChildren != null && errorNodeChildren.size() > 0)
					{
						for (Node<HtmlElement> childNode : errorNodeChildren)
						{
							if (!errorElements.contains(childNode))
							{
								refinedErrorElements.add(childNode);
							}
						}
					}
				}
			}
			errorElements.addAll(refinedErrorElements);
		}
		while (!refinedErrorElements.isEmpty()); // check if the result has
													// stabilized

		return errorElements;
	}

	public Set<Point> specialRegionsProcessing() throws IOException, SAXException
	{
		SpecialRegionsProcessing srp = new SpecialRegionsProcessing();

		Set<Point> specialRegionsDifferencePixels = new HashSet<Point>();
		for (SpecialRegion sr : specialRegions)
		{
			specialRegionsDifferencePixels.addAll(srp.processSpecialRegions(comparisonImagePath + File.separatorChar + savedHtmlFile.getName(), comparisonImagePath + File.separatorChar + comparisonImageName, sr, rTree));
		}
		return specialRegionsDifferencePixels;
	}

	public List<Node<HtmlElement>> resultSetProcessing(Set<Node<HtmlElement>> errorElements, String reportFileName) throws IOException
	{
		ResultSetProcessing fr = new ResultSetProcessing();

		Node<HtmlElement> root = null;
		if (R_TREE_FLAG)
		{
			root = rTree.getRoot();
		}
		// Set<Node<HtmlElement>> elements =
		// fr.filterResultsWithHeuristic12(errorElements);
		Set<Node<HtmlElement>> elements = fr.computeHeuristics(errorElements, comparisonImagePath + File.separatorChar + comparisonImageName, referenceImagePath + File.separatorChar + referenceImageName);
		List<Node<HtmlElement>> filteredList = fr.decidePriority(elements, root, true, true, true, true, true);

		return filteredList;
	}

	public void rootCauseAnalysis(List<Node<HtmlElement>> processedResult, long startTime) throws IOException, InvalidConfigurationException
	{
		Logger resultsLog = Util.getNewLogger(comparisonImagePath + File.separatorChar + "RCA_results.txt", "RCA_results" + System.currentTimeMillis());
		Logger detailsLog = Util.getNewLogger(comparisonImagePath + File.separatorChar + "RCA_details.txt", "RCA_details" + System.currentTimeMillis());

		int numberOfDifferencePixels = Integer.MAX_VALUE;
		HtmlElement rootCauseElement = new HtmlElement();
		String rootCauseProperty = null;
		String fixValue = null;
		boolean fixFound = false;
		int count = 0;
		String fixString = "";
		int retValue = -1;
		for (Node<HtmlElement> node : processedResult)
		{
			if (isRCATimeoutInMins && Util.convertNanosecondsToSeconds(System.nanoTime() - startTime) > Constants.RCA_TIMEOUT_IN_MINS * 60)
			{
				detailsLog.info("");
				detailsLog.info("RCA timed out. Remaining elements cannot be checked.");
				break;
			}

			if (isRCAElementsCutoff && count >= Constants.RCA_WEBSEE_RANKING_BASED_ELEMENTS_CUTOFF)
			{
				detailsLog.info("");
				detailsLog.info("RCA terminated based on WebSee elements cutoff. Remaining elements cannot be checked.");
				break;
			}

			detailsLog.info("************************** Processing element " + count++ + " of " + processedResult.size() + " **************************");
			HtmlElement element = node.getData();
			RootCauseAnalysis rca = new RootCauseAnalysis(element, referenceImagePath + File.separatorChar + referenceImageName, savedHtmlFile.getAbsolutePath(), resultsLog, detailsLog, isRCANumericAnalysisRateOfChange);
			rca.runRootCauseAnalysis();
			retValue = rca.getReducedNumberOfDifferencePixels();

			if (retValue > numberOfDifferencePixels)
				continue;

			rootCauseElement = element;
			rootCauseProperty = rca.getRootCauseProperty();
			fixValue = rca.getFixValue();
			if (retValue == 0 || Util.isCurrentInAcceptableReductionThreshold(retValue, differencePixels.size()))
			{
				rootCauseElement = element;
				fixFound = true;
				fixString = (retValue == 0) ? " => exact" : " => acceptable";
				fixString = fixString + " root cause found!";
				break;
			}
			else if (retValue < numberOfDifferencePixels)
			{
				numberOfDifferencePixels = retValue;
			}
		}
		NumericAnalysis.resetTranslationValues();
		long endTime = System.nanoTime() - startTime;

		DecimalFormat decimal = new DecimalFormat("0.00");
		resultsLog.info("");
		resultsLog.info("===================================================================================");
		resultsLog.info("Final result is of element " + rootCauseElement.getXpath() + ", the visual property is " + rootCauseProperty + " with value " + fixValue + fixString + ". Number of difference pixels reduced from "
				+ differencePixels.size() + " to " + retValue);
		resultsLog.info("Total time = " + decimal.format(Util.convertNanosecondsToSeconds(endTime)) + " sec");

		detailsLog.info("");
		detailsLog.info("===================================================================================");
		detailsLog.info("Final result is of element " + rootCauseElement.getXpath() + ", the visual property is " + rootCauseProperty + " with value " + fixValue + fixString + ". Number of difference pixels reduced from "
				+ differencePixels.size() + " to " + retValue);
		detailsLog.info("Total time = " + decimal.format(Util.convertNanosecondsToSeconds(endTime)) + " sec");
	}

	public Map<Integer, List<Point>> clustering()
	{
		Map<Integer, List<Point>> retVal = new HashMap<Integer, List<Point>>();
		if(isClusteringToBeUsed)
		{
			DifferencePixelsClustering dpc = new DifferencePixelsClustering(differenceImageFullPath);
			retVal = dpc.performClustering(differencePixels);
		}
		else
		{
			// group all the difference pixels into one cluster
			retVal.put(0, differencePixels);
		}
		return retVal;
	}

	private void runWebSeeCore(String oracleImageName, String oracleImagePath, String testWebPagePath, String testHTMLFileName, String reportFileName, String specialRegionsFileFullPath, Logger log, boolean doNotOverwrite)
			throws IOException, SAXException, InvalidConfigurationException
	{
		savedHtmlFile = new File(testWebPagePath + File.separatorChar + testHTMLFileName);
		referenceImagePath = oracleImagePath;
		referenceImageName = oracleImageName;

		comparisonImagePath = testWebPagePath;
		comparisonImageName = Util.getFileNameAndExtension(testHTMLFileName)[0] + Constants.SCREENSHOT_FILE_EXTENSION;

		// compute difference text name (diff_oracle_test.txt)
		String tempDiffName[] = Util.getFileNameAndExtension(Constants.COMPARE_IMAGES_DIFFERENCE_TEXT_FILENAME, "_" + Util.getFileNameAndExtension(oracleImageName)[0] + "_" + Util.getFileNameAndExtension(testHTMLFileName)[0]);
		differenceText = tempDiffName[0] + tempDiffName[1];

		startTimeThisTestCase = System.nanoTime();

		// extract special regions information
		specialRegions = new HashSet<SpecialRegion>();
		if (specialRegionsFileFullPath != null && !specialRegionsFileFullPath.isEmpty() && new File(specialRegionsFileFullPath).exists())
		{
			SpecialRegion sr = new SpecialRegion(specialRegionsFileFullPath);
			specialRegions = sr.getSpecialRegions();
		}

		// ------------------------------------------------------------------------------------------------------
		DecimalFormat decimal = new DecimalFormat("0.00");

		System.out.println("Detection started");
		long startTime = System.nanoTime();
		System.out.println("Comparing images using PID");
		differencePixels = detection(doNotOverwrite);
		long endTime = System.nanoTime() - startTime;
		timeMapInSec.put("detection", Util.convertNanosecondsToSeconds(endTime));		

		long rTreeBuildStartTime = 0;
		long rTreeBuildEndTime = 0;
		if(specialRegionsFileFullPath != null && !specialRegionsFileFullPath.isEmpty())
		{
			rTreeBuildStartTime = System.nanoTime();
			if(differencePixels.size() > 0)
			{
				System.out.println("\nBuilding R-tree started");
				buildRTree();
				System.out.println("Building R-tree finished");
			}
			rTreeBuildEndTime = System.nanoTime() - rTreeBuildStartTime;
			timeMapInSec.put("rTreeBuildTime", Util.convertNanosecondsToSeconds(rTreeBuildEndTime));
			
			System.out.println("\nFiltering difference pixels belonging to special regions");
			startTime = System.nanoTime();
			Set<Point> specialRegionDifferencePixels = specialRegionsProcessing();
			differencePixels.addAll(specialRegionDifferencePixels);
			endTime = System.nanoTime() - startTime;
			timeMapInSec.put("specialRegionsProcessing", Util.convertNanosecondsToSeconds(endTime));
		}

		System.out.println("Clustering difference pixels using DBSCAN");
		startTime = System.nanoTime();
		Map<Integer, List<Point>> clusterDifferencePixelsMap = clustering();
		endTime = System.nanoTime() - startTime;
		timeMapInSec.put("clustering", Util.convertNanosecondsToSeconds(endTime));
		
		System.out.println("Number of clusters = " + clusterDifferencePixelsMap.size());
		System.out.println("Detection finished");
		
		if(specialRegionsFileFullPath == null || specialRegionsFileFullPath.isEmpty())
		{
			rTreeBuildStartTime = System.nanoTime();
			if(differencePixels.size() > 0)
			{
				System.out.println("\nBuilding R-tree started");
				buildRTree();
				System.out.println("Building R-tree finished");
			}
			rTreeBuildEndTime = System.nanoTime() - rTreeBuildStartTime;
			timeMapInSec.put("rTreeBuildTime", Util.convertNanosecondsToSeconds(rTreeBuildEndTime));
		}
		
		long localizationTotalTime = 0;
		long resultSetProcessingTotalTime = 0;
		for (Integer cid : clusterDifferencePixelsMap.keySet())
		{
			System.out.println("\nCluster " + (cid+1));
			System.out.println("\tLocalization started");
			startTime = System.nanoTime();
			Set<Node<HtmlElement>> errorElements = localization(clusterDifferencePixelsMap.get(cid), reportFileName, doNotOverwrite);
			endTime = System.nanoTime() - startTime;
			localizationTotalTime = localizationTotalTime + endTime;
			System.out.println("\tLocalization finished");

			System.out.println("\n\tResult set processing started");
			startTime = System.nanoTime();
			List<Node<HtmlElement>> processedResult = resultSetProcessing(errorElements, reportFileName);

			endTime = System.nanoTime() - startTime;
			
			resultSetProcessingTotalTime = resultSetProcessingTotalTime + endTime;
			System.out.println("\tResult set processing finished");
			clusterElementsMap.put(cid, processedResult);
		}
		timeMapInSec.put("localization", Util.convertNanosecondsToSeconds(localizationTotalTime));
		timeMapInSec.put("resultSetProcessing", Util.convertNanosecondsToSeconds(resultSetProcessingTotalTime));
		// ------------------------------------------------------------------------------------------------------
	}

	public void runWebSeeToolWithoutRCA(String oracleImageName, String oracleImagePath, String testWebPagePath, String testHTMLFileName, String reportFileName, String specialRegionsFileFullPath, Logger log, boolean doNotOverwrite)
			throws IOException, SAXException, InvalidConfigurationException
	{
		long startTime = System.nanoTime();
		runWebSeeCore(oracleImageName, oracleImagePath, testWebPagePath, testHTMLFileName, reportFileName, specialRegionsFileFullPath, log, doNotOverwrite);
		postProcessing(reportFileName);
		long endTime = System.nanoTime() - startTime;
		timeMapInSec.put("totalTime", Util.convertNanosecondsToSeconds(endTime));
	}

	private void postProcessing(String reportFileName) throws IOException
	{
		endTimeThisTestCase = System.nanoTime() - startTimeThisTestCase;

		WebDriverSingleton.closeDriver();

		String newFileName = Util.getNextAvailableFileName(comparisonImagePath, reportFileName);
		File out = new File(comparisonImagePath + File.separatorChar + newFileName);

		PrintWriter outWriter = new PrintWriter(new FileWriter(out));
		for (Integer cid : clusterElementsMap.keySet())
		{
			outWriter.println("\n\n******* CLUSTER " + (cid + 1) + " ***************");
			writeReportFile(clusterElementsMap.get(cid), outWriter);
			// markResultOnSavedHtml(clusterElementsMap.get(cid),
			// savedHtmlFile);
		}
		outWriter.println();
		outWriter.println();
		outWriter.println(Constants.RESULT_FILE_TIME_REQUIRED_LINE + Util.convertNanosecondsToSeconds(endTimeThisTestCase) + " " + Constants.TIME_REQUIRED_UNIT);
		outWriter.close();
	}

	public void runVisualInvariantsToolWithRCATimeout(String oracleImageName, String oracleImagePath, String testWebPagePath, String testHTMLFileName, String reportFileName, String specialRegionsFileFullPath, Logger log,
			boolean doNotOverwrite) throws IOException, SAXException, InvalidConfigurationException
	{
		runWebSeeCore(oracleImageName, oracleImagePath, testWebPagePath, testHTMLFileName, reportFileName, specialRegionsFileFullPath, log, doNotOverwrite);

		// System.out.println("Phase 5: Root cause analysis");
		long startTime = System.nanoTime();
		for (Integer cid : clusterElementsMap.keySet())
		{
			rootCauseAnalysis(clusterElementsMap.get(cid), startTime);
			long endTime = System.nanoTime() - startTime;
			if (log != null)
			{
				DecimalFormat decimal = new DecimalFormat("0.00");
				log.info("Phase 5: " + decimal.format(Util.convertNanosecondsToSeconds(endTime)) + "s");
			}
		}
		postProcessing(reportFileName);
	}

	public Set<Node<HtmlElement>> runWebSeeToolWithOnlyDetectionAndLocalization() throws IOException, SAXException
	{
		differencePixels = detection(true);
		buildRTree();
		Set<Node<HtmlElement>> errorElements = localization(differencePixels, "report.txt", true);
		WebDriverSingleton.closeDriver();
		return errorElements;
	}
	
	public List<Node<HtmlElement>> runWebSeeToolWithoutRCAWrapper()
	{
		try
		{
			runWebSeeCore(this.referenceImageName, this.referenceImagePath, this.comparisonImagePath, this.savedHtmlFile.getName(), "report.txt", "", null, false);
			postProcessing("websee_report.txt");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (SAXException e)
		{
			e.printStackTrace();
		}
		catch (InvalidConfigurationException e)
		{
			e.printStackTrace();
		}
		WebDriverSingleton.closeDriver();
		return clusterElementsMap.get(0);
	}
	
	public void runWebSeeTool() throws IOException, SAXException, InvalidConfigurationException
	{
		File file = new File("err.txt");
		FileOutputStream fos = new FileOutputStream(file);
		PrintStream ps = new PrintStream(fos);
		System.setErr(ps);
		
		long startTime = System.nanoTime();
		setConfig(true, false, false, true, true, true, false, "");
		runWebSeeCore(referenceImageName, referenceImagePath, comparisonImagePath, savedHtmlFile.getName(), "report.txt", specialRegionsFullPath, null, true);
		postProcessing("report.txt");
		long endTime = System.nanoTime() - startTime;
		timeMapInSec.put("totalTime", Util.convertNanosecondsToSeconds(endTime));
	}
	
	public boolean runWebSeeToolWithOnlyDetection() throws IOException
	{
		// extract special regions information
		specialRegions = new HashSet<SpecialRegion>();
		if (specialRegionsFullPath != null && !specialRegionsFullPath.isEmpty() && new File(specialRegionsFullPath).exists())
		{
			SpecialRegion sr = new SpecialRegion(specialRegionsFullPath);
			specialRegions = sr.getSpecialRegions();
		}
		
		differencePixels = detection(true);
		WebDriverSingleton.closeDriver();
		if(differencePixels.size() > 0)
		{
			return true;
		}
		return false;
	}
	
	public void runWebSeeToolWithNoClustering(String timeResultsFile, String outputFile) throws IOException, SAXException, InvalidConfigurationException
	{
		setClusteringToBeUsed(false);
		
		long startTime = System.nanoTime();
		runWebSeeCore(referenceImageName, referenceImagePath, comparisonImagePath, savedHtmlFile.getName(), "report.txt", specialRegionsFullPath, null, true);
		postProcessing("report.txt");
		long endTime = System.nanoTime() - startTime;
		timeMapInSec.put("totalTime", Util.convertNanosecondsToSeconds(endTime));
		
		// write time results to file
		PrintWriter outTime = new PrintWriter(new File(timeResultsFile));
		if(timeMapInSec.containsKey("detection") && timeMapInSec.containsKey("clustering"))
			outTime.println("detection time in sec = " + (timeMapInSec.get("detection") + timeMapInSec.get("clustering")));
		if(timeMapInSec.containsKey("localization") && timeMapInSec.containsKey("rTreeBuildTime"))
			outTime.println("localization time in sec = " + (timeMapInSec.get("localization") + timeMapInSec.get("rTreeBuildTime")));
		if(timeMapInSec.containsKey("resultSetProcessing"))
			outTime.println("resultSetProcessing time in sec = " + timeMapInSec.get("resultSetProcessing"));
		if(timeMapInSec.containsKey("totalTime"))
			outTime.println("total time in sec = " + timeMapInSec.get("totalTime"));
		outTime.close();
		
		// write clusterElementsMap.get(0) to output file
		PrintWriter outResults = new PrintWriter(new File(outputFile));
		if(clusterElementsMap.size() > 0)
		{
			for(Node<HtmlElement> n : clusterElementsMap.get(0))
			{
				outResults.println(n.getData().getXpath());
			}
		}
		outResults.close();
	}
}