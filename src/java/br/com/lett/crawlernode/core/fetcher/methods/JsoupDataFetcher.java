package br.com.lett.crawlernode.core.fetcher.methods;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import org.json.JSONObject;
import org.jsoup.Connection.Method;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.lett.crawlernode.aws.s3.S3Service;
import br.com.lett.crawlernode.core.fetcher.FetchUtilities;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.LettProxy;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.RequestsStatistics;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.fetcher.models.Response.ResponseBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.crawler.EqiCrawlerSession;
import br.com.lett.crawlernode.core.session.crawler.TestCrawlerSession;
import br.com.lett.crawlernode.core.session.ranking.EqiRankingDiscoverKeywordsSession;
import br.com.lett.crawlernode.exceptions.ResponseCodeException;
import br.com.lett.crawlernode.main.GlobalConfigurations;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

public class JsoupDataFetcher implements DataFetcher {

   private static final Logger logger = LoggerFactory.getLogger(JsoupDataFetcher.class);

   @Override
   public Response get(Session session, Request request) {
      return fetch(session, request, true);
   }


   @Override
   public Response post(Session session, Request request) {
      return fetch(session, request, false);
   }

   private Response fetch(Session session, Request request, boolean getMethod) {
      Response response = new Response();

      List<RequestsStatistics> requests = new ArrayList<>();

      int attempt = 1;

      long requestsStartTime = System.currentTimeMillis();
      List<String> proxiesTemp = request.getProxyServices() != null ? new ArrayList<>(request.getProxyServices()) : new ArrayList<>();
      List<String> proxies = new ArrayList<>();

      if (proxies != null && (session instanceof EqiCrawlerSession || session instanceof EqiRankingDiscoverKeywordsSession)) {
         for (String proxy : proxiesTemp) {
            proxies.add(proxy.toLowerCase().contains("infatica") ? ProxyCollection.INFATICA_RESIDENTIAL_BR_EQI : proxy);
         }
      } else if (proxies != null) {
         for (String proxy : proxiesTemp) {

            if (proxy.toLowerCase().contains("netnut_residential_br")) {
               proxies.add(ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY);
            } else if (proxy.toLowerCase().contains("netnut_residential_es")) {
               proxies.add(ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY);
            } else if (proxy.toLowerCase().contains("infatica")) {
               proxies.add(ProxyCollection.INFATICA_RESIDENTIAL_BR_HAPROXY);
            } else if (proxy.toLowerCase().contains("luminati_server")) {
               proxies.add(ProxyCollection.LUMINATI_SERVER_BR_HAPROXY);
            } else {
               proxies.add(proxy);
            }
         }
      }

      Map<String, String> headers = request.getHeaders();

      if (headers == null) {
         headers = new HashMap<>();
      }

      String randUserAgent =
            headers.containsKey(FetchUtilities.USER_AGENT) ? headers.get(FetchUtilities.USER_AGENT) : FetchUtilities.randUserAgent();

      int attemptsNumber = proxies.isEmpty() ? 2 : proxies.size();

      while (attempt <= attemptsNumber && (response.getBody() == null || response.getBody().isEmpty())) {
         long requestStartTime = System.currentTimeMillis();
         String requestHash = FetchUtilities.generateRequestHash(session);
         RequestsStatistics requestStats = new RequestsStatistics();
         requestStats.setAttempt(attempt);
         String proxyServiceName = proxies.isEmpty() ? ProxyCollection.LUMINATI_SERVER_BR_HAPROXY : proxies.get(attempt - 1);
         List<LettProxy> proxySelected = GlobalConfigurations.proxies.getProxy(proxyServiceName);

         try {

            Logging.printLogDebug(logger, session, "Performing GET request with JSOUP with " + proxyServiceName + ": " + request.getUrl());

            org.jsoup.Connection.Response res;

            if (getMethod) {
               res = Jsoup.connect(request.getUrl())
                     .method(Method.GET)
                     .ignoreContentType(true)
                     .ignoreHttpErrors(true)
                     .sslSocketFactory(FetchUtilities.createSSLSocketFactory())
                     .headers(headers)
                     .timeout(20000)
                     .followRedirects(request.isFollowRedirects())
                     .proxy(new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(proxySelected.get(0).getAddress(), proxySelected.get(0).getPort())))
                     .execute();
            } else {
               res = Jsoup.connect(request.getUrl())
                     .method(Method.POST)
                     .ignoreHttpErrors(true)
                     .ignoreContentType(true)
                     .sslSocketFactory(FetchUtilities.createSSLSocketFactory())
                     .headers(headers)
                     .timeout(20000)
                     .followRedirects(request.isFollowRedirects())
                     .proxy(new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(proxySelected.get(0).getAddress(), proxySelected.get(0).getPort())))
                     .requestBody(request.getPayload())
                     .execute();
            }

            String content = res.body();

            if (!content.isEmpty()) {
               FetcherOptions options = request.getFetcherOptions();
               if (options != null) {
                  String selector = options.getForbiddenCssSelector();

                  if (selector != null) {
                     Document doc = Jsoup.parse(content);

                     if (!doc.select(selector).isEmpty()) {
                        Logging.printLogWarn(logger, session, "[ATTEMPT " + attempt + "] Error performing " + (getMethod ? FetchUtilities.GET_REQUEST : FetchUtilities.POST_REQUEST) + " request. Error: Forbidden Selector detected.");
                        throw new ResponseCodeException(403);
                     }
                  }
               }

            }

            requestStats.setElapsedTime(System.currentTimeMillis() - requestStartTime);
            S3Service.saveResponseContent(session, requestHash, content);

            Map<String, String> responseHeaders = res.headers();

            response = new ResponseBuilder()
                  .setBody(content)
                  .setProxyused(!proxySelected.isEmpty() ? proxySelected.get(0) : null)
                  .setRedirecturl(res.url().toString())
                  .setHeaders(responseHeaders)
                  .setCookies(FetchUtilities.getCookiesFromHeadersMap(responseHeaders))
                  .setLastStatusCode(res.statusCode())
                  .build();

            requestStats.setHasPassedValidation(true);
            requests.add(requestStats);
            session.addRedirection(request.getUrl(), res.url().toString());

            FetchUtilities.sendRequestInfoLog(attempt, request, requestStats, proxyServiceName, getMethod ? FetchUtilities.GET_REQUEST : FetchUtilities.POST_REQUEST, randUserAgent, session,
                  res.statusCode(), requestHash);
         } catch (Exception e) {
            Logging.printLogDebug(logger, session, "Attempt " + attempt + " -> Error performing GET request. Error: " + e.getMessage());

            int code = e instanceof ResponseCodeException ? ((ResponseCodeException) e).getCode() : 0;

            FetchUtilities.sendRequestInfoLog(attempt, request, requestStats, proxyServiceName, getMethod ? FetchUtilities.GET_REQUEST : FetchUtilities.POST_REQUEST, randUserAgent, session,
                  code, requestHash);

            if (session instanceof TestCrawlerSession) {
               Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
            }

            response.setLastStatusCode(0);
         }
         attempt++;
      }

      JSONObject jsoupMetadata = new JSONObject().put("req_elapsed_time", System.currentTimeMillis() - requestsStartTime)
            .put("req_attempts_number", attempt)
            .put("req_type", "url_request")
            .put("req_lib", "jsoup");

      Logging.logInfo(logger, session, jsoupMetadata, "JSOUP REQUESTS INFO");

      response.setRequests(requests);

      return response;
   }

   @Override
   public File fetchImage(Session session, Request request) {
      return new ApacheDataFetcher().fetchImage(session, request);
   }

   public Response delete(Session session, Request request) {
      Response response = new Response();

      List<RequestsStatistics> requests = new ArrayList<>();

      String targetURL = request.getUrl();

      long requestsStartTime = System.currentTimeMillis();
      List<String> proxies = request.getProxyServices();

      long requestStartTime = System.currentTimeMillis();

      String content = "";

      try {
         Logging.printLogDebug(logger, session, "Performing GET request with HttpURLConnection: " + targetURL);
         String proxyService = proxies == null || proxies.isEmpty() ? ProxyCollection.LUMINATI_SERVER_BR_HAPROXY : proxies.get(0);

         List<LettProxy> proxyStorm = GlobalConfigurations.proxies.getProxy(proxyService);

         RequestsStatistics requestStats = new RequestsStatistics();
         requestStats.setAttempt(1);

         Map<String, String> headers = request.getHeaders();

         // http://www.nakov.com/blog/2009/07/16/disable-certificate-validation-in-java-ssl-connections/
         // on July 23, the comper site expired the ssl certificate, with that I had to ignore ssl
         // verification to happen the capture
         HostnameVerifier hostNameVerifier = new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
               return true;
            }
         };

         int timeout = request.getTimeout() != null ? request.getTimeout() : FetchUtilities.DEFAULT_CONNECT_TIMEOUT * 2;

         URL url = new URL(targetURL);
         HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
         connection.setHostnameVerifier(hostNameVerifier);
         connection.setSSLSocketFactory(FetchUtilities.createSSLSocketFactory());
         connection.setRequestMethod(FetchUtilities.DELETE_REQUEST);
         connection.setInstanceFollowRedirects(true);
         connection.setDoOutput(true);
         connection.setUseCaches(false);
         connection.setReadTimeout(timeout);
         connection.setConnectTimeout(timeout);


         for (Entry<String, String> entry : headers.entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
         }

         // Get Response
         InputStream is = connection.getInputStream();
         BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
         StringBuilder responseStr = new StringBuilder(); // or StringBuffer if Java version 5+
         String line;
         while ((line = rd.readLine()) != null) {
            responseStr.append(line);
            responseStr.append('\r');
         }
         rd.close();

         content = responseStr.toString();

         requestStats.setElapsedTime(System.currentTimeMillis() - requestStartTime);

         Map<String, String> responseHeaders = FetchUtilities.headersJavaNetToMap(connection.getHeaderFields());

         response = new ResponseBuilder()
               .setBody(content)
               .setProxyused(!proxyStorm.isEmpty() ? proxyStorm.get(0) : null)
               .setRedirecturl(connection.getURL().toString())
               .setHeaders(responseHeaders)
               .setCookies(FetchUtilities.getCookiesFromHeadersJavaNet(connection.getHeaderFields()))
               .setLastStatusCode(connection.getResponseCode())
               .build();

         requestStats.setHasPassedValidation(true);
         requests.add(requestStats);
         session.addRedirection(request.getUrl(), connection.getURL().toString());
      } catch (Exception e) {
         Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
         if (session instanceof TestCrawlerSession) {
            Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
         }

         response.setLastStatusCode(0);
      }

      JSONObject apacheMetadata = new JSONObject()
            .put("req_javanet_elapsed_time", System.currentTimeMillis() - requestsStartTime)
            .put("req_javanet_attempts_number", 1);

      Logging.logInfo(logger, session, apacheMetadata, "JAVANET REQUEST DELETE INFO");

      response.setRequests(requests);

      return response;
   }
}
