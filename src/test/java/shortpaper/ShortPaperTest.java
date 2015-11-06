package shortpaper;

import java.io.File;
import java.io.IOException;

import org.jgap.InvalidConfigurationException;

import rca.GAFitnessFunction;
import rca.RandomSearch;
import util.Util;
import websee.HtmlElement;

public class ShortPaperTest
{
	public static void main(String[] args) throws IOException, InvalidConfigurationException
	{
		String basePath = "C:\\USC\\visual_checking\\evaluation\\root_cause_analysis\\short_paper";
		String oracleFullPath = basePath + File.separatorChar + "oracle.png";
		String testPageFullPath = basePath + File.separatorChar + "test.html";
		
		Util.getScreenshot("oracle.html", basePath, "oracle.png", null, true);
		
		HtmlElement element = new HtmlElement();
		//String xpath = "/html[1]/body[1]/div[1]/div[2]/div[2]/div[2]/div[1]/h4[1]";
		//String xpath = "/html[1]/body[1]/div[1]/div[3]/div[1]/ul[1]/li[4]/a[1]";
		String xpath = "/html[1]/body[1]/div[1]/div[2]/div[2]/form[1]/input[10]";
		element.setXpath(xpath);
		
		/*String visualProperty = "style:padding:px";
		String visualPropertyValue = "10px";*/
		String visualProperty = "style:border-top-color:";
		String visualPropertyValue = "#80f216";
		
		int low = -10;
		int high = 10;
		
		// run genetic algorithm without initialization search space heuristics
		/*GAFitnessFunction ga = new GAFitnessFunction(element, visualProperty, oracleFullPath, testPageFullPath, Util.getDecimalNumberFromString(visualPropertyValue), Util.getNewLogger(basePath + File.separatorChar + "RCA_details.txt", "RCA_details"));
		long startTime = System.nanoTime();
		ga.runGAForNumericAnalysis(low, high);
		long totalTime = System.nanoTime() - startTime;
		
		// run random search
		RandomSearch rs = new RandomSearch(element, visualProperty, testPageFullPath, oracleFullPath);
		rs.randomNumericSearch(totalTime, low, high);*/
		
		// ******************************** COLOR ANALYSIS *******************************************
		low = Util.getDecimalFromHex("C0C040");
		high = Util.getDecimalFromHex("C0C0C0");

		System.out.println("FINDING ORACLE COLOR USING AVERAGE COLOR");
		GAFitnessFunction ga = new GAFitnessFunction(element, visualProperty, oracleFullPath, testPageFullPath, Util.getDecimalNumberFromString(visualPropertyValue), Util.getNewLogger(basePath + File.separatorChar + "RCA_details.txt", "RCA_details"));
		ga.setOracleColor(ga.computeAverageColor(oracleFullPath));
		
		System.out.println("GA COLOR ANALYSIS");
		long startTime = System.nanoTime();
		ga.runGAForColorAnalysis(low, high);
		long totalTime = System.nanoTime() - startTime;
		
		System.out.println("GA TOTAL TIME = " + Util.convertNanosecondsToSeconds(totalTime) + " sec");
		
		System.out.println("RANDOM COLOR ANALYSIS");
		// run random search
		RandomSearch rs = new RandomSearch(element, visualProperty, testPageFullPath, oracleFullPath);
		rs.randomColorSearch(ga.getOracleColor(), ga.getPixelsInElement(), totalTime, low, high);
	}
}