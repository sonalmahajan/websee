package websee;

import java.text.DecimalFormat;
import java.util.Map;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import util.Util;

public class HtmlElement
{
	private String xpath;
	private String tagName;
	private String id;
	private WebElement seleniumWebElement;
	
	// location of top left hand corner	
	private int x;
	private int y;
	
	// rectangular dimensions
	private int width;
	private int height;

	// to be used later to find root cause
	private Map<String, String> cssProperties;
	private Map<String, String> htmlAttributes;

	// R-tree rectangle id
	private int rectId;

	// for priority calculation
	private int numberOfDifferencePixels;
	private int cascadingError = 0;
	private double cascadingDisplacement;
	private double score;
	private int heuristic1 = 0;
	private int heuristic2 = 0;
	private double ratio = 0.0;
	private double positionInDOMTree = 0;
	
	public HtmlElement() {}
	
	public HtmlElement(JavascriptExecutor js, WebElement e)
	{
		this.xpath = Util.getElementXPath(js, e);
		this.tagName = e.getTagName();
		this.id = e.getAttribute("id");
		this.seleniumWebElement = e;
		this.x = e.getLocation().x;
		this.y = e.getLocation().y;
		this.width = e.getSize().width;
		this.height = e.getSize().height;
	}
	
	public String getXpath()
	{
		return xpath;
	}
	public void setXpath(String xpath)
	{
		this.xpath = xpath;
	}
	public String getTagName()
	{
		return tagName;
	}
	public void setTagName(String tagName)
	{
		this.tagName = tagName;
	}
	public String getId()
	{
		return id;
	}
	public void setId(String id)
	{
		this.id = id;
	}
	public WebElement getSeleniumWebElement()
	{
		return seleniumWebElement;
	}
	public void setSeleniumWebElement(WebElement seleniumWebElement)
	{
		this.seleniumWebElement = seleniumWebElement;
	}
	public int getX()
	{
		return x;
	}
	public void setX(int x)
	{
		this.x = x;
	}
	public int getY()
	{
		return y;
	}
	public void setY(int y)
	{
		this.y = y;
	}
	public int getWidth()
	{
		return width;
	}
	public void setWidth(int width)
	{
		this.width = width;
	}
	public int getHeight()
	{
		return height;
	}
	public void setHeight(int height)
	{
		this.height = height;
	}
	public Map<String, String> getCssProperties()
	{
		return cssProperties;
	}
	public void setCssProperties(Map<String, String> cssProperties)
	{
		this.cssProperties = cssProperties;
	}
	public Map<String, String> getHtmlAttributes()
	{
		return htmlAttributes;
	}
	public void setHtmlAttributes(Map<String, String> htmlAttributes)
	{
		this.htmlAttributes = htmlAttributes;
	}
	public int getRectId()
	{
		return rectId;
	}
	public void setRectId(int rectId)
	{
		this.rectId = rectId;
	}

	public int getNumberOfDifferencePixels()
	{
		return numberOfDifferencePixels;
	}

	public void setNumberOfDifferencePixels(int numberOfDifferencePixels)
	{
		this.numberOfDifferencePixels = numberOfDifferencePixels;
	}

	public int getCascadingError()
	{
		return cascadingError;
	}

	public void setCascadingError(int cascadingError)
	{
		this.cascadingError = cascadingError;
	}

	public double getCascadingDisplacement()
	{
		return cascadingDisplacement;
	}

	public void setCascadingDisplacement(double cascadingDisplacement)
	{
		this.cascadingDisplacement = cascadingDisplacement;
	}

	public double getScore()
	{
		return score;
	}

	public void setScore(double score)
	{
		this.score = score;
	}

	public int getHeuristic1()
	{
		return heuristic1;
	}

	public void setHeuristic1(int heuristic1)
	{
		this.heuristic1 = heuristic1;
	}

	public int getHeuristic2()
	{
		return heuristic2;
	}

	public void setHeuristic2(int heuristic2)
	{
		this.heuristic2 = heuristic2;
	}

	public double getRatio()
	{
		return ratio;
	}

	public void setRatio(double ratio)
	{
		this.ratio = ratio;
	}

	public double getPositionInDOMTree()
	{
		return positionInDOMTree;
	}

	public void setPositionInDOMTree(double positionInDOMTree)
	{
		this.positionInDOMTree = positionInDOMTree;
	}

	@Override
	public String toString()
	{
		return "HtmlElement [xpath=" + xpath + ", tagName=" + tagName + ", id=" + id + ", seleniumWebElement=" + seleniumWebElement + ", x=" + x + ", y=" + y + ", width=" + width + ", height=" + height + ", cssProperties=" + cssProperties
				+ ", htmlAttributes=" + htmlAttributes + ", rectId=" + rectId + ", numberOfDifferencePixels=" + numberOfDifferencePixels + ", isCascadingError=" + cascadingError + ", cascadingDisplacement=" + cascadingDisplacement
				+ ", score=" + score + ", heuristic1=" + heuristic1 + ", heuristic2=" + heuristic2 + ", positionInDOMTree=" + positionInDOMTree + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((xpath == null) ? 0 : xpath.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		HtmlElement other = (HtmlElement) obj;
		if (xpath == null)
		{
			if (other.xpath != null)
				return false;
		}
		else if (!xpath.equals(other.xpath))
			return false;
		return true;
	}

	public String printInformation()
	{
		String print = "";
		DecimalFormat decimalNumber = new DecimalFormat("0.00");
		if(cascadingError == 1)
		{
			print = print + "cascading, ";
		}
		print = print + "ratio = " + numberOfDifferencePixels + "/" + (width * height) + " = " + decimalNumber.format(ratio);
		print = print + ", h1 = " + heuristic1;
		print = print + ", h2 = " + heuristic2;
		print = print + ", DOM tree position = " + positionInDOMTree;
		print = print + ", score = " + decimalNumber.format(score);
		return print;
	}
}
