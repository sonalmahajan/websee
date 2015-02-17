package evalframework;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import util.Util;
import websee.HtmlElement;
import websee.Node;
import websee.ResultSetProcessing;
import config.Constants;

public class ResultsVerification
{
	//heuristic 1: if parent and all its children are reported in error, then keep only parent and remove children (contained)
	//heuristic 2: if parent and at least one child (but not all children) are reported in error, then keep children and remove parent (overlapped)
	
	private double precisionNoH;
	private double precisionH1;
	private double precisionH2;
	private double precisionH12;

	private double recallNoH;
	private double recallH1;
	private double recallH2;
	private double recallH12;
	
	private int truePositiveCountNoH = 0;
	private int truePositiveCountH1 = 0;
	private int truePositiveCountH2 = 0;
	private int truePositiveCountH12 = 0;
	private int falsePositiveCount = 0;
	private int trueNegativeCount = 0;
	private int falseNegativeCountNoH = 0;
	private int falseNegativeCountH1 = 0;
	private int falseNegativeCountH2 = 0;
	private int falseNegativeCountH12 = 0;
	
	private static int imageSizeMismatchForFalseNegative = 0;
	private static int noImageDifferenceForFalseNegative = 0;
	private static int someNeighborReportedAsPositiveForFalseNegative = 0;

	private Map<String, String> noHeuristic = new LinkedHashMap<String, String>();
	private Map<String, String> heuristic1 = new LinkedHashMap<String, String>();
	private Map<String, String> heuristic2 = new LinkedHashMap<String, String>();
	private Map<String, String> heuristic3 = new LinkedHashMap<String, String>();
	private Map<String, String> heuristic4 = new LinkedHashMap<String, String>();
	private Map<String, String> heuristic5 = new LinkedHashMap<String, String>();
	private Map<String, String> allHeuristic = new LinkedHashMap<String, String>();

	private final int PRIORITY_COUNT = 5;
	
