package prioritization;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import websee.HtmlElement;
import websee.WebDriverSingleton;

public class Extract {
	private File oraclePage;
	private File oracleImage;
	private File testPage;
	private File testImage;
	private String elementXpath;

	private int diff_pixel;
	private Size oracleImageSize;
	private Size testImageSize;
	private Point location;
	private Dimension size;
	private String tag;
	private String text;
	private Set<Integer> oracleColor;
	private Set<Integer> testColor;
	private Mat oracleMat;
	private Mat testMat;

	private List<Integer> features;

	public Extract(String oracleImageFilename, String testPageFilename,
			String testImageFilename, HtmlElement element) {
		this.testPage = new File(testPageFilename);
		this.elementXpath = element.getXpath();
		this.oracleImage = new File(oracleImageFilename);
		this.testImage = new File(testImageFilename);
		this.getPageFeature();
		this.getColorFeature();
	}
	
	public Extract(String oracleImageFilename, String testPageFilename,
			String testImageFilename, String elementXpath) {
		this.testPage = new File(testPageFilename);
		this.elementXpath = elementXpath;
		this.oracleImage = new File(oracleImageFilename);
		this.testImage = new File(testImageFilename);
		this.getPageFeature();
		this.getColorFeature();
	}

	public Extract(String oraclePageFilename, String testPageFilename,
			String elementXpath) {
		this.oraclePage = new File(oraclePageFilename);
		this.testPage = new File(testPageFilename);
		this.elementXpath = elementXpath;
		try {
			this.testImage = File.createTempFile("test", ".png");
			this.testImage.deleteOnExit();
			this.oracleImage = File.createTempFile("oracle", ".png");
			this.oracleImage.deleteOnExit();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.getScreenShot(this.oraclePage, this.oracleImage);
		this.getScreenShot(this.testPage, this.testImage);
		this.getPageFeature();
		this.getColorFeature();
	}

	public String getFeatureFromPython() {
		/*
		 * String[] cmd = { "python", "scripts/input/ml/feature.py", "-o",
		 * "scripts/input/ml/data2/test1/oracle.png", "-t",
		 * "scripts/input/ml/data2/test1/test.html" };
		 */
		String[] cmd = { "python", "scripts/input/ml/feature.py" };
		String s = "";
		try {
			Process p = Runtime.getRuntime().exec(cmd);
			BufferedReader br = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			while ((s = br.readLine()) != null) {
				System.out.println(s);
			}
			int exit_value = p.waitFor();
			System.out.println(exit_value);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return s;
	}

	private Set<Integer> getColor(Mat image) {
		Set<Integer> set = new HashSet<Integer>();
		if (image == null)
			return set;
		for (int x = 0; x < image.cols(); x++)
			for (int y = 0; y < image.rows(); y++) {
				double[] data = image.get(y, x);
				int color = (int) (data[0]) + ((int) (data[1]) << 8)
						+ ((int) (data[2]) << 16);
				set.add(color);
			}
		return set;
	}

	private int getNumberOfDifferentPixels(Mat image1, Mat image2) {
		int diff_total = 0;
		for (int col = 0; col < image1.cols() || col < image2.cols(); col++) {
			for (int row = 0; row < image1.rows() || row < image2.rows(); row++) {
				double[] img1RGB = image1.get(row, col);
				double[] img2RGB = image2.get(row, col);
				if (img1RGB == null || img2RGB == null) {
					diff_total++;
				} else {
					for (int i = 0; i < img1RGB.length; i++) {
						if (img1RGB[i] != img2RGB[i]) {
							diff_total++;
							break;
						}
					}
				}
			}
		}
		return diff_total;
	}

	private String patchXpath(String xpath) {
		if (xpath.indexOf("tbody") == -1)
			return xpath.replaceAll("(/table(\\[\\d+\\])?)", "$1/tbody[1]");
		else
			return xpath;
	}

	private void getPageFeature() {
		WebDriverSingleton instance = WebDriverSingleton.getInstance();
		WebDriver driver = instance.getDriver();
		driver.get("file://" + this.testPage.getAbsolutePath());
		WebElement element = driver.findElement(By.xpath(this
				.patchXpath(this.elementXpath)));
		this.location = element.getLocation();
		this.size = element.getSize();
		this.tag = element.getTagName();
		this.text = element.getText();
	}

	private void getScreenShot(File page, File image) {
		WebDriverSingleton instance = WebDriverSingleton.getInstance();
		WebDriver driver = instance.getDriver();
		driver.get("file://" + page.getAbsolutePath());
		File screenshotFile = ((TakesScreenshot) driver)
				.getScreenshotAs(OutputType.FILE);
		try {
			FileUtils.copyFile(screenshotFile, image);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void getColorFeature() {
		this.oracleMat = Highgui.imread(this.oracleImage.getAbsolutePath());
		this.oracleImageSize = oracleMat.size();
		this.oracleColor = getColor(oracleMat);
		this.testMat = Highgui.imread(this.testImage.getAbsolutePath());
		this.testImageSize = testMat.size();
		this.testColor = getColor(testMat);
		this.diff_pixel = getNumberOfDifferentPixels(oracleMat, testMat);
	}

	private boolean exactMatch(Mat m1, Mat m2, Point l, Dimension s) {
		if (l.x + s.width > m1.width())
			return false;
		if (l.y + s.height > m1.height())
			return false;
		for (int x = 0; x < s.width; x++)
			for (int y = 0; y < s.height; y++) {
				double[] img1RGB = m1.get(l.y + y, l.x + x);
				double[] img2RGB = m2.get(l.y + y, l.x + x);
				if (img1RGB == null || img2RGB == null) {
					return false;
				} else {
					for (int i = 0; i < img1RGB.length; i++) {
						if (img1RGB[i] != img2RGB[i]) {
							return false;
						}
					}
				}
			}
		return true;
	}

	private int boolToInt(boolean b) {
		return b ? 1 : 0;
	}

	public Feature extract() {
		features = new ArrayList<Integer>();
		features.add(boolToInt(this.oracleImageSize.equals(this.testImageSize)));
		features.add(boolToInt(this.oracleColor.size() == this.testColor.size()));
		Set<Integer> add_set = new HashSet<Integer>(this.testColor);
		add_set.removeAll(this.oracleColor);
		features.add(boolToInt(add_set.size() > 0));
		Set<Integer> remove_set = new HashSet<Integer>(this.oracleColor);
		remove_set.removeAll(this.testColor);
		features.add(boolToInt(remove_set.size() > 0));
		features.add(boolToInt(this.diff_pixel < 100));
		features.add(boolToInt(this.text.trim().length() > 0));
		features.add(boolToInt(this.tag.toLowerCase().equals("img")));
		features.add(boolToInt(exactMatch(this.oracleMat, this.testMat,
				this.location, this.size)));
		return new Feature(features);
	}

	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append(String.format("Different Pixels: %d\n", this.diff_pixel));
		s.append(String.format("Oracle Size: %s\n", this.oracleImageSize));
		s.append(String.format("Test Size: %s\n", this.testImageSize));
		s.append(String.format("Location: %s\n", this.location));
		s.append(String.format("Size: %s\n", this.size));
		s.append(String.format("Text: %s\n", this.text));
		s.append(String.format("Tag: %s\n", this.tag));
		return s.toString();
	}

	public static void main(String args[]) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		// Extract e = new Extract(
		// "scripts/input/ml/data2/test1/oracle.html",
		// "scripts/input/ml/data2/test1/test.html",
		// "/html[1]/body[1]/div[3]/div[1]/div[1]/div[2]/div[2]/div[1]/div[1]/ul[1]/li[5]/a[1]");
		Extract e = new Extract("scripts/input/ml/data2/test6/oracle.html",
				"scripts/input/ml/data2/test6/test.html",
				"/html[1]/body[1]/div[2]/div[1]/table[1]/tr[1]/td[4]/a[1]");
		System.out.println(e);
		System.out.println(e.extract());
		WebDriverSingleton.closeDriver();
		// Feature f = new Feature(e.extract());
		// System.out.println(f);
	}
}
