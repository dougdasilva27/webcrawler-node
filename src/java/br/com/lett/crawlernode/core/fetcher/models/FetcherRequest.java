package br.com.lett.crawlernode.core.fetcher.models;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class FetcherRequest {

  public static final String FETCHER_PARAMETER_RETRIEVE_STATISTICS = "retrieve_statistics";
  public static final String FETCHER_PARAMETER_PROXIES = "forced_proxies";
  public static final String FETCHER_PARAMETER_REQUEST_PARAMETERS = "request_parameters";
  public static final String FETCHER_PARAMETER_USE_PROXY_BY_MOVING_AVERAGE = "use_proxy_by_moving_average";
  public static final String FETCHER_PARAMETER_METHOD = "request_type";
  public static final String FETCHER_PARAMETER_URL = "url";
  public static final String FETCHER_PARAMETER_BODY_IS_REQUIRED = "body_is_required";
  public static final String FETCHER_PARAMETER_IGNORE_STATUS_CODE = "ignore_status_code";
  public static final String FETCHER_PARAMETER_STATUS_CODES_TO_IGNORE = "status_codes_to_ignore";
  public static final String FETCHER_PARAMETER_REQUIRED_CSS_SELECTOR = "required_css_selector";
  public static final String FETCHER_PARAMETER_FORBIDDEN_CSS_SELECTOR = "forbidden_css_selector";

  private String url;
  private String requestType;
  private boolean mustUseMovingAverage = true;
  private boolean retrieveStatistics = true;
  private boolean ignoreStatusCode = false;
  private boolean bodyIsRequired = true;
  private String requiredCssSelector;
  private String forbiddenCssSelector;
  private List<Integer> statusCodesToIgnore = new ArrayList<>();
  private FetcherRequestsParameters parameters;
  private FetcherRequestForcedProxies forcedProxies;

  public JSONObject toJson() {
    JSONObject fetcherParameters = new JSONObject();

    fetcherParameters.put(FETCHER_PARAMETER_URL, url);
    fetcherParameters.put(FETCHER_PARAMETER_METHOD, requestType);
    fetcherParameters.put(FETCHER_PARAMETER_RETRIEVE_STATISTICS, retrieveStatistics);
    fetcherParameters.put(FETCHER_PARAMETER_USE_PROXY_BY_MOVING_AVERAGE, mustUseMovingAverage);
    fetcherParameters.put(FETCHER_PARAMETER_IGNORE_STATUS_CODE, ignoreStatusCode);
    fetcherParameters.put(FETCHER_PARAMETER_BODY_IS_REQUIRED, bodyIsRequired);

    if (statusCodesToIgnore != null && !statusCodesToIgnore.isEmpty()) {
      JSONArray array = new JSONArray();

      for (int statusCode : statusCodesToIgnore) {
        array.put(Integer.toString(statusCode));
      }

      fetcherParameters.put(FETCHER_PARAMETER_STATUS_CODES_TO_IGNORE, array);
    }

    if (parameters != null) {
      fetcherParameters.put(FETCHER_PARAMETER_REQUEST_PARAMETERS, parameters.toJson());
    }

    if (forcedProxies != null) {
      fetcherParameters.put(FETCHER_PARAMETER_PROXIES, forcedProxies.toJson());
    }

    if (requiredCssSelector != null) {
      fetcherParameters.put(FETCHER_PARAMETER_REQUIRED_CSS_SELECTOR, requiredCssSelector);
    }

    if (forbiddenCssSelector != null) {
      fetcherParameters.put(FETCHER_PARAMETER_FORBIDDEN_CSS_SELECTOR, forbiddenCssSelector);
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

  public boolean isIgnoreStatusCode() {
    return ignoreStatusCode;
  }

  public void setIgnoreStatusCode(boolean ignoreStatusCode) {
    this.ignoreStatusCode = ignoreStatusCode;
  }

  public boolean isBodyIsRequired() {
    return bodyIsRequired;
  }

  public void setBodyIsRequired(boolean bodyIsRequired) {
    this.bodyIsRequired = bodyIsRequired;
  }

  public List<Integer> getStatusCodesToIgnore() {
    return statusCodesToIgnore;
  }

  public void setStatusCodesToIgnore(List<Integer> statusCodesToIgnore) {
    this.statusCodesToIgnore = statusCodesToIgnore;
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
}
