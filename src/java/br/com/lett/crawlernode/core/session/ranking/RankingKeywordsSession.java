package br.com.lett.crawlernode.core.session.ranking;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.server.request.Request;

public class RankingKeywordsSession extends RankingSession {

   private boolean sendDiscover;

   public RankingKeywordsSession(Request request, String queueName, Market market) {
      super(request, queueName, market);

      sendDiscover = request.isSendDiscover();

   }

   public boolean isSendDiscover() {
      return sendDiscover;
   }

}
