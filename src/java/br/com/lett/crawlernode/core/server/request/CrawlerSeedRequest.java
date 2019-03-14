package br.com.lett.crawlernode.core.server.request;

public class CrawlerSeedRequest extends Request {

  private String taskId;

  public CrawlerSeedRequest() {
    super();
  }

  public String getTaskId() {
    return taskId;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }
}
