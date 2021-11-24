package br.com.lett.crawlernode.core.session.ranking;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.server.request.CrawlerRankingKeywordsRequest;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.core.session.Session;

public class RankingSession extends Session {

   private String location;
   private Long locationId;
   private Boolean takeAScreenshot;

   public RankingSession(Request request, String queueName, Market market) {
      super(request, queueName, market);

      if (request instanceof CrawlerRankingKeywordsRequest) {
         this.location = ((CrawlerRankingKeywordsRequest) request).getLocation();
         this.takeAScreenshot = ((CrawlerRankingKeywordsRequest) request).getTakeAScreenshot();
      }
   }

   public String getLocation() {
      return location;
   }

   public void setLocation(String location) {
      this.location = location;
   }

   public Boolean mustTakeAScreenshot() {
      return takeAScreenshot;
   }

   public void setTakeAScreenshot(Boolean takeAScreenshot) {
      this.takeAScreenshot = takeAScreenshot;
   }

   public Long getLocationId() {
      return locationId;
   }

   public void setLocationId(Long locationId) {
      this.locationId = locationId;
   }
}
