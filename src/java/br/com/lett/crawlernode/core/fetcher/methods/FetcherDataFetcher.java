package br.com.lett.crawlernode.core.fetcher.methods;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpHeaders;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.lett.crawlernode.aws.s3.S3Service;
import br.com.lett.crawlernode.core.fetcher.FetchUtilities;
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
import br.com.lett.crawlernode.exceptions.ResponseCodeException;
import br.com.lett.crawlernode.main.GlobalConfigurations;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;

public class FetcherDataFetcher implements DataFetcher {

   private static final String FETCHER_CONTENT_TYPE = "application/json";
   public static final String FETCHER_HOST = GlobalConfigurations.executionParameters.getFetcherUrl();
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

      FetcherRequest payload = fetcherPayloadBuilder(request, method, session);

      Logging.printLogDebug(logger, session,
            "Performing POST request in fetcher to perform a " + payload.getRequestType() + " request in: " + payload.getUrl());

      long requestsStartTime = System.currentTimeMillis();

      try {
         Integer defaultTimeout = request.getTimeout() != null ? request.getTimeout() : FetchUtilities.DEFAULT_CONNECTION_REQUEST_TIMEOUT * 18;

         URL url = new URL(FETCHER_HOST);
         HttpURLConnection connection = (HttpURLConnection) url.openConnection();
         connection.setRequestMethod(FetchUtilities.POST_REQUEST);
         connection.setInstanceFollowRedirects(true);
         connection.setUseCaches(false);
         connection.setReadTimeout(defaultTimeout);
         connection.setDoInput(true);
         connection.setDoOutput(true);
         connection.setRequestProperty(HttpHeaders.CONTENT_TYPE, FETCHER_CONTENT_TYPE);

         // Inserting payload
         OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
         writer.write(payload.toJson().toString());
         writer.close();

         // Get Response
         InputStream is = connection.getInputStream();
         BufferedReader rd = new BufferedReader(new InputStreamReader(is));
         StringBuilder responseStr = new StringBuilder(); // or StringBuffer if Java version 5+
         String line;
         while ((line = rd.readLine()) != null) {
            responseStr.append(line);
            responseStr.append('\r');
         }
         rd.close();
         String content = responseStr.toString();

         // analysing the status code
         // if there was some response code that indicates forbidden access or server error we want to
         // try again
         int responseCode = connection.getResponseCode();

         if (Integer.toString(responseCode).charAt(0) != '2' && Integer.toString(responseCode).charAt(0) != '3' && responseCode != 404) { // errors
            throw new ResponseCodeException(responseCode);
         }

         // see if some code error occured
         // sometimes the remote server doesn't send the http error code on the headers
         // but rater on the page bytes
         content = content.trim();
         for (String errorCode : FetchUtilities.errorCodes) {
            if (content.equals(errorCode)) {
               throw new ResponseCodeException(Integer.parseInt(errorCode));
            }
         }

         // process response and parse
         JSONObject responseJson = CrawlerUtils.stringToJson(content);
         if (responseJson.has("statistics")) {
            JSONObject statistics = responseJson.getJSONObject("statistics");

            if (statistics.has("request_id")) {
               Logging.printLogInfo(logger, session, "Request Fetcher Id: " + statistics.get("request_id"));
            }
         }

         response = responseBuilder(responseJson);
         session.addRedirection(request.getUrl(), response.getRedirectUrl());
         S3Service.saveResponseContent(session, requestHash, response.getBody());
      } catch (Exception e) {
         Logging.printLogWarn(logger, session, "Fetcher did not returned the expected response: " + e.getMessage());
         Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e));
      }

      sendRequestInfoLog(request, response, method, session, requestHash);

      JSONObject apacheMetadata = new JSONObject().put("req_fetcher_elapsed_time", System.currentTimeMillis() - requestsStartTime)
            .put("req_fetcher_attempts_number", 1);

      Logging.logInfo(logger, session, apacheMetadata, "FETCHER REQUESTS INFO");

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

      int statusCode = requestsStatistics.isEmpty() ? 0 : requestsStatistics.get(requestsStatistics.size() - 1).getStatusCode();
      response.setLastStatusCode(statusCode);

      if (!requestsStatistics.isEmpty()) {
         response.setProxyUsed(requestsStatistics.get(requestsStatistics.size() - 1).getProxy());
      }

      if (fetcherResponse.has("response")) {
         JSONObject responseJson = fetcherResponse.getJSONObject("response");

         if (responseJson.has("body")) {
            response.setBody(responseJson.get("body").toString().trim());
         }

         if (responseJson.has("body")) {
            response.setBody(responseJson.get("body").toString().trim());
         }

         if (responseJson.has("redirect_url")) {
            response.setRedirectUrl(responseJson.get("redirect_url").toString());
         }

         setHeadersAndCookies(response, responseJson);
      }

      return response;
   }

   private void setHeadersAndCookies(Response response, JSONObject responseJson) {
      Map<String, String> headers = new HashMap<>();

      if (responseJson.has("headers")) {
         JSONObject headersJson = responseJson.getJSONObject("headers");

         for (String key : headersJson.keySet()) {
            String headerName = key;

            if (headerName.equalsIgnoreCase(FetchUtilities.HEADER_SET_COOKIE)) {
               headerName = FetchUtilities.HEADER_SET_COOKIE;
               response.setCookies(getCookies(headersJson.get(key)));
            }

            headers.put(headerName, headersJson.get(key).toString());
         }
      }

      response.setHeaders(headers);
   }

   private List<Cookie> getCookies(Object cookiesObject) {
      List<Cookie> cookies = new ArrayList<>();

      if (cookiesObject instanceof JSONArray) {
         for (Object o : ((JSONArray) cookiesObject)) {
            String cookieHeader = o.toString();
            String cookieName = cookieHeader.split("=")[0].trim();

            String cookieValue;

            int x = cookieHeader.indexOf(cookieName + "=") + cookieName.length() + 1;

            if (cookieHeader.contains(";")) {
               int y = cookieHeader.indexOf(';', x);

               cookieValue = cookieHeader.substring(x, y).trim();
            } else {
               cookieValue = cookieHeader.substring(x).trim();
            }

            BasicClientCookie cookie = new BasicClientCookie(cookieName, cookieValue);
            cookie.setPath("/");
            cookies.add(cookie);
         }
      }

      return cookies;
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

               if (request.has("elapsedTime")) {
                  requestStats.setElapsedTime(request.optLong("elapsedTime"));
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
                     String source = proxy.getString("source");
                     lettProxy.setSource(source);

                     List<LettProxy> proxies = GlobalConfigurations.proxies.getProxy(source);
                     if (!proxies.isEmpty()) {
                        LettProxy temp = proxies.get(0);

                        lettProxy.setUser(temp.getUser());
                        lettProxy.setPass(temp.getPass());
                     }
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
   private static FetcherRequest fetcherPayloadBuilder(Request request, String method, Session session) {
      FetcherRequest payload;
      FetcherOptions options = request.getFetcherOptions();

      Map<String, String> finalHeaders = request.getHeaders() != null ? request.getHeaders() : new HashMap<>();
      if (!request.mustSendContentEncoding()) {
         finalHeaders.put(HttpHeaders.CONTENT_ENCODING, "");
      }

      List<Cookie> cookies = request.getCookies();
      if (cookies != null && !cookies.isEmpty()) {
         StringBuilder cookiesHeader = new StringBuilder();
         for (Cookie c : cookies) {
            cookiesHeader.append(c.getName() + "=" + c.getValue() + ";");
         }

         finalHeaders.put("Cookie", cookiesHeader.toString());
      }

      if (!request.mustSendUserAgent()) {
         finalHeaders.put(HttpHeaders.USER_AGENT, "");
      }

      String url = request.getUrl();
      try {
         url = URI.create(request.getUrl()).toASCIIString();
      } catch (Exception e) {
         Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
      }

      if (options != null) {
         payload = FetcherRequestBuilder.create()
               .setUrl(url)
               .setMustUseMovingAverage(options.isMustUseMovingAverage())
               .setRequestType(method)
               .setRetrieveStatistics(options.isRetrieveStatistics())
               .setForbiddenCssSelector(options.getForbiddenCssSelector())
               .setRequiredCssSelector(options.getRequiredCssSelector())
               .setForcedProxies(
                     new FetcherRequestForcedProxies()
                           .setAny(request.getProxyServices())
                           .setSpecific(request.getProxy())
               )
               .setParameters(
                     new FetcherRequestsParameters().setHeaders(finalHeaders)
                           .setPayload(request.getPayload())
                           .setMustFollowRedirects(request.isFollowRedirects())
               )
               .setIgnoreStatusCode(request.mustIgnoreStatusCode())
               .setBodyIsRequired(request.bodyIsRequired())
               .setStatusCodesToIgnore(request.getStatusCodesToIgnore())
               .build();
      } else {
         payload = FetcherRequestBuilder.create()
               .setUrl(url)
               .setMustUseMovingAverage(true)
               .setRetrieveStatistics(true)
               .setRequestType(method)
               .setForcedProxies(
                     new FetcherRequestForcedProxies()
                           .setAny(request.getProxyServices())
                           .setSpecific(request.getProxy())
               )
               .setParameters(
                     new FetcherRequestsParameters().setHeaders(finalHeaders)
                           .setPayload(request.getPayload())
                           .setMustFollowRedirects(request.isFollowRedirects())
               )
               .setIgnoreStatusCode(request.mustIgnoreStatusCode())
               .setBodyIsRequired(request.bodyIsRequired())
               .setStatusCodesToIgnore(request.getStatusCodesToIgnore())
               .build();
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
      JSONObject requestMetadata = new JSONObject();

      List<RequestsStatistics> requestsStatistics = response.getRequests();
      if (!requestsStatistics.isEmpty()) {
         Integer statusCode = requestsStatistics.get(requestsStatistics.size() - 1).getStatusCode();
         requestMetadata.put("res_http_code", statusCode);

         Logging.printLogDebug(logger, session, "STATUS CODE: " + statusCode);
      } else {
         requestMetadata.put("res_http_code", 0);
      }

      requestMetadata.put("req_hash", requestHash);
      requestMetadata.put("proxy_name", "FETCHER");
      requestMetadata.put("proxy_ip", FETCHER_HOST);
      requestMetadata.put("req_method", method);
      requestMetadata.put("req_location", request != null ? request.getUrl() : "");
      requestMetadata.put("req_type", "FETCHER");

      Logging.logInfo(logger, session, requestMetadata, "Registrando requisição...");

   }
}
