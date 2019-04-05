package br.com.lett.crawlernode.core.fetcher.models;

public class FetcherRequestBuilder {

  private String url;
  private String requestType;
  private boolean mustUseMovingAverage;
  private boolean retrieveStatistics;
  private FetcherRequestsParameters parameters;
  private FetcherRequestForcedProxies forcedProxies;

  public static FetcherRequestBuilder create() {
    return new FetcherRequestBuilder();
  }

  public String getUrl() {
    return url;
  }

  public FetcherRequestBuilder setUrl(String url) {
    this.url = url;
    return this;
  }

  public FetcherRequestBuilder setRequestType(String requestType) {
    this.requestType = requestType;
    return this;
  }

  public FetcherRequestBuilder setMustUseMovingAverage(boolean mustUseMovingAverage) {
    this.mustUseMovingAverage = mustUseMovingAverage;
    return this;
  }

  public FetcherRequestBuilder setRetrieveStatistics(boolean retrieveStatistics) {
    this.retrieveStatistics = retrieveStatistics;
    return this;
  }

  public FetcherRequestBuilder setParameters(FetcherRequestsParameters parameters) {
    this.parameters = parameters;
    return this;
  }

  public FetcherRequestBuilder setForcedProxies(FetcherRequestForcedProxies forcedProxies) {
    this.forcedProxies = forcedProxies != null && forcedProxies.isEmpty() ? null : forcedProxies;
    return this;
  }

  public FetcherRequest build() {
    FetcherRequest fetcher = new FetcherRequest();
    fetcher.setUrl(this.url);
    fetcher.setMustUseMovingAverage(this.mustUseMovingAverage);
    fetcher.setRetrieveStatistics(this.retrieveStatistics);
    fetcher.setRequestType(this.requestType);
    fetcher.setForcedProxies(this.forcedProxies);
    fetcher.setParameters(this.parameters);

    return fetcher;
  }
}
