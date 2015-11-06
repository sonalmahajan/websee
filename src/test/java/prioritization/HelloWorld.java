package prioritization;

import java.io.IOException;
import java.util.TreeMap;

import org.opencv.core.Core;

import websee.WebDriverSingleton;

public class HelloWorld {
	public static void main(String args[]) throws NumberFormatException,
			IOException {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		String modelfile = "src/com/src/java/prioritization/test/mix.all";
		String oraclePage = "evaluation/new/www.gmail.com/test1/oracle.html";
		String testPage = "evaluation/new/www.gmail.com/test1/test.html";
		String elementXpath = "/html[1]/body[1]/div[1]/div[3]/div[1]/ul[1]/li[2]/a[1]";
		Predict p = new Predict(modelfile);
		Extract e = new Extract(oraclePage, testPage, elementXpath);
		Feature f = e.extract();
		TreeMap<VisualProperty, Double> score = p.predict(f);
		System.out.println(score);
		System.out.println(p.scoreToRank(score));
		WebDriverSingleton.closeDriver();
	}
}