	public void printDetailedReport(List<HashMap<String, String>> expected, Set<Node<HtmlElement>> actual, Logger log, String path, long time, long totalTime, String savedHTMLFileName, String oracleFilePath, Node<HtmlElement> root, Set<Node<HtmlElement>> processedResult) throws IOException
	{
		if (!new File(path + File.separatorChar + Constants.COMPARE_IMAGES_DIFFERENCE_TEXT_FILENAME).exists())
		{
			imageSizeMismatchForFalseNegative++;
		}
		else if (actual.size() == 0)
		{
			noImageDifferenceForFalseNegative++;
		}

		ArrayList<String> truePositives = new ArrayList<String>();

		// categorize true and false positives
		for (Node<HtmlElement> actualNode : actual)
		{
			for (HashMap<String, String> summary : expected)
			{
				if (!summary.get("xpath").equals(actualNode.getData().getXpath()))
				{
					falsePositiveCount++;
				}
				else
				{
					truePositiveCountNoH++;
					truePositiveCountH1++;
					truePositiveCountH2++;
					truePositiveCountH12++;
					truePositives.add(actualNode.getData().getXpath());
				}
			}
		}
		
		// categorize false negatives
		for (HashMap<String, String> summary : expected)
		{
			String xpath = summary.get("xpath");
			
			boolean found = false; 
			for (Node<HtmlElement> actualNode : actual)
			{
				if(actualNode.getData().getXpath().equals(xpath))
				{
					found = true;
					break;
				}
			}
			if (!found)
			{
				falseNegativeCountNoH++;
				falseNegativeCountH1++;
				falseNegativeCountH2++;
				falseNegativeCountH12++;
			}
		}

		if (falsePositiveCount > 0 && truePositiveCountNoH == 0)
		{
			someNeighborReportedAsPositiveForFalseNegative++;
		}

		// create a deep copy for operating different heuristics
		for (Node<HtmlElement> actualNode : actual)
		{
			String xpath = actualNode.getData().getXpath();
			noHeuristic.put(xpath, "dist = " + Util.getDistance(expected.get(0).get("xpath"), xpath) + ", " + actualNode.getData().printInformation());
		}

		ResultSetProcessing fr = new ResultSetProcessing();

		String testImageFullPath = path + File.separatorChar + Util.getFileNameAndExtension(Constants.NEW_FILE_NAME)[0] + Constants.SCREENSHOT_FILE_EXTENSION;
		String oracleImageFullPath = oracleFilePath + File.separatorChar + Util.getFileNameAndExtension(Constants.ORIGINAL_FILENAME)[0] + Constants.SCREENSHOT_FILE_EXTENSION;
		
		// heuristic 1 only
		if(Constants.HEURISTIC_1_ON)
		{
			List<Node<HtmlElement>> prioritizedList = fr.decidePriority(processedResult, root, true, false, false, false, false);
			
			for(Node<HtmlElement> node : prioritizedList)
			{
				String xpath = node.getData().getXpath();
				heuristic1.put(xpath, "dist = " + Util.getDistance(expected.get(0).get("xpath"), xpath) + ", " + node.getData().printInformation());
			}
		}
		
		// heuristic 2 only
		if(Constants.HEURISTIC_2_ON)
		{
			List<Node<HtmlElement>> prioritizedList = fr.decidePriority(processedResult, root, false, true, false, false, false);
			
			for(Node<HtmlElement> node : prioritizedList)
			{
				String xpath = node.getData().getXpath();
				heuristic2.put(xpath, "dist = " + Util.getDistance(expected.get(0).get("xpath"), xpath) + ", " + node.getData().printInformation());
			}
		}
		
		// heuristic 3 only
		if(Constants.HEURISTIC_3_ON)
		{
			List<Node<HtmlElement>> prioritizedList = fr.decidePriority(processedResult, root, false, false, true, false, false);
			
			for(Node<HtmlElement> node : prioritizedList)
			{
				String xpath = node.getData().getXpath();
				heuristic3.put(xpath, "dist = " + Util.getDistance(expected.get(0).get("xpath"), xpath) + ", " + node.getData().printInformation());
			}
		}
		
		// heuristic 4 only
		if(Constants.HEURISTIC_4_ON)
		{
			List<Node<HtmlElement>> prioritizedList = fr.decidePriority(processedResult, root, false, false, false, true, false);
			
			for(Node<HtmlElement> node : prioritizedList)
			{
				String xpath = node.getData().getXpath();
				heuristic4.put(xpath, "dist = " + Util.getDistance(expected.get(0).get("xpath"), xpath) + ", " + node.getData().printInformation());
			}
		}
		
		// heuristic 4 only
		if(Constants.HEURISTIC_5_ON)
		{
			List<Node<HtmlElement>> prioritizedList = fr.decidePriority(processedResult, root, false, false, false, true, false);
			
			for(Node<HtmlElement> node : prioritizedList)
			{
				String xpath = node.getData().getXpath();
				heuristic5.put(xpath, "dist = " + Util.getDistance(expected.get(0).get("xpath"), xpath) + ", " + node.getData().printInformation());
			}
		}
		
		// all heuristics
		for(Node<HtmlElement> node : processedResult)
		{
			String xpath = node.getData().getXpath();
			allHeuristic.put(xpath, "dist = " + Util.getDistance(expected.get(0).get("xpath"), xpath) + ", " + node.getData().printInformation());
		}
		
		// check if true positive has been removed because of the heuristics application
		String h1 = "";
		String h2 = "";
		String h12 = "";
		
		for(String xpath : truePositives)
		{
			// if true was present earlier and now removed from result after applying heuristics
			if(noHeuristic.containsKey(xpath))
			{
				if(!heuristic1.containsKey(xpath))
				{
					h1 = "true positive removed heuristic1";
					truePositiveCountH1--;
					falseNegativeCountH1++;
				}
				if(!heuristic2.containsKey(xpath))
				{
					h2 = "true positive removed heuristic2";
					truePositiveCountH2--;
					falseNegativeCountH2++;
				}
				if(!allHeuristic.containsKey(xpath))
				{
					h12 = "true positive removed heuristic12";
					truePositiveCountH12--;
					falseNegativeCountH12++;
				}
			}
		}
		
		// compute accuracy metrics
		calculatePrecision();
		calculateRecall();
		
		// print results
		printResults(log, path, expected, h1, h2, h12, time, totalTime);
	}

	public void printInitial(Logger log, String path)
	{
		log.info("**************************************************************** " + new File(path).getName() + " ****************************************************************");
		log.info("Path: " + path);
		log.info("");

	}
	
