package fulltest;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.jgap.InvalidConfigurationException;
import org.xml.sax.SAXException;

import util.Util;
import websee.WebSeeTool;

public class InternationalizationEval
{
	private static void detectionEval(String[] args) throws IOException, SAXException, InvalidConfigurationException
	{
		// -b baseFilePath -> path of oracle HTML page
		// -t testFilePath -> path of test HTML page
		// -o outputFilePath -> path of output text file
		// [optional] -s specialRegionsFilePath :: full path of special regions XML file
		
		if(args.length < 6)
		{
			usageDetection();
			return;
		}
		
		String baseFilePath = "", testFilePath = "", outputFilePath = "", specialRegionsFilePath = "";
		if(args[0].equalsIgnoreCase("-b"))
		{
			baseFilePath = args[1];
		}
		if(args[2].equalsIgnoreCase("-t"))
		{
			testFilePath = args[3];
		}
		if(args[4].equalsIgnoreCase("-o"))
		{
			outputFilePath = args[5];
		}
		if(args.length > 6 && args[6].equalsIgnoreCase("-s"))
		{
			specialRegionsFilePath = args[7];
		}
		if(baseFilePath.isEmpty() || testFilePath.isEmpty() || outputFilePath.isEmpty())
		{
			usageDetection();
			return;
		}
		
		
		long startTime = System.nanoTime();
		
		String basePathAndNameOracle[] = Util.getPathAndFileNameFromFullPath(baseFilePath);

		// take oracle screenshot
		Util.getScreenshot(basePathAndNameOracle[1], basePathAndNameOracle[0], "oracle.png", null, true);
		String oracleImagePath = basePathAndNameOracle[0] + File.separatorChar + "oracle.png";
		
		// run tool
		WebSeeTool wst;
		if(specialRegionsFilePath.isEmpty())
		{
			wst = new WebSeeTool(oracleImagePath, testFilePath);
		}
		else
		{
			wst = new WebSeeTool(oracleImagePath, testFilePath, specialRegionsFilePath);
		}
		boolean isDifferent = wst.runWebSeeToolWithOnlyDetection();
		
		// write output as true = if presentation failure detected, false otherwise
		PrintWriter out = new PrintWriter(new File(outputFilePath));
		if(isDifferent)
		{
			out.println("true");
		}
		else
		{
			out.println("false");
		}
		out.close();
		
		System.out.println("Done");		
		long endTime = System.nanoTime();
		System.out.println("Total time = " + Util.convertNanosecondsToSeconds(endTime - startTime));	
	}
	
	private static void usageDetection()
	{
		System.out.println("Usage:");
		System.out.println("-b baseFilePath :: full path of base/oracle HTML page");
		System.out.println("-t testFilePath :: full path of test HTML page");
		System.out.println("-o outputFilePath :: full path of output text file");
		System.out.println("[optional] -s specialRegionsFilePath :: full path of special regions XML file");
	}
	
	private static void localizationEval(String[] args) throws IOException, SAXException, InvalidConfigurationException
	{
		// -b baseFilePath -> path of oracle HTML page
		// -t testFilePath -> path of test HTML page
		// -l localizedOutputFilePath -> path of output text file to store localized results
		// -m timeFilePath -> path of output text file to store time results
		// [optional] -s specialRegionsFilePath :: full path of special regions XML file
		
		if(args.length < 8)
		{
			usageLocalization();
			return;
		}
		
		String baseFilePath = "", testFilePath = "", outputFilePath = "", specialRegionsFilePath = "", timeResultsFilePath = "";
		if(args[0].equalsIgnoreCase("-b"))
		{
			baseFilePath = args[1];
		}
		if(args[2].equalsIgnoreCase("-t"))
		{
			testFilePath = args[3];
		}
		if(args[4].equalsIgnoreCase("-l"))
		{
			outputFilePath = args[5];
		}
		if(args[6].equalsIgnoreCase("-m"))
		{
			timeResultsFilePath = args[7];
		}
		if(args.length > 8 && args[8].equalsIgnoreCase("-s"))
		{
			specialRegionsFilePath = args[9];
		}
		if(baseFilePath.isEmpty() || testFilePath.isEmpty() || outputFilePath.isEmpty())
		{
			usageLocalization();
			return;
		}
		
		String basePathAndNameOracle[] = Util.getPathAndFileNameFromFullPath(baseFilePath);

		// take oracle screenshot
		Util.getScreenshot(basePathAndNameOracle[1], basePathAndNameOracle[0], "oracle.png", null, true);
		String oracleImagePath = basePathAndNameOracle[0] + File.separatorChar + "oracle.png";
		
		// run tool
		WebSeeTool wst;
		if(specialRegionsFilePath.isEmpty())
		{
			wst = new WebSeeTool(oracleImagePath, testFilePath);
		}
		else
		{
			wst = new WebSeeTool(oracleImagePath, testFilePath, specialRegionsFilePath);
		}
		wst.runWebSeeToolWithNoClustering(timeResultsFilePath, outputFilePath);
		
		System.out.println("Done");		
	}
	
	private static void usageLocalization()
	{
		System.out.println("Usage:");
		System.out.println("-b baseFilePath :: full path of base/oracle HTML page");
		System.out.println("-t testFilePath :: full path of test HTML page");
		System.out.println("-l outputFilePath :: full path of localized output text file");
		System.out.println("-m timeFilePath :: full path of time text file");
		System.out.println("[optional] -s specialRegionsFilePath :: full path of special regions XML file");
	}
	
	public static void main(String[] args) throws IOException, SAXException, InvalidConfigurationException
	{
		//detectionEval(args);
		
		long startTime = System.nanoTime();
		localizationEval(args);
		long endTime = System.nanoTime();
		System.out.println("Total time = " + Util.convertNanosecondsToSeconds(endTime - startTime));	
	}
}