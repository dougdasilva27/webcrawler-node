package br.com.lett.crawlernode.core.fetcher.models;

public class FetcherResponse {

  private String body;
  private String redirectUrl;
  private FetcherReponseStatistics statistics;

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public String getRedirectUrl() {
    return redirectUrl;
  }

  public void setRedirectUrl(String redirectUrl) {
    this.redirectUrl = redirectUrl;
  }

  public FetcherReponseStatistics getStatistics() {
    return statistics;
  }

  public void setStatistics(FetcherReponseStatistics statistics) {
    this.statistics = statistics;
  }


}
