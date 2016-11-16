package br.com.lett.crawlernode.core.fetcher;

import java.io.File;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.FileUtils;

import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.util.Logging;

/**
 * This class encapsulates an instance of a Remote WebDriver
 * that uses a PhantomJS as backend for Selenium WebDriver.
 * This class also provide methods to manipulate web elements
 * and take web pages screenshots.
 *
 * @author Samir Leao
 * 
 */
public class CrawlerWebdriver {

	private static Logger logger = LoggerFactory.getLogger(CrawlerWebdriver.class);
	
	/**
	 * The URL of the hub that connects to the remote WebDriver instances
	 */
	private final String HUB_URL = "http://52.183.27.200:4444/";
	
	
	private WebDriver driver;


	public CrawlerWebdriver() {
		initPhantomJSDriver();
	}

	public WebElement findElementByCssSelector(String selector) {
		return driver.findElement(By.cssSelector(selector));
	}

	public List<WebElement> findElementsByCssSelector(String selector) {
		return driver.findElements(By.cssSelector(selector));
	}

	/**
	 * Get the html source of the current page loaded in the webdriver.
	 * 
	 * @return
	 */
	public String getCurrentPageSource() {
		return this.driver.getPageSource();
	}
	
	/**
	 * Loads a webpage without any explicit wait.
	 * 
	 * @param url
	 * @return
	 */
	public String loadUrl(String url) {
		driver.get(url);
		return driver.getPageSource();
	}

	/**
	 * Loads a webpage and wait for some time.
	 * 
	 * @param url
	 * @param waitTime
	 * @return A String containing the page html
	 */
	public String loadUrl(String url, int waitTime) {
		this.driver.get(url);
		this.waitLoad(waitTime);

		return this.driver.getPageSource();
	}

	private void waitLoad(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Get the current loaded page on the webdriver instance.
	 * 
	 * @return
	 */
	public String getCurURL() {
		return this.driver.getCurrentUrl();
	}

	/**
	 * Terminate the web driver.
	 */
	public void closeDriver() {
		Logging.printLogDebug(logger, "Terminating webdriver...");

		this.driver.close();
		this.driver.quit();

		Logging.printLogDebug(logger, "Webdriver terminated.");
	}
	
	/**
	 * Get a screenshot from a webpage.
	 * 
	 * @param url
	 * @return
	 */
	public File takeScreenshot(String url) {
		driver.get(url);
		File screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
		
		return screenshot;
	}
	
	/**
	 * Get a screenshot from a webpage and save the file.
	 * 
	 * @param url
	 * @param path the path where the screenshot will be saved
	 */
	public void takeScreenshot(String url, String path) {
		driver.get(url);
		File screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
		try {
			FileUtils.copyFile(screenshot, new File(path));
		} catch (Exception ex) {
			Logging.printLogError(logger, "Error saving screenshot! [" + ex.getMessage() + "]");
		}
	}

	private void initPhantomJSDriver() {
		try {
			URL url = new URL(HUB_URL);

			DesiredCapabilities capabilities = DesiredCapabilities.phantomjs();
			
			driver = new RemoteWebDriver(url, capabilities);
			
		} catch (MalformedURLException ex) {
			Logging.printLogError(logger, "Hub URL error! " + ex.getMessage());
		}
	}

}
