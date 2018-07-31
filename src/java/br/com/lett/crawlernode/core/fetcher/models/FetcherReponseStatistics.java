package br.com.lett.crawlernode.core.fetcher.models;

import java.util.List;

public class FetcherReponseStatistics {

  private String requestId;
  private String url;
  private List<FetcherReponseStatisticsRequest> requests;

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public List<FetcherReponseStatisticsRequest> getRequests() {
    return requests;
  }

  public void setRequests(List<FetcherReponseStatisticsRequest> requests) {
    this.requests = requests;
  }

}
