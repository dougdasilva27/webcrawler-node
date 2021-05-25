package br.com.lett.crawlernode.core.session.ranking;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.main.GlobalConfigurations;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.ScraperInformation;
import org.json.JSONArray;

import java.util.Arrays;

public class TestRankingKeywordsSession extends TestRankingSession {

   public TestRankingKeywordsSession(Request request, String queueName, Market market) {
      super(request, queueName, market);

   }

   public TestRankingKeywordsSession(Market market, String keyword, ScraperInformation scraperInformation) {
      super(market, keyword);

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

}
