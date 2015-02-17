package rca;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;

import util.Util;

/**
 * Experiment for Scenario 1. Given oracle image and test page.
 * 
 */
public class RunS1Test {
	
	private DescriptiveStatistics webSeeRankStats;
	private DescriptiveStatistics webSeeResultSetSizeStats;
	private DescriptiveStatistics webSeeDistanceStats;
	private DescriptiveStatistics webSeeP1TimeStats;
	private DescriptiveStatistics webSeeP2TimeStats;
	private DescriptiveStatistics webSeeP3TimeStats;
	private DescriptiveStatistics webSeeP4TimeStats;
	private DescriptiveStatistics webSeeP5TimeStats;
	public DescriptiveStatistics getWebSeeP5TimeStats()
	{
		return webSeeP5TimeStats;
	}
	public void setWebSeeP5TimeStats(DescriptiveStatistics webSeeP5TimeStats)
	{
		this.webSeeP5TimeStats = webSeeP5TimeStats;
	}

	private DescriptiveStatistics webSeeTotalTimeStats;
	
	public RunS1Test()
	{
		webSeeRankStats = new DescriptiveStatistics();
		webSeeResultSetSizeStats = new DescriptiveStatistics();
		webSeeDistanceStats = new DescriptiveStatistics();
		webSeeP1TimeStats = new DescriptiveStatistics();
		webSeeP2TimeStats = new DescriptiveStatistics();
		webSeeP3TimeStats = new DescriptiveStatistics();
		webSeeP4TimeStats = new DescriptiveStatistics();
		webSeeP5TimeStats = new DescriptiveStatistics();
		webSeeTotalTimeStats = new DescriptiveStatistics();
	}
	public DescriptiveStatistics getWebSeeRankStats()
	{
		return webSeeRankStats;
	}
	public void setWebSeeRankStats(DescriptiveStatistics webSeeRankStats)
	{
		this.webSeeRankStats = webSeeRankStats;
	}
	public DescriptiveStatistics getWebSeeResultSetSizeStats()
	{
		return webSeeResultSetSizeStats;
	}
	public void setWebSeeResultSetSizeStats(DescriptiveStatistics webSeeResultSetSizeStats)
	{
		this.webSeeResultSetSizeStats = webSeeResultSetSizeStats;
	}
	public DescriptiveStatistics getWebSeeDistanceStats()
	{
		return webSeeDistanceStats;
	}
	public void setWebSeeDistanceStats(DescriptiveStatistics webSeeDistanceStats)
	{
		this.webSeeDistanceStats = webSeeDistanceStats;
	}
	public DescriptiveStatistics getWebSeeP1TimeStats()
	{
		return webSeeP1TimeStats;
	}
	public void setWebSeeP1TimeStats(DescriptiveStatistics webSeeP1TimeStats)
	{
		this.webSeeP1TimeStats = webSeeP1TimeStats;
	}
	public DescriptiveStatistics getWebSeeP2TimeStats()
	{
		return webSeeP2TimeStats;
	}
	public void setWebSeeP2TimeStats(DescriptiveStatistics webSeeP2TimeStats)
	{
		this.webSeeP2TimeStats = webSeeP2TimeStats;
	}
	public DescriptiveStatistics getWebSeeP3TimeStats()
	{
		return webSeeP3TimeStats;
	}
	public void setWebSeeP3TimeStats(DescriptiveStatistics webSeeP3TimeStats)
	{
		this.webSeeP3TimeStats = webSeeP3TimeStats;
	}
	public DescriptiveStatistics getWebSeeP4TimeStats()
	{
		return webSeeP4TimeStats;
	}
	public void setWebSeeP4TimeStats(DescriptiveStatistics webSeeP4TimeStats)
	{
		this.webSeeP4TimeStats = webSeeP4TimeStats;
	}
	public DescriptiveStatistics getWebSeeTotalTimeStats()
	{
		return webSeeTotalTimeStats;
	}
	public void setWebSeeTotalTimeStats(DescriptiveStatistics webSeeTotalTimeStats)
	{
		this.webSeeTotalTimeStats = webSeeTotalTimeStats;
	}

	public static void main(String[] args) throws IOException {
		RunS1Test r = new RunS1Test();
		r.run("evaluation/test");
	}

	public void run(String basePath) throws IOException {
		Logger webSeeLog = Util.getNewLogger(basePath + File.separatorChar + "WebSee_statistics.txt", "WebSee_statistics" + System.currentTimeMillis());
		webSeeLog.info("");
		webSeeLog.info("--------------------------------------------------------------------------");
		webSeeLog.info("Test case" + "\t" + "Rank" + "\t" + "Result set size" + "\t" + "Hit/Miss");
		
		BufferedReader br = new BufferedReader(new FileReader(basePath
				+ "/description.txt"));
		String line;
		while ((line = br.readLine()) != null) {
			String[] lineSplit = line.split("\t");
			String id = lineSplit[1];
			if (id.equals("no seeding")) {
				System.out.println("skip");
				continue;
			}
			String testFolder = lineSplit[8];
			String xpath = lineSplit[7];
			try {
				WebSeeTestCase wstc = new WebSeeTestCase(basePath + "/"
						+ testFolder);
				wstc.run(xpath, webSeeLog, this);
				wstc.cleanIntermediateFiles();
			} catch (Exception e) {
				System.err.println(testFolder);
				System.err.println(e.getMessage());
				e.printStackTrace();
			}
			// Compute WebSee statistics
			double mean = webSeeRankStats.getMean();
			double std = webSeeRankStats.getStandardDeviation();
			double median = webSeeRankStats.getPercentile(50);
			
			webSeeLog.info("");
			webSeeLog.info("\t\t\t\t\tResults so far");
			webSeeLog.info("\t\t\t\t\tMean rank = " + mean);
			webSeeLog.info("\t\t\t\t\tMedian rank = " + median);
			webSeeLog.info("\t\t\t\t\tStandard deviation rank = " + std);
		}
		br.close();

		// Compute WebSee statistics
		webSeeLog.info("");
		webSeeLog.info("-------- FINAL RESULTS --------");
		webSeeLog.info("");
		webSeeLog.info("Mean rank = " + webSeeRankStats.getMean());
		webSeeLog.info("Median rank = " + webSeeRankStats.getPercentile(50));
		webSeeLog.info("Standard deviation rank = " + webSeeRankStats.getStandardDeviation());
		webSeeLog.info("");
		webSeeLog.info("Mean result set size = " + webSeeResultSetSizeStats.getMean());
		webSeeLog.info("Median result set size = " + webSeeResultSetSizeStats.getPercentile(50));
		webSeeLog.info("Standard deviation result set size = " + webSeeResultSetSizeStats.getStandardDeviation());
		webSeeLog.info("");
		webSeeLog.info("Mean distance = " + webSeeDistanceStats.getMean());
		webSeeLog.info("Median distance = " + webSeeDistanceStats.getPercentile(50));
		webSeeLog.info("Standard deviation distance = " + webSeeDistanceStats.getStandardDeviation());
		webSeeLog.info("");
		webSeeLog.info("Mean P1 time = " + webSeeP1TimeStats.getMean());
		webSeeLog.info("Mean P2 time = " + webSeeP2TimeStats.getMean());
		webSeeLog.info("Mean P3 time = " + webSeeP3TimeStats.getMean());
		webSeeLog.info("Mean P4 time = " + webSeeP4TimeStats.getMean());
		webSeeLog.info("Mean P5 time = " + webSeeP5TimeStats.getMean());
		webSeeLog.info("Mean Total time = " + webSeeTotalTimeStats.getMean());
	}
}