	private void printResults(Logger log, String path, List<HashMap<String, String>> expected, String h1, String h2, String h12, long time, long totalTime) throws IOException
	{
		DecimalFormat decimal = new DecimalFormat("0.00");

		log.info("Time for this test case: " + decimal.format(Util.convertNanosecondsToSeconds(time)) + "s");
		log.info("Total time till now: " + decimal.format(Util.convertNanosecondsToSeconds(totalTime)) + "s");
			
		log.info("");
		log.info("");
		
		String expectedElements = "";
		String seededError = "";
		for(HashMap<String, String> map : expected)
		{
			expectedElements = expectedElements + map.get("xpath");
			seededError = seededError + "(elementChangeType=" + map.get("elementChangeType") + ", newElements=" + map.get("newElements") + ")\n";
		}
		log.info("Seeded error: " + seededError);
		log.info("Expected (seeded elements): " + expectedElements);
		log.info("");
		log.info("Actual (error elements) No heuristic: (size=" + noHeuristic.size() + "): ");
		printElements(log, noHeuristic, expectedElements);
		log.info("");
		log.info("Actual (error elements) Heuristic1: (size=" + heuristic1.size() + "): ");
		printElements(log, heuristic1, expectedElements);
		log.info("");
		log.info("Actual (error elements) Heuristic2: (size=" + heuristic2.size() + "): ");
		printElements(log, heuristic2, expectedElements);
		log.info("");
		log.info("Actual (error elements) Heuristic3: (size=" + heuristic3.size() + "): ");
		printElements(log, heuristic3, expectedElements);
		log.info("");
		log.info("Actual (error elements) Heuristic4: (size=" + heuristic4.size() + "): ");
		printElements(log, heuristic4, expectedElements);
		log.info("");
		log.info("Actual (error elements) Heuristic5: (size=" + heuristic5.size() + "): ");
		printElements(log, heuristic5, expectedElements);
		log.info("");
		log.info("Actual (error elements) All Heuristic: (size=" + allHeuristic.size() + "): ");
		printElements(log, allHeuristic, expectedElements);
		log.info("");
		/*log.info("|-------|-----------------------------------|-----------------------------------|---------------------------------------|");
		log.info("|\t\t|\tTP\t\tFP\t\tFN\t\tTN\t\t|\t\tPrecision\t\tRecall\t\t|\tP1\t\tP2\t\tP3\t\tP4\t\tP5\t|");
		log.info("|\t\t|\t\t\t\t\t\t\t\t\t|\t\t\t\t\t\t\t\t\t|\t\t\t\t(distance)\t\t\t\t|");
		log.info("|-------|-----------------------------------|-----------------------------------|---------------------------------------|");
		log.info("|NoH\t|\t" + truePositiveCountNoH + "\t\t" + (noHeuristic.size() - truePositiveCountNoH) + 
				"\t\t" + falseNegativeCountNoH + "\t\t" + trueNegativeCount + 
				"\t\t|\t\t" + decimal.format((precisionNoH * 100)) + "%" + 
				"\t\t\t" + decimal.format((recallNoH * 100)) + "%" + "\t\t|" + getPrioritiesString(noHeuristic) + "|");
		log.info("|H1\t\t|\t" + truePositiveCountH1 + "\t\t" + (heuristic1.size() - truePositiveCountH1) + 
				"\t\t" + falseNegativeCountH1 + "\t\t" + trueNegativeCount + 
				"\t\t|\t\t" + decimal.format((precisionH1 * 100)) + "%" + 
				"\t\t\t" + decimal.format((recallH1 * 100)) + "%" + "\t\t|" + getPrioritiesString(heuristic1) + "|");
		log.info("|H2\t\t|\t" + truePositiveCountH2 + "\t\t" + (heuristic2.size() - truePositiveCountH2) + 
				"\t\t" + falseNegativeCountH2 + "\t\t" + trueNegativeCount + 
				"\t\t|\t\t" + decimal.format((precisionH2 * 100)) + "%" + 
				"\t\t\t" + decimal.format((recallH2 * 100)) + "%" + "\t\t|" + getPrioritiesString(heuristic2) + "|");
		log.info("|H12\t|\t" + truePositiveCountH12 + "\t\t" + (allHeuristic.size() - truePositiveCountH12) + 
				"\t\t" + falseNegativeCountH12 + "\t\t" + trueNegativeCount + 
				"\t\t|\t\t" + decimal.format((precisionH12 * 100)) + "%" + 
				"\t\t\t" + decimal.format((recallH12 * 100)) + "%" + "\t\t|" + getPrioritiesString(allHeuristic) + "|");
		log.info("|-------|-----------------------------------|-----------------------------------|---------------------------------------|");
		
		log.info("");
		
		if(!h1.isEmpty())
		{
			log.info(h1);
		}
		if(!h2.isEmpty())
		{
			log.info(h2);
		}
		if(!h12.isEmpty())
		{
			log.info(h12);
		}
		
		log.info("");*/
		log.info("");
	}
	
