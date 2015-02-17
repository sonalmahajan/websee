package websee;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.openqa.selenium.WebDriver;
import org.xml.sax.SAXException;

import config.Constants;

public class ResultSetProcessing
{
	public Set<Node<HtmlElement>> filterResultsWithHeuristic1(Set<Node<HtmlElement>> errorElements) throws IOException
	{
		List<String> elementsToRemove = new ArrayList<String>();
		
		for (Node<HtmlElement> node : errorElements)
		{
			List<Node<HtmlElement>> children = node.getChildren();

			if (children != null && children.size() > 1)
			{
				int childCount = 0;
				for (Node<HtmlElement> nodeTemp : errorElements)
				{
					for(Node<HtmlElement> child : children)
					{
						if (nodeTemp.getData().getXpath().equals(child.getData().getXpath()))
						{
							childCount++;
						}
					}
				}
				if (childCount == children.size())
				{
					// remove children, keep only parent
					for (Node<HtmlElement> c : children)
					{
						//elementsToRemove.add(c.getData().getXpath());
						c.getData().setHeuristic1(1);
					}
				}
			}
		}
		return cleanList(errorElements, elementsToRemove);
	}
	
	public Set<Node<HtmlElement>> filterResultsWithHeuristic2(Set<Node<HtmlElement>> errorElements) throws IOException
	{
		List<String> elementsToRemove = new ArrayList<String>();
		
		for (Node<HtmlElement> node : errorElements)
		{
			List<Node<HtmlElement>> children = node.getChildren();

			if (children != null && children.size() > 1)
			{
				int childCount = 0;
				for (Node<HtmlElement> nodeTemp : errorElements)
				{
					for(Node<HtmlElement> child : children)
					{
						if (nodeTemp.getData().getXpath().equals(child.getData().getXpath()))
						{
							childCount++;
						}
					}
				}
				if (childCount > 0 && childCount < children.size())
				{
					// keep children in the list, remove parent
					//elementsToRemove.add(node.getData().getXpath());
					node.getData().setHeuristic2(1);
				}
			}
		}
		return cleanList(errorElements, elementsToRemove);
	}
	
	public Set<Node<HtmlElement>> filterResultsWithHeuristic12(Set<Node<HtmlElement>> errorElements) throws IOException
	{
		Set<Node<HtmlElement>> heuristic1 = filterResultsWithHeuristic1(errorElements);
		Set<Node<HtmlElement>> heuristic2 = filterResultsWithHeuristic2(errorElements);
		
		// get intersection of heuristic1 and heuristic2
		List<String> elementsToRemove = new ArrayList<String>();
		for(Node<HtmlElement> node : errorElements)
		{
			// remove element from the set which is not part of the intersection
			boolean presentInHeuristic1 = false;
			boolean presentInHeuristic2 = false;
			
			for(Node<HtmlElement> h1 : heuristic1)
			{
				if(node.getData().getXpath().equals(h1.getData().getXpath()))
				{
					presentInHeuristic1 = true;
					break;
				}
			}
			
			for(Node<HtmlElement> h2 : heuristic2)
			{
				if(node.getData().getXpath().equals(h2.getData().getXpath()))
				{
					presentInHeuristic2 = true;
					break;
				}
			}
			
			if(!(presentInHeuristic1 && presentInHeuristic2))
			{
				//elementsToRemove.add(node.getData().getXpath());
				node.getData().setHeuristic1(0);
				node.getData().setHeuristic2(0);
			}
		}
		return cleanList(errorElements, elementsToRemove);
	}
	
	/**
	 * remove elements in elementsToRemove from the given list
	 * @param list
	 * @param elementsToRemove
	 * @return
	 */
	private Set<Node<HtmlElement>> cleanList(Set<Node<HtmlElement>> elements, List<String> elementsToRemove)
	{
		Set<Node<HtmlElement>> temp = new LinkedHashSet<Node<HtmlElement>>();
		for (Node<HtmlElement> e : elements)
		{
			if (!elementsToRemove.contains(e.getData().getXpath()))
			{
				temp.add(e);
			}
		}
		return temp;
	}
	
	public Set<Node<HtmlElement>> computeHeuristics(Set<Node<HtmlElement>> errorElements, final String testImageFullPath, final String oracleImageFullPath) throws IOException
	{
		errorElements = filterResultsWithHeuristic1(errorElements);
		errorElements = filterResultsWithHeuristic2(errorElements);
		for(Node<HtmlElement> node : errorElements)
		{
			HtmlElement element = node.getData();
			element.setCascadingError(isCascadingError(element, testImageFullPath, oracleImageFullPath) ? 1 : 0);
		
			Double ratio = element.getNumberOfDifferencePixels() / (double) (element.getWidth() * element.getHeight());
			if(ratio > 1.0)
			{
				ratio = 1.0;
			}
			if((double) (element.getWidth() * element.getHeight()) == 0.0)
			{
				ratio = 0.0;
			}
			element.setRatio(ratio);
		}
		return errorElements;
	}
	
