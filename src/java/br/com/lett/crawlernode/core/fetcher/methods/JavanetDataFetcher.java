package br.com.lett.crawlernode.core.fetcher.methods;

import br.com.lett.crawlernode.aws.s3.S3Service;
import br.com.lett.crawlernode.core.fetcher.FetchUtilities;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.LettProxy;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.RequestsStatistics;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.fetcher.models.Response.ResponseBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.crawler.TestCrawlerSession;
import br.com.lett.crawlernode.main.GlobalConfigurations;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import org.apache.http.cookie.Cookie;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class JavanetDataFetcher implements DataFetcher {

   private static final Logger logger = LoggerFactory.getLogger(JavanetDataFetcher.class);

   @Override
   public Response get(Session session, Request request) {
      Response response = new Response();

      List<RequestsStatistics> requests = new ArrayList<>();

      String targetURL = request.getUrl();
      int attempt = 1;

      long requestsStartTime = System.currentTimeMillis();
      List<String> proxies = request.getProxyServices() != null ? new ArrayList<>(request.getProxyServices()) : new ArrayList<>();


      while (attempt < 4 && (response.getBody() == null || response.getBody().isEmpty())) {
         long requestStartTime = System.currentTimeMillis();
         try {
            Logging.printLogDebug(logger, session, "Performing GET request with HttpURLConnection: " + targetURL);
            String proxyService = proxies == null || proxies.isEmpty() ? ProxyCollection.LUMINATI_SERVER_BR_HAPROXY : proxies.get(0);

            List<LettProxy> proxySelected = GlobalConfigurations.proxies.getProxy(proxyService);

            RequestsStatistics requestStats = new RequestsStatistics();
            requestStats.setAttempt(attempt);

            Map<String, String> headers = request.getHeaders();
            String randUserAgent =
               headers.containsKey(FetchUtilities.USER_AGENT) ? headers.get(FetchUtilities.USER_AGENT) : FetchUtilities.randUserAgent();
            String requestHash = FetchUtilities.generateRequestHash(session);

            if (request.getCookies() != null && !request.getCookies().isEmpty()) {
               headers.put("cookie", CommonMethods.cookiesToString(request.getCookies()));
            }

            String content = "";
            Proxy proxy = null;

            if (!proxySelected.isEmpty() && attempt < 4) {
               Logging.printLogDebug(logger, session, "Using " + proxyService + " for this request.");
               proxy = new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(proxySelected.get(0).getAddress(), proxySelected.get(0).getPort()));
            } else {
               Logging.printLogWarn(logger, session, "Using NO_PROXY for this request: " + targetURL);
            }


            // http://www.nakov.com/blog/2009/07/16/disable-certificate-validation-in-java-ssl-connections/
            // on July 23, the comper site expired the ssl certificate, with that I had to ignore ssl
            // verification to happen the capture
            HostnameVerifier hostNameVerifier = new HostnameVerifier() {
               @Override
               public boolean verify(String hostname, SSLSession session) {
                  return true;
               }
            };

            URL url = new URL(targetURL);
            HttpsURLConnection connection = proxy != null ? (HttpsURLConnection) url.openConnection(proxy) : (HttpsURLConnection) url.openConnection();
            connection.setHostnameVerifier(hostNameVerifier);
            connection.setSSLSocketFactory(FetchUtilities.createSSLSocketFactory());
            connection.setRequestMethod(FetchUtilities.GET_REQUEST);
            connection.setInstanceFollowRedirects(true);
            connection.setUseCaches(false);
            connection.setReadTimeout(FetchUtilities.DEFAULT_CONNECT_TIMEOUT * 2);
            connection.setConnectTimeout(FetchUtilities.DEFAULT_CONNECT_TIMEOUT * 2);

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
            S3Service.saveResponseContent(session, requestHash, content);

            Map<String, String> responseHeaders = FetchUtilities.headersJavaNetToMap(connection.getHeaderFields());

            response = new ResponseBuilder()
               .setBody(content)
               .setProxyused(!proxySelected.isEmpty() ? proxySelected.get(0) : null)
               .setRedirecturl(connection.getURL().toString())
               .setHeaders(responseHeaders)
               .setCookies(FetchUtilities.getCookiesFromHeadersJavaNet(connection.getHeaderFields()))
               .setLastStatusCode(connection.getResponseCode())
               .build();

            requestStats.setHasPassedValidation(true);
            requests.add(requestStats);
            session.addRedirection(request.getUrl(), connection.getURL().toString());

            FetchUtilities.sendRequestInfoLog(attempt, request, requestStats, ProxyCollection.LUMINATI_SERVER_BR_HAPROXY, FetchUtilities.GET_REQUEST, randUserAgent, session,
               connection.getResponseCode(), requestHash);
         } catch (Exception e) {
            Logging.printLogDebug(logger, session, "Attempt " + attempt + " -> Error performing GET request. Error: " + e.getMessage());
            if (session instanceof TestCrawlerSession) {
               Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
            }

            response.setLastStatusCode(0);
         }
         attempt++;
      }

      JSONObject apacheMetadata = new JSONObject().put("req_elapsed_time", System.currentTimeMillis() - requestsStartTime)
         .put("req_attempts_number", attempt)
         .put("req_type", "url_request")
         .put("req_lib", "javanet");

      Logging.logInfo(logger, session, apacheMetadata, "JAVANET REQUESTS INFO");

      response.setRequests(requests);

      return response;
   }

   /**
    * Utility method to serialize cookies list into a header string
    *
    * @param cookies
    * @return
    */
   private String buildCookiesString(List<Cookie> cookies) {
      String cookie = "";
      for (Cookie ck : cookies) {
         cookie += (ck.getName() + "=" + ck.getValue() + "; ");
      }
      cookie = cookie.length() > 2 ? cookie.substring(0, cookie.length() - 2) : "";

      return cookie;
   }

   @Override
   public Response post(Session session, Request request) {
      Response response = new Response();
      List<RequestsStatistics> requests = new ArrayList<>();

      String targetURL = request.getUrl();
      int attempt = 1;

      while (attempt < 4 && (response.getBody() == null || response.getBody().isEmpty())) {
         long requestStartTime = System.currentTimeMillis();
         try {
            Logging.printLogDebug(logger, session, "Performing GET request with HttpURLConnection: " + targetURL);
            List<LettProxy> proxyStorm = GlobalConfigurations.proxies.getProxy(ProxyCollection.LUMINATI_SERVER_BR_HAPROXY);

            RequestsStatistics requestStats = new RequestsStatistics();
            requestStats.setAttempt(attempt);

            Map<String, String> headers = request.getHeaders();
            String randUserAgent =
               headers.containsKey(FetchUtilities.USER_AGENT) ? headers.get(FetchUtilities.USER_AGENT) : FetchUtilities.randUserAgent();
            String requestHash = FetchUtilities.generateRequestHash(session);

            String content = "";
            Proxy proxy = null;

            if (!proxyStorm.isEmpty() && attempt < 4) {
               Logging.printLogDebug(logger, session, "Using " + ProxyCollection.LUMINATI_SERVER_BR_HAPROXY + " for this request.");
               proxy = new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(proxyStorm.get(0).getAddress(), proxyStorm.get(0).getPort()));
            } else {
               Logging.printLogWarn(logger, session, "Using NO_PROXY for this request: " + targetURL);
            }

            URL url = new URL(targetURL);
            HttpURLConnection connection = proxy != null ? (HttpURLConnection) url.openConnection(proxy) : (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(FetchUtilities.POST_REQUEST);
            connection.setInstanceFollowRedirects(true);
            connection.setUseCaches(false);
            connection.setReadTimeout(FetchUtilities.DEFAULT_CONNECT_TIMEOUT * 2);
            connection.setConnectTimeout(FetchUtilities.DEFAULT_CONNECT_TIMEOUT * 2);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            for (Entry<String, String> entry : headers.entrySet()) {
               connection.setRequestProperty(entry.getKey(), entry.getValue());
            }

            connection.setRequestProperty("Cookie", buildCookiesString(request.getCookies()));

            // Inserting payload
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
            writer.write(request.getPayload());
            writer.close();

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
            S3Service.saveResponseContent(session, requestHash, content);

            response = new ResponseBuilder().setBody(content).setProxyused(!proxyStorm.isEmpty() ? proxyStorm.get(0) : null).build();
            requestStats.setHasPassedValidation(true);
            requests.add(requestStats);

            FetchUtilities.sendRequestInfoLog(attempt, request, requestStats, ProxyCollection.LUMINATI_SERVER_BR_HAPROXY, FetchUtilities.GET_REQUEST, randUserAgent, session,
               connection.getResponseCode(), requestHash);
         } catch (Exception e) {
            Logging.printLogWarn(logger, session, "Attempt " + attempt + " -> Error performing POST request. Error: " + e.getMessage());
            if (session instanceof TestCrawlerSession) {
               Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
            }

            response.setLastStatusCode(0);
         }
         attempt++;
      }

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
