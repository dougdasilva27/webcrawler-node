package br.com.lett.crawlernode.core.session.ranking;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.server.request.Request;

public class RankingKeywordsSession extends RankingSession {

   public RankingKeywordsSession(Request request, String queueName, Market market) {
      super(request, queueName, market);

   }

}