	/**
	 * Precision = tp / (tp + fp)
	 */
	private void calculatePrecision()
	{
		precisionNoH = 0.0;
		precisionH1 = 0.0;
		precisionH2 = 0.0;
		precisionH12 = 0.0;
		
		// handle case if precision has zero in the denominator
		if(truePositiveCountNoH + noHeuristic.size() > 0)
		{
			precisionNoH = truePositiveCountNoH / (double) noHeuristic.size();
		}
		if(truePositiveCountH1 + heuristic1.size() > 0)
		{
			precisionH1 = truePositiveCountH1 / (double) heuristic1.size();
		}
		if(truePositiveCountH2 + heuristic2.size() > 0)
		{
			precisionH2 = truePositiveCountH2 / (double) heuristic2.size();
		}
		if(truePositiveCountH12 + allHeuristic.size() > 0)
		{
			precisionH12 = truePositiveCountH12 / (double) allHeuristic.size();
		}
	}
	
	/**
	 * Recall = tp / (tp + fn)
	 */
	private void calculateRecall()
	{
		recallNoH = 0.0;
		recallH1 = 0.0;
		recallH2 = 0.0;
		recallH12 = 0.0;
		
		// handle case if recall has zero in the denominator
		if ((truePositiveCountNoH + falseNegativeCountNoH) > 0.0)
		{
			recallNoH = truePositiveCountNoH / (double) (truePositiveCountNoH + falseNegativeCountNoH);
		}
		if ((truePositiveCountH1 + falseNegativeCountNoH) > 0.0)
		{
			recallH1 = truePositiveCountH1 / (double) (truePositiveCountH1 + falseNegativeCountH1);
		}
		if ((truePositiveCountH2 + falseNegativeCountNoH) > 0.0)
		{
			recallH2 = truePositiveCountH2 / (double) (truePositiveCountH2 + falseNegativeCountH2);
		}
		if ((truePositiveCountH12 + falseNegativeCountNoH) > 0.0)
		{
			recallH12 = truePositiveCountH2 / (double) (truePositiveCountH2 + falseNegativeCountH12);
		}
	}