	/**
	 * @param errorElements
	 * @param testImageFullPath
	 * @param oracleImageFullPath
	 * @param root
	 * @param isH1On: contained elements heuristic
	 * @param isH2On: overlapped elements heuristic
	 * @param isH3On: cascading heuristic
	 * @param isH4On: pixels ratio heuristic
	 * @return
	 * @throws IOException
	 */
	public List<Node<HtmlElement>> decidePriority(Set<Node<HtmlElement>> errorElements, Node<HtmlElement> root, boolean isH1On, boolean isH2On, boolean isH3On, boolean isH4On, boolean isH5On) throws IOException
	{
		List<Node<HtmlElement>> list = new ArrayList<Node<HtmlElement>>(errorElements);
		
		if(isH5On)
		{
			getPositionOfElementsInDOMTree(list, root);
		}
		
		for(Node<HtmlElement> node : list)
		{
			HtmlElement element = node.getData();
			
			double score = 0;
			
			if(isH1On)
			{
				score = score + Constants.WEIGHT_FOR_HEURISTIC1 * element.getHeuristic1();
			}
			if(isH2On)
			{
				score = score + Constants.WEIGHT_FOR_HEURISTIC2 * element.getHeuristic2();
			}
			if(isH3On)
			{
				score = score + Constants.WEIGHT_FOR_CASCADING_CALCULATION * element.getCascadingError();
			}
			if(isH4On)
			{
				score = score + Constants.WEIGHT_FOR_RATIO_CALCULATION * (1 - element.getRatio());
			}

			if(isH5On)
			{
				score = score + Constants.WEIGHT_FOR_NORMALIZED_DOM_HEIGHT * element.getPositionInDOMTree();
			}
			
			element.setScore(score);
			node.setData(element);
		}
		
		Comparator<Node<HtmlElement>> comparator = new Comparator<Node<HtmlElement>>()
		{
			public int compare(Node<HtmlElement> o1, Node<HtmlElement> o2)
			{
				return new org.apache.commons.lang.builder.CompareToBuilder().
						append(o1.getData().getScore(), o2.getData().getScore()).	// ascending
						toComparison();
			}
		};
		
		Collections.sort(list, comparator);
		
		// check if all cascading
		/*if(isH3On)
		{
			boolean isAllCascaded = true;
			for(Node<HtmlElement> node : list)
			{
				if(node.getData().getCascadingError() == 0)
				{
					isAllCascaded = false;
					break;
				}
			}
			if(isAllCascaded)
			{
				list = sortAllCascading(list, root);
			}
		}*/
		
		return list;
	}
	
	public Map<String, String> getXpathsFromNodeSet(List<Node<HtmlElement>> errorElements)
	{
		Map<String, String> xpaths = new LinkedHashMap<String, String>();
		for(Node<HtmlElement> node : errorElements)
		{
			xpaths.put(node.getData().getXpath(), node.getData().printInformation());
		}
		return xpaths;
	}
	
	public boolean isCascadingError(HtmlElement element, String testImageFullPath, String oracleImageFullPath) throws IOException
	{
		// crop element from test image
		ImageProcessing ip = new ImageProcessing();
		String templateImageFileNameFullPath = ip.cropImage(element.getX(), element.getY(), element.getWidth(), element.getHeight(), testImageFullPath);
		
		// check if it is a subimage in the oracle image
		Rectangle rect = ip.findSubImage(oracleImageFullPath, templateImageFileNameFullPath);
		
		// if yes, tag as cascading error
		if(rect != null)
		{
			return true;
		}
		return false;
	}
	
	private double getDistance(int x1, int y1, int x2, int y2)
	{
		return Math.sqrt((x2 - x1)*(x2 - x1) + (y2 - y1)*(y2 - y1));
	}
	
	private double getDisplacement(double distance1, double distance2)
	{
		return Math.abs(distance1 - distance2);
	}
	
	private List<HtmlElement> getElementsFromXpaths(Set<String> xpaths, List<Node<HtmlElement>> nodes)
	{
		List<HtmlElement> elements = new ArrayList<HtmlElement>();
		for(Node<HtmlElement> node : nodes)
		{
			if(xpaths.contains(node.getData().getXpath()))
			{
				elements.add(node.getData());
			}
		}
		return elements;
	}
	
	private List<Node<HtmlElement>> sortAllCascading(List<Node<HtmlElement>> list, Node<HtmlElement> root)
	{
		// return first leftmost element (breadth first)
		List<Node<HtmlElement>> leftToRightList = new ArrayList<Node<HtmlElement>>();
		
		Queue<Node<HtmlElement>> q = new LinkedList<Node<HtmlElement>>();
		if(root != null)
		{
			q.add(root);
		}
		
		while(!q.isEmpty())
		{
			Node<HtmlElement> node = q.remove();
			for(Node<HtmlElement> cascadedNode : list)
			{
				if(cascadedNode.getData().getXpath().equals(node.getData().getXpath()))
				{
					leftToRightList.add(cascadedNode);
					break;
				}
			}
			if (node.getChildren() != null)
			{
				for (Node<HtmlElement> child : node.getChildren())
				{
					q.add(child);
				}
			}
		}
		
		return leftToRightList;
	}
	
