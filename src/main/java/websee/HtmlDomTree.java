package websee;

import gnu.trove.TIntProcedure;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.xml.sax.SAXException;

import util.Util;

import com.infomatiq.jsi.Point;
import com.infomatiq.jsi.Rectangle;
import com.infomatiq.jsi.SpatialIndex;
import com.infomatiq.jsi.rtree.RTree;

import config.Constants;

public class HtmlDomTree
{
	private Node<HtmlElement> root;
	private SpatialIndex spatialIndex;
	private Map<Integer, Rectangle> rects;
	private int rectId;
	private Map<Integer, Node<HtmlElement>> rectIdHtmlDomTreeNodeMap;
	private CSSParser cssParser;
	private HtmlAttributesParser htmlAttributesParser;

	public HtmlDomTree(WebDriver driver, String htmlFileFullPath) throws SAXException, IOException
	{
		// parse CSS
		cssParser = new CSSParser(htmlFileFullPath);
		cssParser.parseCSS();
		
		// parse HTML attributes
		htmlAttributesParser = new HtmlAttributesParser(htmlFileFullPath);
		
		WebElement rootElementFromSelenium = driver.findElement(By.xpath("//*"));
		HtmlElement htmlRootElement = new HtmlElement();
		int x = rootElementFromSelenium.getLocation().x;
		int y = rootElementFromSelenium.getLocation().y;
		int w = rootElementFromSelenium.getSize().width;
		int h = rootElementFromSelenium.getSize().height;
				
		htmlRootElement.setSeleniumWebElement(rootElementFromSelenium);
		htmlRootElement.setTagName(rootElementFromSelenium.getTagName());
		htmlRootElement.setX(x);
		htmlRootElement.setY(y);
		htmlRootElement.setWidth(w);
		htmlRootElement.setHeight(h);
		this.root = new Node<HtmlElement>(null, htmlRootElement);
		htmlRootElement.setXpath(computeXpath(this.root));
		htmlRootElement.setHtmlAttributes(htmlAttributesParser.getHTMLAttributesForElement(htmlRootElement.getXpath()));
		try
		{
			htmlRootElement.setCssProperties(cssParser.getCSSPropertiesForElement(htmlRootElement.getXpath()));
		}
		catch (XPathExpressionException e)
		{
			e.printStackTrace();
		}
		
		htmlRootElement.setRectId(rectId);
		
		// Create and initialize an rtree
		spatialIndex = new RTree();
		spatialIndex.init(null);
		rects = new HashMap<Integer, Rectangle>();
		rectIdHtmlDomTreeNodeMap = new HashMap<Integer, Node<HtmlElement>>();
		
		Rectangle r = new Rectangle(x, y, x+w, y+h);
		rects.put(rectId, r);
		rectIdHtmlDomTreeNodeMap.put(rectId, root);
		spatialIndex.add(r, rectId++);
	}

	public Node<HtmlElement> getRoot()
	{
		return root;
	}

	public void setRoot(Node<HtmlElement> root)
	{
		this.root = root;
	}

	public void buildHtmlDomTree()
	{
		buildHtmlDomTreeFromNode(this.root);
		//postOrderTraversalForAdjustingElementSize(this.root);
	}
	
