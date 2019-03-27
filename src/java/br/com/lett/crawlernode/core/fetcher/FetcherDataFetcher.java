package br.com.lett.crawlernode.core.fetcher;

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
import org.apache.http.HttpHeaders;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.lett.crawlernode.aws.s3.S3Service;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.FetcherRequest;
import br.com.lett.crawlernode.core.fetcher.models.FetcherRequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.FetcherRequestForcedProxies;
import br.com.lett.crawlernode.core.fetcher.models.FetcherRequestsParameters;
import br.com.lett.crawlernode.core.fetcher.models.LettProxy;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.RequestsStatistics;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;

public class FetcherDataFetcher implements DataFetcher {

  private static final String FETCHER_CONTENT_TYPE = "application/json";
  public static final String FETCHER_HOST = "http://placeholder-fetcher-prod.us-east-1.elasticbeanstalk.com/";
  public static final String FETCHER_HOST_DEV = "http://fetcher-development.us-east-1.elasticbeanstalk.com/";
  private static final Logger logger = LoggerFactory.getLogger(FetcherDataFetcher.class);

  @Override
  public Response get(Session session, Request request) {
    return fetcherRequest(session, request, FetchUtilities.GET_REQUEST);
  }

  @Override
  public Response post(Session session, Request request) {
    return fetcherRequest(session, request, FetchUtilities.POST_REQUEST);
  }

  @Override
  public File fetchImage(Session session, Request request) {
    return new ApacheDataFetcher().fetchImage(session, request);
  }

  /**
   * 
   * @param session
   * @param request
   * @param method
   * @return
   */
  private Response fetcherRequest(Session session, Request request, String method) {
    Response response = new Response();
    String requestHash = FetchUtilities.generateRequestHash(session);

    try {
      FetcherRequest payload = fetcherPayloadBuilder(request, method);

      Logging.printLogDebug(logger, session,
          "Performing POST request in fetcher to perform a " + payload.getRequestType() + " request in: " + payload.getUrl());

      Integer defaultTimeout = FetchUtilities.DEFAULT_CONNECTION_REQUEST_TIMEOUT * 15;

      URL url = new URL(FETCHER_HOST);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod(method);
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
      S3Service.uploadCrawlerSessionContentToAmazon(session, requestHash, response.getBody());
    } catch (Exception e) {
      Logging.printLogError(logger, session, "Fetcher did not returned the expected response: " + CommonMethods.getStackTrace(e));
    }

    sendRequestInfoLog(request, response, method, session, requestHash);

    return response;
  }

  /**
   * 
   * @param fetcherResponse
   * @return
   */
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
      response.setCookies(FetchUtilities.getCookiesFromHeadersMap(headers));

    }

    return response;
  }

  /**
   * 
   * @param responseJson
   * @return
   */
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

  /**
   * 
   * @param fetcherResponse
   * @return
   */
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

  /**
   * Log for requests
   * 
   * @param request
   * @param response
   * @param method
   * @param session
   * @param requestHash
   */
  public static void sendRequestInfoLog(Request request, Response response, String method, Session session, String requestHash) {
    LettProxy proxy = response != null ? response.getProxyUsed() : null;

    JSONObject requestMetadata = new JSONObject();
    requestMetadata.put("req_hash", requestHash);
    requestMetadata.put("proxy_name", (proxy == null ? ProxyCollection.NO_PROXY : proxy.getSource()));
    requestMetadata.put("proxy_ip", (proxy == null ? "" : proxy.getAddress()));
    requestMetadata.put("req_method", method);
    requestMetadata.put("req_location", request != null ? request.getUrl() : "");
    requestMetadata.put("req_type", "FETCHER");

    Logging.logDebug(logger, session, requestMetadata, "Registrando requisição...");

  }
}
