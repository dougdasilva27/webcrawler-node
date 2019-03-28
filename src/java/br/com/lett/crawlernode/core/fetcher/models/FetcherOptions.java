package br.com.lett.crawlernode.core.fetcher.models;

import org.json.JSONObject;

public class FetcherOptions {

  public static final String FETCHER_PARAMETER_USE_PROXY_BY_MOVING_AVERAGE = "use_proxy_by_moving_average";
  public static final String FETCHER_PARAMETER_RETRIEVE_STATISTICS = "retrieve_statistics";

  private boolean mustUseMovingAverage;
  private boolean retrieveStatistics;

  public JSONObject toJson() {
    JSONObject fetcherParameters = new JSONObject();

    fetcherParameters.put(FETCHER_PARAMETER_RETRIEVE_STATISTICS, retrieveStatistics);
    fetcherParameters.put(FETCHER_PARAMETER_USE_PROXY_BY_MOVING_AVERAGE, mustUseMovingAverage);

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

  public static class FetcherOptionsBuilder {
    private boolean mustUseMovingAverage;
    private boolean retrieveStatistics;

    public static FetcherOptionsBuilder create() {
      return new FetcherOptionsBuilder();
    }

    public FetcherOptionsBuilder mustUseMovingAverage(boolean must) {
      this.mustUseMovingAverage = must;
      return this;
    }

    public FetcherOptionsBuilder mustRetrieveStatistics(boolean must) {
      this.retrieveStatistics = must;
      return this;
    }

    public FetcherOptions build() {
      FetcherOptions options = new FetcherOptions();

      options.setMustUseMovingAverage(this.mustUseMovingAverage);
      options.setMustUseMovingAverage(this.retrieveStatistics);

      return options;
    }

  }
}
