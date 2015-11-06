package prioritization;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.opencv.core.Core;

import websee.WebDriverSingleton;

public class TrainModel {
	public static void main(String args[]) throws IOException {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		String test_dir = "scripts/input/gen/subjects/www.virginamerica.com";
		BufferedReader br = new BufferedReader(new FileReader(test_dir
				+ "/description.txt"));
		PrintWriter pw = new PrintWriter(
				new FileWriter(test_dir + "/model.txt"));
		String line;
		while ((line = br.readLine()) != null) {
			String split[] = line.split("\t");
			String name = split[2];
			String type = split[3];
			String xpath = split[7];
			String test = split[8];
			System.out.println(name + " " + type + " " + xpath + " " + test);
			Extract e = new Extract(test_dir + "/" + test + "/oracle.html",
					test_dir + "/" + test + "/test.html", xpath);
			Feature f = e.extract();
			System.out.println(f);
			pw.println(type + "\t" + name + "\t" + f.tabString());
		}
		br.close();
		pw.close();
		WebDriverSingleton.closeDriver();
	}
}