	public Map<String, String> getSummaryResults(List<ResultsVerification> totalResults, long totalTime, String path, int appCount, int testCaseCount, Logger logger) throws IOException
	{
		double totalPrecisionNoH = 0.0;
		double totalPrecisionH1 = 0.0;
		double totalPrecisionH2 = 0.0;
		double totalPrecisionH12 = 0.0;
		double totalRecallNoH = 0.0;
		double totalRecallH1 = 0.0;
		double totalRecallH2 = 0.0;
		double totalRecallH12 = 0.0;

		int totalFalsePositiveNoH = 0;
		int totalFalsePositiveH1 = 0;
		int totalFalsePositiveH2 = 0;
		int totalFalsePositiveH12 = 0;
		
		int totalFalseNegativeNoH = 0;
		int totalFalseNegativeH1 = 0;
		int totalFalseNegativeH2 = 0;
		int totalFalseNegativeH12 = 0;

		for (ResultsVerification resultsVerification : totalResults)
		{
			totalPrecisionNoH = totalPrecisionNoH + resultsVerification.precisionNoH;
			totalPrecisionH1 = totalPrecisionH1 + resultsVerification.precisionH1;
			totalPrecisionH2 = totalPrecisionH2 + resultsVerification.precisionH2;
			totalPrecisionH12 = totalPrecisionH12 + resultsVerification.precisionH12;
			totalRecallNoH = totalRecallNoH + resultsVerification.recallNoH;
			totalRecallH1 = totalRecallH1 + resultsVerification.recallH1;
			totalRecallH2 = totalRecallH2 + resultsVerification.recallH2;
			totalRecallH12 = totalRecallH12 + resultsVerification.recallH12;

			totalFalsePositiveNoH = totalFalsePositiveNoH + (resultsVerification.noHeuristic.size() - resultsVerification.truePositiveCountNoH);
			totalFalsePositiveH1 = totalFalsePositiveH1 + (resultsVerification.heuristic1.size() - resultsVerification.truePositiveCountH1);
			totalFalsePositiveH2 = totalFalsePositiveH2 + (resultsVerification.heuristic2.size() - resultsVerification.truePositiveCountH2);
			totalFalsePositiveH12 = totalFalsePositiveH12 + (resultsVerification.allHeuristic.size() - resultsVerification.truePositiveCountH12);
			
			totalFalseNegativeNoH = totalFalseNegativeNoH + resultsVerification.falseNegativeCountNoH;
			totalFalseNegativeH1 = totalFalseNegativeH1 + resultsVerification.falseNegativeCountH1;
			totalFalseNegativeH2 = totalFalseNegativeH2 + resultsVerification.falseNegativeCountH2;
			totalFalseNegativeH12 = totalFalseNegativeH12 + resultsVerification.falseNegativeCountH12;
		}

		int newSize = totalResults.size() - noImageDifferenceForFalseNegative;
		if(newSize == 0)
			return new HashMap<String, String>();

		double averagePrecisionNoH = totalPrecisionNoH / (double)newSize;
		double averagePrecisionH1 = totalPrecisionH1 / (double)newSize;
		double averagePrecisionH2 = totalPrecisionH2 / (double)newSize;
		double averagePrecisionH12 = totalPrecisionH12 / (double)newSize;
		
		double averageRecallNoH = totalRecallNoH / (double)newSize;
		double averageRecallH1 = totalRecallH1 / (double)newSize;
		double averageRecallH2 = totalRecallH2 / (double)newSize;
		double averageRecallH12 = totalRecallH12 / (double)newSize;
		
		double averageFalsePositivesNoH = totalFalsePositiveNoH / (double) newSize;
		double averageFalsePositivesH1 = totalFalsePositiveH1 / (double) newSize;
		double averageFalsePositivesH2 = totalFalsePositiveH2 / (double) newSize;
		double averageFalsePositivesH12 = totalFalsePositiveH12 / (double) newSize;
		
		double averageFalseNegativesNoH = totalFalseNegativeNoH / (double) newSize;
		double averageFalseNegativesH1 = totalFalseNegativeH1 / (double) newSize;
		double averageFalseNegativesH2 = totalFalseNegativeH2 / (double) newSize;
		double averageFalseNegativesH12 = totalFalseNegativeH12 / (double) newSize;
		
		DecimalFormat decimalNumber = new DecimalFormat("0.00");
		double totalTimePerTestCase = TimeUnit.NANOSECONDS.toSeconds(totalTime) / (double)testCaseCount;
		
		Map<String, String> resultToBePrinted = new HashMap<String, String>();
		
		String general = appCount + "\t\t" + new File(path).getName() + "\t\t" + 
		Util.getTotalNumberOfHtmlElementsInPage(path + File.separatorChar + Constants.ORIGINAL_FILENAME) + "\t\t" + 
		testCaseCount + "\t\t\t" + newSize + "\t\t\t" +  decimalNumber.format(totalTimePerTestCase) + "s";
		
		String precisionRecall = appCount + "\t\t\t" + 
		decimalNumber.format(averagePrecisionNoH * 100) + "%\t\t" + decimalNumber.format((averagePrecisionH1 * 100)) + "%\t\t" + 
		decimalNumber.format(averagePrecisionH2 * 100) + "%\t\t" + decimalNumber.format((averagePrecisionH12 * 100)) + "%\t\t\t" + 
		decimalNumber.format(averageRecallNoH * 100) + "%\t\t" + decimalNumber.format((averageRecallH1 * 100)) + "%\t\t" + 
		decimalNumber.format(averageRecallH2 * 100) + "%\t\t" + decimalNumber.format((averageRecallH12 * 100)) + "%";
		
		String falsePositivesNegatives = appCount + "\t\t\t" + 
		decimalNumber.format(averageFalsePositivesNoH) + "\t\t" + decimalNumber.format(averageFalsePositivesH1) + "\t\t" + 
		decimalNumber.format(averageFalsePositivesH2) + "\t\t" + decimalNumber.format(averageFalsePositivesH12) + "\t\t\t" + 
		decimalNumber.format(averageFalseNegativesNoH) + "\t\t" + decimalNumber.format(averageFalseNegativesH1) + "\t\t" + 
		decimalNumber.format(averageFalseNegativesH2) + "\t\t" + decimalNumber.format(averageFalseNegativesH12);
		
		resultToBePrinted.put("general", general);
		resultToBePrinted.put("precisionRecall", precisionRecall);
		resultToBePrinted.put("falsePositivesNegatives", falsePositivesNegatives);
		
		logger.info("");
		logger.info("");
		logger.info("SUMMARY PRIORITY FREQUENCY RESULTS");
		logger.info("");
		logger.info("----|---------------------------|---------------------------|---------------------------|---------------------------|---------------------------|");
		logger.info("dist|\t\t\tP1\t\t\t\t|\t\t\tP2\t\t\t\t|\t\t\tP3\t\t\t\t|\t\t\tP4\t\t\t\t|\t\t\tP5\t\t\t\t|");
		logger.info("\t|NoH\tH1\t\tH2\t\tH12\t|NoH\tH1\t\tH2\t\tH12\t|NoH\tH1\t\tH2\t\tH12\t|NoH\tH1\t\tH2\t\tH12\t|NoH\tH1\t\tH2\t\tH12\t|");
		logger.info("----|---------------------------|---------------------------|---------------------------|---------------------------|---------------------------|");

		List<Map<Integer, Integer>> priorityListNoH = new ArrayList<Map<Integer,Integer>>();
		List<Map<Integer, Integer>> priorityListH1 = new ArrayList<Map<Integer,Integer>>();
		List<Map<Integer, Integer>> priorityListH2 = new ArrayList<Map<Integer,Integer>>();
		List<Map<Integer, Integer>> priorityListH12 = new ArrayList<Map<Integer,Integer>>();
		
		for (int j = 0; j < PRIORITY_COUNT; j++)
		{
			Map<Integer, Integer> distFreqMapNoH = new HashMap<Integer, Integer>();
			Map<Integer, Integer> distFreqMapH1 = new HashMap<Integer, Integer>();
			Map<Integer, Integer> distFreqMapH2 = new HashMap<Integer, Integer>();
			Map<Integer, Integer> distFreqMapH12 = new HashMap<Integer, Integer>();

			for (ResultsVerification resultsVerification : totalResults)
			{
				if (resultsVerification.noHeuristic.size() > j)
				{
					String value = new ArrayList<String>(resultsVerification.noHeuristic.values()).get(j);
					int val = getDistanceFromString(value);
					int freq = 1;
					if (distFreqMapNoH.containsKey(val))
					{
						freq = distFreqMapNoH.get(val);
						freq++;
					}
					distFreqMapNoH.put(val, freq);
				}

				if (resultsVerification.heuristic1.size() > j)
				{
					String value = new ArrayList<String>(resultsVerification.heuristic1.values()).get(j);
					int val = getDistanceFromString(value);
					int freq = 1;
					if (distFreqMapH1.containsKey(val))
					{
						freq = distFreqMapH1.get(val);
						freq++;
					}
					distFreqMapH1.put(val, freq);
				}

				if (resultsVerification.heuristic2.size() > j)
				{
					String value = new ArrayList<String>(resultsVerification.heuristic2.values()).get(j);
					int val = getDistanceFromString(value);
					int freq = 1;
					if (distFreqMapH2.containsKey(val))
					{
						freq = distFreqMapH2.get(val);
						freq++;
					}
					distFreqMapH2.put(val, freq);
				}

				if (resultsVerification.allHeuristic.size() > j)
				{
					String value = new ArrayList<String>(resultsVerification.allHeuristic.values()).get(j);
					int val = getDistanceFromString(value);
					int freq = 1;
					if (distFreqMapH12.containsKey(val))
					{
						freq = distFreqMapH12.get(val);
						freq++;
					}
					distFreqMapH12.put(val, freq);
				}
			}
			priorityListNoH.add(distFreqMapNoH);
			priorityListH1.add(distFreqMapH1);
			priorityListH2.add(distFreqMapH2);
			priorityListH12.add(distFreqMapH12);
		}

		for(int i = 0; i <= getMaxDistance(totalResults); i++)
		{
			String temp = "";
			boolean flag = false;
			for(int j = 0; j < PRIORITY_COUNT; j++)
			{
				if(priorityListNoH.get(j).containsKey(i))
				{
					temp = temp + priorityListNoH.get(j).get(i) + "\t\t";
					flag = true;
				}
				else
				{
					temp = temp + "\t\t";
				}
				if(priorityListH1.get(j).containsKey(i))
				{
					temp = temp + priorityListH1.get(j).get(i) + "\t\t";
					flag = true;
				}
				else
				{
					temp = temp + "\t\t";
				}
				if(priorityListH2.get(j).containsKey(i))
				{
					temp = temp + priorityListH2.get(j).get(i) + "\t\t";
					flag = true;
				}
				else
				{
					temp = temp + "\t\t";
				}
				if(priorityListH12.get(j).containsKey(i))
				{
					temp = temp + priorityListH12.get(j).get(i) + "\t|";
					flag = true;
				}
				else
				{
					temp = temp + "\t|";
				}
			}
			if(flag)
			{
				logger.info(i + "\t|" + temp);
				logger.info("----|---------------------------|---------------------------|---------------------------|---------------------------|---------------------------|");
			}
		}
		return resultToBePrinted;
	}
	
