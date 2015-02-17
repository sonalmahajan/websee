package evalframework;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import util.Util;

public class ProcessResultFile
{
	private String expectedString = "expected (seeded elements)";
	private String actualString = "Actual (error elements) All Heuristic";
	private String expectedXpathRegex = "xpath=(.+)}";
	private String actualXpathRegex = "\\[(.+)\\]";
	
	public void readCumulativeResultsFile(String dir, String fileName) throws IOException
	{
		File file = new File(dir + File.separatorChar + fileName);
		String newFileName[] = Util.getFileNameAndExtension(fileName, "_with_distance");
		File outFile = new File(dir + File.separatorChar + newFileName[0] + newFileName[1]);
		
		if(file.exists())
		{
			BufferedReader in = new BufferedReader(new FileReader(file));
			PrintWriter out = new PrintWriter(new FileWriter(outFile));
			String line;
			
			List<String> expectedXpaths = new ArrayList<String>();
			List<String> actualXpaths = new ArrayList<String>();

			while((line = in.readLine()) != null)
			{
				if(line.startsWith("path = "))
				{
					out.println("\n" + line);
				}
				if(line.startsWith(expectedString))
				{
					// parse xpaths
					expectedXpaths = getXpathsInList(Util.getValueFromRegex(expectedXpathRegex, line));
				}
				else if(line.startsWith(actualString))
				{
					// parse xpaths
					actualXpaths = getXpathsInList(Util.getValueFromRegex(actualXpathRegex, line));
					
					for(String eXpath : expectedXpaths)
					{
						out.println("\te -> " + eXpath);
						for(String aXpath : actualXpaths)
						{
							out.println("\ta -> " + aXpath + "  (d = " + Util.getDistance(eXpath, aXpath) + ")");
						}
					}
				}
			}
			in.close();
			out.close();
		}
	}

	private List<String> getXpathsInList(String xpaths)
	{
		List<String> xpathList = new ArrayList<String>();
		if(xpaths != null)
		{
			String xpathsArray[] = xpaths.split(",");
			
			for (String xpath : xpathsArray)
			{
				xpathList.add(xpath.trim());
			}
		}
		
		return xpathList;
	}

