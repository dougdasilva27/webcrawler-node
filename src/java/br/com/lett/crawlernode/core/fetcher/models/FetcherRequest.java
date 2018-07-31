package br.com.lett.crawlernode.core.fetcher.models;

import org.json.JSONObject;

public class FetcherRequest {

  private String url;
  private String requestType;
  private boolean mustUseMovingAverage;
  private boolean retrieveStatistics;
  private FetcherRequestsParameters parameters;
  private FetcherRequestForcedProxies forcedProxies;

  public JSONObject toJson() {
    JSONObject fetcherParameters = new JSONObject();

    fetcherParameters.put("url", url);
    fetcherParameters.put("request_type", requestType);
    fetcherParameters.put("retrieve_statistics", retrieveStatistics);
    fetcherParameters.put("use_proxy_by_moving_average", mustUseMovingAverage);

    if (parameters != null) {
      fetcherParameters.put("request_parameters", parameters);
    }

    if (forcedProxies != null) {
      fetcherParameters.put("forced_proxies", forcedProxies);
    }

    return fetcherParameters;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getRequestType() {
    return requestType;
  }

  public void setRequestType(String requestType) {
    this.requestType = requestType;
  }

  public boolean isMustUseMovingAverage() {
    return mustUseMovingAverage;
  }

  public void setMustUseMovingAverage(boolean mustUseMovingAverage) {
    this.mustUseMovingAverage = mustUseMovingAverage;
  }

  public boolean isRetrieveStatistics() {
    return retrieveStatistics;
  }

  public void setRetrieveStatistics(boolean retrieveStatistics) {
    this.retrieveStatistics = retrieveStatistics;
  }

  public FetcherRequestsParameters getParameters() {
    return parameters;
  }

  public void setParameters(FetcherRequestsParameters parameters) {
    this.parameters = parameters;
  }

  public FetcherRequestForcedProxies getForcedProxies() {
    return forcedProxies;
  }

  public void setForcedProxies(FetcherRequestForcedProxies forcedProxies) {
    this.forcedProxies = forcedProxies;
  }
}
