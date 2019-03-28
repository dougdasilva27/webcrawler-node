package br.com.lett.crawlernode.core.fetcher.models;

import org.json.JSONObject;

public class FetcherRequest {

  public static final String FETCHER_PARAMETER_RETRIEVE_STATISTICS = "retrieve_statistics";
  public static final String FETCHER_PARAMETER_PROXIES = "forced_proxies";
  public static final String FETCHER_PARAMETER_REQUEST_PARAMETERS = "request_parameters";
  public static final String FETCHER_PARAMETER_USE_PROXY_BY_MOVING_AVERAGE = "use_proxy_by_moving_average";
  public static final String FETCHER_PARAMETER_METHOD = "request_type";
  public static final String FETCHER_PARAMETER_URL = "url";

  private String url;
  private String requestType;
  private boolean mustUseMovingAverage = true;
  private boolean retrieveStatistics = true;
  private FetcherRequestsParameters parameters;
  private FetcherRequestForcedProxies forcedProxies;

  public JSONObject toJson() {
    JSONObject fetcherParameters = new JSONObject();

    fetcherParameters.put(FETCHER_PARAMETER_URL, url);
    fetcherParameters.put(FETCHER_PARAMETER_METHOD, requestType);
    fetcherParameters.put(FETCHER_PARAMETER_RETRIEVE_STATISTICS, retrieveStatistics);
    fetcherParameters.put(FETCHER_PARAMETER_USE_PROXY_BY_MOVING_AVERAGE, mustUseMovingAverage);

    if (parameters != null) {
      fetcherParameters.put(FETCHER_PARAMETER_REQUEST_PARAMETERS, parameters.toJson());
    }

    if (forcedProxies != null) {
      fetcherParameters.put(FETCHER_PARAMETER_PROXIES, forcedProxies.toJson());
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