	private void buildHtmlDomTreeFromNode(Node<HtmlElement> node)
	{
		try
		{
			List<WebElement> children = node.getData().getSeleniumWebElement().findElements(By.xpath("*"));
			for (WebElement child : children)
			{
				int x = child.getLocation().x;
				int y = child.getLocation().y;
				int w = child.getSize().width;
				int h = child.getSize().height;
				
				// adjust size of option to that of the parent (select)
				if(child.getTagName().equals("option"))
				{
					if(node.getData().getTagName().equals("select"))
					{
						x = node.getData().getX();
						y = node.getData().getY();
					}
				}
				
				// don't process elements with no visual impact
				//if(x >= 0 && y >= 0 && w > 0 && h > 0)
				if(!Arrays.asList(Constants.NON_VISUAL_TAGS).contains(child.getTagName()))
				{
					HtmlElement newChild = new HtmlElement();
					
					// set tag name
					newChild.setTagName(child.getTagName());
					
					// set id
					newChild.setId(child.getAttribute("id"));
					
					// set web element
					newChild.setSeleniumWebElement(child);
					
					// set rectangle information
					newChild.setX(x);
					newChild.setY(y);
					newChild.setWidth(w);
					newChild.setHeight(h);
					
					Node<HtmlElement> newNode = new Node<HtmlElement>(node, newChild);
					// set xpath by traversing the built html dom tree
					newChild.setXpath(computeXpath(newNode));

					// set css properties
					newChild.setCssProperties(cssParser.getCSSPropertiesForElement(newChild.getXpath()));
					
					// set html attributes
					newChild.setHtmlAttributes(htmlAttributesParser.getHTMLAttributesForElement(newChild.getXpath()));
					
					newChild.setRectId(rectId);
					rectIdHtmlDomTreeNodeMap.put(rectId, newNode);
					
					Rectangle r = new Rectangle(x, y, x+w, y+h);
					rects.put(rectId, r);
					spatialIndex.add(r, rectId++);
					
					buildHtmlDomTreeFromNode(newNode);
				}
			}
		}
		catch (NoSuchElementException e)
		{
			return;
		}
		catch (XPathExpressionException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void postOrderTraversalForAdjustingElementSize(Node<HtmlElement> node)
	{
		if (node == null)
		{
			return;
		}
		
		if (node.getChildren() != null)
		{
			for (Node<HtmlElement> child : node.getChildren())
			{
				postOrderTraversalForAdjustingElementSize(child);
			}
		}
		
		int x = node.getData().getX();
		int y = node.getData().getY();
		int width = node.getData().getWidth();
		int height = node.getData().getHeight();
		
		// adjust node rectangle from siblings
/*		List<Node<HtmlElement>> allSiblings = node.getNodeSiblings();
		
		if(allSiblings != null && node.getData().getXpath().equals("/html/body/div/div[2]"))
		{
			for(Node<HtmlElement> sibling : allSiblings)
			{
				HtmlElement siblingElement = sibling.getData();
				System.out.println("sibling: " + siblingElement + "\n");

				int newX = x + 1;
				int newY = y + 1;
				int newWidth = width - 2;
				int newHeight = height - 2;
				
				// left top
				if(isPointInRectangle(newX, newY, siblingElement.getX(), siblingElement.getY(), siblingElement.getWidth(), siblingElement.getHeight(), false))
				{
					if(newX < (siblingElement.getX() + siblingElement.getWidth()))
					{
						x = siblingElement.getX() + siblingElement.getWidth() + 1;
						node.getData().setX(x);
					}
					if(newY < (siblingElement.getY() + siblingElement.getHeight()))
					{
						y = siblingElement.getY() + siblingElement.getHeight() + 1;
						node.getData().setY(y);
					}
				}
				
				// right top
				if(isPointInRectangle((newX + newWidth), newY, siblingElement.getX(), siblingElement.getY(), siblingElement.getWidth(), siblingElement.getHeight(), false))
				{
					if((newX + newWidth) > siblingElement.getX())
					{
						width = width - ((x + width) - siblingElement.getX()) - 1;
						node.getData().setWidth(width);
					}
					if(newY < (siblingElement.getY() + siblingElement.getHeight()))
					{
						y = siblingElement.getY() + siblingElement.getHeight() + 1;
						node.getData().setY(y);
					}
				}
				
				// left bottom
				if(isPointInRectangle(newX, (newY + newHeight), siblingElement.getX(), siblingElement.getY(), siblingElement.getWidth(), siblingElement.getHeight(), false))
				{
					if(newX < (siblingElement.getX() + siblingElement.getWidth()))
					{
						x = siblingElement.getX() + siblingElement.getWidth() + 1;
						node.getData().setX(x);
					}
					if((newY + newHeight) > siblingElement.getY())
					{
						height = height - ((y + height) - siblingElement.getY()) - 1;
						node.getData().setHeight(height);
					}
				}

				// right bottom
				if(isPointInRectangle((newX + newWidth), (newY + newHeight), siblingElement.getX(), siblingElement.getY(), siblingElement.getWidth(), siblingElement.getHeight(), false))
				{
					if((newX + newWidth) > siblingElement.getX())
					{
						width = width - ((x + width) - siblingElement.getX()) - 1;
						node.getData().setWidth(width);
					}
					if((newY + newHeight) > siblingElement.getY())
					{
						height = height - ((y + height) - siblingElement.getY()) - 1;
						node.getData().setHeight(height);
					}
				}
			}
		}
*/		
/*		if(allSiblings != null)
		{
			for(Node<HtmlElement> sibling : allSiblings)
			{
				HtmlElement siblingElement = sibling.getData();
				
				// check if horizontal sibling
				if(y == siblingElement.getY())
				{
					// match height
					if(height < siblingElement.getHeight())
					{
						node.getData().setHeight(siblingElement.getHeight());
					}
				}
				else // vertical sibling
				{
					// match width
					if(width < siblingElement.getWidth())
					{
						node.getData().setWidth(siblingElement.getWidth());
					}
				}
			}
		}
*/
		Node<HtmlElement> parent = node.getParent();
		
		// adjust size of option to that of the parent (select)
		if(node.getData().getTagName().equals("option"))
		{
			if(parent.getData().getTagName().equals("select"))
			{
				x = parent.getData().getX();
				y = parent.getData().getY();
				
				node.getData().setX(x);
				node.getData().setY(y);
			}
		}
		
		// adjust parent rectangle
/*		if(parent == null)
			return;
		
		HtmlElement parentElement = node.getParent().getData();
		
		if(width > 0 && height > 0)
		{
			if(parentElement.getX() > x)
			{
				parentElement.setX(x);
				parent.setData(parentElement);
			}
			if(parentElement.getY() > y)
			{
				parentElement.setY(y);
				parent.setData(parentElement);
			}
			if((parentElement.getX() + parentElement.getWidth()) < (x + width))
			{
				parentElement.setWidth(parentElement.getWidth() + ((x + width) - (parentElement.getX() + parentElement.getWidth())));
				parent.setData(parentElement);
			}
			if((parentElement.getY() + parentElement.getHeight()) < (y + height))
			{
				parentElement.setHeight(parentElement.getHeight() + ((y + height) - (parentElement.getY() + parentElement.getHeight())));
				parent.setData(parentElement);
			}
		}*/
	}
	
	/**
	 * 	compute xpath of the invoking element from the root
	 */
	private String computeXpath(Node<HtmlElement> node)
	{
		//HtmlElement element = node.getData();
		/*if(element != null && element.getId() != null && !element.getId().isEmpty())
		{
			return "//*[@id=\"" + element.getId() + "\"]";
		}
		else
		{*/
			return getElementTreeXPath(node);
		//}
	}
	
	private static String getElementTreeXPath(Node<HtmlElement> node)
	{
		ArrayList<String> paths = new ArrayList<String>();
		for(; node != null ; node = node.getParent())
		{
			HtmlElement element = node.getData();
			int index = 0;
			/*if(node.getData().getId() != null && !node.getData().getId().isEmpty())
			{
				paths.add("/*[@id=\"" + node.getData().getId() + "\"]");
				break;
			}*/
			
			int siblingIndex = node.getCurrentNodeSiblingIndex();
			for(Node<HtmlElement> sibling = node.getSiblingNodeAtIndex(--siblingIndex) ; sibling != null ; sibling = node.getSiblingNodeAtIndex(--siblingIndex))
			{
				if(sibling.getData().getTagName().equals(element.getTagName()))
				{
					++index;
				}
			}
			String tagName = element.getTagName().toLowerCase();
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
	
	public List<Node<HtmlElement>> searchRTreeByPoint(int x, int y)
	{
		final List<Node<HtmlElement>> resultSet = new ArrayList<Node<HtmlElement>>();
		final List<Integer> resultRectIds = new ArrayList<Integer>();
		
		final Point p = new Point(x, y);
		spatialIndex.nearest(p, new TIntProcedure()
		{
			public boolean execute(int i)
			{
				resultRectIds.add(i);
				return true;
			}
		}, Float.MAX_VALUE);
		
		// filter result set based on containment relationship
		for(Integer id : resultRectIds)
		{
			//System.out.println(resultRectIds);
			List<Integer> containedElementsRectIds = getContainedElements(id);
			
			for (Integer cid : containedElementsRectIds)
			{
				HtmlElement containingElement = rectIdHtmlDomTreeNodeMap.get(id).getData();
				HtmlElement containedElement = rectIdHtmlDomTreeNodeMap.get(cid).getData();
				
				// check if the containing and contained element don't have the same size
				if(resultRectIds.contains(cid) && 
						containingElement.getX() != containedElement.getX() &&
						containingElement.getY() != containedElement.getY() &&
						containingElement.getWidth() != containedElement.getWidth() &&
						containingElement.getHeight() != containedElement.getHeight() && cid > id)
				{
					//System.out.println("rect " + id + " contains rect " + cid);
					// keep contained element, remove containing element
					int index = resultRectIds.indexOf(id);
					resultRectIds.set(index, -1);
					break;
				}
			}
		}
		
		// clean results
		for(Integer id : resultRectIds)
		{
			if(id != -1)
			{
				resultSet.add(rectIdHtmlDomTreeNodeMap.get(id));
			}
		}
		
		// further filter the results based on xpath containment
		// this is necessary because there can be some children which are outside parent
		// causing both the children and parent to be reported in error
		Map<Integer, String> xpaths = new HashMap<Integer, String>();
		for(Node<HtmlElement> node : resultSet)
		{
			xpaths.put(node.getData().getRectId(), node.getData().getXpath());
		}
		
		for(Integer key : xpaths.keySet())
		{
			for(Integer key2 : xpaths.keySet())
			{
				// check that it not the same element itself
				if(key != key2 && xpaths.get(key2) != null && xpaths.get(key) != null && xpaths.get(key2).contains(xpaths.get(key)))
				{
					HtmlElement ele1 = rectIdHtmlDomTreeNodeMap.get(key).getData();
					HtmlElement ele2 = rectIdHtmlDomTreeNodeMap.get(key2).getData();
					
					if(ele1.getX() != ele2.getX() && ele1.getY() != ele2.getY() && 
							ele1.getWidth() != ele2.getWidth() && ele1.getHeight() != ele2.getHeight())
					{
						xpaths.put(key, null);
						break;
					}
				}
			}
		}
		
		List<Node<HtmlElement>> finalResultSet = new ArrayList<Node<HtmlElement>>();
		for(Integer key : xpaths.keySet())
		{
			if(xpaths.get(key) != null)
			{
				finalResultSet.add(rectIdHtmlDomTreeNodeMap.get(key));
			}
		}

		return finalResultSet;
	}
	
	private List<Integer> getContainedElements(final int rectId)
	{
		final List<Integer> resultRectIds = new ArrayList<Integer>();
		spatialIndex.contains(rects.get(rectId), new TIntProcedure()
		{
			public boolean execute(int i)
			{
				if(i != rectId)
				{
					resultRectIds.add(i);
				}
				return true;
			}
		});
		return resultRectIds;
	}
	
	public Node<HtmlElement> searchHtmlDomTreeByRectId(int rectId)
	{
		Queue<Node<HtmlElement>> q = new LinkedList<Node<HtmlElement>>();
		q.add(this.root);
		
		while(!q.isEmpty())
		{
			Node<HtmlElement> node = q.remove();
			if(node.getData().getRectId() == rectId)
			{
				return node;
			}
			if (node.getChildren() != null)
			{
				for (Node<HtmlElement> child : node.getChildren())
				{
					q.add(child);
				}
			}
		}
		return null;
	}
	
	public Node<HtmlElement> searchHtmlDomTreeByNode(Node<HtmlElement> searchNode)
	{
		Queue<Node<HtmlElement>> q = new LinkedList<Node<HtmlElement>>();
		q.add(this.root);
		
		while(!q.isEmpty())
		{
			Node<HtmlElement> node = q.remove();
			if(node.equals(searchNode))
			{
				return node;
			}
			if (node.getChildren() != null)
			{
				for (Node<HtmlElement> child : node.getChildren())
				{
					q.add(child);
				}
			}
		}
		return null;
	}
	
	public HtmlElement searchHtmlDomTreeByXpath(String xpath)
	{
		Queue<Node<HtmlElement>> q = new LinkedList<Node<HtmlElement>>();
		q.add(this.root);
		
		while(!q.isEmpty())
		{
			Node<HtmlElement> node = q.remove();
			if(node.getData().getXpath().equalsIgnoreCase(xpath))
			{
				return node.getData();
			}
			if (node.getChildren() != null)
			{
				for (Node<HtmlElement> child : node.getChildren())
				{
					q.add(child);
				}
			}
		}
		return null;
	}
	
	public Node<HtmlElement> searchHtmlDomTreeByPoint(int x, int y)
	{
		return searchHtmlDomTreeByPoint(this.root, x, y);
	}
	
	public Node<HtmlElement> searchHtmlDomTreeByPoint(Node<HtmlElement> node, int x, int y)
	{
		//traverse in pre-order 
		//for visit, check if the node contains this point
		//if yes, go to children
		//if node is leaf and contains the point return node
		//else return parent

		HtmlElement element = node.getData();
		if(node.getChildren() == null && Util.isPointInRectangle(x, y, element.getX(), element.getY(), element.getWidth(), element.getHeight(), true))
		{
			return node;
		}
		else
		{
			if (node.getChildren() != null)
			{
				for (Node<HtmlElement> child : node.getChildren())
				{
					if(Util.isPointInRectangle(x, y, child.getData().getX(), child.getData().getY(), child.getData().getWidth(), child.getData().getHeight(), true))
					{
						node = searchHtmlDomTreeByPoint(child, x, y);
						return node;
					}
				}
			}
			return node;
		}
	}
	
	public void preOrderTraversalRTree()
	{
		preOrderTraversalRTree(this.root);
	}
	
	private void preOrderTraversalRTree(Node<HtmlElement> node)
	{
		if (node == null)
		{
			return;
		}
		System.out.println(node.getData().getTagName() + ": " + node.getData());
		if (node.getChildren() != null)
		{
			for (Node<HtmlElement> child : node.getChildren())
			{
				preOrderTraversalRTree(child);
			}
		}
	}	
	
	public void drawRectangles(String imageFileName, String path) throws IOException
	{
		String fileName[] = Util.getFileNameAndExtension(imageFileName, "_rect");
		String newFileName = Util.getNextAvailableFileName(path, fileName[0] + fileName[1]);
		FileUtils.copyFile(new File(path + File.separatorChar + imageFileName), new File(path + File.separatorChar + newFileName));
		drawRectangles(newFileName, path, this.root);
	}
	
	private void drawRectangles(String imageFileName, String path, Node<HtmlElement> node) throws IOException
	{
		if (node == null)
		{
			return;
		}
		
		HtmlElement element = node.getData();
		Util.drawRectangleOnImage(imageFileName, path, element.getX(), element.getY(), element.getWidth(), element.getHeight());
		
		if (node.getChildren() != null)
		{
			for (Node<HtmlElement> child : node.getChildren())
			{
				drawRectangles(imageFileName, path, child);
			}
		}
	}
	
	public static void main(String[] args) throws SAXException, IOException
	{
		WebDriverSingleton instance = WebDriverSingleton.getInstance();
		instance.loadPage("C:\\USC\\visual_checking\\evaluation\\inconsistency\\page1.html");
		WebDriver driver = instance.getDriver();
		
		//WebDriver driver = new FirefoxDriver();
		//driver.manage().window().maximize();
		//driver.get("file:///C:\\USC\\visual_checking\\evaluation\\root_cause_analysis\\color\\test.html");
		HtmlDomTree rt = new HtmlDomTree(driver, "C:\\USC\\visual_checking\\evaluation\\inconsistency\\page1.html");
		rt.buildHtmlDomTree();
		rt.preOrderTraversalRTree();
		//rt.searchRTreeByPoint(150, 766);
		/*JavascriptExecutor js = (JavascriptExecutor) driver;
		WebElement e = Util.getElementFromCoordinates(js, 215, 768);
		System.out.println(e.getTagName());
		System.out.println(e.getText());
		System.out.println(Util.getElementXPath(js, e));*/
		//rt.drawRectangles("test.gif", "C:\\USC\\visual_checking\\demo");
//		rt.preOrderTraversalRTree();
//		driver.close();
		//driver.quit();
		WebDriverSingleton.closeDriver();
	}
}
