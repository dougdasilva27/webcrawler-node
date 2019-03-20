package br.com.lett.crawlernode.core.fetcher.models;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.http.HttpHeaders;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.DataFetcherNO;
import br.com.lett.crawlernode.core.fetcher.LettProxy;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;

public class FetcherDataFetcher implements DataFetcher {

  private static final String FETCHER_CONTENT_TYPE = "application/json";
  public static final String FETCHER_HOST = "http://placeholder-fetcher-prod.us-east-1.elasticbeanstalk.com/";
  public static final String FETCHER_HOST_DEV = "http://fetcher-development.us-east-1.elasticbeanstalk.com/";
  protected static final Logger logger = LoggerFactory.getLogger(FetcherDataFetcher.class);

  @Override
  public Response get(Session session, Request request) {
    return fetcherRequest(session, request, "GET");
  }

  @Override
  public Response post(Session session, Request request) {
    return fetcherRequest(session, request, "POST");
  }

  @Override
  public File fetchImage(Session session, Request request) {
    // TODO Auto-generated method stub
    return null;
  }

  public Response fetcherRequest(Session session, Request request, String method) {
    Response response = new Response();

    try {
      FetcherRequest payload = fetcherPayloadBuilder(request, method);

      Logging.printLogDebug(logger, session,
          "Performing POST request in fetcher to perform a " + payload.getRequestType() + " request in: " + payload.getUrl());

      Integer defaultTimeout = DataFetcherNO.DEFAULT_CONNECTION_REQUEST_TIMEOUT * 15;

      URL url = new URL(FETCHER_HOST);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod(DataFetcherNO.GET_REQUEST);
      connection.setInstanceFollowRedirects(true);
      connection.setUseCaches(false);
      connection.setReadTimeout(defaultTimeout);
      connection.setRequestProperty(HttpHeaders.CONTENT_TYPE, FETCHER_CONTENT_TYPE);

      // Get Response
      InputStream is = connection.getInputStream();
      BufferedReader rd = new BufferedReader(new InputStreamReader(is));
      StringBuilder content = new StringBuilder(); // or StringBuffer if Java version 5+
      String line;
      while ((line = rd.readLine()) != null) {
        content.append(line);
        content.append('\r');
      }
      rd.close();

      // process response and parse
      JSONObject responseJson = CrawlerUtils.stringToJson(content.toString());

      if (responseJson.has("statistics")) {
        JSONObject statistics = responseJson.getJSONObject("statistics");

        if (statistics.has("request_id")) {
          Logging.printLogInfo(logger, session, "Request Fetcher Id: " + statistics.get("request_id"));
        }
      }

      response = responseBuilder(responseJson);
    } catch (Exception e) {
      Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
      Logging.printLogWarn(logger, session, "Fetcher did not returned the expected response.");
    }

    return response;
  }

  private Response responseBuilder(JSONObject fetcherResponse) {
    Response response = new Response();
    List<RequestsStatistics> requestsStatistics = getStats(fetcherResponse);
    response.setRequests(requestsStatistics);

    if (requestsStatistics.isEmpty()) {
      response.setProxyUsed(requestsStatistics.get(requestsStatistics.size() - 1).getProxy());
    }

    if (fetcherResponse.has("response")) {
      JSONObject responseJson = fetcherResponse.getJSONObject("fetcherResponse");

      if (responseJson.has("body")) {
        response.setBody(responseJson.get("body").toString());
      }

      if (responseJson.has("body")) {
        response.setBody(responseJson.get("body").toString());
      }

      if (responseJson.has("redirect_url")) {
        response.setRedirectUrl(responseJson.get("redirect_url").toString());
      }

      Map<String, String> headers = getHeaders(responseJson);
      response.setHeaders(headers);
      response.setCookies(getCookies(headers));

    }

    return response;
  }

  private List<Cookie> getCookies(Map<String, String> headers) {
    List<Cookie> cookies = new ArrayList<>();

    for (Entry<String, String> entry : headers.entrySet()) {
      String cookieHeader = entry.getValue();
      String cookieName = cookieHeader.split("=")[0].trim();

      int x = cookieHeader.indexOf(cookieName + "=") + cookieName.length() + 1;
      int y = cookieHeader.indexOf(";", x);

      String cookieValue = cookieHeader.substring(x, y).trim();

      BasicClientCookie cookie = new BasicClientCookie(cookieName, cookieValue);
      cookies.add(cookie);
    }

    return cookies;
  }

  private Map<String, String> getHeaders(JSONObject responseJson) {
    Map<String, String> headers = new HashMap<>();

    if (responseJson.has("headers")) {
      JSONObject headersJson = responseJson.getJSONObject("headers");

      for (String key : headersJson.keySet()) {
        headers.put(key, headersJson.get(key).toString());
      }
    }

    return headers;
  }

  private List<RequestsStatistics> getStats(JSONObject fetcherResponse) {
    List<RequestsStatistics> requestStatistics = new ArrayList<>();

    if (fetcherResponse.has("statistics")) {
      JSONObject statistics = fetcherResponse.getJSONObject("statistics");

      if (statistics.has("requests")) {
        for (Object o : statistics.getJSONArray("requests")) {
          RequestsStatistics requestStats = new RequestsStatistics();
          JSONObject request = (JSONObject) o;

          if (request.has("cod")) {
            requestStats.setStatusCode(request.getInt("cod"));
          }

          if (request.has("attempt")) {
            requestStats.setAttempt(request.getInt("attempt"));
          }

          if (request.has("passedValidation")) {
            requestStats.setHasPassedValidation(request.getBoolean("passedValidation"));
          }

          if (request.has("proxy")) {
            JSONObject proxy = request.getJSONObject("proxy");

            LettProxy lettProxy = new LettProxy();
            if (proxy.has("host")) {
              lettProxy.setAddress(proxy.getString("host"));
            }

            if (proxy.has("port")) {
              lettProxy.setPort(proxy.getInt("port"));
            }

            if (proxy.has("source")) {
              lettProxy.setSource(proxy.getString("source"));
            }

            if (proxy.has("location")) {
              lettProxy.setLocation(proxy.getString("location"));
            }

            requestStats.setProxy(lettProxy);
          }

          requestStatistics.add(requestStats);
        }
      }
    }

    return requestStatistics;
  }

  /**
   * Build payload to request 'FETCHER'
   * 
   * @param {@link Request}
   * @param method Get OR Post
   * 
   * @return FetcherRequest
   */
  private static FetcherRequest fetcherPayloadBuilder(Request request, String method) {
    FetcherRequest payload;
    FetcherOptions options = request.getFetcherOptions();

    if (options != null) {
      payload = FetcherRequestBuilder.create().setUrl(request.getUrl()).setMustUseMovingAverage(options.isMustUseMovingAverage())
          .setRequestType(method).setRetrieveStatistics(options.isRetrieveStatistics())
          .setForcedProxies(new FetcherRequestForcedProxies().setAny(request.getProxyServices()).setSpecific(request.getProxy()))
          .setParameters(new FetcherRequestsParameters().setHeaders(request.getHeaders()).setPayload(request.getPayload())).build();
    } else {
      payload =
          FetcherRequestBuilder.create().setUrl(request.getUrl()).setMustUseMovingAverage(true).setRequestType(method).setRetrieveStatistics(true)
              .setForcedProxies(new FetcherRequestForcedProxies().setAny(request.getProxyServices()).setSpecific(request.getProxy()))
              .setParameters(new FetcherRequestsParameters().setHeaders(request.getHeaders()).setPayload(request.getPayload())).build();
    }

    return payload;
  }
}
