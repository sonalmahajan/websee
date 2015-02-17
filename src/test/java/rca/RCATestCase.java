package rca;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jgap.InvalidConfigurationException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.xml.sax.SAXException;

import util.Util;
import websee.CSSParser;
import websee.HtmlAttributesParser;
import websee.HtmlElement;
import websee.WebDriverSingleton;
import websee.WebSeeTool;

public class RCATestCase {

	private String testcasepath;
	private String oraclepath;
	private String testpath;
	private String diffpath;
	private String resultpath;
	private String detailpath;
	private Logger resultsLog;
	private Logger detailsLog;

	private HtmlElement element;

	RCATestCase(String testcasepath) throws IOException {
		System.out.println(testcasepath);
		String testcasename = new File(testcasepath).getParent() + '_'
				+ new File(testcasepath).getName();
		this.testcasepath = new File(testcasepath).getAbsolutePath();
		this.oraclepath = this.testcasepath + "/oracle.png";
		this.testpath = this.testcasepath + "/test.html";
		this.diffpath = this.testcasepath + "/filtered_diff_oracle_test.txt";
		this.resultpath = this.testcasepath + "/RCA_results.txt";
		this.detailpath = this.testcasepath + "/RCA_details.txt";
		cleanResultFiles();
		cleanIntermediateFiles();
		this.resultsLog = Util.getNewLogger(this.resultpath, "RCA_results_"
				+ testcasename);
		this.detailsLog = Util.getNewLogger(this.detailpath, "RCA_details_"
				+ testcasename);
	}

