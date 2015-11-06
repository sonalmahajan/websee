package fulltest;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jgap.InvalidConfigurationException;
import org.xml.sax.SAXException;

import util.Util;
import websee.WebSeeTool;
import config.Constants;
import evalframework.ResultsVerification;

public class RunDemoExample
{
	public static void main(String[] args) throws IOException, SAXException, InvalidConfigurationException
	{
		String basePath = "/home/sonal/websee";

		Util.getScreenshot("oracle.html", basePath, "oracle.png", true);

		WebSeeTool wst = new WebSeeTool(basePath + File.separatorChar + "oracle.png", basePath + File.separatorChar + "test.html");
		String reportFileName = "test_report.txt";		
		
		// run tool
		String specialRegions = basePath + File.separatorChar + Constants.SPECIAL_REGIONS_XML_FILENAME;
		System.out.println("Done");
	}	
}
