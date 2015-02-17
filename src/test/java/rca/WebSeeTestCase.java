package rca;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jgap.InvalidConfigurationException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.xml.sax.SAXException;

import util.Util;
import websee.HtmlElement;
import websee.Node;
import websee.WebSeeTool;
import config.Constants;

/**
 * Test case for Scenario 1. Given oracle image and test page.
 */
public class WebSeeTestCase {

	// files
	private String testcasepath;
	private String specialregionpath;
	private String logpath;
	private String reportpath;
	private Logger detailedLog;

	WebSeeTestCase(String testcasepath) throws IOException {
		System.out.println(testcasepath);
		String testcasename = new File(testcasepath).getParent() + '_'
				+ new File(testcasepath).getName();
		System.out.println("Init " + testcasename);
		this.testcasepath = new File(testcasepath).getAbsolutePath();
		this.specialregionpath = this.testcasepath + "/"
				+ Constants.SPECIAL_REGIONS_XML_FILENAME;
		this.logpath = this.testcasepath
				+ "/"
				+ Util.getNextAvailableFileName(this.testcasepath,
						Constants.CUMULATIVE_RESULT_DETAILED_FILENAME);
		this.detailedLog = Util.getNewLogger(this.logpath, "detailedLogger"
				+ testcasename);
		this.reportpath = "test_report.txt";
		cleanResultFiles();
		cleanIntermediateFiles();
	}