	public void run() {
		try {
			
			Document doc = Jsoup.parse(new File(this.testcasepath + File.separatorChar + "oracle.html"), null);
			doc.outputSettings().prettyPrint(false);
			String html = doc.html();
			PrintWriter out = new PrintWriter(new File(this.testcasepath + File.separatorChar + "oracle.html"));
			out.print(html);
			out.close();
			doc = Jsoup.parse(new File(this.testcasepath + File.separatorChar + "oracle.html"), null);
			doc.outputSettings().prettyPrint(false);
			html = doc.html();
			out = new PrintWriter(new File(this.testcasepath + File.separatorChar + "oracle.html"));
			out.print(html);
			out.close();

			doc = Jsoup.parse(new File(this.testcasepath + File.separatorChar + "test.html"), null);
			doc.outputSettings().prettyPrint(false);
			html = doc.html();
			out = new PrintWriter(new File(this.testcasepath + File.separatorChar + "test.html"));
			out.print(html);
			out.close();
			doc = Jsoup.parse(new File(this.testcasepath + File.separatorChar + "test.html"), null);
			doc.outputSettings().prettyPrint(false);
			html = doc.html();
			out = new PrintWriter(new File(this.testcasepath + File.separatorChar + "test.html"));
			out.print(html);
			out.close();
			
			Util.getScreenshot("oracle.html", testcasepath, "oracle.png", true);
			/*
			 * VisualInvariantsTool vit = new VisualInvariantsTool(
			 * this.oraclepath, this.testpath); vit.setConfig(true, false, true,
			 * true, true, true, false, ""); vit.detection(false);
			 */
			RootCauseAnalysis rca = new RootCauseAnalysis(this.element,
					this.oraclepath, this.testpath, this.resultsLog,
					this.detailsLog);
			rca.setConfig(true, true, true, true, "");
			rca.runRootCauseAnalysis();
			NumericAnalysis.resetTranslationValues();
			WebDriverSingleton.closeDriver();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void runWithoutPrioritization() {
		try {
			Util.getScreenshot("oracle.html", testcasepath, "oracle.png", true);
			WebSeeTool vit = new WebSeeTool(
					this.oraclepath, this.testpath);
			vit.detection(false);
			RootCauseAnalysis rca = new RootCauseAnalysis(this.element,
					this.oraclepath, this.testpath, this.resultsLog,
					this.detailsLog);
			rca.runRootCauseAnalysisWithoutPrioritization();
			NumericAnalysis.resetTranslationValues();
			WebDriverSingleton.closeDriver();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void runWithoutSearchSpace(String value) {
		try {
			Util.getScreenshot("oracle.html", testcasepath, "oracle.png", true);
			/*
			 * VisualInvariantsTool vit = new VisualInvariantsTool(
			 * this.oraclepath, this.testpath); vit.detection(false);
			 */
			RootCauseAnalysis rca = new RootCauseAnalysis(this.element,
					this.oraclepath, this.testpath, this.resultsLog,
					this.detailsLog);
			rca.setConfig(true, false, true, false, value);
			rca.runRootCauseAnalysis();
			NumericAnalysis.resetTranslationValues();
			WebDriverSingleton.closeDriver();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void runWithoutFitnessFunction() {
		try {
			Util.getScreenshot("oracle.html", testcasepath, "oracle.png", true);
			WebSeeTool vit = new WebSeeTool(
					this.oraclepath, this.testpath);
			vit.detection(false);
			RootCauseAnalysis rca = new RootCauseAnalysis(this.element,
					this.oraclepath, this.testpath, this.resultsLog,
					this.detailsLog);
			rca.setConfig(true, true, false, true, "");
			rca.runRootCauseAnalysis();
			NumericAnalysis.resetTranslationValues();
			WebDriverSingleton.closeDriver();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void runWithoutSimulatedAnnealing() {
		try {
			Util.getScreenshot("oracle.html", testcasepath, "oracle.png", true);
			WebSeeTool vit = new WebSeeTool(
					this.oraclepath, this.testpath);
			vit.detection(false);
			RootCauseAnalysis rca = new RootCauseAnalysis(this.element,
					this.oraclepath, this.testpath, this.resultsLog,
					this.detailsLog);
			rca.setConfig(true, true, true, false, "");
			rca.runRootCauseAnalysis();
			NumericAnalysis.resetTranslationValues();
			WebDriverSingleton.closeDriver();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void setCssProperties() throws SAXException, IOException,
			XPathExpressionException {
		Map<String, String> css = new HashMap<String, String>();
		CSSParser cssParser = new CSSParser(this.testpath);
		cssParser.parseCSS();
		css = cssParser.getCSSPropertiesForElement(this.element.getXpath());
		element.setCssProperties(css);
	}

	private void setCssProperties(String name, String value) {
		Map<String, String> css = new HashMap<String, String>();
		if (name != null)
			css.put(name, value);
		element.setCssProperties(css);
	}

	private String patchXpath(String xpath) {
		if (xpath.indexOf("tbody") == -1)
			return xpath.replaceAll("(/table(\\[\\d+\\])?)/", "$1/tbody[1]/");
		else
			return xpath;
	}

	private void setElementByXPath(String xpath) {
		this.element = new HtmlElement();
		WebDriverSingleton instance = WebDriverSingleton.getInstance();
		instance.loadPage(this.testpath);
		WebDriver d = instance.getDriver();
		WebElement el = d.findElement(By.xpath(patchXpath(xpath)));
		element.setX(el.getLocation().x);
		element.setY(el.getLocation().y);
		element.setWidth(el.getSize().width);
		element.setHeight(el.getSize().height);

		xpath = Util.getElementXPath((JavascriptExecutor) d, el);
		element.setXpath(xpath);

		WebDriverSingleton.closeDriver();
		// d.close();
		// d.quit();
	}

	private void setHtmlAttributes() throws IOException {
		Map<String, String> html = new HashMap<String, String>();
		HtmlAttributesParser hap = new HtmlAttributesParser(this.testpath);
		html = hap.getHTMLAttributesForElement(this.element.getXpath());
		this.element.setHtmlAttributes(html);
	}

	private void setHtmlAttributes(String name, String value) {
		Map<String, String> html = new HashMap<String, String>();
		if (name != null)
			html.put(name, value);
		this.element.setHtmlAttributes(html);
	}

	public void setupByElement(String xpath) {
		try {
			setElementByXPath(xpath);
			setHtmlAttributes();
			setCssProperties();
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	void setupByElementAndVisualProperty(String xpath, String type,
			String name, String value) {
		setElementByXPath(xpath);
		if (type.equals("html")) {
			setHtmlAttributes(name, value);
			setCssProperties(null, null);
		} else {
			setHtmlAttributes(null, null);
			setCssProperties(name, value);
		}
	}

	public void cleanIntermediateFiles() throws IOException {
		FileUtils.deleteDirectory(new File(this.testcasepath + "/RCA"));
		new File(this.testcasepath + "/diff_oracle_test.txt").delete();
		new File(this.testcasepath + "/filtered_diff_oracle_test.txt").delete();
	}

	public void cleanResultFiles() {
		new File(this.testcasepath + "/RCA_details.txt").delete();
		new File(this.testcasepath + "/RCA_results.txt").delete();
		new File(this.testcasepath + "/diff_oracle_test.png").delete();
		new File(this.testcasepath + "/oracle.png").delete();
		new File(this.testcasepath + "/test.png").delete();
	}

	public static void main(String args[]) {
//		System.out.println(patchXpath("/bookstore/book[1]/table/title"));
//		System.out.println(patchXpath("/bookstore/book[1]/table[2]/title"));
//		System.out
//				.println(patchXpath("/bookstore/table/book[1]/table[2]/title"));
//		System.out.println(patchXpath("/bookstore/table/book[1]/table[2]/div"));
//		System.out.println(patchXpath("/bookstore/table/book[1]/table/p"));
//		System.out.println(patchXpath("/bookstore/table/book[1]/table[2]"));
//		System.out.println(patchXpath("/bookstore/table/book[1]/table"));
	}
}