package br.com.lett.crawlernode.core.session.ranking;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.server.request.CrawlerRankingKeywordsRequest;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.core.session.Session;

public class RankingDiscoverSession extends Session {

   private String location;

   public RankingDiscoverSession(Request request, String queueName, Market market) {
      super(request, queueName, market);

      this.location = ((CrawlerRankingKeywordsRequest) request).getLocation();
   }

   public String getLocation() {
      return location;
   }

   public void setLocation(String location) {
      this.location = location;
   }

}
