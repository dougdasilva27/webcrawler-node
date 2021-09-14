package br.com.lett.crawlernode.core.fetcher;

import br.com.lett.crawlernode.core.fetcher.models.LettProxy;
import br.com.lett.crawlernode.core.fetcher.models.PageContent;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.RequestsStatistics;
import br.com.lett.crawlernode.core.parser.Parser;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.crawler.EqiCrawlerSession;
import br.com.lett.crawlernode.core.session.crawler.ImageCrawlerSession;
import br.com.lett.crawlernode.core.session.ranking.EqiRankingDiscoverKeywordsSession;
import br.com.lett.crawlernode.main.GlobalConfigurations;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.DateUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.joda.time.DateTime;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class FetchUtilities {

   private FetchUtilities() {
   }

   public static final String USER_AGENT = "user-agent";
   public static final String FETCHER = "FETCHER";
   public static final String CRAWLER = "CRAWLER";
   public static final String GET_REQUEST = "GET";
   public static final String POST_REQUEST = "POST";
   public static final String DELETE_REQUEST = "DELETE";
   public static final String CONTENT_ENCODING = "compress, gzip";

   public static final String HEADER_SET_COOKIE = "Set-Cookie";

   public static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT = 10000; // ms
   public static final int DEFAULT_CONNECT_TIMEOUT = 10000; // ms
   public static final int DEFAULT_SOCKET_TIMEOUT = 10000; // ms

   public static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT_IMG = 20000; // ms
   public static final int DEFAULT_CONNECT_TIMEOUT_IMG = 20000; // ms
   public static final int DEFAULT_SOCKET_TIMEOUT_IMG = 20000; // ms
   public static final int THIRTY_SECONDS_TIMEOUT = 30000; // ms

   private static final Logger logger = LoggerFactory.getLogger(FetchUtilities.class);

   /**
    * catch from https://www.whatismybrowser.com/guides/the-latest-user-agent/?utm_source=whatismybrowsercom&utm_medium=internal&utm_campaign=breadcrumbs
    */
   public static List<String> userAgents;
   public static List<String> mobileUserAgents;
   public static List<String> errorCodes;
   public static List<String> highTimeoutMarkets;
   static {
      userAgents = Arrays.asList(

         "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 Safari/537.36",
         "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.82 Safari/537.36",
         "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.82 Safari/537.36",
         "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.82 Safari/537.36",
         "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:92.0) Gecko/20100101 Firefox/92.0",
         "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:92.0) Gecko/20100101 Firefox/92.0",
         "Mozilla/5.0 (X11; Fedora; Linux x86_64; rv:92.0) Gecko/20100101 Firefox/92.0",
         "Mozilla/5.0 (X11; Linux i686; rv:91.0) Gecko/20100101 Firefox/91.0",
         "Mozilla/5.0 (Linux x86_64; rv:91.0) Gecko/20100101 Firefox/91.0",
         "Mozilla/5.0 (X11; Ubuntu; Linux i686; rv:91.0) Gecko/20100101 Firefox/91.0",
         "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:91.0) Gecko/20100101 Firefox/91.0",
         "Mozilla/5.0 (X11; Fedora; Linux x86_64; rv:91.0) Gecko/20100101 Firefox/91.0",
         "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_6) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.2 Safari/605.1.15",
         "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.82 Safari/537.36 Edg/93.0.961.38",
         "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.82 Safari/537.36 Edg/93.0.961.38",
         "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.82 Safari/537.36 OPR/78.0.4093.184",
         "Mozilla/5.0 (Windows NT 10.0; WOW64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.82 Safari/537.36 OPR/78.0.4093.184",
         "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.82 Safari/537.36 OPR/78.0.4093.184",
         "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.82 Safari/537.36 OPR/78.0.4093.184");


         mobileUserAgents = Arrays.asList(

            "Mozilla/5.0 (Linux; Android 10; SM-A205U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.82 Mobile Safari/537.36",
            "Mozilla/5.0 (Linux; Android 10; LM-Q720) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.82 Mobile Safari/537.36",
            "Mozilla/5.0 (Linux; Android 10; LM-X420) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.82 Mobile Safari/537.36",
            "Mozilla/5.0 (iPad; CPU OS 14_8 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/93.0.4577.78 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (iPod; CPU iPhone OS 14_8 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/93.0.4577.78 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (Linux; Android 10; LM-Q710(FGN)) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.82 Mobile Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 11_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) FxiOS/37.0 Mobile/15E148 Safari/605.1.15",
            "Mozilla/5.0 (iPad; CPU OS 11_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) FxiOS/37.0 Mobile/15E148 Safari/605.1.15",
            "Mozilla/5.0 (iPod touch; CPU iPhone OS 11_6 like Mac OS X) AppleWebKit/604.5.6 (KHTML, like Gecko) FxiOS/37.0 Mobile/15E148 Safari/605.1.15",
            "Mozilla/5.0 (Android 11; Mobile; rv:68.0) Gecko/68.0 Firefox/92.0",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 14_8 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.2 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (iPad; CPU OS 14_8 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.2 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (iPod touch; CPU iPhone 14_8 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.2 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (Linux; Android 10; SM-G970F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.82 Mobile Safari/537.36 OPR/63.3.3216.58675",
            "Mozilla/5.0 (Linux; Android 10; SM-N975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.82 Mobile Safari/537.36 OPR/63.3.3216.58675");

            errorCodes = Arrays.asList("403");

      highTimeoutMarkets = Arrays.asList("bemol", "abxclimatizacao", "drogariapovao", "webcontinental", "drogarianissei", "lacomer", "poupafarma",
         "unicaarcondicionado", "multisom", "confianca", "medicamentosbrasil", "extramarketplace", "pontofrio", "casasbahia");
   }

   /**
    * Retrieve a random user agent from the user agents array.
    *
    * @return
    */
   public static String randUserAgent() {
      return userAgents.get(MathUtils.randInt(0, userAgents.size() - 1));
   }


   /**
    * Retrieve a random mobile user agent from the user agents array.
    *
    * @return
    */
   public static String randMobileUserAgent() {
      return mobileUserAgents.get(MathUtils.randInt(0, mobileUserAgents.size() - 1));
   }

   public static String generateRequestHash(Session session) {
      String s = session.getSessionId() + new DateTime(DateUtils.timeZone).toString("yyyy-MM-dd HH:mm:ss.SSS");
      return DigestUtils.md5Hex(s);
   }

   public static String getLoggingMessage(String requestType, String url) {
      return new StringBuilder().append("Request : [").append(requestType).append(", ").append(url).append("]").toString();
   }

   public static LettProxy getNextProxy(Session session, Request request, int attempt) {
      if (request.getProxyServices() != null) {
         return getNextProxy(request, attempt);
      }
      LettProxy nextProxy = null;
      List<LettProxy> proxies = new ArrayList<>();
      Integer attemptTemp = Integer.valueOf(attempt);
      Integer maxAttempts;

      maxAttempts = session.getMaxConnectionAttemptsCrawler();


      String serviceName;

      while (proxies.isEmpty() && attemptTemp <= maxAttempts) {
         serviceName = getProxyService(attemptTemp, session);

         if ((session instanceof EqiCrawlerSession || session instanceof EqiRankingDiscoverKeywordsSession) && serviceName.equals(ProxyCollection.INFATICA_RESIDENTIAL_BR_HAPROXY)) {
            serviceName = ProxyCollection.INFATICA_RESIDENTIAL_BR_EQI;
         }

         if (serviceName != null) {
            if (GlobalConfigurations.proxies != null) {
               proxies = GlobalConfigurations.proxies.getProxy(serviceName);
            }

            if (!proxies.isEmpty()) {
               nextProxy = proxies.get(MathUtils.randInt(0, proxies.size() - 1));
            } else {
               attemptTemp += 1;

               if (!ProxyCollection.NO_PROXY.equals(serviceName)) {
                  Logging.printLogWarn(logger, session,
                     "Error: trying use proxy service " + serviceName + ", but there was no proxy fetched for this service.");
               }
            }
         } else {
            attemptTemp += 1;
            Logging.printLogWarn(logger, session, "Error: trying to use an unknown proxy service. I'll try the next one.");
         }
      }

      return nextProxy;
   }

   public static LettProxy getNextProxy(Request request, int attempt) {
      LettProxy lettProxy = null;
      if (request.getProxyServices() != null && !request.getProxyServices().isEmpty()) {
         List<String> proxyServices = request.getProxyServices();
         String proxy = proxyServices.get((attempt - 1) % proxyServices.size());
         lettProxy = GlobalConfigurations.proxies.getProxy(proxy).stream().findAny().orElse(null);
      }
      return lettProxy;
   }

   /**
    * Select a proxy service according to the number of attempt.
    *
    * @param attempt
    * @param session
    * @return
    */
   public static String getProxyService(int attempt, Session session) {
      String service = null;

      if (session instanceof ImageCrawlerSession) {
         service = GlobalConfigurations.proxies.selectProxy(session, false, attempt);
      } else {
         service = GlobalConfigurations.proxies.selectProxy(session, true, attempt);
      }

      return service;
   }

   /**
    * @param proxy
    * @return
    */
   public static RequestConfig getRequestConfig(HttpHost proxy, boolean followRedirect, Session session) {
      RequestConfig requestConfig;

      if (proxy != null) {

         if (session.getMarket().getName() != null && highTimeoutMarkets.contains(session.getMarket().getName())) {
            requestConfig = RequestConfig.custom()
               .setCookieSpec(CookieSpecs.STANDARD)
               .setRedirectsEnabled(followRedirect) // set // true
               .setConnectionRequestTimeout(THIRTY_SECONDS_TIMEOUT)
               .setConnectTimeout(THIRTY_SECONDS_TIMEOUT)
               .setSocketTimeout(THIRTY_SECONDS_TIMEOUT)
               .setProxy(proxy).build();
         } else {
            requestConfig = RequestConfig.custom()
               .setCookieSpec(CookieSpecs.STANDARD)
               .setRedirectsEnabled(followRedirect) // set // true
               .setConnectionRequestTimeout(DEFAULT_CONNECTION_REQUEST_TIMEOUT)
               .setConnectTimeout(DEFAULT_CONNECT_TIMEOUT)
               .setSocketTimeout(DEFAULT_SOCKET_TIMEOUT)
               .setProxy(proxy)
               .build();
         }

      } else {
         if (session.getMarket().getName() != null && highTimeoutMarkets.contains(session.getMarket().getName())) {
            requestConfig = RequestConfig.custom()
               .setCookieSpec(CookieSpecs.STANDARD)
               .setRedirectsEnabled(followRedirect) // set // true
               .setConnectionRequestTimeout(THIRTY_SECONDS_TIMEOUT)
               .setConnectTimeout(THIRTY_SECONDS_TIMEOUT)
               .setSocketTimeout(THIRTY_SECONDS_TIMEOUT)
               .build();
         } else {
            requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD)
               .setRedirectsEnabled(followRedirect) // set // true
               .setConnectionRequestTimeout(DEFAULT_CONNECTION_REQUEST_TIMEOUT)
               .setConnectTimeout(DEFAULT_CONNECT_TIMEOUT)
               .setSocketTimeout(DEFAULT_SOCKET_TIMEOUT)
               .build();
         }
      }

      return requestConfig;
   }

   /**
    * Parse the page content, either to get a html or a plain text In case we are expecting JSONObject or JSONArray response from an API, the content will be parsed as a plain text. Otherwise it will
    * be parsed as a htlm format.
    *
    * @param pageContent
    * @param session
    * @return String with the request response, either in html or plain text format
    */
   public static String processContent(PageContent pageContent, Session session) {
      Parser parser = new Parser(session);
      parser.parse(pageContent);

      if (pageContent.getHtmlParseData() != null) {
         return pageContent.getHtmlParseData().getHtml();
      }
      if (pageContent.getTextParseData() != null) {
         return pageContent.getTextParseData().getTextContent();
      }

      return "";
   }

   public static Map<String, String> headersToMap(Header[] headers) {
      Map<String, String> headersMap = new HashMap<>();

      for (Header header : headers) {
         String headerName = header.getName();

         if (headerName.equalsIgnoreCase(HEADER_SET_COOKIE)) {
            headerName = HEADER_SET_COOKIE;
         }

         headersMap.put(headerName, header.getValue());
      }

      return headersMap;
   }

   public static Map<String, String> headersJavaNetToMap(Map<String, List<String>> headers) {
      Map<String, String> headersMap = new HashMap<>();

      for (Entry<String, List<String>> entry : headers.entrySet()) {
         String headerName = entry.getKey();

         if (headerName != null) {
            List<String> values = entry.getValue();

            if (headerName.equalsIgnoreCase(HEADER_SET_COOKIE)) {
               headerName = HEADER_SET_COOKIE;
            }

            headersMap.put(headerName, values.toString().replace("[", "").replace("]", ""));
         }
      }

      return headersMap;
   }

   public static List<Cookie> getCookiesFromHeadersJavaNet(Map<String, List<String>> headers) {
      List<Cookie> cookies = new ArrayList<>();

      for (Entry<String, List<String>> entry : headers.entrySet()) {
         String headerName = entry.getKey();

         if (headerName != null && headerName.equalsIgnoreCase(HEADER_SET_COOKIE)) {
            List<String> cookiesHeader = entry.getValue();

            for (String cookieHeader : cookiesHeader) {
               String cookieName = cookieHeader.split("=")[0].trim();

               int x = cookieHeader.indexOf(cookieName + "=") + cookieName.length() + 1;
               String cookieValue;
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
      }

      return cookies;
   }

   public static List<Cookie> getCookiesFromHeaders(Header[] headers) {
      List<Cookie> cookies = new ArrayList<>();

      for (Header header : headers) {
         try {
            String cookieHeader = header.getValue();
            String cookieName = cookieHeader.split("=")[0].trim();

            int x = cookieHeader.indexOf(cookieName + "=") + cookieName.length() + 1;
            String cookieValue;
            if (cookieHeader.contains(";")) {
               int y = cookieHeader.indexOf(';', x);
               cookieValue = cookieHeader.substring(x, y).trim();
            } else {
               cookieValue = cookieHeader.substring(x).trim();
            }

            BasicClientCookie cookie = new BasicClientCookie(cookieName, cookieValue);
            cookie.setPath("/");
            cookies.add(cookie);
         } catch (Exception e) {
            Logging.printLogWarn(logger, CommonMethods.getStackTrace(e));
         }
      }

      return cookies;
   }

   /**
    * @param headers
    * @return
    */
   public static List<Cookie> getCookiesFromHeadersMap(Map<String, String> headers) {
      List<Cookie> cookies = new ArrayList<>();

      if (headers.containsKey(HEADER_SET_COOKIE)) {
         String cookieHeader = headers.get(HEADER_SET_COOKIE);
         String cookieName = cookieHeader.split("=")[0].trim();

         int x = cookieHeader.indexOf(cookieName + "=") + cookieName.length() + 1;
         String cookieValue;
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

      return cookies;
   }

   /**
    * @return
    * @throws NoSuchAlgorithmException
    * @throws KeyManagementException
    */
   public static SSLConnectionSocketFactory createSSLConnectionSocketFactory() throws NoSuchAlgorithmException, KeyManagementException {
      TrustManager trustManager = new TrustManager();
      SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
      sslContext.init(null, new TrustManager[]{
         trustManager
      }, null);

      return new SSLConnectionSocketFactory(sslContext);
   }

   /**
    * @return
    * @throws NoSuchAlgorithmException
    * @throws KeyManagementException
    */
   public static SSLSocketFactory createSSLSocketFactory() throws NoSuchAlgorithmException, KeyManagementException {
      TrustManager trustManager = new TrustManager();
      SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
      sslContext.init(null, new TrustManager[]{
         trustManager
      }, null);

      return sslContext.getSocketFactory();
   }

   /**
    * @param request
    * @param response
    * @param method
    * @param userAgent
    * @param session
    * @param status
    * @param requestHash
    */
   public static void sendRequestInfoLog(int attempt, Request request, RequestsStatistics requestSatistic, LettProxy proxy, String method, String userAgent, Session session,
                                         int status, String requestHash) {

      JSONObject requestMetadata =
         new JSONObject().put("req_hash", requestHash)
            .put("proxy_name", (proxy == null ? ProxyCollection.NO_PROXY : proxy.getSource()))
            .put("proxy_ip", (proxy == null ? MDC.get("HOST_NAME") : proxy.getAddress()))
            .put("user_agent", userAgent)
            .put("req_method", method)
            .put("req_location", request != null ? request.getUrl() : "")
            .put("res_http_code", status)
            .put("req_elapsed_time", requestSatistic != null ? requestSatistic.getElapsedTime() : 0);

      Logging.logInfo(logger, session, requestMetadata, "[ATTEMPT " + attempt + "][REQUEST INFORMATION]");
   }

   public static void sendRequestInfoLog(int attempt, Request request, RequestsStatistics requestSatistic, String proxy, String method, String userAgent, Session session,
                                         int status, String requestHash) {

      JSONObject requestMetadata = new JSONObject().put("req_hash", requestHash).put("proxy_name", (proxy == null ? ProxyCollection.NO_PROXY : proxy))
         .put("user_agent", userAgent).put("req_method", method).put("req_location", request != null ? request.getUrl() : "")
         .put("res_http_code", status).put("req_elapsed_time", requestSatistic != null ? requestSatistic.getElapsedTime() : 0);

      Logging.logInfo(logger, session, requestMetadata, "[ATTEMPT " + attempt + "][REQUEST INFORMATION]");
   }

   public static class TrustManager implements X509TrustManager {

      @Override
      public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {

      }

      @Override
      public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {

      }

      @Override
      public X509Certificate[] getAcceptedIssuers() {
         return null;
      }
   }
}
