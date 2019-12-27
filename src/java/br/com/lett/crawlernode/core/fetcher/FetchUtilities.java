package br.com.lett.crawlernode.core.fetcher;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLContext;
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
import br.com.lett.crawlernode.core.fetcher.models.LettProxy;
import br.com.lett.crawlernode.core.fetcher.models.PageContent;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.parser.Parser;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.crawler.ImageCrawlerSession;
import br.com.lett.crawlernode.main.GlobalConfigurations;
import br.com.lett.crawlernode.util.DateUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;

public class FetchUtilities {

   private FetchUtilities() {}

   public static final String USER_AGENT = "user-agent";
   public static final String FETCHER = "FETCHER";
   public static final String CRAWLER = "CRAWLER";
   public static final String GET_REQUEST = "GET";
   public static final String POST_REQUEST = "POST";
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
    * Most popular agents, retrieved from
    * https://techblog.willshouse.com/2012/01/03/most-common-user-agents/
    */
   public static List<String> userAgents;
   public static List<String> userAgentsWithoutChrome;
   public static List<String> mobileUserAgents;
   public static List<String> errorCodes;
   public static List<String> highTimeoutMarkets;

   static {
      userAgents = Arrays.asList("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_5) AppleWebKit/601.6.17 (KHTML, like Gecko) Version/9.1.1 Safari/601.6.17",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36",
            "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:46.0) Gecko/20100101 Firefox/46.0",
            "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:46.0) Gecko/20100101 Firefox/46.0",
            "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.84 Safari/537.36",
            "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.84 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36",
            "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:46.0) Gecko/20100101 Firefox/46.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_4) AppleWebKit/601.5.17 (KHTML, like Gecko) Version/9.1 Safari/601.5.17",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.84 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.11; rv:46.0) Gecko/20100101 Firefox/46.0",
            "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36");