	public void printSummaryReport(Logger log, List<Map<String, String>> result)
	{
		// general table
		log.info("Subject Applications");
		log.info("----------------------------------------------------------------------");
		log.info("App#\tApp Name\t\t\tSize\t#T\t\tActual #T\t\tAvg. Time");
		log.info("----------------------------------------------------------------------");
		for (Map<String, String> map : result)
		{
			log.info(map.get("general"));
		}
		log.info("----------------------------------------------------------------------");
		
		// average precision & recall table
		log.info("\n\n\nAverage Precision and Recall");
		log.info("------------------------------------------------------------------------------------------------------------");
		log.info("App#\t\t\t\t\tAverage Precision\t\t\t\t\t\t\t\t\tAverage Recall");
		log.info("\t\t\tNoH\t\t\tH1\t\t\tH2\t\t\tH12\t\t\t\tNoH\t\t\tH1\t\t\tH2\t\t\tH12");
		log.info("------------------------------------------------------------------------------------------------------------");
		for (Map<String, String> map : result)
		{
			log.info(map.get("precisionRecall"));
		}
		log.info("------------------------------------------------------------------------------------------------------------");
		
		// average number of false positives and negatives table
		log.info("\n\n\nAverage Number of False Positives and Negatives");
		log.info("------------------------------------------------------------------------------------------------------------");
		log.info("App#\t\t\t\t\tAverage False Positives\t\t\t\t\t\t\tAverage False Negatives");
		log.info("\t\t\tNoH\t\t\tH1\t\t\tH2\t\t\tH12\t\t\t\tNoH\t\t\tH1\t\t\tH2\t\t\tH12");
		log.info("------------------------------------------------------------------------------------------------------------");
		for (Map<String, String> map : result)
		{
			log.info(map.get("falsePositivesNegatives"));
		}
		log.info("------------------------------------------------------------------------------------------------------------");
	}

