package shortpaper;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jgap.InvalidConfigurationException;

import rca.GAFitnessFunction;
import rca.RandomSearch;
import util.Util;
import websee.HtmlElement;

public class SPTestCase {

	// files
	private String testcasepath;
	private String oraclepath;
	private String testpath;
	private String detailpath;
	private Logger detailsLog;

	SPTestCase(String testcasepath) throws IOException {
		System.out.println(testcasepath);
		String testcasename = new File(testcasepath).getParent() + '_'
				+ new File(testcasepath).getName();
		this.testcasepath = new File(testcasepath).getAbsolutePath();
		this.oraclepath = this.testcasepath + "/oracle.png";
		this.testpath = this.testcasepath + "/test.html";
		this.detailpath = this.testcasepath + "/RCA_details.txt";
		cleanResultFiles();
		cleanIntermediateFiles();
		this.detailsLog = Util.getNewLogger(this.detailpath, "RCA_details_"
				+ testcasename);
	}

	public void runNumeric(String xpath, String property, String value)
			throws IOException, InvalidConfigurationException {
		Util.getScreenshot("oracle.html", this.testcasepath, "oracle.png", null, true);
		HtmlElement element = new HtmlElement();
		element.setXpath(xpath);
		System.out.println(xpath);
		String visualProperty = "style:" + property + ":px";
		String visualPropertyValue = "100.0px";

		if (value.indexOf("px") != -1)
			value = value.substring(0, value.length() - 2);
		int value_num = Integer.parseInt(value);
		int low = value_num - 30;
		int high = value_num + 30;

		// run genetic algorithm without initialization search space heuristics
		GAFitnessFunction ga = new GAFitnessFunction(element, visualProperty,
				this.oraclepath, this.testpath,
				Util.getDecimalNumberFromString(visualPropertyValue),
				this.detailsLog);
		long startTime = System.nanoTime();
		ga.runGAForNumericAnalysis(low, high);
		long totalTime = System.nanoTime() - startTime;
		this.detailsLog.info("GA TOTAL TIME = "
				+ Util.convertNanosecondsToSeconds(totalTime) + " sec");

		// run random search
		RandomSearch rs = new RandomSearch(element, visualProperty,
				this.testpath, this.oraclepath, this.detailsLog);
		startTime = System.nanoTime();
		rs.randomNumericSearch(totalTime, low, high);
		totalTime = System.nanoTime() - startTime;
		this.detailsLog.info("RANDOM TOTAL TIME = "
				+ Util.convertNanosecondsToSeconds(totalTime) + " sec");
	}

	public void runColor(String xpath, String property, String value)
			throws IOException, InvalidConfigurationException {
		Util.getScreenshot("oracle.html", this.testcasepath, "oracle.png", null, true);
		HtmlElement element = new HtmlElement();
		element.setXpath(xpath);
		System.out.println(xpath);
		String visualProperty = "style:" + property + ":";
		String visualPropertyValue = "#FFF000";

		if (value.indexOf("#") != -1)
			value = value.substring(1);
		int low = Util.getDecimalFromHex(value);
		if (low > 128)
			low = low - 128;
		int high = low + 128;

		// run genetic algorithm without initialization search space heuristics
		System.out.println("FINDING ORACLE COLOR USING AVERAGE COLOR");
		GAFitnessFunction ga = new GAFitnessFunction(element, visualProperty,
				this.oraclepath, this.testpath,
				Util.getDecimalNumberFromString(visualPropertyValue),
				this.detailsLog);
		ga.setOracleColor(ga.computeAverageColor(this.testcasepath
				+ File.separatorChar + "oracle.png"));
		long startTime = System.nanoTime();
		ga.runGAForColorAnalysis(low, high);
		long totalTime = System.nanoTime() - startTime;
		this.detailsLog.info("GA TOTAL TIME = "
				+ Util.convertNanosecondsToSeconds(totalTime) + " sec");

		// run random search
		RandomSearch rs = new RandomSearch(element, visualProperty,
				this.testpath, this.oraclepath, this.detailsLog);
		startTime = System.nanoTime();
		rs.randomColorSearch(ga.getOracleColor(), ga.getPixelsInElement(),
				totalTime, low, high);
		totalTime = System.nanoTime() - startTime;
		this.detailsLog.info("RANDOM TOTAL TIME = "
				+ Util.convertNanosecondsToSeconds(totalTime) + " sec");
	}

	public void cleanIntermediateFiles() throws IOException {
		FileUtils.deleteDirectory(new File(this.testcasepath + "/GA"));
		FileUtils
				.deleteDirectory(new File(this.testcasepath + "/RandomNumeric"));
		new File(this.testcasepath + "/diff_oracle_test.txt").delete();
		new File(this.testcasepath + "/filtered_diff_oracle_test.txt").delete();
	}

	public void cleanResultFiles() {
		new File(this.testcasepath + "/RCA_details.txt").delete();
		new File(this.testcasepath + "/oracle.png").delete();
		new File(this.testcasepath + "/diff_oracle_test.png").delete();
	}
}
