package fulltest;

import java.io.File;
import java.io.IOException;

import org.jgap.InvalidConfigurationException;
import org.xml.sax.SAXException;

import util.Util;
import websee.WebSeeTool;
import config.Constants;

public class RunDemoExample
{
	public static void runRegressionDebuggingDemo() throws IOException, SAXException, InvalidConfigurationException
	{
		long startTime = System.nanoTime();
		
		String basePath = "/Users/sonal/USC/papers/2015/mahajan15icst-tool/demo/regression_debugging";

		// take oracle screenshot
		Util.getScreenshot("oracle.html", basePath + "/oracle/dbi.perl.org", "oracle.png", null, true);
		
		String oracleImagePath = basePath + "/oracle/dbi.perl.org/oracle.png";
		String testPagePath = basePath + "/test/dbi.perl.org/test.html";
		
		// run tool
		WebSeeTool wst = new WebSeeTool(oracleImagePath, testPagePath);
		wst.runWebSeeTool();
		
		System.out.println("Done");		
		long endTime = System.nanoTime();
		System.out.println("Total time = " + Util.convertNanosecondsToSeconds(endTime - startTime));
	}
	
	public static void runMockupDrivenDevelopmentDemo() throws IOException, SAXException, InvalidConfigurationException
	{
		long startTime = System.nanoTime();
		
		String basePath = "/Users/sonal/USC/papers/2015/mahajan15icst-tool/demo/mockup_driven_development";
		String specialRegions = basePath + File.separatorChar + Constants.SPECIAL_REGIONS_XML_FILENAME;

		// run tool
		WebSeeTool wst = new WebSeeTool(basePath + "/oracle.png", basePath + "/test.html", specialRegions);
		wst.runWebSeeTool();

		System.out.println("Done");		
		long endTime = System.nanoTime();
		System.out.println("Total time = " + Util.convertNanosecondsToSeconds(endTime - startTime));
	}
	
	public static void main(String[] args) throws IOException, SAXException, InvalidConfigurationException
	{
		runRegressionDebuggingDemo();
		//runMockupDrivenDevelopmentDemo();
	}
}
