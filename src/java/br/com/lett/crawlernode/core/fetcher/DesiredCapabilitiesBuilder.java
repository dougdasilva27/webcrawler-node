package br.com.lett.crawlernode.core.fetcher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.openqa.selenium.Platform;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

public class DesiredCapabilitiesBuilder {
  protected static final Logger logger = LoggerFactory.getLogger(DesiredCapabilitiesBuilder.class);

  private static final String DEFAULT_PROXY = "191.235.90.114:3333";

  private Session session;
  private String userAgent;
  private Proxy proxy;

  public static DesiredCapabilitiesBuilder create() {
    return new DesiredCapabilitiesBuilder();
  }

  protected DesiredCapabilitiesBuilder() {
    super();
  }

  public DesiredCapabilitiesBuilder setUserAgent(String userAgent) {
    this.userAgent = userAgent;
    return this;
  }

  public DesiredCapabilitiesBuilder setSession(Session session) {
    this.session = session;
    return this;
  }

  public DesiredCapabilitiesBuilder setProxy(Proxy proxy) {
    this.proxy = proxy;
    return this;
  }

  public DesiredCapabilities build() {
    DesiredCapabilities desiredCapabilities = DesiredCapabilities.chrome();

    desiredCapabilities.setPlatform(Platform.ANY);
    desiredCapabilities.setVersion("ANY");

    if (proxy != null) {
      desiredCapabilities.setCapability(CapabilityType.PROXY, proxy);
    } else {
      Proxy defaultProxy = new Proxy();
      defaultProxy.setHttpProxy(DEFAULT_PROXY);
      defaultProxy.setSslProxy(DEFAULT_PROXY);
      desiredCapabilities.setCapability(CapabilityType.PROXY, defaultProxy);
    }

    ChromeOptions chromeOptions = new ChromeOptions();

    try {
      File extensionFile;
      if (Main.globalResources == null) { // testando
        extensionFile = new File(getClass().getClassLoader().getResource("modheader_2_1_1.crx").getFile());
      } else {
        extensionFile = Main.globalResources.getWebdriverExtension();
      }

      Logging.printLogDebug(logger, session, "Seting webdriver extension from file: " + extensionFile.getAbsolutePath());
      chromeOptions.addExtensions(extensionFile);

    } catch (Exception e) {
      Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
    }

    if (userAgent != null) {
      List<String> chromeArgs = new ArrayList<>();
      chromeArgs.add("--user-agent=" + userAgent);
      chromeArgs.add("--allow-insecure-localhost");
      chromeArgs.add("--ssl-version-max=tls1.3");
      chromeArgs.add("--ssl-version-min=tls1");
      chromeArgs.add("--ignore-certificate-errors");
      chromeArgs.add("--ignore-urlfetcher-cert-requests");

      chromeOptions.addArguments(chromeArgs);

      desiredCapabilities.setCapability("chromeOptions", chromeOptions);
    }

    desiredCapabilities.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);

    return desiredCapabilities;
  }

}
