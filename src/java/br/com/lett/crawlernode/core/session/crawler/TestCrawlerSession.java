package br.com.lett.crawlernode.core.session.crawler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.SessionError;
import br.com.lett.crawlernode.core.session.TestType;
import br.com.lett.crawlernode.main.GlobalConfigurations;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.ScraperInformation;
import enums.ScrapersTypes;
import org.json.JSONArray;

public class TestCrawlerSession extends Session {


   /**
    * Number of readings to prevent a void status
    */
   private int voidAttemptsCounter;

   private TestType type;

   private String lastError;

   private List<Product> products;

   public TestCrawlerSession(String url, Market market, ScraperInformation scraperInformation) {
      super(market);

      // initialize counters
      this.voidAttemptsCounter = 0;

      // creating the errors list
      this.crawlerSessionErrors = new ArrayList<SessionError>();

      // setting session id
      this.sessionId = "test";

      // setting Market
      this.market = market;

      // setting URL and originalURL
      this.originalURL = url;


      this.options = JSONUtils.stringToJson(scraperInformation.getOptionsScraper());


      JSONArray proxiesArray = this.options.optJSONArray("proxies");
      if (proxiesArray != null && !proxiesArray.isEmpty()) {
         for (Object o : proxiesArray) {
            String proxy = (String) o;
            proxies.add(proxy);
         }
      } else {
         proxies = Arrays.asList(ProxyCollection.BUY, ProxyCollection.LUMINATI_SERVER_BR, ProxyCollection.NO_PROXY);
      }

      JSONArray imageProxiesArray = this.options.optJSONArray("proxies");
      if (imageProxiesArray != null && !imageProxiesArray.isEmpty()) {
         for (Object o : imageProxiesArray) {
            String proxy = (String) o;
            imageProxies.add(proxy);
         }
      } else {
         imageProxies = Arrays.asList(ProxyCollection.BUY, ProxyCollection.LUMINATI_SERVER_BR, ProxyCollection.NO_PROXY);
      }

      for (String proxy : this.proxies) {
         maxConnectionAttemptsWebcrawler += GlobalConfigurations.proxies.getProxyMaxAttempts(proxy);
      }


   }


   public TestCrawlerSession(Request request, Market market) {
      super(market);

      if (ScrapersTypes.DISCOVERER.toString().equals(request.getScraperType())) {
         setType(TestType.DISCOVER);
      } else if (ScrapersTypes.IMAGES_DOWNLOAD.toString().equals(request.getScraperType())) {
         setType(TestType.IMAGE);
      } else if (ScrapersTypes.CORE.toString().equals(request.getScraperType())) {
         setType(TestType.INSIGHTS);
      } else if (ScrapersTypes.RATING.toString().equals(request.getScraperType())) {
         setType(TestType.RATING);
      } else {
         setType(TestType.SEED);
      }

      // initialize counters
      this.voidAttemptsCounter = 0;

      // creating the errors list
      this.crawlerSessionErrors = new ArrayList<SessionError>();

      // setting session id
      this.sessionId = request.getMessageId();

      // setting Market
      this.market = market;

      // setting URL and originalURL
      this.originalURL = request.getParameter();
   }

   @Override
   public int getVoidAttempts() {
      return voidAttemptsCounter;
   }

   public void setVoidAttempts(int voidAttempts) {
      this.voidAttemptsCounter = voidAttempts;
   }

   public TestType getType() {
      return type;
   }


   public void setType(TestType type) {
      this.type = type;
   }

   @Override
   public void incrementVoidAttemptsCounter() {
      this.voidAttemptsCounter++;
   }

   public String getLastError() {
      return lastError;
   }

   public void setLastError(String lastError) {
      this.lastError = lastError;
   }

   public List<Product> getProducts() {
      return products;
   }

   public void setProducts(List<Product> products) {
      this.products = products;
   }

}