	private void printElements(Logger log, Map<String, String> elements, String expected)
	{
		int count = 1;
		for (String key : elements.keySet())
		{
			log.info(count + Constants.RESULT_FILE_PRIORITY_VALUE_SEPARATOR + key + " -> " + elements.get(key));
			count++;
		}
	}
	
	private int getDistanceFromString(String str)
	{
		String regex = "dist = (.+?),";
		String result = Util.getValueFromRegex(regex, str);
		int dist;
		try
		{
			dist = Integer.parseInt(result);
		}
		catch (NumberFormatException e)
		{
			dist = -1;
		}
		return dist;
	}
	
	private String getPrioritiesString(Map<String, String> elements)
	{
		String result = "";
		
		int count  = 1;
		for (String value : elements.values())
		{
			if(count > PRIORITY_COUNT || count > elements.size())
			{
				break;
			}
			result = result + "\t" + getDistanceFromString(value) + "\t";
			count++;
		}
		
		if(count < PRIORITY_COUNT)
		{
			for (int i = count; i <= PRIORITY_COUNT; i++)
			{
				result = result + "\t\t";
			}
		}
		
		return result;
	}
	
	private int getMaxDistance(List<ResultsVerification> totalResults)
	{
		int max = -1;
		for(ResultsVerification rv : totalResults)
		{
			for (String value : rv.noHeuristic.values())
			{
				int val = getDistanceFromString(value);
				if(val > max)
				{
					max = val; 
				}
			}
		}
		return max;
	}
}