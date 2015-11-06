package websee;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Platform;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.DesiredCapabilities;

import util.Util;
import config.Constants;

public class WebDriverSingleton
{
	private static WebDriverSingleton instance = null;
	private static WebDriver driver = null;
	
	private WebDriverSingleton()
	{
		if(Constants.WEBDRIVER_BROWSER.equalsIgnoreCase("FIREFOX"))
		{
			if(Constants.HEADLESS_FIREFOX)
			{
				String Xport = System.getProperty("lmportal.xvfb.id", ":2");
		        final File firefoxPath = new File(System.getProperty("lmportal.deploy.firefox.path", "/usr/bin/firefox"));
		        FirefoxBinary firefoxBinary = new FirefoxBinary(firefoxPath);
		        firefoxBinary.setEnvironmentProperty("DISPLAY", Xport);
		        
				driver = new FirefoxDriver(firefoxBinary, null);
			}
			else
			{
				driver = new FirefoxDriver();
			}
		}
		else
		{
			DesiredCapabilities desiredCapabilities = new DesiredCapabilities(BrowserType.PHANTOMJS, "", Platform.ANY);
			desiredCapabilities.setCapability("phantomjs.binary.path", Constants.PHANTOM_JS_EXECUTABLE_PATH);
			desiredCapabilities.setJavascriptEnabled(true);
			driver = new PhantomJSDriver(desiredCapabilities);
		}
		driver.manage().window().maximize();
//		driver.manage().window().setSize(new Dimension(1231, 1180));
//		driver.manage().window().setSize(new Dimension(640, 480));
//		driver.manage().window().setSize(new Dimension(640, 480));
	}

	public static WebDriverSingleton getInstance()
	{
		if(instance == null)
		{
			instance = new WebDriverSingleton();
		}
		return instance;
	}
	
	public WebDriver getDriver()
	{
		return driver;
	}
	
	public static void closeDriver()
	{
		if(instance != null)
		{
			driver.quit();
			instance = null;
		}
	}
	
	public static void restartDriver()
	{
		closeDriver();		
		getInstance();
	}
	
	public void loadPage(String htmlFileFullPath)
	{
		//System.out.println("Loading page " + htmlFileFullPath);
		String urlString = "";
		if(Constants.WEBDRIVER_BROWSER.equalsIgnoreCase("FIREFOX"))
		{
			urlString = "file:///" + htmlFileFullPath;
		}
		else
		{
			// phantom JS
			// copy html file to webapps of apache server
			try
			{
				FileUtils.copyFile(new File(htmlFileFullPath), new File(Constants.APACHE_DEPLOY_FILE_PATH));
			}
			catch (FileNotFoundException e1)
			{
				System.err.println("HTML file not found. Create " + Constants.APACHE_DEPLOY_FILE_PATH);
				System.exit(0);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			urlString = Constants.APACHE_DEPLOYED_FILE_HOST;
			if(driver.getCurrentUrl().equalsIgnoreCase(urlString))
			{
				driver.navigate().refresh();
				return;
			}
		}
		try
		{
			driver.manage().timeouts().pageLoadTimeout(10, TimeUnit.SECONDS);
			driver.get(urlString);
		}
		catch(Exception e)
		{
			// restart the browser
			restartDriver();
			driver.get(urlString);
		}
	}
}