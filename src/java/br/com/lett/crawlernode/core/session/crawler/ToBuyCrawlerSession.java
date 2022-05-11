package br.com.lett.crawlernode.core.session.crawler;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.server.request.Request;

public class ToBuyCrawlerSession extends InsightsCrawlerSession {

   public ToBuyCrawlerSession(Request request, String queueName, Market market) {
      super(request, queueName, market);
   }
}
