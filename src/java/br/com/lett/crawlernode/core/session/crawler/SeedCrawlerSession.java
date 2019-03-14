package br.com.lett.crawlernode.core.session.crawler;

import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.server.request.CrawlerSeedRequest;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.core.session.Session;

public class SeedCrawlerSession extends Session {

  private String taskId;

  public SeedCrawlerSession(Request request, String queueName, Markets markets) {
    super(request, queueName, markets);

    setTaskId(((CrawlerSeedRequest) request).getTaskId());
  }

  public String getTaskId() {
    return taskId;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

}
