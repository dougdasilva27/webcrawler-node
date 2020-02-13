package br.com.lett.crawlernode.core.fetcher;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import br.com.lett.crawlernode.aws.s3.S3Service;
import br.com.lett.crawlernode.core.fetcher.models.LettProxy;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.crawler.TestCrawlerSession;
import br.com.lett.crawlernode.core.session.ranking.TestRankingSession;
import br.com.lett.crawlernode.main.GlobalConfigurations;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.test.Test;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;

public class DynamicDataFetcher {

   protected static final Logger logger = LoggerFactory.getLogger(DynamicDataFetcher.class);

   /**
    * Use the webdriver to fetch a page.
    * 
    * @param url
    * @param session
    * @return a webdriver instance with the page already loaded
    */
   public static CrawlerWebdriver fetchPageWebdriver(String url, Session session) {
      Logging.printLogDebug(logger, session, "Fetching " + url + " using webdriver...");
      String requestHash = FetchUtilities.generateRequestHash(session);

      try {
         String phantomjsPath = null;
         if (session instanceof TestCrawlerSession || session instanceof TestRankingSession) {
            phantomjsPath = Test.phantomjsPath;
         } else {
            phantomjsPath = GlobalConfigurations.executionParameters.getPhantomjsPath();
         }

         // choose a proxy randomly
         String proxyString = ProxyCollection.LUMINATI_SERVER_BR;

         // // Bifarma block luminati_server
         if (session.getMarket().getName().equals("bifarma")) {
            proxyString = ProxyCollection.LUMINATI_SERVER_BR;
         }
         // Carrefour block
         if (session.getMarket().getName().equals("carrefour")) {
            proxyString = ProxyCollection.INFATICA_RESIDENTIAL_BR;
         }

         // Dufrio block luminati_server
         if (session.getMarket().getName().equals("dufrio") || session.getMarket().getName().equals("petz")) {
            proxyString = ProxyCollection.BUY;
         }

         LettProxy proxy = randomProxy(proxyString);

         DesiredCapabilities caps = DesiredCapabilities.phantomjs();
         caps.setJavascriptEnabled(true);
         caps.setCapability("takesScreenshot", true);
         caps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, phantomjsPath);

         //
         // Set proxy via client args
         // Proxy authorization doesnt work with client args
         // we must set the header Authorization or use a custom header
         // that the HAProxy is expecting
         //
         List<String> cliArgsCap = new ArrayList<>();

         if (proxy != null) {
            cliArgsCap.add("--proxy=" + proxy.getAddress() + ":" + proxy.getPort());
            cliArgsCap.add("--proxy-auth=" + proxy.getUser() + ":" + proxy.getPass());
            cliArgsCap.add("--proxy-type=http");
         }

         cliArgsCap.add("--ignore-ssl-errors=true"); // ignore errors in https requests
         cliArgsCap.add("--load-images=false");
         cliArgsCap.add("--webdriver-loglevel=NONE");

         caps.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, cliArgsCap);
         caps.setCapability(PhantomJSDriverService.PHANTOMJS_GHOSTDRIVER_CLI_ARGS, "--webdriver-loglevel=NONE");

         String userAgent = FetchUtilities.randUserAgent();
         caps.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_CUSTOMHEADERS_PREFIX + "User-Agent", userAgent);


         sendRequestInfoLogWebdriver(url, FetchUtilities.GET_REQUEST, proxy, userAgent, session, requestHash);

         CrawlerWebdriver webdriver = new CrawlerWebdriver(caps, session);

         if (!(session instanceof TestCrawlerSession || session instanceof TestRankingSession)) {
            Main.server.incrementWebdriverInstances();
         }

         webdriver.loadUrl(url);

         // saving request content result on Amazon
         S3Service.saveResponseContent(session, requestHash, webdriver.getCurrentPageSource());

         return webdriver;
      } catch (Exception e) {
         Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
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

   /**
    * 
    * @param webdriver
    * @param url
    * @return
    */
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
