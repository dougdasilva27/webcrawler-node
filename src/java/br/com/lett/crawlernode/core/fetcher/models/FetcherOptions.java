package br.com.lett.crawlernode.core.fetcher.models;

import org.json.JSONObject;

public class FetcherOptions {

  public static final String FETCHER_PARAMETER_USE_PROXY_BY_MOVING_AVERAGE = "use_proxy_by_moving_average";
  public static final String FETCHER_PARAMETER_RETRIEVE_STATISTICS = "retrieve_statistics";
  public static final String FETCHER_PARAMETER_REQUIRED_CSS_SELECTOR = "required_css_selector";
  public static final String FETCHER_PARAMETER_FORBIDDEN_CSS_SELECTOR = "forbidden_css_selector";

  private boolean mustUseMovingAverage;
  private boolean retrieveStatistics;
  private String requiredCssSelector;
  private String forbiddenCssSelector;

  public JSONObject toJson() {
    JSONObject fetcherParameters = new JSONObject();

    fetcherParameters.put(FETCHER_PARAMETER_RETRIEVE_STATISTICS, retrieveStatistics);
    fetcherParameters.put(FETCHER_PARAMETER_USE_PROXY_BY_MOVING_AVERAGE, mustUseMovingAverage);

    if (requiredCssSelector != null) {
      fetcherParameters.put(FETCHER_PARAMETER_REQUIRED_CSS_SELECTOR, requiredCssSelector);
    }

    if (forbiddenCssSelector != null) {
      fetcherParameters.put(FETCHER_PARAMETER_FORBIDDEN_CSS_SELECTOR, forbiddenCssSelector);
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

  public String getRequiredCssSelector() {
    return requiredCssSelector;
  }

  public void setRequiredCssSelector(String requiredCssSelector) {
    this.requiredCssSelector = requiredCssSelector;
  }

  public String getForbiddenCssSelector() {
    return forbiddenCssSelector;
  }

  public void setForbiddenCssSelector(String forbiddenCssSelector) {
    this.forbiddenCssSelector = forbiddenCssSelector;
  }

  public static class FetcherOptionsBuilder {
    private boolean mustUseMovingAverage;
    private boolean retrieveStatistics;
    private String requiredCssSelector;
    private String forbiddenCssSelector;

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

    public FetcherOptionsBuilder setRequiredCssSelector(String requiredCssSelector) {
      this.requiredCssSelector = requiredCssSelector;
      return this;
    }

    public FetcherOptionsBuilder setForbiddenCssSelector(String forbiddenCssSelector) {
      this.forbiddenCssSelector = forbiddenCssSelector;
      return this;
    }

    public FetcherOptions build() {
      FetcherOptions options = new FetcherOptions();

      options.setMustUseMovingAverage(this.mustUseMovingAverage);
      options.setRetrieveStatistics(this.retrieveStatistics);
      options.setForbiddenCssSelector(this.forbiddenCssSelector);
      options.setRequiredCssSelector(this.requiredCssSelector);

      return options;
    }

  }
}