	public void writeExcelFile(String app, String resultFileWithPath, FileOutputStream excelOut) throws IOException
	{
		DecimalFormat decimal = new DecimalFormat("0.00");
		
		File file = new File(resultFileWithPath);
		if(file.exists())
		{
			BufferedReader in = new BufferedReader(new FileReader(file));

			HSSFWorkbook workbook = new HSSFWorkbook();
			HSSFSheet sheet = workbook.createSheet(app);
			Row row = sheet.createRow(0);
			Cell cell = row.createCell(0);
			cell.setCellValue("Test case no.");
			cell = row.createCell(1);
			cell.setCellValue("FE?");
			cell = row.createCell(2);
			cell.setCellValue("Rank NoH");
			cell = row.createCell(3);
			cell.setCellValue("Rank H1");
			cell = row.createCell(4);
			cell.setCellValue("Rank H2");
			cell = row.createCell(5);
			cell.setCellValue("Rank H3");
			cell = row.createCell(6);
			cell.setCellValue("Rank H4");
			cell = row.createCell(7);
			cell.setCellValue("Rank all H");
			
			
			int rownum = 1;
			int testCaseNumber = 0;
			String faultyElementFoundNoH = "";
			String faultyElementFoundH1 = "";
			String faultyElementFoundH2 = "";
			String faultyElementFoundH12 = "";
			int rankNoH = -1;
			int rankH1 = -1;
			int rankH2 = -1;
			int rankH3 = -1;
			int rankH4 = -1;
			int rankAllH = -1;
			int resultSetSizeNoH = -1;
			int resultSetSizeH1 = -1;
			int resultSetSizeH2 = -1;
			int resultSetSizeH3 = -1;
			int resultSetSizeH4 = -1;
			int resultSetSizeAllH = -1;
			double avgDistNoH = 0.0;
			double avgDistH1 = 0.0;
			double avgDistH2 = 0.0;
			double avgDistH12 = 0.0;
			double minDistNoH = 0.0;
			double minDistH1 = 0.0;
			double minDistH2 = 0.0;
			double minDistH12 = 0.0;
			double maxDistNoH = 0.0;
			double maxDistH1 = 0.0;
			double maxDistH2 = 0.0;
			double maxDistH12 = 0.0;
			double p1Time = -1;
			double p2Time = -1;
			double p3Time = -1;
			double p4Time = -1;
			double totalAvgTime = -1;
			
			String line;
			while((line = in.readLine()) != null)
			{
				if(line.contains("*************"))
				{
					testCaseNumber = Util.getNumbersFromString(line).get(0);
				}
				else if(line.contains("Phase 1"))
				{
					p1Time = Double.valueOf(Util.getValueFromRegex(": (\\d+\\.\\d+)", line));
				}
				else if(line.contains("Phase 2"))
				{
					p2Time = Double.valueOf(Util.getValueFromRegex(": (\\d+\\.\\d+)", line));
				}
				else if(line.contains("Phase 3"))
				{
					p3Time = Double.valueOf(Util.getValueFromRegex(": (\\d+\\.\\d+)", line));
				}
				else if(line.contains("Phase 4"))
				{
					p4Time = Double.valueOf(Util.getValueFromRegex(": (\\d+\\.\\d+)", line));
				}
				else if(line.contains("Time for this test case"))
				{
					totalAvgTime = Double.valueOf(Util.getValueFromRegex(": (\\d+\\.\\d+)", line));
				}
				else if(line.contains("Actual (error elements) No heuristic:"))
				{
					resultSetSizeNoH = Integer.valueOf(Util.getValueFromRegex("size=(\\d+)", line));
					int size = Integer.valueOf(resultSetSizeNoH);
					int totalDist = 0;
					faultyElementFoundNoH = "";
					
					for (int i = 0; i < size; i++)
					{
						line = in.readLine();
						int dist = Integer.valueOf(Util.getValueFromRegex("dist = (\\d+),", line));
						totalDist = totalDist + dist;
						
						// find rank
						if(dist == 0)
						{
							rankNoH = i + 1;
							faultyElementFoundNoH = "yes";
						}
						
						// find min dist
						if(dist < minDistNoH)
						{
							minDistNoH = dist;
						}
						
						// find max dist
						if(dist > maxDistNoH)
						{
							maxDistNoH = dist;
						}
					}
					if(faultyElementFoundNoH.isEmpty())
					{
						rankNoH = size;
					}
					
					// find avg dist
					if(size > 0)
					{
						avgDistNoH = Double.valueOf(decimal.format((double) totalDist/(double)size));
					}
				}
				else if(line.contains("Actual (error elements) Heuristic1:"))
				{
					resultSetSizeH1 = Integer.valueOf(Util.getValueFromRegex("size=(\\d+)", line));
					int size = Integer.valueOf(resultSetSizeH1);
					int totalDist = 0;
					faultyElementFoundH1 = "";
					
					for (int i = 0; i < size; i++)
					{
						line = in.readLine();
						int dist = Integer.valueOf(Util.getValueFromRegex("dist = (\\d+),", line));
						totalDist = totalDist + dist;
						
						// find rank
						if(dist == 0)
						{
							rankH1 = i + 1;
							faultyElementFoundH1 = "yes";
						}
						
						// find min dist
						if(dist < minDistH1)
						{
							minDistH1 = dist;
						}
						
						// find max dist
						if(dist > maxDistH1)
						{
							maxDistH1 = dist;
						}
					}
					if(faultyElementFoundH1.isEmpty())
					{
						rankH1 = size;
					}
					
					// find avg dist
					if(size > 0)
					{
						avgDistH1 = Double.valueOf(decimal.format((double) totalDist/(double)size));
					}
				}
				else if(line.contains("Actual (error elements) Heuristic2:"))
				{
					resultSetSizeH2 = Integer.valueOf(Util.getValueFromRegex("size=(\\d+)", line));
					int size = Integer.valueOf(resultSetSizeH2);
					int totalDist = 0;
					faultyElementFoundH2 = "";
					
					for (int i = 0; i < size; i++)
					{
						line = in.readLine();
						int dist = Integer.valueOf(Util.getValueFromRegex("dist = (\\d+),", line));
						totalDist = totalDist + dist;
						
						// find rank
						if(dist == 0)
						{
							rankH2 = i + 1;
							faultyElementFoundH2 = "yes";
						}
						
						// find min dist
						if(dist < minDistH2)
						{
							minDistH2 = dist;
						}
						
						// find max dist
						if(dist > maxDistH2)
						{
							maxDistH2 = dist;
						}
					}
					if(faultyElementFoundH2.isEmpty())
					{
						rankH2 = size;
					}
					
					// find avg dist
					if(size > 0)
					{
						avgDistH2 = Double.valueOf(decimal.format((double) totalDist/(double)size));
					}
				}
				else if(line.contains("Actual (error elements) Heuristic3:"))
				{
					resultSetSizeH3 = Integer.valueOf(Util.getValueFromRegex("size=(\\d+)", line));
					int size = Integer.valueOf(resultSetSizeH3);
					int totalDist = 0;
					faultyElementFoundH12 = "";
					
					for (int i = 0; i < size; i++)
					{
						line = in.readLine();
						try
						{
							Integer.valueOf(Util.getValueFromRegex("dist = (\\d+),", line));
						}
						catch(NumberFormatException e)
						{
							System.out.println("size = " + size);
							System.out.println("line = " + line);
						}
						int dist = Integer.valueOf(Util.getValueFromRegex("dist = (\\d+),", line));
						totalDist = totalDist + dist;
						
						// find rank
						if(dist == 0)
						{
							rankH3 = i + 1;
							faultyElementFoundH12 = "yes";
						}
						
						// find min dist
						if(dist < minDistH12)
						{
							minDistH12 = dist;
						}
						
						// find max dist
						if(dist > maxDistH12)
						{
							maxDistH12 = dist;
						}
					}
					if(faultyElementFoundH12.isEmpty())
					{
						rankH3 = size;
					}
					
					// find avg dist
					if(size > 0)
					{
						avgDistH12 = Double.valueOf(decimal.format((double) totalDist/(double)size));
					}
				}
				else if(line.contains("Actual (error elements) Heuristic4:"))
				{
					resultSetSizeH4 = Integer.valueOf(Util.getValueFromRegex("size=(\\d+)", line));
					int size = Integer.valueOf(resultSetSizeH4);
					int totalDist = 0;
					faultyElementFoundH12 = "";
					
					for (int i = 0; i < size; i++)
					{
						line = in.readLine();
						int dist = Integer.valueOf(Util.getValueFromRegex("dist = (\\d+),", line));
						totalDist = totalDist + dist;
						
						// find rank
						if(dist == 0)
						{
							rankH4 = i + 1;
							faultyElementFoundH12 = "yes";
						}
						
						// find min dist
						if(dist < minDistH12)
						{
							minDistH12 = dist;
						}
						
						// find max dist
						if(dist > maxDistH12)
						{
							maxDistH12 = dist;
						}
					}
					if(faultyElementFoundH12.isEmpty())
					{
						rankH4 = size;
					}
					
					// find avg dist
					if(size > 0)
					{
						avgDistH12 = Double.valueOf(decimal.format((double) totalDist/(double)size));
					}
				}
				else if(line.contains("Actual (error elements) All Heuristic:"))
				{
					resultSetSizeAllH = Integer.valueOf(Util.getValueFromRegex("size=(\\d+)", line));
					int size = Integer.valueOf(resultSetSizeAllH);
					int totalDist = 0;
					faultyElementFoundH12 = "";
					
					for (int i = 0; i < size; i++)
					{
						line = in.readLine();
						int dist = Integer.valueOf(Util.getValueFromRegex("dist = (\\d+),", line));
						totalDist = totalDist + dist;
						
						// find rank
						if(dist == 0)
						{
							rankAllH = i + 1;
							faultyElementFoundH12 = "yes";
						}
						
						// find min dist
						if(dist < minDistH12)
						{
							minDistH12 = dist;
						}
						
						// find max dist
						if(dist > maxDistH12)
						{
							maxDistH12 = dist;
						}
					}
					if(faultyElementFoundH12.isEmpty())
					{
						rankAllH = size;
					}
					
					// find avg dist
					if(size > 0)
					{
						avgDistH12 = Double.valueOf(decimal.format((double) totalDist/(double)size));
					}
				}
				
				if(resultSetSizeNoH > 0 && resultSetSizeH1 > 0 && resultSetSizeH2 > 0 && resultSetSizeH3 > 0 && resultSetSizeH4 > 0
						&& resultSetSizeAllH > 0)
				{
					row = sheet.createRow(rownum++);
					cell = row.createCell(0);
					cell.setCellValue(testCaseNumber);
					cell = row.createCell(1);
					cell.setCellValue(faultyElementFoundNoH);
					cell = row.createCell(2);
					cell.setCellValue(rankNoH);
					cell = row.createCell(3);
					cell.setCellValue(rankH1);
					cell = row.createCell(4);
					cell.setCellValue(rankH2);
					cell = row.createCell(5);
					cell.setCellValue(rankH3);
					cell = row.createCell(6);
					cell.setCellValue(rankH4);
					cell = row.createCell(7);
					cell.setCellValue(rankAllH);
					
					resultSetSizeNoH = -1;
					resultSetSizeH1 = -1;
					resultSetSizeH2 = -1;
					resultSetSizeH3 = -1;
					resultSetSizeH4 = -1;
					resultSetSizeAllH = -1;
					testCaseNumber = 0;
					faultyElementFoundNoH = "";
					faultyElementFoundH1 = "";
					faultyElementFoundH2 = "";
					faultyElementFoundH12 = "";
					rankNoH = -1;
					rankH1 = -1;
					rankH2 = -1;
					rankH3 = -1;
					rankH4 = -1;
					rankAllH = -1;
					avgDistNoH = 0.0;
					avgDistH1 = 0.0;
					avgDistH2 = 0.0;
					avgDistH12 = 0.0;
					minDistNoH = 10000.0;
					minDistH1 = 10000.0;
					minDistH2 = 10000.0;
					minDistH12 = 10000.0;
					maxDistNoH = -1.0;
					maxDistH1 = -1.0;
					maxDistH2 = -1.0;
					maxDistH12 = -1.0;
					p1Time = -1;
					p2Time = -1;
					p3Time = -1;
					p4Time = -1;
					totalAvgTime = -1;
				}
			}
			rownum = rownum + 5;
			row = sheet.createRow(rownum);
			cell = row.createCell(5);
			cell.setCellValue("NoH");
			cell = row.createCell(6);
			cell.setCellValue("H1");
			cell = row.createCell(7);
			cell.setCellValue("H2");
			cell = row.createCell(8);
			cell.setCellValue("H3");
			cell = row.createCell(9);
			cell.setCellValue("H4");
			cell = row.createCell(10);
			cell.setCellValue("All H");
			row = sheet.createRow(++rownum);
			cell = row.createCell(1);
			cell.setCellValue("avg rank overall");
			row = sheet.createRow(++rownum);
			cell = row.createCell(1);
			cell.setCellValue("avg rank faulty ele found");
			
			workbook.write(excelOut);
			in.close();
		}
	}
	
	public static void main(String[] args) throws IOException
	{
		ProcessResultFile prf = new ProcessResultFile();
		String basePath = "C:\\USC\\visual_checking\\evaluation\\ICSE";
		
		try {
		    FileOutputStream out = new FileOutputStream(new File(basePath + File.separatorChar + "new_oracle_eval.xls"));
		    prf.writeExcelFile("oracle", basePath + File.separatorChar + "oracle.txt", out);
		    out.close();
		    System.out.println("Excel written successfully..");
		     
		} catch (FileNotFoundException e) {
		    e.printStackTrace();
		} catch (IOException e) {
		    e.printStackTrace();
		}
		//System.out.println("Actual (error elements) Heuristic12: (size=1):".contains("Actual (error elements) Heuristic1:"));
	}
}
