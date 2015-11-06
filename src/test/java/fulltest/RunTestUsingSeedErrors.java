package fulltest;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jgap.InvalidConfigurationException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.xml.sax.SAXException;

import util.Util;
import websee.HtmlElement;
import websee.Node;
import websee.WebSeeTool;
import config.Constants;
import evalframework.AttributeHelperValuesMaster;
import evalframework.ResultsVerification;
import evalframework.SeedErrors;
import evalframework.SeedErrorsInitializer;
import evalframework.SeedErrorsSpecialRegions;

public class RunTestUsingSeedErrors
{
	private static Logger logger = Logger.getLogger(RunTestUsingSeedErrors.class);
	
	public static void main(String[] args) throws IOException, SAXException, InvalidConfigurationException
	{	
		String basePath = "C:\\USC\\visual_checking\\evaluation";
		String specialRegionsXmlFilePath = "evaluationframework/com/evaluation/framework/java/special_regions_initializer.xml";
		List<String> basePaths = new ArrayList<String>();
		String dirPath = null;
		String testFilesPath;
		
		ArrayList<String> urls = new ArrayList<String>();
		//urls.add(basePath + File.separatorChar + "test_run_7");
		urls.add("http://www.gmail.com");
		//urls.add("http://losangeles.craigslist.org/i/autos");
		//urls.add("http://w3schools.com/dtd/default.asp");
		//urls.add("http://www.cs.usc.edu/research");
		//urls.add("http://www.blindtextgenerator.com");
		//urls.add("http://news.yahoo.com");
		//urls.add("http://en.wikipedia.org/wiki/HtmlUnit");
		//urls.add("http://www.wikinews.org/");
//		urls.add("http://docs.oracle.com/javase/tutorial/essential/io/summary.html");
		
		// create new directory for new test run only if at least one url is present in the list implying new test run
		boolean allDirectories = true;
		boolean atLeastOneDirectory = false;
		String newTestRunDirectoryBasePathCopy = "";
		for (String urlString : urls)
		{
			// if url
			if(!new File(urlString).isDirectory())
			{
				allDirectories = false;
			}
			else
			{
				atLeastOneDirectory = true;
				basePaths.add(urlString);
			}
		}
		
		if(!allDirectories)
		{
			int testRunCount = 0;
			for (File testRunDir : new File(basePath).listFiles())
			{
				if(testRunDir.isDirectory())
				{
					if(testRunDir.getName().matches("^" + Constants.TEST_RUN_DIRECTORY_NAME + "(.+)"))
					{
						testRunCount++;
					}
				}
			}
			basePath = Util.createDirectory(basePath, Constants.TEST_RUN_DIRECTORY_NAME + ++testRunCount);
			basePaths.add(basePath);
			if(atLeastOneDirectory)
			{
				newTestRunDirectoryBasePathCopy = basePath;
			}
		}
		// prepare evaluation environment with seeded errors
		String[] originalFileName = Util.getFileNameAndExtension(Constants.ORIGINAL_FILENAME);
		for (String urlString : urls)
		{	
			if(new File(urlString).isDirectory())
			{
				basePath = new File(urlString).getAbsolutePath();
			}
			else
			{
				if(atLeastOneDirectory)
				{
					basePath = newTestRunDirectoryBasePathCopy;
				}
				
				// save html page and get screenshot of original
				URL url = new URL(urlString);
				dirPath = Util.createDirectory(basePath, url.getHost());
			
				Util.saveHTMLPageWindows(urlString, dirPath, originalFileName[0] + originalFileName[1], Constants.DO_NOT_OVERWRITE);
				Util.getScreenshot(Constants.ORIGINAL_FILENAME, dirPath, originalFileName[0] + Constants.SCREENSHOT_FILE_EXTENSION, null, Constants.DO_NOT_OVERWRITE);
				
				// seed errors and create new html pages
				SeedErrorsInitializer sei = new SeedErrorsInitializer();
				sei.runSeedErrorsInitializer(Constants.SEED_ERRORS_XML_FILENAME, basePath, Constants.ORIGINAL_FILENAME, dirPath, false);
			}
		}
		
		// start evaluation process
		for(String basePathString : basePaths)
		{
			SeedErrors se = new SeedErrors();
			se.runSeedErrors(Constants.SEED_ERRORS_XML_FILENAME, basePathString, Constants.DO_NOT_OVERWRITE);
			LinkedHashMap<String, ArrayList<HashMap<String, String>>> seededElementsSummary = se.getSeededElementsSummary();

			Logger cumulativeResultsSummary = RunTestUsingSeedErrors.getNewLogger(basePathString + File.separatorChar + Util.getNextAvailableFileName(basePathString, Constants.CUMULATIVE_RESULT_SUMMARY_FILENAME), "summaryLogger" + basePathString);
			List<Map<String, String>> summaryResults = new ArrayList<Map<String,String>>();
			int appCount = 1;
			
			for (File urlDir : new File(basePathString).listFiles())
			{
				if (urlDir.isDirectory())
				{
					dirPath = urlDir.getAbsolutePath();

					// add special regions xml to test directories 
					SeedErrorsSpecialRegions sesr = new SeedErrorsSpecialRegions();
					seededElementsSummary = sesr.processRegions(specialRegionsXmlFilePath, dirPath, seededElementsSummary);
					
					testFilesPath = urlDir.getAbsolutePath() + File.separatorChar + Constants.NEW_FILES_DIRECTORY;

					Logger cumulativeResultsDetailed = RunTestUsingSeedErrors.getNewLogger(dirPath + File.separatorChar + Util.getNextAvailableFileName(dirPath, Constants.CUMULATIVE_RESULT_DETAILED_FILENAME), "detailedLogger" + dirPath);
					
					List<ResultsVerification> totalResults = new ArrayList<ResultsVerification>(); 
					long totalTime = 0;
					int testCaseCount = 0;
					
					for(File testDir : new File(testFilesPath).listFiles())
					{
						if(testDir.isDirectory())
						{
							String testFilePath = testDir.getAbsolutePath();
							String[] testFileName = Util.getFileNameAndExtension(Constants.NEW_FILE_NAME);

							String specialRegionsFileFullPath = testFilePath + File.separatorChar + Constants.SPECIAL_REGIONS_XML_FILENAME;
							String reportFileName = testFileName[0] + Constants.REPORT_FILENAME + "." + Constants.REPORT_FILE_EXTENSION; 

							ResultsVerification rv = new ResultsVerification();
							rv.printInitial(cumulativeResultsDetailed, testFilePath);
							
							long startTimeThisTestCase = System.nanoTime();

							//run visual invariants tool
							WebSeeTool wst = new WebSeeTool(dirPath + File.separatorChar + originalFileName[0] + Constants.SCREENSHOT_FILE_EXTENSION, testFilePath + File.separatorChar + testFileName[0] + testFileName[1]);
							wst.runVisualInvariantsTool(originalFileName[0] + Constants.SCREENSHOT_FILE_EXTENSION, 
									dirPath, testFilePath, testFileName[0] + testFileName[1], reportFileName, specialRegionsFileFullPath, cumulativeResultsDetailed, Constants.DO_NOT_OVERWRITE);
							
							long endTimeThisTestCase = System.nanoTime() - startTimeThisTestCase;
							totalTime = totalTime + endTimeThisTestCase;
							
							testCaseCount++;
						}
					}
					ResultsVerification summaryRv = new ResultsVerification();
					summaryResults.add(summaryRv.getSummaryResults(totalResults, totalTime, dirPath, appCount, testCaseCount, cumulativeResultsDetailed));
					appCount++;
				}
			}
			ResultsVerification printRv = new ResultsVerification();
			printRv.printSummaryReport(cumulativeResultsSummary, summaryResults);
		}
	}
	
