package br.com.lett.crawlernode.core.fetcher;

import br.com.lett.crawlernode.aws.s3.S3Service;
import br.com.lett.crawlernode.core.fetcher.models.LettProxy;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.main.GlobalConfigurations;
import br.com.lett.crawlernode.metrics.Exporter;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.List;

public class DynamicDataFetcher {

   private DynamicDataFetcher() {
   }

   private static final Logger logger = LoggerFactory.getLogger(DynamicDataFetcher.class);

   /**
    * @deprecated Use fetchPageWebdriver(String url, String proxyString, Session session)
    */
   @Deprecated
   public static CrawlerWebdriver fetchPageWebdriver(String url, Session session) {
      // choose a proxy randomly
      String proxyString = ProxyCollection.BUY_HAPROXY;

      return fetchPageWebdriver(url, proxyString, session);
   }

   /**
    * Use the webdriver to fetch a page.
    *
    * @return a webdriver instance with the page already loaded
    */
   public static CrawlerWebdriver fetchPageWebdriver(String url, String proxyString, Session session) {
      Logging.printLogDebug(logger, session, "Fetching " + url + " using webdriver...");
      String requestHash = FetchUtilities.generateRequestHash(session);

      CrawlerWebdriver webdriver = null;
      try {
         LettProxy proxy = randomProxy(proxyString != null ? proxyString : ProxyCollection.BUY_HAPROXY);

         Proxy proxySel = new Proxy();
         proxySel.setHttpProxy(proxy.getAddress() + ":" + proxy.getPort());
         proxySel.setSslProxy(proxy.getAddress() + ":" + proxy.getPort());

         String userAgent = FetchUtilities.randUserAgent();

         ChromeOptions chromeOptions = new ChromeOptions();
         chromeOptions.setProxy(proxySel);
         chromeOptions.setHeadless(true);
         //chromeOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);

         chromeOptions.setCapability("browserName", "chrome");
         chromeOptions.addArguments("--user-agent=" + userAgent);
         chromeOptions.addArguments("--window-size=1024,768", "--no-sandbox");
         chromeOptions.addArguments("--disable-dev-shm-usage", "--disable-gpu");

         sendRequestInfoLogWebdriver(url, FetchUtilities.GET_REQUEST, proxy, userAgent, session, requestHash);

         webdriver = new CrawlerWebdriver(chromeOptions, session);

         webdriver.loadUrl(url);

         // saving request content result on Amazon
         S3Service.saveResponseContent(session, requestHash, webdriver.getCurrentPageSource());

         return webdriver;
      } catch (Exception e) {
         Exporter.collectError(e, session);
         Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));

         // close the webdriver
         if (webdriver != null) {
            Logging.printLogDebug(logger, session, "Terminating Chrome instance because it gave error...");
            webdriver.terminate();
         }
         return null;
      }
   }

   private static void sendRequestInfoLogWebdriver(String url, String requestType, LettProxy proxy, String userAgent, Session session, String requestHash) {

      JSONObject requestMetadata = new JSONObject();

      requestMetadata.put("req_hash", requestHash);
      requestMetadata.put("proxy_name", (proxy == null ? ProxyCollection.NO_PROXY : proxy.getSource()));
      requestMetadata.put("proxy_ip", (proxy == null ? MDC.get("HOST_NAME") : proxy.getAddress()));
      requestMetadata.put("user_agent", userAgent);
      requestMetadata.put("req_method", requestType);
      requestMetadata.put("req_location", url);

      Logging.logDebug(logger, session, requestMetadata, "Registrando requisição...");

   }

   private static LettProxy randomProxy(String proxyService) {
      List<LettProxy> proxies = GlobalConfigurations.proxies.getProxy(proxyService);

      if (!proxies.isEmpty()) {
         int i = MathUtils.randInt(0, proxies.size() - 1);
         return proxies.get(i);
      }
      return null;
   }

   public static Document fetchPage(CrawlerWebdriver webdriver, String url, Session session) {
      try {
         Logging.printLogDebug(logger, session, "Fetching " + url + " using webdriver...");

         String requestHash = FetchUtilities.generateRequestHash(session);
         Document doc = new Document(url);
         webdriver.loadUrl(url);

         session.addRedirection(url, webdriver.getCurURL());

         String docString = webdriver.getCurrentPageSource();

         if (docString != null) {
            doc = Jsoup.parse(docString);
         }

         // saving request content result on Amazon
         S3Service.saveResponseContent(session, requestHash, docString);

         return doc;
      } catch (Exception e) {
         Logging.printLogWarn(logger, "Erro ao realizar requisição: " + CommonMethods.getStackTraceString(e));
         return new Document(url);
      }
   }
}
