package rca;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Experiment for Scenario 2. Given oracle image, test page, and HTML element
 * (by XPath).
 * 
 */
public class RunS2NoFitnessTest {

	public static void main(String[] args) throws IOException {
		run("evaluation/new/www.gmail.com");
		run("evaluation/new/www.cs.usc.edu");
		run("evaluation/new/losangeles.craigslist.org");
		run("evaluation/new/docs.oracle.com");
		run("evaluation/new/www.virginamerica.com");
	}

	public static void run(String basePath) throws IOException {
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
			try {
				RCATestCase rtc = new RCATestCase(basePath + "/" + testFolder);
				String xpath = lineSplit[7];
				rtc.setupByElement(xpath);
				rtc.runWithoutFitnessFunction();
				rtc.cleanIntermediateFiles();
			} catch (Exception e) {
				System.err.println(testFolder);
				System.err.println(e.getMessage());
				e.printStackTrace();
			}
		}
		br.close();
	}
}
