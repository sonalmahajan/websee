package fulltest;

import java.io.IOException;

import org.jgap.InvalidConfigurationException;
import org.xml.sax.SAXException;

import util.Util;
import websee.WebSeeTool;

public class RunLocalDemo
{
	public static void main(String[] args) throws IOException, SAXException, InvalidConfigurationException
	{
		long startTime = System.nanoTime();
		
		String basePath = "src/test/resources/testcases/localDemo";

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

}