	public void run(String expectedElementXpath, Logger webSeeLog, RunS1Test s1Obj) {
		try {
			Document doc = Jsoup.parse(new File(this.testcasepath + File.separatorChar + "oracle.html"), null);
			doc.outputSettings().prettyPrint(false);
			String html = doc.html();
			PrintWriter out = new PrintWriter(new File(this.testcasepath + File.separatorChar + "oracle.html"));
			out.print(html);
			out.close();
			doc = Jsoup.parse(new File(this.testcasepath + File.separatorChar + "oracle.html"), null);
			doc.outputSettings().prettyPrint(false);
			html = doc.html();
			out = new PrintWriter(new File(this.testcasepath + File.separatorChar + "oracle.html"));
			out.print(html);
			out.close();

			doc = Jsoup.parse(new File(this.testcasepath + File.separatorChar + "test.html"), null);
			doc.outputSettings().prettyPrint(false);
			html = doc.html();
			out = new PrintWriter(new File(this.testcasepath + File.separatorChar + "test.html"));
			out.print(html);
			out.close();
			doc = Jsoup.parse(new File(this.testcasepath + File.separatorChar + "test.html"), null);
			doc.outputSettings().prettyPrint(false);
			html = doc.html();
			out = new PrintWriter(new File(this.testcasepath + File.separatorChar + "test.html"));
			out.print(html);
			out.close();

			Util.getScreenshot("oracle.html", testcasepath, "oracle.png", true);

			/*WebSeeTool vit = new WebSeeTool(
					this.testcasepath + File.separatorChar + "oracle.png",
					this.testcasepath + File.separatorChar + "test.html");*/
			WebSeeTool vit = new WebSeeTool(
					this.testcasepath + File.separatorChar + "oracle.png",
					this.testcasepath + File.separatorChar + "test.html", s1Obj);
			//vit.setConfig(true, false, true, true, true, true, false, "");
			
			//vit.runVisualInvariantsTool("oracle.png", this.testcasepath, this.testcasepath, "test.html", this.reportpath, this.specialregionpath, this.detailedLog, Constants.DO_NOT_OVERWRITE);
			vit.runWebSeeToolWithoutRCA("oracle.png", this.testcasepath, this.testcasepath, "test.html", this.reportpath, this.specialregionpath, this.detailedLog, Constants.DO_NOT_OVERWRITE);
			
			boolean isFound = false;
			for(Integer cid : vit.getClusterElementsMap().keySet())
			{
				s1Obj.getWebSeeResultSetSizeStats().addValue(vit.getClusterElementsMap().get(cid).size());
				List<Node<HtmlElement>> procesedResultSet = vit.getClusterElementsMap().get(cid);
				int rankCounter = 0;
				for (Node<HtmlElement> node : procesedResultSet)
				{
					rankCounter++;
					if(node.getData().getXpath().equalsIgnoreCase(expectedElementXpath))
					{
						webSeeLog.info(testcasepath + "\t" + rankCounter + "\t" + procesedResultSet.size() + "\t");
						s1Obj.getWebSeeRankStats().addValue(rankCounter);
						isFound = true;
						break;
					}
				}
				if(!isFound)
				{
					s1Obj.getWebSeeRankStats().addValue(rankCounter);
					webSeeLog.info(testcasepath + "\t" + rankCounter + "\t" + procesedResultSet.size() + "\t" + "miss");
					double totalDistance = 0.0;
					for (Node<HtmlElement> node : procesedResultSet)
					{
						totalDistance = totalDistance + Util.getDistance(expectedElementXpath, node.getData().getXpath());
					}
					if(procesedResultSet.size() > 0)
					{
						s1Obj.getWebSeeDistanceStats().addValue(totalDistance/procesedResultSet.size());
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*public void runSA(String expectedElementXpath, Logger webSeeLog, DescriptiveStatistics webSeeStats) {
		try {
			Document doc = Jsoup.parse(new File(this.testcasepath + File.separatorChar + "oracle.html"), null);
			doc.outputSettings().prettyPrint(false);
			String html = doc.html();
			PrintWriter out = new PrintWriter(new File(this.testcasepath + File.separatorChar + "oracle.html"));
			out.print(html);
			out.close();
			doc = Jsoup.parse(new File(this.testcasepath + File.separatorChar + "oracle.html"), null);
			doc.outputSettings().prettyPrint(false);
			html = doc.html();
			out = new PrintWriter(new File(this.testcasepath + File.separatorChar + "oracle.html"));
			out.print(html);
			out.close();

			doc = Jsoup.parse(new File(this.testcasepath + File.separatorChar + "test.html"), null);
			doc.outputSettings().prettyPrint(false);
			html = doc.html();
			out = new PrintWriter(new File(this.testcasepath + File.separatorChar + "test.html"));
			out.print(html);
			out.close();
			doc = Jsoup.parse(new File(this.testcasepath + File.separatorChar + "test.html"), null);
			doc.outputSettings().prettyPrint(false);
			html = doc.html();
			out = new PrintWriter(new File(this.testcasepath + File.separatorChar + "test.html"));
			out.print(html);
			out.close();

			Util.getScreenshot("oracle.html", testcasepath, "oracle.png", true);

			WebSeeTool vit = new WebSeeTool(
					this.testcasepath + File.separatorChar + "oracle.png",
					this.testcasepath + File.separatorChar + "test.html");
			vit.setConfig(true, false, true, true, true, true, false, "");
			
			List<Set<Node<HtmlElement>>> returnValue = vit.runVisualInvariantsTool("oracle.png", this.testcasepath,
					this.testcasepath, "test.html", this.reportpath,
					this.specialregionpath, this.detailedLog,
					Constants.DO_NOT_OVERWRITE);
			
			
			Set<Node<HtmlElement>> procesedResultSet = returnValue.get(1);
			int rankCounter = 0;
			boolean isFound = false;
			for (Node<HtmlElement> node : procesedResultSet)
			{
				rankCounter++;
				if(node.getData().getXpath().equalsIgnoreCase(expectedElementXpath))
				{
					webSeeLog.info(testcasepath + "\t" + rankCounter + "\t" + procesedResultSet.size() + "\t");
					webSeeStats.addValue(rankCounter);
					isFound = true;
					break;
				}
			}
			if(!isFound)
			{
				webSeeStats.addValue(rankCounter);
				webSeeLog.info(testcasepath + "\t" + rankCounter + "\t" + procesedResultSet.size() + "\t" + "miss");
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}*/
	
	public void cleanIntermediateFiles() throws IOException {
		//FileUtils.deleteDirectory(new File(this.testcasepath + "/RCA"));
		FileUtils.deleteDirectory(new File(this.testcasepath + "/crops"));
		//new File(this.testcasepath + "/diff_oracle_test.txt").delete();
		//new File(this.testcasepath + "/filtered_diff_oracle_test.txt").delete();
	}

	public void cleanResultFiles() {
		new File(this.testcasepath + "/cumulative_result_detailed.txt")
				.delete();
		new File(this.testcasepath + "/marked_test.html").delete();
		new File(this.testcasepath + "/RCA_details.txt").delete();
		new File(this.testcasepath + "/RCA_results.txt").delete();
		new File(this.testcasepath + "/test_report.txt").delete();
		new File(this.testcasepath + "/diff_oracle_test.png").delete();
		new File(this.testcasepath + "/oracle.png").delete();
		new File(this.testcasepath + "/test.png").delete();
	}
}
