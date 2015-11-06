package websee;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.openqa.selenium.Point;
import org.xml.sax.SAXException;

import util.Util;
import config.Constants;

public class SpecialRegionsProcessing
{
	public Set<Point> processSpecialRegions(String htmlPageFullPath, String oracleFullPath, SpecialRegion specialRegion, HtmlDomTree rTree) throws IOException, SAXException
	{
		if(specialRegion instanceof TextRegion)
		{
			return processTextRegion(htmlPageFullPath, oracleFullPath, (TextRegion) specialRegion, rTree);
		}
		else if(specialRegion instanceof ExclusionRegion)
		{
			return processExclusionRegion();
		}
		return null;
	}
	
	private Set<Point> processExclusionRegion()
	{
		return new HashSet<Point>();
	}
	
	private Set<Point> processTextRegion(String P, String O, TextRegion textRegion, HtmlDomTree rTree) throws IOException, SAXException
	{
		Rectangle A = textRegion.getRectangle();
		
		// copy HTML page P
		String[] PNameAndPath = Util.getPathAndFileNameFromFullPath(P);
		String newPFileName = Util.getNextAvailableFileName(PNameAndPath[0], Constants.TEXT_REGION_COPY_FILENAME);
		String newP = PNameAndPath[0] + File.separatorChar + newPFileName;
		File newPFile = new File(newP);
		newPFile.createNewFile();		
		FileUtils.copyFile(new File(P), newPFile);
		
		// find HTML elements contained in special region area
		Set<Node<HtmlElement>> SRE = new HashSet<Node<HtmlElement>>();
		for(int x = A.x; x <= (A.x + A.width); x++)
		{
			for(int y = A.y; y <= (A.y + A.height); y++)
			{
				List<Node<HtmlElement>> elements = rTree.searchRTreeByPoint(x, y);
				SRE.addAll(elements);
			}
		}
		
		// apply styling properties to elements in text region
		Document document = Jsoup.parse(newPFile, null);
		for(Node<HtmlElement> node : SRE)
		{
			Element element = Util.getElementFromXPathJava(node.getData().getXpath(), document);			
			String style = element.attr("style");
			
			for(String styleProperty : textRegion.getStyleProperties())
			{
				style = style + styleProperty;
			}
			element.attr("style", style);
		}
		document.outputSettings().prettyPrint(false);
		document.outputSettings().escapeMode(Entities.EscapeMode.extended);
		String html = document.html();		
		PrintWriter out = new PrintWriter(newPFile);
		out.print(html);
		out.close();
		
		// take new page screenshot
		String[] newPNameAndPath = Util.getPathAndFileNameFromFullPath(newP);
		String[] newPNameAndExtention = Util.getFileNameAndExtension(newPNameAndPath[1]);
		String imageName = Util.getNextAvailableFileName(newPNameAndPath[0], newPNameAndExtention[0] + Constants.SCREENSHOT_FILE_EXTENSION);
		Util.getScreenshot(newPNameAndPath[1], newPNameAndPath[0], imageName, null, Constants.DO_NOT_OVERWRITE);
		
		// call WebSee's detect and localize functions
		WebSeeTool vit = new WebSeeTool(O, newP);
		String[] ONameAndPath = Util.getPathAndFileNameFromFullPath(O);
		String diffText = Util.getNextAvailableFileName(newPNameAndPath[0], newPNameAndExtention[0] + "_" + Constants.COMPARE_IMAGES_DIFFERENCE_TEXT_FILENAME);

		vit.setReferenceImagePath(ONameAndPath[0]);
		vit.setReferenceImageName(ONameAndPath[1]);
		vit.setComparisonImagePath(newPNameAndPath[0]);
		vit.setComparisonImageName(imageName);
		vit.setDifferenceText(diffText);
		vit.setSavedHtmlFile(newPFile);
		
		List<Point> lDifferencePixels = vit.detection(false);
		Set<Point> differencePixelsSetTextRegion = new HashSet<Point>(lDifferencePixels);
		
		return differencePixelsSetTextRegion;
	}
}
