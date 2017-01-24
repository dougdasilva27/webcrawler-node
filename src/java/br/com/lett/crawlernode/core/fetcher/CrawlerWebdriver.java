package br.com.lett.crawlernode.core.fetcher;

import java.io.File;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.core.session.Session;
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

	protected static final Logger logger = LoggerFactory.getLogger(CrawlerWebdriver.class);

	/**
	 * The URL of the hub that connects to the remote WebDriver instances
	 */
	private static final String HUB_URL = "http://52.175.217.27:4444/wd/hub";

	public WebDriver driver;

	private Session session;


	public CrawlerWebdriver(DesiredCapabilities capabilities, Session session) {
		try {
			driver = new RemoteWebDriver(new URL(HUB_URL), capabilities);
			this.session = session;

		} catch (MalformedURLException ex) {
			Logging.printLogError(logger, "Hub URL error! " + ex.getMessage());
		}

		//driver = new ChromeDriver(capabilities);
		

		this.session = session;
	}

	public void addHeaders(Map<String, String> headers) {
		driver.get("chrome-extension://idgpnmonknjnojddfkpgkljpfnnfcklj/icon.png");
		
		StringBuilder headersOptions = new StringBuilder();
		for (Entry<String, String> entry : headers.entrySet()) {
			headersOptions.append("    {enabled: true, name: '" + entry.getKey() + "', value: '" + entry.getValue() + "', comment: ''}, ");
		}
		
		((JavascriptExecutor)driver).executeScript(
				"localStorage.setItem('profiles', JSON.stringify([{                " +
						"  title: 'Selenium', hideComment: true, appendMode: '',           " +
						"  headers: [                                                      " +
						headersOptions.toString()											 +
						"  ],                                                              " +
						"  respHeaders: [],                                                " +
						"  filters: []                                                     " +
				"}]));");
		
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

	public void waitLoad(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public Actions getActionsBuilder() {
		return new Actions(driver);
	}

	public WebElement executeJavascript(String javascript) {
		JavascriptExecutor jse = (JavascriptExecutor) driver;
		return (WebElement)jse.executeScript(javascript);
	}

	public void clickOnElementViaJavascript(WebElement element) {
		JavascriptExecutor jse = (JavascriptExecutor) driver;
		jse.executeScript("arguments[0].click();", element);
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
	public void terminate() {
		this.driver.close();
		this.driver.quit();
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
			Logging.printLogError(logger, session, "Error saving screenshot! [" + ex.getMessage() + "]");
		}
	}

	/**
	 * Get a screenshot from the current loaded webpage and save the file.
	 * 
	 * @param url
	 * @param path the path where the screenshot will be saved
	 */
	public void takeScreenshotFromCurrentLoadedPage(String path) {
		Augmenter augmenter = new Augmenter();
		File screenshot = ((TakesScreenshot)augmenter.augment(driver)).getScreenshotAs(OutputType.FILE);
		//File screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
		try {
			FileUtils.copyFile(screenshot, new File(path));
		} catch (Exception ex) {
			System.err.println("Error saving screenshot! [" + ex.getMessage() + "]");
		}
	}

}