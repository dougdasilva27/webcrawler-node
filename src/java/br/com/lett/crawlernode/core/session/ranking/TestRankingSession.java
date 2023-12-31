package br.com.lett.crawlernode.core.session.ranking;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.server.request.CrawlerRankingKeywordsRequest;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.core.session.Session;

public class TestRankingSession extends Session {

   private String location;

   public TestRankingSession(Request request, String queueName, Market market) {
      super(request, queueName, market);

      this.location = ((CrawlerRankingKeywordsRequest) request).getLocation();
   }

   public TestRankingSession(Market market, String location) {
      super(market);

      // setting Market
      this.market = market;

      // setting location
      this.location = location;
   }

   public String getLocation() {
      return location;
   }

   public void setLocation(String keyword) {
      this.location = keyword;
   }

}
