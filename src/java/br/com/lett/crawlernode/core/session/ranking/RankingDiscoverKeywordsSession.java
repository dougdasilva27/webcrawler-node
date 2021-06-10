package br.com.lett.crawlernode.core.session.ranking;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.server.request.Request;

public class RankingDiscoverKeywordsSession extends RankingDiscoverSession {

   public RankingDiscoverKeywordsSession(Request request, String queueName, Market market) {
      super(request, queueName, market);


   }
}
