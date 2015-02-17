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
		Logger log = getNewLogger(basePath + File.separatorChar + Util.getNextAvailableFileName(basePath, Constants.CUMULATIVE_RESULT_DETAILED_FILENAME), "detailedLogger" + basePath);
		ResultsVerification rv = new ResultsVerification();
		rv.printInitial(log, basePath);
		wst.setConfig(true, false, false, true, true, true, false, "");
		wst.runWebSeeToolWithoutRCA("oracle.png", basePath, basePath, "test.html", reportFileName, specialRegions, log, Constants.DO_NOT_OVERWRITE);
		System.out.println("Done");
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
