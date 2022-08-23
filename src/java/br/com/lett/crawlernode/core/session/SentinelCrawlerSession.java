package br.com.lett.crawlernode.core.session;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.server.request.CrawlerSeedRequest;
import br.com.lett.crawlernode.core.server.request.Request;

public class SentinelCrawlerSession extends Session {


   public SentinelCrawlerSession(Request request, String queueName, Market market) {
      super(request, queueName, market);

   }

}