	private List<Node<HtmlElement>> sortByAncestry(List<Node<HtmlElement>> list)
	{
		List<Node<HtmlElement>> sortedList = new ArrayList<Node<HtmlElement>>(list);

		for(Node<HtmlElement> currentNode : list)
		{
			String currentXpath = currentNode.getData().getXpath();
			//System.out.println("for element: " + currentXpath);
			
			for(Node<HtmlElement> node : list)
			{
				String xpath = node.getData().getXpath();
				
				// not the same element
				if(!xpath.equals(currentXpath))
				{
					// check if the element is cascaded & current node is not cascaded
					if(node.getData().getCascadingError() == 1 && currentNode.getData().getCascadingError() == 0)
					{
						// check if current xpath is a substring in xpath
						if(xpath.contains(currentXpath))
						{
							// swap current node and node
							int currentIndex = sortedList.indexOf(currentNode);
							int index = sortedList.indexOf(node);
							Collections.swap(sortedList, currentIndex, index);
						}
					}
				}
			}
		}
		return sortedList;
	}
	
	/**
	 * Position = <height>.<breadth level position from left to right>
	 * @param list
	 * @param root
	 */
	private void getPositionOfElementsInDOMTree(List<Node<HtmlElement>> list, Node<HtmlElement> root)
	{
		double minPosition = Integer.MAX_VALUE;
		double maxPosition = Integer.MIN_VALUE;

		Map<String, Double> positionMap = new HashMap<String, Double>();
		
		Queue<ArrayList<Node<HtmlElement>>> q = new LinkedList<ArrayList<Node<HtmlElement>>>();
		if(root != null)
		{
			ArrayList<Node<HtmlElement>> rootList = new ArrayList<Node<HtmlElement>>();
			rootList.add(root);
			q.add(rootList);
			q.add(null);
		}
		
		int height = 1;
		int breadthPosition = 0;
		while(!q.isEmpty())
		{
			ArrayList<Node<HtmlElement>> nodeList = q.remove();
			
			if(nodeList == null)
			{
				height++;
				breadthPosition = 0;
				continue;
			}
			
			while(q.peek() != null && !q.isEmpty())
			{
				ArrayList<Node<HtmlElement>> temp = q.remove();
				nodeList.addAll(temp);
			}
			
			for (Node<HtmlElement> node : nodeList) 
			{
				breadthPosition++;
				boolean isFound = false;
				if (isFound) 
				{
					continue;
				}

				for (Node<HtmlElement> n : list) 
				{
					if (n.getData().getXpath().equals(node.getData().getXpath())) 
					{
						String positionString = height + "." + String.format("%03d", breadthPosition);
						positionMap.put(n.getData().getXpath(), Double.valueOf(positionString));
						node.getData().setPositionInDOMTree(Double.valueOf(positionString));
						if (Double.valueOf(positionString) < minPosition) 
						{
							minPosition = Double.valueOf(positionString);
						}
						if (Double.valueOf(positionString) > maxPosition) 
						{
							maxPosition = Double.valueOf(positionString);
						}
						isFound = true;
						break;
					}
				}
			}
			for(Node<HtmlElement> node : nodeList)
			{
				if (node.getChildren() != null)
				{
					ArrayList<Node<HtmlElement>> childList = new ArrayList<Node<HtmlElement>>(node.getChildren());
					q.add(childList);
				}
			}
			q.add(null);
		}
		
		// add normalized height in elements
		for(Node<HtmlElement> n : list)
		{
			//n.getData().setNormalizedHeightInDOMTree(Util.getNormalizedValue(minHeight, maxHeight, heightMap.get(n.getData().getXpath())));
			n.getData().setPositionInDOMTree(positionMap.get(n.getData().getXpath()));
			//n.getData().setPositionInDOMTree(Util.getNormalizedValue(minPosition, maxPosition, positionMap.get(n.getData().getXpath())));
		}
	}
	
	public static void main(String[] args) throws SAXException, IOException 
	{
		WebDriverSingleton instance = WebDriverSingleton.getInstance();
		instance.loadPage("C:\\USC\\Research\\WebSee\\evaluation\\test\\test1\\test.html");
		WebDriver driver = instance.getDriver();
		
		HtmlDomTree rt = new HtmlDomTree(driver, "C:\\USC\\Research\\WebSee\\evaluation\\test\\test1\\test.html");
		rt.buildHtmlDomTree();
		
		ResultSetProcessing rp = new ResultSetProcessing();
		rp.getPositionOfElementsInDOMTree(null, rt.getRoot());
		
		WebDriverSingleton.closeDriver();
	}
}