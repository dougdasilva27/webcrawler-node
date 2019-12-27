package br.com.lett.crawlernode.core.fetcher.methods;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.http.cookie.Cookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

public class JavanetDataFetcher implements DataFetcher {

   private static final Logger logger = LoggerFactory.getLogger(JavanetDataFetcher.class);

   @Override
   public Response get(Session session, Request request) {
      Response response = new Response();

      String targetURL = request.getUrl();
      int attempt = 1;

      while (attempt < 4 && (response.getBody() == null || response.getBody().isEmpty())) {
         try {
            Logging.printLogDebug(logger, session, "Performing GET request with HttpURLConnection: " + targetURL);
            List<LettProxy> proxyStorm = GlobalConfigurations.proxies.getProxy(ProxyCollection.STORM_RESIDENTIAL_US);

            RequestsStatistics requestStats = new RequestsStatistics();
            requestStats.setAttempt(attempt);

            Map<String, String> headers = request.getHeaders();
            String randUserAgent =
                  headers.containsKey(FetchUtilities.USER_AGENT) ? headers.get(FetchUtilities.USER_AGENT) : FetchUtilities.randUserAgent();
            String requestHash = FetchUtilities.generateRequestHash(session);

            String content = "";
            Proxy proxy = null;

            if (!proxyStorm.isEmpty() && attempt < 4) {
               Logging.printLogDebug(logger, session, "Using " + ProxyCollection.STORM_RESIDENTIAL_US + " for this request.");
               proxy = new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(proxyStorm.get(0).getAddress(), proxyStorm.get(0).getPort()));
            } else {
               Logging.printLogWarn(logger, session, "Using NO_PROXY for this request: " + targetURL);
            }

            URL url = new URL(targetURL);
            HttpURLConnection connection = proxy != null ? (HttpURLConnection) url.openConnection(proxy) : (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(FetchUtilities.GET_REQUEST);
            connection.setInstanceFollowRedirects(true);
            connection.setUseCaches(false);
            connection.setReadTimeout(FetchUtilities.DEFAULT_CONNECT_TIMEOUT * 2);

            for (Entry<String, String> entry : headers.entrySet()) {
               connection.setRequestProperty(entry.getKey(), entry.getValue());
            }

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
            content = responseStr.toString();

            S3Service.saveResponseContent(session, requestHash, content);


            response = new ResponseBuilder().setBody(content).setProxyused(!proxyStorm.isEmpty() ? proxyStorm.get(0) : null).build();
            requestStats.setHasPassedValidation(true);

            FetchUtilities.sendRequestInfoLog(attempt, request, response, ProxyCollection.STORM_RESIDENTIAL_US, FetchUtilities.GET_REQUEST, randUserAgent, session,
                  connection.getResponseCode(), requestHash);
         } catch (Exception e) {
            Logging.printLogWarn(logger, session, "Attempt " + attempt + " -> Error performing GET request. Error: " + e.getMessage());
            if (session instanceof TestCrawlerSession) {
               Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
            }
         }
         attempt++;
      }

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

      String targetURL = request.getUrl();
      int attempt = 1;

      while (attempt < 4 && (response.getBody() == null || response.getBody().isEmpty())) {
         try {
            Logging.printLogDebug(logger, session, "Performing GET request with HttpURLConnection: " + targetURL);
            List<LettProxy> proxyStorm = GlobalConfigurations.proxies.getProxy(ProxyCollection.STORM_RESIDENTIAL_US);

            RequestsStatistics requestStats = new RequestsStatistics();
            requestStats.setAttempt(attempt);

            Map<String, String> headers = request.getHeaders();
            String randUserAgent =
                  headers.containsKey(FetchUtilities.USER_AGENT) ? headers.get(FetchUtilities.USER_AGENT) : FetchUtilities.randUserAgent();
            String requestHash = FetchUtilities.generateRequestHash(session);

            String content = "";
            Proxy proxy = null;

            if (!proxyStorm.isEmpty() && attempt < 4) {
               Logging.printLogDebug(logger, session, "Using " + ProxyCollection.STORM_RESIDENTIAL_US + " for this request.");
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
            connection.setDoInput(true);
            connection.setDoOutput(true);

            for (Entry<String, String> entry : headers.entrySet()) {
               connection.setRequestProperty(entry.getKey(), entry.getValue());
            }

            connection.setRequestProperty("Cookie", buildCookiesString(request.getCookies()));

            // Inserting payload
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
            writer.write(request.getPayload());
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
            content = responseStr.toString();

            S3Service.saveResponseContent(session, requestHash, content);


            response = new ResponseBuilder().setBody(content).setProxyused(!proxyStorm.isEmpty() ? proxyStorm.get(0) : null).build();
            requestStats.setHasPassedValidation(true);

            FetchUtilities.sendRequestInfoLog(attempt, request, response, ProxyCollection.STORM_RESIDENTIAL_US, FetchUtilities.GET_REQUEST, randUserAgent, session,
                  connection.getResponseCode(), requestHash);
         } catch (Exception e) {
            Logging.printLogWarn(logger, session, "Attempt " + attempt + " -> Error performing POST request. Error: " + e.getMessage());
            if (session instanceof TestCrawlerSession) {
               Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
            }
         }
         attempt++;
      }

      return response;
   }

   @Override
   public File fetchImage(Session session, Request request) {
      return new ApacheDataFetcher().fetchImage(session, request);
   }

}
