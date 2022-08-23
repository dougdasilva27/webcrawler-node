package br.com.lett.crawlernode.core.session;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.server.request.CrawlerSeedRequest;
import br.com.lett.crawlernode.core.server.request.Request;

public class SentinelCrawlerSession extends Session {

   private String taskId;
   public SentinelCrawlerSession(Request request, String queueName, Market market) {
      super(request, queueName, market);
      setTaskId(((CrawlerSeedRequest) request).getTaskId());
   }

   public String getTaskId() {
      return taskId;
   }

   public void setTaskId(String taskId) {
      this.taskId = taskId;
   }
}
