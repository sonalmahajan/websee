package shortpaper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class RunSPTest {

	public static void main(String[] args) throws IOException {
		run("evaluation/color");
	}

	public static void run(String basePath) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(basePath
				+ "/description.txt"));
		String line;
		while ((line = br.readLine()) != null) {
			String[] lineSplit = line.split("\t");
			String testFolder = lineSplit[8];
			try {
				SPTestCase sptc = new SPTestCase(basePath + "/" + testFolder);
				String prop = lineSplit[2];
				String original_value = lineSplit[5];
				String value = lineSplit[6];
				String xpath = lineSplit[7];
				if (prop.indexOf("color") != -1) {
					sptc.runColor(xpath, prop, original_value);
				} else {
					sptc.runNumeric(xpath, prop, original_value);
				}
				sptc.cleanIntermediateFiles();
			} catch (Exception e) {
				System.err.println(testFolder);
				System.err.println(e.getMessage());
				e.printStackTrace();
			}
		}
		br.close();
	}
}
