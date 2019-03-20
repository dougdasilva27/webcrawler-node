package br.com.lett.crawlernode.core.fetcher.models;

import org.json.JSONObject;

public class FetcherOptions {

  public static final String FETCHER_PARAMETER_USE_PROXY_BY_MOVING_AVERAGE = "use_proxy_by_moving_average";
  public static final String FETCHER_PARAMETER_RETRIEVE_STATISTICS = "retrieve_statistics";
  public static final String FETCHER_PARAMETER_PROXIES = "forced_proxies";

  private boolean mustUseMovingAverage;
  private boolean retrieveStatistics;
  private FetcherRequestForcedProxies forcedProxies;

  public JSONObject toJson() {
    JSONObject fetcherParameters = new JSONObject();

    fetcherParameters.put(FETCHER_PARAMETER_RETRIEVE_STATISTICS, retrieveStatistics);
    fetcherParameters.put(FETCHER_PARAMETER_USE_PROXY_BY_MOVING_AVERAGE, mustUseMovingAverage);

    if (forcedProxies != null) {
      fetcherParameters.put(FETCHER_PARAMETER_PROXIES, forcedProxies.toJson());
    }

    return fetcherParameters;
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

  public FetcherRequestForcedProxies getForcedProxies() {
    return forcedProxies;
  }

  public void setForcedProxies(FetcherRequestForcedProxies forcedProxies) {
    this.forcedProxies = forcedProxies;
  }
}
