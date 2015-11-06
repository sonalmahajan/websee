package rca;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Experiment for Scenario 3. Given oracle image, test page, HTML element (by
 * XPath), and visual property.
 * 
 */
public class RunS3Test {

	public static void main(String[] args) throws IOException {
		run("evaluation/new/www.gmail.com");
		run("evaluation/new/docs.oracle.com");
		run("evaluation/new/losangeles.craigslist.org");
		run("evaluation/new/www.cs.usc.edu");
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
				String name = lineSplit[2];
				String type = lineSplit[3];
				String value = lineSplit[6];
				rtc.setupByElementAndVisualProperty(xpath, type, name, value);
				rtc.run();
				rtc.cleanIntermediateFiles();
			} catch (Exception e) {
				System.err.println(testFolder);
				e.printStackTrace();
			}
		}
		br.close();
	}
}
