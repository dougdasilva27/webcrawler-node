package br.com.lett.crawlernode.core.fetcher;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.crawler.TestCrawlerSession;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.github.bonigarcia.wdm.config.OperatingSystem;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChromeOptionsBuilder {
   protected static final Logger logger = LoggerFactory.getLogger(ChromeOptionsBuilder.class);

   private static final String DEFAULT_PROXY = "191.235.90.114:3333";
   private Session session;
   private String userAgent;
   private Proxy proxy = new Proxy();
   private boolean headless;

   public static ChromeOptionsBuilder create() {
      return new ChromeOptionsBuilder();
   }

   protected ChromeOptionsBuilder() {
      super();
   }

   public ChromeOptionsBuilder setSession(Session session) {
      this.session = session;
      return this;
   }

   public ChromeOptionsBuilder setUserAgent(String userAgent) {
      this.userAgent = userAgent;
      return this;
   }

   public ChromeOptionsBuilder setProxy(Proxy proxy) {
      this.proxy = proxy;
      return this;
   }

   public ChromeOptionsBuilder setHeadless(boolean headless) {
      this.headless = headless;
      return this;
   }

   public ChromeOptions build() {
      WebDriverManager.chromedriver()
         .operatingSystem(OperatingSystem.LINUX)
         .setup();

      ChromeOptions chromeOptions = new ChromeOptions();

      if (proxy == null) {
         proxy.setHttpProxy(DEFAULT_PROXY);
         proxy.setSslProxy(DEFAULT_PROXY);
      }

      if (userAgent != null) {
         chromeOptions.addArguments("--user-agent=" + userAgent);
      }

      if (!(session instanceof TestCrawlerSession) || headless) {
         chromeOptions.setHeadless(true);
      }
      String binaryPath = WebDriverManager.chromedriver().getBinaryPath();

      logger.debug("Web driver binary path: " + binaryPath);

      System.setProperty("webdriver.chrome.driver", binaryPath);
      chromeOptions.setBinary(binaryPath);
      chromeOptions.setCapability(CapabilityType.TAKES_SCREENSHOT, true);
      chromeOptions.setProxy(proxy)
         .addArguments("--window-size=1920,1080", "--ignore-certificate-errors",
            "--blink-settings=imagesEnabled=false", "--verbose", "--disable-dev-shm-usage");

      return chromeOptions;
   }
}