	//TODO: can be made better
	public static List<Set<Node<HtmlElement>>> reProcess(LinkedHashMap<String, ArrayList<HashMap<String, String>>> seededElementsSummary, String testFilePath, 
			String[] testFileName, String[] originalFileName, String dirPath, String reportFileName, String specialRegionsFileFullPath, Logger log) throws IOException, SAXException, InvalidConfigurationException
	{
		List<Set<Node<HtmlElement>>> newOriginalSetOfErrorElements = new ArrayList<Set<Node<HtmlElement>>>();
		boolean change = false;
		
		List<HashMap<String, String>> seededElements = seededElementsSummary.get(testFilePath);

		AttributeHelperValuesMaster ahvm = AttributeHelperValuesMaster.getInstance();
		HashMap<String, ArrayList<String>> helperValues = ahvm.getHelperValues();

		String[] testFile = Util.getFileNameAndExtension(Constants.NEW_FILE_NAME);
		Document document = Jsoup.parse(new File(testFilePath + File.separatorChar + testFile[0] + testFile[1]), null);
		
		for (HashMap<String, String> newElement : seededElements)
		{
			Element e = Util.getElementFromXPathJava(newElement.get("xpath"), document);
			//parse attributes for key-value
			Pattern p = Pattern.compile("name=(.*), value=(.*),", Pattern.DOTALL);
			Matcher m = p.matcher(newElement.get("newElements"));
			if (m.find())
			{
				String attr = m.group(1);
				String value = m.group(2);

				//indicates css properties
				if(value.contains(":"))
				{
					String newValue = "";
					
					String[] css = value.split(";");
					for (int i = 0; i < css.length; i++)
					{
						String[] cssProperty = css[i].split(":");
						//cssProperty[0] = property
						//cssProperty[1] = value

						for(int k = 0; k < cssProperty.length; k=k+2)
						{
							if(helperValues.get(cssProperty[k]) != null)
							{
								boolean newValueFound = false;
								for (String val : helperValues.get(cssProperty[k]))
								{
									if(!val.equalsIgnoreCase(cssProperty[k+1]))
									{
										newValue = newValue + cssProperty[k] + ":" + val;
										newValueFound = true;
										change = true;
									}
								}
								if(!newValueFound)
								{
									newValue = newValue + cssProperty[k] + ":" + cssProperty[k+1];
								}
							}
						}
					}
					e.attr(attr, newValue);
				}
				//html attributes
				else
				{
					if(helperValues.get(attr) != null)
					{
						for(String val : helperValues.get(attr))
						{
							if(!val.equalsIgnoreCase(value))
							{
								e.attr(attr, val);
								change = true;
							}
						}
					}
				}
			}
		}
		
		if(change)
		{
			File file = new File(testFilePath + File.separatorChar + "original_test.html");
			document.outputSettings().prettyPrint(false);
			document.outputSettings().escapeMode(Entities.EscapeMode.extended);
			String html = document.html();		
			PrintWriter out = new PrintWriter(file);
			out.print(html);
			out.close();
			
			// get test file screenshot again
			Util.getScreenshot(testFileName[0] + testFileName[1], testFilePath, testFileName[0] + Constants.SCREENSHOT_FILE_EXTENSION, null, false);
			
			// run tool again 
			WebSeeTool ci = new WebSeeTool(dirPath + File.separatorChar + originalFileName[0] + Constants.SCREENSHOT_FILE_EXTENSION, testFilePath + File.separatorChar + testFileName[0] + testFileName[1]);
			ci.runVisualInvariantsTool(originalFileName[0] + Constants.SCREENSHOT_FILE_EXTENSION, dirPath, 
					testFilePath, testFileName[0] + testFileName[1], reportFileName, specialRegionsFileFullPath, log, false);
			if(newOriginalSetOfErrorElements.get(0).size() > 0)
			{
				System.out.println("resolved");
			}
		}
		return newOriginalSetOfErrorElements;
	}
	
	private static Logger getNewLogger(String filePathWithName, String loggerName) throws IOException
	{
		Logger log = org.apache.log4j.Logger.getLogger(loggerName);
		PatternLayout layout = new PatternLayout(Layout.LINE_SEP + "%m");
	    FileAppender appender = new FileAppender(layout, filePathWithName, true);    
	    log.addAppender(appender);
	    return log;
	}
}