      userAgentsWithoutChrome = Arrays.asList(
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_5) AppleWebKit/601.6.17 (KHTML, like Gecko) Version/9.1.1 Safari/601.6.17",
            "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:46.0) Gecko/20100101 Firefox/46.0",
            "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:46.0) Gecko/20100101 Firefox/46.0",
            "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:46.0) Gecko/20100101 Firefox/46.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_4) AppleWebKit/601.5.17 (KHTML, like Gecko) Version/9.1 Safari/601.5.17",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.11; rv:46.0) Gecko/20100101 Firefox/46.0"
      );

      mobileUserAgents = Arrays.asList(
            "Mozilla/5.0 (Linux; Android 7.0; SM-G930V Build/NRD90M) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.125 Mobile Safari/537.36",
            "Mozilla/5.0 (Linux; Android 7.0; SM-A310F Build/NRD90M) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.91 Mobile Safari/537.36 OPR/42.7.2246.114996",
            "Opera/9.80 (Android 4.1.2; Linux; Opera Mobi/ADR-1305251841) Presto/2.11.355 Version/12.10",
            "Mozilla/5.0 (Linux; Android 5.1.1; Nexus 5 Build/LMY48B; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/43.0.2357.65 Mobile Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 10_3 like Mac OS X) AppleWebKit/603.1.23 (KHTML, like Gecko) Version/10.0 Mobile/14E5239e Safari/602.1",
            "Mozilla/5.0 (Linux; Android 4.0.4; Galaxy Nexus Build/IMM76B) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.133 Mobile Safari/535.19");

      errorCodes = Arrays.asList("403");

      highTimeoutMarkets = Arrays.asList("bemol", "abxclimatizacao", "drogariapovao", "webcontinental", "drogarianissei", "lacomer", "poupafarma",
            "unicaarcondicionado", "multisom", "confianca", "medicamentosbrasil");
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
    * Retrieve a random user agent (chrome is not considered for this fucntion) from the user agents
    * array.
    * 
    * @return
    */
   public static String randUserAgentWithoutChrome() {
      return userAgentsWithoutChrome.get(MathUtils.randInt(0, userAgentsWithoutChrome.size() - 1));
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

   public static LettProxy getNextProxy(Session session, int attempt) {
      LettProxy nextProxy = null;
      List<LettProxy> proxies = new ArrayList<>();
      Integer attemptTemp = Integer.valueOf(attempt);
      Integer maxAttempts;

      if (session instanceof ImageCrawlerSession) {
         maxAttempts = session.getMaxConnectionAttemptsImages();
      } else {
         maxAttempts = session.getMaxConnectionAttemptsCrawler();
      }

      String serviceName = null;

      while (proxies.isEmpty() && attemptTemp <= maxAttempts) {
         serviceName = getProxyService(attemptTemp, session);

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

   /**
    * Select a proxy service according to the number of attempt.
    * 
    * @param attempt
    * @param session
    * @param proxyServices
    * @return
    */
   public static String getProxyService(int attempt, Session session) {
      String service = null;

      if (session instanceof ImageCrawlerSession) {
         service = GlobalConfigurations.proxies.selectProxy(session.getMarket(), false, attempt);
      } else {
         service = GlobalConfigurations.proxies.selectProxy(session.getMarket(), true, attempt);
      }

      return service;
   }

   /**
    * 
    * @param proxy
    * @return
    */
   public static RequestConfig getRequestConfig(HttpHost proxy, boolean followRedirect, Session session) {
      RequestConfig requestConfig;

      if (proxy != null) {

         if (session.getMarket().getName() != null && highTimeoutMarkets.contains(session.getMarket().getName())) {
            requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).setRedirectsEnabled(followRedirect) // set // true
                  .setConnectionRequestTimeout(THIRTY_SECONDS_TIMEOUT).setConnectTimeout(THIRTY_SECONDS_TIMEOUT).setSocketTimeout(THIRTY_SECONDS_TIMEOUT)
                  .setProxy(proxy).build();
         } else {
            requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).setRedirectsEnabled(followRedirect) // set // true
                  .setConnectionRequestTimeout(DEFAULT_CONNECTION_REQUEST_TIMEOUT).setConnectTimeout(DEFAULT_CONNECT_TIMEOUT)
                  .setSocketTimeout(DEFAULT_SOCKET_TIMEOUT).setProxy(proxy).build();
         }

      } else {
         if (session.getMarket().getName() != null && highTimeoutMarkets.contains(session.getMarket().getName())) {
            requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).setRedirectsEnabled(followRedirect) // set // true
                  .setConnectionRequestTimeout(THIRTY_SECONDS_TIMEOUT).setConnectTimeout(THIRTY_SECONDS_TIMEOUT).setSocketTimeout(THIRTY_SECONDS_TIMEOUT)
                  .build();
         } else {
            requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).setRedirectsEnabled(followRedirect) // set // true
                  .setConnectionRequestTimeout(DEFAULT_CONNECTION_REQUEST_TIMEOUT).setConnectTimeout(DEFAULT_CONNECT_TIMEOUT)
                  .setSocketTimeout(DEFAULT_SOCKET_TIMEOUT).build();
         }
      }

      return requestConfig;
   }

   /**
    * Parse the page content, either to get a html or a plain text In case we are expecting JSONObject
    * or JSONArray response from an API, the content will be parsed as a plain text. Otherwise it will
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

   public static List<Cookie> getCookiesFromHeaders(Header[] headers) {
      List<Cookie> cookies = new ArrayList<>();

      for (Header header : headers) {
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
      }

      return cookies;
   }

   /**
    * 
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
    * 
    * @return
    * @throws NoSuchAlgorithmException
    * @throws KeyManagementException
    */
   public static SSLConnectionSocketFactory createSSLConnectionSocketFactory() throws NoSuchAlgorithmException, KeyManagementException {
      TrustManager trustManager = new TrustManager();
      SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
      sslContext.init(null, new TrustManager[] {
               trustManager
      }, null);

      return new SSLConnectionSocketFactory(sslContext);
   }

   /**
    * 
    * @param request
    * @param response
    * @param method
    * @param userAgent
    * @param session
    * @param status
    * @param requestHash
    */
   public static void sendRequestInfoLog(int attempt, Request request, Response response, LettProxy proxy, String method, String userAgent, Session session,
         int status, String requestHash) {

      JSONObject requestMetadata =
            new JSONObject().put("req_hash", requestHash).put("proxy_name", (proxy == null ? ProxyCollection.NO_PROXY : proxy.getSource()))
                  .put("proxy_ip", (proxy == null ? MDC.get("HOST_NAME") : proxy.getAddress())).put("user_agent", userAgent).put("req_method", method)
                  .put("req_location", request != null ? request.getUrl() : "").put("res_http_code", status);

      Logging.logDebug(logger, session, requestMetadata, "[ATTEMPT " + attempt + "][REQUEST INFORMATION] " + request.getUrl());
   }

   public static void sendRequestInfoLog(int attempt, Request request, Response response, String proxy, String method, String userAgent, Session session,
         int status, String requestHash) {

      JSONObject requestMetadata = new JSONObject().put("req_hash", requestHash).put("proxy_name", (proxy == null ? ProxyCollection.NO_PROXY : proxy))
            .put("user_agent", userAgent).put("req_method", method).put("req_location", request != null ? request.getUrl() : "")
            .put("res_http_code", status);

      Logging.logDebug(logger, session, requestMetadata, "[ATTEMPT " + attempt + "][REQUEST INFORMATION] " + request.getUrl());
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
