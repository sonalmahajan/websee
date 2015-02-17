package evalframework;

import java.util.ArrayList;

public class HtmlElementEval
{
	private String tagName;
	private ArrayList<String> htmlAttributes;
	private ArrayList<String> cssAttributes;
	private ArrayList<String> requiredElements;
	
	public HtmlElementEval()
	{
		htmlAttributes = new ArrayList<String>();
		cssAttributes = new ArrayList<String>();
		requiredElements = new ArrayList<String>();
	}
	
	public String getTagName()
	{
		return tagName;
	}
	public void setTagName(String tagName)
	{
		this.tagName = tagName;
	}
	public ArrayList<String> getHtmlAttributes()
	{
		return htmlAttributes;
	}
	public void setHtmlAttributes(ArrayList<String> htmlAttributes)
	{
		this.htmlAttributes = htmlAttributes;
	}
	public ArrayList<String> getCssAttributes()
	{
		return cssAttributes;
	}
	public void setCssAttributes(ArrayList<String> cssAttributes)
	{
		this.cssAttributes = cssAttributes;
	}
	public ArrayList<String> getRequiredElements()
	{
		return requiredElements;
	}
	public void setRequiredElements(ArrayList<String> requiredElements)
	{
		this.requiredElements = requiredElements;
	}
	@Override
	public String toString()
	{
		return "HtmlElement [tagName=" + tagName + ", htmlAttributes=" + htmlAttributes + ", cssAttributes=" + cssAttributes + ", requiredElements=" + requiredElements + "]";
	}
}