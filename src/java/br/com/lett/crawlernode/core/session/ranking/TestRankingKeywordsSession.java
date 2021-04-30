package br.com.lett.crawlernode.core.session.ranking;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.server.request.Request;

public class TestRankingKeywordsSession extends TestRankingSession {

   public TestRankingKeywordsSession(Request request, String queueName, Market market) {
      super(request, queueName, market);

   }

   public TestRankingKeywordsSession(Market market, String keyword) {
      super(market, keyword);

   }

}
