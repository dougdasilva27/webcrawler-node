package br.com.lett.crawlernode.core.fetcher;

import br.com.lett.crawlernode.aws.s3.S3Service;
import br.com.lett.crawlernode.core.fetcher.models.LettProxy;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.crawler.TestCrawlerSession;
import br.com.lett.crawlernode.core.session.ranking.TestRankingSession;
import br.com.lett.crawlernode.main.GlobalConfigurations;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.List;

public class DynamicDataFetcher {

   protected static final Logger logger = LoggerFactory.getLogger(DynamicDataFetcher.class);

   /**
    * @param url
    * @param session
    * @return
    * @Deprecated Use fetchPageWebdriver(String url, String proxyString, Session session)
    */
   @Deprecated
   public static CrawlerWebdriver fetchPageWebdriver(String url, Session session) {
      // choose a proxy randomly
      String proxyString = ProxyCollection.LUMINATI_SERVER_BR_HAPROXY;

      // // Bifarma block luminati_server
      if (session.getMarket().getName().equals("bifarma")) {
         proxyString = ProxyCollection.BONANZA;
      }

      // Dufrio block luminati_server
      if (session.getMarket().getName().equals("dufrio") || session.getMarket().getName().equals("petz")) {
         proxyString = ProxyCollection.BUY;
      }

      return fetchPageWebdriver(url, proxyString, session);
   }

   public static CrawlerWebdriver fetchPageWebdriver(String url, String proxyString, Session session) {
      return fetchPageWebdriver(url, proxyString, true, session);
   }

   /**
    * Use the webdriver to fetch a page.
    *
    * @param url
    * @param session
    * @return a webdriver instance with the page already loaded
    */
   public static CrawlerWebdriver fetchPageWebdriver(String url, String proxyString, boolean headless, Session session) {
      Logging.printLogDebug(logger, session, "Fetching " + url + " using webdriver...");
      String requestHash = FetchUtilities.generateRequestHash(session);

      try {
         LettProxy proxy = randomProxy(proxyString != null ? proxyString : ProxyCollection.LUMINATI_SERVER_BR_HAPROXY);

         Proxy proxySel = new Proxy();
         proxySel.setHttpProxy(proxy.getAddress() + ":" + proxy.getPort());
         proxySel.setSslProxy(proxy.getAddress() + ":" + proxy.getPort());

         String userAgent = FetchUtilities.randUserAgent();
         ChromeOptions chromeOptions = ChromeOptionsBuilder.create()
            .setProxy(proxySel)
            .setHeadless(headless)
            .setSession(session)
            .setUserAgent(userAgent)
            .build();

         sendRequestInfoLogWebdriver(url, FetchUtilities.GET_REQUEST, proxy, userAgent, session, requestHash);

         CrawlerWebdriver webdriver = new CrawlerWebdriver(chromeOptions, session);

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
