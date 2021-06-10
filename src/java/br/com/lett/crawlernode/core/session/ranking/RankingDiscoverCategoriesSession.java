package br.com.lett.crawlernode.core.session.ranking;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.server.request.Request;

public class RankingDiscoverCategoriesSession extends RankingDiscoverSession {

   public RankingDiscoverCategoriesSession(Request request, String queueName, Market market) {
      super(request, queueName, market);
   }

   private String categoryUrl;

   public String getCategoryUrl() {
      return categoryUrl;
   }

   public void setCategoryUrl(String categoryUrl) {
      this.categoryUrl = categoryUrl;
   }
}
