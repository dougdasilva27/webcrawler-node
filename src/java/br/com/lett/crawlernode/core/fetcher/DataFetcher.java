package br.com.lett.crawlernode.core.fetcher;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import br.com.lett.crawlernode.aws.s3.S3Service;
import br.com.lett.crawlernode.core.fetcher.methods.GETFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.POSTFetcher;
import br.com.lett.crawlernode.core.parser.Parser;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.crawler.ImageCrawlerSession;
import br.com.lett.crawlernode.exceptions.ResponseCodeException;
import br.com.lett.crawlernode.main.GlobalConfigurations;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.DateUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;


/**
 * Auxiliar class for http requests
 * 
 * @author Samir Leão
 *
 */
public class DataFetcher {

  protected static final Logger logger = LoggerFactory.getLogger(DataFetcher.class);

  public static final String HTTP_COOKIE_HEADER = "Set-Cookie";

  public static final String GET_REQUEST = "GET";
  public static final String POST_REQUEST = "POST";

  public static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";
  public static final String HTTP_HEADER_ACCEPT = "Accept";

  // public static final int MAX_ATTEMPTS_FOR_CONECTION_WITH_PROXY = 10;

  public static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT = 10000; // ms
  public static final int DEFAULT_CONNECT_TIMEOUT = 10000; // ms
  public static final int DEFAULT_SOCKET_TIMEOUT = 10000; // ms

  public static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT_IMG = 20000; // ms
  public static final int DEFAULT_CONNECT_TIMEOUT_IMG = 20000; // ms
  public static final int DEFAULT_SOCKET_TIMEOUT_IMG = 20000; // ms

  public static final int THIRTY_SECONDS_TIMEOUT = 30000;

  public static final String CONTENT_ENCODING = "compress, gzip";



  /**
   * Most popular agents, retrieved from
   * https://techblog.willshouse.com/2012/01/03/most-common-user-agents/
   */
  public static List<String> userAgents;

  public static List<String> mobileUserAgents;

  public static List<String> errorCodes;

  public static List<String> highTimeoutMarkets;

  /**
   * Static initialization block I commented these two user, because in the site homerefil was
   * considering them outdated:
   * 
   * Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; rv:11.0) like Gecko Mozilla/5.0 (Macintosh; U;
   * Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2
   * 
   */
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
   * Fetch a text string from a URL, either by a GET ou POST http request.
   * 
   * @param reqType The type of the http request. GET_REQUEST or POST_REQUEST.
   * @param session
   * @param url The url from which we will fetch the data.
   * @param urlParameters The urlParameters, or parameter field of the request, in case of a POST
   *        request. null if we have a GET request.
   * @param cookies
   * @return A string containing the page html content
   */
  public static String fetchString(String reqType, Session session, String url, String urlParameters, List<Cookie> cookies) {
    return fetchPage(reqType, session, url, urlParameters, cookies, 1);
  }

  /**
   * 
   * @param session
   * @param localFileDir
   * @return
   * @throws IOException
   */
  public static File fetchImage(Session session) throws IOException {
    return downloadImageFromMarket(1, session);
  }

  /**
   * Fetch a HTML Document from a URL, either by a GET ou POST http request.
   * 
   * @param reqType The type of the http request. GET_REQUEST or POST_REQUEST.
   * @param session
   * @param url The url from which we will fetch the data.
   * @param urlParameters The urlParameters, or parameter field of the request, in case of a POST
   *        request. null if we have a GET request.
   * @param cookies
   * @return A Document with the data from the url passed, or null if something went wrong.
   */
  public static Document fetchDocument(String reqType, Session session, String url, String urlParameters, List<Cookie> cookies) {
    return Jsoup.parse(fetchPage(reqType, session, url, urlParameters, cookies, 1));
  }

  /**
   * Fetch a XML Document from a URL, either by a GET ou POST http request.
   * 
   * @param reqType The type of the http request. GET_REQUEST or POST_REQUEST.
   * @param session
   * @param url The url from which we will fetch the data.
   * @param urlParameters The urlParameters, or parameter field of the request, in case of a POST
   *        request. null if we have a GET request.
   * @param cookies
   * @return A Document with the data from the url passed, or null if something went wrong.
   */
  public static Document fetchDocumentXml(String reqType, Session session, String url, String urlParameters, List<Cookie> cookies) {
    return Jsoup.parse(fetchPage(reqType, session, url, urlParameters, cookies, 1), "", org.jsoup.parser.Parser.xmlParser());
  }

  /**
   * Fetch a json object from the API, either by a GET ou POST http request.
   * 
   * @param reqType The type of the http request. GET_REQUEST or POST_REQUEST.
   * @param session
   * @param url The url from which we will fetch the data.
   * @param payload The payload, or parameter field of the request, in case of a POST request. null if
   *        we have a GET request.
   * @param cookies
   * @return A JSONObject with the data from the url passed, or null if something went wrong.
   */
  public static JSONObject fetchJSONObject(String reqType, Session session, String url, String payload, List<Cookie> cookies) {
    try {
      return CrawlerUtils.stringToJson(fetchJson(reqType, session, url, payload, cookies, 1));
    } catch (JSONException e) {
      Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
      return new JSONObject();
    }
  }

  /**
   * Fetch a json array from the API, either by a GET ou POST http request.
   * 
   * @param reqType The type of the http request. GET_REQUEST or POST_REQUEST.
   * @param session
   * @param url The url from which we will fetch the data.
   * @param payload The payload, or parameter field of the request, in case of a POST request. null if
   *        we have a GET request.
   * @param cookies
   * @return A JSONArray with the data from the url passed, or null if something went wrong.
   */
  public static JSONArray fetchJSONArray(String reqType, Session session, String url, String payload, List<Cookie> cookies) {
    try {
      return CrawlerUtils.stringToJsonArray(fetchJson(reqType, session, url, payload, cookies, 1));
    } catch (JSONException e) {
      Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
      return new JSONArray();
    }
  }

  /**
   * Fetch a stringfied json object.
   * 
   * @param reqType
   * @param session
   * @param url
   * @param payload
   * @param cookies
   * @param attempt
   * @return a json object string, even if it is an empty json object
   */
  private static String fetchJson(String reqType, Session session, String url, String payload, List<Cookie> cookies, int attempt) {
    try {

      if (reqType.equals(GET_REQUEST)) {
        return GETFetcher.fetchPageGET(session, url, cookies, attempt);
      } else if (reqType.equals(POST_REQUEST)) {
        if (payload != null) {
          return POSTFetcher.fetchJsonPOST(session, url, payload, cookies, attempt);
        } else {
          Logging.printLogWarn(logger, session, "Parametro payload está null.");
        }
      } else {
        Logging.printLogWarn(logger, session, "Parametro reqType é inválido.");
      }

    } catch (Exception e) {
      Logging.printLogWarn(logger, session, "Tentativa " + attempt + " -> Erro ao fazer requisição de JSONObject via " + reqType + ": " + url);
      Logging.printLogWarn(logger, session, CommonMethods.getStackTraceString(e));


      if (attempt >= session.getMaxConnectionAttemptsCrawler()) {
        Logging.printLogWarn(logger, session, "Reached maximum attempts for URL [" + url + "]");
      } else {
        return fetchJson(reqType, session, url, payload, cookies, attempt + 1);
      }

    }

    return new JSONObject().toString();

  }

  public static CookieStore createCookieStore(List<Cookie> cookies) {
    CookieStore cookieStore = new BasicCookieStore();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        cookieStore.addCookie(cookie);
      }
    }
    return cookieStore;
  }

  public static RequestConfig createRequestConfig(HttpHost proxy) {
    RequestConfig requestConfig;
    if (proxy != null) {
      requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).setRedirectsEnabled(true)
          .setConnectionRequestTimeout(DEFAULT_CONNECTION_REQUEST_TIMEOUT).setConnectTimeout(DEFAULT_CONNECT_TIMEOUT)
          .setSocketTimeout(DEFAULT_SOCKET_TIMEOUT).setProxy(proxy).build();
    } else {
      requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).setRedirectsEnabled(true)
          .setConnectionRequestTimeout(DEFAULT_CONNECTION_REQUEST_TIMEOUT).setConnectTimeout(DEFAULT_CONNECT_TIMEOUT)
          .setSocketTimeout(DEFAULT_SOCKET_TIMEOUT).build();
    }

    return requestConfig;
  }

  /**
   * 
   * @param reqType
   * @param session
   * @param url
   * @param urlParameters
   * @param cookies
   * @param attempt
   * @return
   */
  private static String fetchPage(String reqType, Session session, String url, String urlParameters, List<Cookie> cookies, int attempt) {

    try {

      if (reqType.equals(GET_REQUEST)) {
        return GETFetcher.fetchPageGET(session, url, cookies, attempt);
      } else if (reqType.equals(POST_REQUEST)) {
        if (urlParameters != null) {
          return POSTFetcher.fetchPagePOST(session, url, urlParameters, cookies, attempt);
        } else {
          Logging.printLogWarn(logger, session, "Parameter payload is null.");
          return "";
        }
      } else {
        Logging.printLogWarn(logger, session, "Invalid reqType parameter.");
        return "";
      }

    } catch (Exception e) {
      Logging.printLogWarn(logger, session, "Attempt " + attempt + " -> Error in " + reqType + " request for URL: " + url);
      Logging.printLogWarn(logger, session, CommonMethods.getStackTraceString(e));

      if (attempt >= session.getMaxConnectionAttemptsCrawler()) {
        Logging.printLogWarn(logger, session, "Reached maximum attempts for URL [" + url + "]");
        return "";

      } else {
        return fetchPage(reqType, session, url, urlParameters, cookies, attempt + 1);
      }

    }

  }

  public static SSLConnectionSocketFactory createSSLConnectionSocketFactory() throws NoSuchAlgorithmException, KeyManagementException {
    TrustManager trustManager = new TrustManager();
    SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
    sslContext.init(null, new TrustManager[] {trustManager}, null);

    return new SSLConnectionSocketFactory(sslContext);
  }

  public static Map<String, String> fetchCookies(Session session, String url, List<Cookie> cookies, int attempt) {
    return fetchCookies(session, url, cookies, null, attempt);
  }

  public static Map<String, String> fetchCookies(Session session, String url, List<Cookie> cookies, String userAgent, int attempt) {
    return fetchCookies(session, url, cookies, userAgent, null, attempt);
  }

  @Deprecated
  public static Map<String, String> fetchCookies(Session session, String url, List<Cookie> cookies, String userAgent, LettProxy lettProxy,
      int attempt) {
    return fetchCookies(session, url, cookies, userAgent, lettProxy, attempt, null);
  }

  /**
   * Fetch a page By default the redirects are enabled in the RequestConfig
   * 
   * @param session
   * @param url
   * @param cookieName
   * @param cookies
   * @param user agent
   * @param attempt
   * @param headers
   * @return the header value. Will return an empty string if the cookie wasn't found.
   */
  public static Map<String, String> fetchCookies(Session session, String url, List<Cookie> cookies, String userAgent, LettProxy lettProxy,
      int attempt, Map<String, String> headers) {

    LettProxy randProxy = null;
    String randUserAgent = null;
    CloseableHttpResponse closeableHttpResponse = null;
    int responseLength = 0;
    String requestHash = generateRequestHash(session);

    try {
      Logging.printLogDebug(logger, session, "Performing GET request to fetch cookie: " + url);

      if (DataFetcher.mustUseFetcher(attempt, session)) {
        if (headers == null) {
          headers = new HashMap<>();
        }

        if (cookies != null && !cookies.isEmpty()) {
          StringBuilder cookiesHeader = new StringBuilder();

          for (Cookie c : cookies) {
            cookiesHeader.append(c.getName() + "=" + c.getValue() + ";");
          }

          headers.put("Cookie", cookiesHeader.toString());
        }

        JSONObject payload = POSTFetcher.fetcherPayloadBuilder(url, "GET", true, null, headers, new ArrayList<>(), null);
        JSONObject response = POSTFetcher.requestWithFetcher(session, payload, true);

        if (response.has("response")) {
          DataFetcher.setRequestProxyForFetcher(session, response, url);
          session.addRedirection(url, response.getJSONObject("response").getString("redirect_url"));

          String content = response.getJSONObject("response").getString("body");
          S3Service.uploadCrawlerSessionContentToAmazon(session, requestHash, content);

          if (response.has("request_status_code")) {
            int responseCode = response.getInt("request_status_code");
            if (Integer.toString(responseCode).charAt(0) != '2' && Integer.toString(responseCode).charAt(0) != '3' && responseCode != 404) { // errors
              throw new ResponseCodeException(responseCode);
            }
          }

          Map<String, String> cookiesMap = new HashMap<>();

          if (response.has("headers")) {
            JSONObject headersJson = response.getJSONObject("headers");

            if (headersJson.has(HTTP_COOKIE_HEADER.toLowerCase())) {
              JSONArray cookiesArray = headersJson.getJSONArray(HTTP_COOKIE_HEADER.toLowerCase());

              for (Object o : cookiesArray) {
                String cookieHeader = o.toString();
                String cookieName = cookieHeader.split("=")[0].trim();

                int x = cookieHeader.indexOf(cookieName + "=") + cookieName.length() + 1;
                int y = cookieHeader.indexOf(';', x);

                String cookieValue = cookieHeader.substring(x, y).trim();

                cookiesMap.put(cookieName, cookieValue);
              }
            }
          }

          return cookiesMap;
        } else {
          Logging.printLogWarn(logger, session, "Fetcher did not returned the expected response.");
          throw new ResponseCodeException(500);
        }
      }

      randUserAgent = userAgent == null ? randUserAgent() : userAgent;
      randProxy = lettProxy != null ? lettProxy : randLettProxy(attempt, session, session.getMarket().getProxies(), url);

      CookieStore cookieStore = createCookieStore(cookies);

      CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

      if (randProxy != null) {
        session.addRequestProxy(url, randProxy);
        if (randProxy.getUser() != null) {
          credentialsProvider.setCredentials(new AuthScope(randProxy.getAddress(), randProxy.getPort()),
              new UsernamePasswordCredentials(randProxy.getUser(), randProxy.getPass()));
        }
      }

      HttpHost proxy = null;
      if (randProxy != null) {
        proxy = new HttpHost(randProxy.getAddress(), randProxy.getPort());
      }

      RequestConfig requestConfig = createRequestConfig(proxy);

      List<Header> reqHeaders = new ArrayList<>();

      if (headers != null) {
        for (Map.Entry<String, String> header : headers.entrySet()) {
          reqHeaders.add(new BasicHeader(header.getKey(), header.getValue()));
        }
      }

      reqHeaders.add(new BasicHeader(HttpHeaders.CONTENT_ENCODING, "compress, gzip"));

      // http://www.nakov.com/blog/2009/07/16/disable-certificate-validation-in-java-ssl-connections/
      // on July 23, the comper site expired the ssl certificate, with that I had to ignore ssl
      // verification to happen the capture
      HostnameVerifier hostNameVerifier = new HostNameVerifier();
      if (session.getMarket().getNumber() == 115) {
        hostNameVerifier = new HostnameVerifier() {
          public boolean verify(String hostname, SSLSession session) {
            return true;
          }
        };
      }

      CloseableHttpClient httpclient = HttpClients.custom().setDefaultCookieStore(cookieStore).setUserAgent(randUserAgent)
          .setDefaultRequestConfig(requestConfig).setDefaultCredentialsProvider(credentialsProvider).setDefaultHeaders(reqHeaders)
          .setSSLSocketFactory(DataFetcher.createSSLConnectionSocketFactory()).setSSLHostnameVerifier(hostNameVerifier).build();

      HttpContext localContext = new BasicHttpContext();
      localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

      HttpGet httpGet = new HttpGet(url);
      httpGet.setConfig(requestConfig);

      // do request
      closeableHttpResponse = httpclient.execute(httpGet, localContext);

      // analysing the status code
      // if there was some response code that indicates forbidden access or server error we want to
      // try again
      int responseCode = closeableHttpResponse.getStatusLine().getStatusCode();
      if (Integer.toString(responseCode).charAt(0) != '2' && Integer.toString(responseCode).charAt(0) != '3' && responseCode != 404) { // errors
        throw new ResponseCodeException(responseCode);
      }

      // creating the page content result from the http request
      PageContent pageContent = new PageContent(closeableHttpResponse.getEntity());
      pageContent.setStatusCode(closeableHttpResponse.getStatusLine().getStatusCode());
      pageContent.setUrl(url); // setting url

      responseLength = pageContent.getContentData().length;

      // assembling request information log message
      sendRequestInfoLog(url, GET_REQUEST, randProxy, randUserAgent, session, closeableHttpResponse, responseLength, requestHash);

      // saving request content result on Amazon
      String content = "";
      if (pageContent.getContentCharset() == null) {
        content = new String(pageContent.getContentData());
      } else {
        content = new String(pageContent.getContentData(), pageContent.getContentCharset());
      }
      S3Service.uploadCrawlerSessionContentToAmazon(session, requestHash, content);

      // see if some code error occured
      // sometimes the remote server doesn't send the http error code on the headers
      // but rater on the page bytes
      content = content.trim();
      for (String errorCode : errorCodes) {
        if (content.equals(errorCode)) {
          throw new ResponseCodeException(Integer.parseInt(errorCode));
        }
      }

      Map<String, String> cookiesMap = new HashMap<>();

      // get all cookie headers
      Header[] cookieHeaders = closeableHttpResponse.getHeaders(HTTP_COOKIE_HEADER);

      for (Header header : cookieHeaders) {
        String cookieHeader = header.getValue();
        String cookieName = cookieHeader.split("=")[0].trim();

        int x = cookieHeader.indexOf(cookieName + "=") + cookieName.length() + 1;
        int y = cookieHeader.indexOf(';', x);

        String cookieValue = cookieHeader.substring(x, y).trim();

        cookiesMap.put(cookieName, cookieValue);
      }

      return cookiesMap;


    } catch (Exception e) {
      sendRequestInfoLog(url, GET_REQUEST, randProxy, randUserAgent, session, closeableHttpResponse, responseLength, requestHash);

      Logging.printLogWarn(logger, session, "Attempt " + attempt + " -> Error performing GET request for header: " + url);
      Logging.printLogWarn(logger, session, e.getMessage());

      if (attempt >= session.getMaxConnectionAttemptsCrawler()) {
        Logging.printLogWarn(logger, session, "Reached maximum attempts for URL [" + url + "]");
        return new HashMap<>();
      } else {
        return fetchCookies(session, url, cookies, attempt + 1);
      }

    }
  }

  /**
   * Fetch a page By default the redirects are enabled in the RequestConfig
   * 
   * @param session
   * @param url
   * @param cookieName
   * @param cookies
   * @param attempt
   * @return the header value. Will return an empty string if the cookie wasn't found.
   */
  public static String fetchCookie(Session session, String url, String cookieName, List<Cookie> cookies, int attempt) {

    LettProxy randProxy = null;
    String randUserAgent = null;
    CloseableHttpResponse closeableHttpResponse = null;
    int responseLength = 0;
    String requestHash = generateRequestHash(session);

    try {
      Logging.printLogDebug(logger, session, "Performing GET request to fetch cookie: " + url);

      randUserAgent = randUserAgent();
      randProxy = randLettProxy(attempt, session, session.getMarket().getProxies(), url);

      CookieStore cookieStore = createCookieStore(cookies);

      CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

      if (randProxy != null) {
        session.addRequestProxy(url, randProxy);
        if (randProxy.getUser() != null) {
          credentialsProvider.setCredentials(new AuthScope(randProxy.getAddress(), randProxy.getPort()),
              new UsernamePasswordCredentials(randProxy.getUser(), randProxy.getPass()));
        }
      }

      HttpHost proxy = null;
      if (randProxy != null) {
        proxy = new HttpHost(randProxy.getAddress(), randProxy.getPort());
      }

      RequestConfig requestConfig = null;
      if (proxy != null) {
        requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).setRedirectsEnabled(true)
            .setConnectionRequestTimeout(DEFAULT_CONNECTION_REQUEST_TIMEOUT).setConnectTimeout(DEFAULT_CONNECT_TIMEOUT)
            .setSocketTimeout(DEFAULT_SOCKET_TIMEOUT).setProxy(proxy).build();
      } else {
        requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).setRedirectsEnabled(true)
            .setConnectionRequestTimeout(DEFAULT_CONNECTION_REQUEST_TIMEOUT).setConnectTimeout(DEFAULT_CONNECT_TIMEOUT)
            .setSocketTimeout(DEFAULT_SOCKET_TIMEOUT).build();
      }


      List<Header> reqHeaders = new ArrayList<>();
      reqHeaders.add(new BasicHeader(HttpHeaders.CONTENT_ENCODING, CONTENT_ENCODING));

      CloseableHttpClient httpclient = HttpClients.custom().setDefaultCookieStore(cookieStore).setUserAgent(randUserAgent)
          .setDefaultRequestConfig(requestConfig).setDefaultCredentialsProvider(credentialsProvider).setDefaultHeaders(reqHeaders).build();

      HttpContext localContext = new BasicHttpContext();
      localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

      HttpGet httpGet = new HttpGet(url);
      httpGet.setConfig(requestConfig);

      // do request
      closeableHttpResponse = httpclient.execute(httpGet, localContext);

      // analysing the status code
      // if there was some response code that indicates forbidden access or server error we want to
      // try again
      int responseCode = closeableHttpResponse.getStatusLine().getStatusCode();
      if (Integer.toString(responseCode).charAt(0) != '2' && Integer.toString(responseCode).charAt(0) != '3' && responseCode != 404) { // errors
        throw new ResponseCodeException(responseCode);
      }

      // creating the page content result from the http request
      PageContent pageContent = new PageContent(closeableHttpResponse.getEntity());
      pageContent.setStatusCode(closeableHttpResponse.getStatusLine().getStatusCode());
      pageContent.setUrl(url); // setting url

      responseLength = pageContent.getContentData().length;

      // assembling request information log message
      sendRequestInfoLog(url, GET_REQUEST, randProxy, randUserAgent, session, closeableHttpResponse, responseLength, requestHash);

      // saving request content result on Amazon
      String content = "";
      if (pageContent.getContentCharset() == null) {
        content = new String(pageContent.getContentData());
      } else {
        content = new String(pageContent.getContentData(), pageContent.getContentCharset());
      }
      S3Service.uploadCrawlerSessionContentToAmazon(session, requestHash, content);

      // see if some code error occured
      // sometimes the remote server doesn't send the http error code on the headers
      // but rater on the page bytes
      content = content.trim();
      for (String errorCode : errorCodes) {
        if (content.equals(errorCode)) {
          throw new ResponseCodeException(Integer.parseInt(errorCode));
        }
      }

      // get all cookie headers
      Header[] headers = closeableHttpResponse.getHeaders(HTTP_COOKIE_HEADER);

      // get the desired value
      for (Header header : headers) {
        if (header.getValue().contains(cookieName)) {
          int beginIndex = header.getValue().indexOf(cookieName);
          int endIndex = header.getValue().indexOf(';');
          String desiredCookie = header.getValue().substring(beginIndex, endIndex);

          // split the desired cookie to get the value that comes next to '='
          return splitHeaderValue(desiredCookie);
        }
      }

      return "";

    } catch (Exception e) {
      sendRequestInfoLog(url, GET_REQUEST, randProxy, randUserAgent, session, closeableHttpResponse, responseLength, requestHash);

      Logging.printLogWarn(logger, session, "Attempt " + attempt + " -> Error performing GET request for header: " + url);
      Logging.printLogWarn(logger, session, e.getMessage());

      if (attempt >= session.getMaxConnectionAttemptsCrawler()) {
        Logging.printLogWarn(logger, session, "Reached maximum attempts for URL [" + url + "]");
        return "";
      } else {
        return fetchCookie(session, url, cookieName, cookies, attempt + 1);
      }

    }
  }

  /**
   * This function do a get request with HttpURLConnection library using storm proxies
   * 
   * Attempts: 4;
   * 
   * Last attempt will use no proxy
   * 
   * @param url
   * @param headers
   * @param session
   * @param attempt
   * @return
   */
  public static String fetchPageWithHttpURLConnectionUsingStormProxies(String targetURL, Map<String, String> headers, Session session, int attempt) {
    try {
      Logging.printLogDebug(logger, session, "Performing GET request with HttpURLConnection: " + targetURL);
      List<LettProxy> proxyStorm = GlobalConfigurations.proxies.getProxy(ProxyCollection.STORM_RESIDENTIAL_US);

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
      connection.setRequestMethod(DataFetcher.GET_REQUEST);
      connection.setInstanceFollowRedirects(true);
      connection.setUseCaches(false);
      connection.setReadTimeout(DEFAULT_CONNECT_TIMEOUT * 2);

      for (Entry<String, String> entry : headers.entrySet()) {
        connection.setRequestProperty(entry.getKey(), entry.getValue());
      }

      // Get Response
      InputStream is = connection.getInputStream();
      BufferedReader rd = new BufferedReader(new InputStreamReader(is));
      StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
      String line;
      while ((line = rd.readLine()) != null) {
        response.append(line);
        response.append('\r');
      }
      rd.close();
      content = response.toString();

      String requestHash = generateRequestHash(session);
      S3Service.uploadCrawlerSessionContentToAmazon(session, requestHash, content);

      return content;
    } catch (Exception e) {
      Logging.printLogWarn(logger, session, "Attempt " + attempt + " -> Error performing GET request for header: " + targetURL);
      Logging.printLogWarn(logger, session, e.getMessage());

      if (attempt < 4) {
        return fetchPageWithHttpURLConnectionUsingStormProxies(targetURL, headers, session, attempt + 1);
      } else {
        Logging.printLogWarn(logger, session, "Reached maximum attempts for URL [" + targetURL + "]");
        return "";
      }
    }
  }

  /**
   * Only request a url(this is specified for api of urlbox)
   * 
   * @param session
   * @param url
   * @param cookies
   * @param attempt
   * @return
   */
  public static void fetchPageAPIUrlBox(String url, Session session) {
    try {
      POSTFetcher.requestWithFetcher(session, POSTFetcher.fetcherPayloadBuilder(url, GET_REQUEST, false, null, null, null), 1000, false);

    } catch (SocketTimeoutException e) {
      // do nothing
    } catch (Exception e) {
      Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
    }
  }

  /**
   * 
   * @param url
   * @param requestType
   * @param proxy
   * @param session
   * @param responseCode
   * @return
   */
  public static void sendRequestInfoLog(String url, String requestType, LettProxy proxy, String userAgent, Session session,
      CloseableHttpResponse response, String requestHash) {

    JSONObject requestMetadata = new JSONObject();

    requestMetadata.put("req_hash", requestHash);
    requestMetadata.put("proxy_name", (proxy == null ? ProxyCollection.NO_PROXY : proxy.getSource()));
    requestMetadata.put("proxy_ip", (proxy == null ? MDC.get("HOST_NAME") : proxy.getAddress()));
    requestMetadata.put("user_agent", userAgent);
    requestMetadata.put("req_method", requestType);
    requestMetadata.put("req_location", url);
    requestMetadata.put("res_http_code", (response == null) ? 0 : response.getStatusLine().getStatusCode());
    requestMetadata.put("res_length", (response == null) ? 0 : response.getEntity().getContentLength());

    Logging.logDebug(logger, session, requestMetadata, "Registrando requisição...");

  }

  public static void sendRequestInfoLog(String url, String requestType, LettProxy proxy, String userAgent, Session session,
      CloseableHttpResponse response, Integer responseLength, String requestHash) {

    JSONObject requestMetadata = new JSONObject();

    requestMetadata.put("req_hash", requestHash);
    requestMetadata.put("proxy_name", (proxy == null ? ProxyCollection.NO_PROXY : proxy.getSource()));
    requestMetadata.put("proxy_ip", (proxy == null ? MDC.get("HOST_NAME") : proxy.getAddress()));
    requestMetadata.put("user_agent", userAgent);
    requestMetadata.put("req_method", requestType);
    requestMetadata.put("req_location", url);
    requestMetadata.put("res_http_code", (response == null) ? 0 : response.getStatusLine().getStatusCode());
    requestMetadata.put("res_length", responseLength);

    Logging.logDebug(logger, session, requestMetadata, "Registrando requisição...");

  }

  public static void sendRequestInfoLogWebdriver(String url, String requestType, LettProxy proxy, String userAgent, Session session,
      String requestHash) {

    JSONObject requestMetadata = new JSONObject();

    requestMetadata.put("req_hash", requestHash);
    requestMetadata.put("proxy_name", (proxy == null ? ProxyCollection.NO_PROXY : proxy.getSource()));
    requestMetadata.put("proxy_ip", (proxy == null ? MDC.get("HOST_NAME") : proxy.getAddress()));
    requestMetadata.put("user_agent", userAgent);
    requestMetadata.put("req_method", requestType);
    requestMetadata.put("req_location", url);

    Logging.logDebug(logger, session, requestMetadata, "Registrando requisição...");

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

  /**
   * 
   * @param attempt
   * @param session
   * @param localFileDir
   * @return
   */
  private static File downloadImageFromMarket(int attempt, Session session) {
    File localFile = null;
    LettProxy randProxy = null;
    String randUserAgent = null;
    CloseableHttpResponse closeableHttpResponse = null;
    int responseLength = 0;
    String requestHash = generateRequestHash(session);

    try {

      // choosing the preferred proxy service
      randUserAgent = randUserAgent();
      if (session instanceof ImageCrawlerSession) {
        randProxy = randLettProxy(attempt, session, session.getMarket().getImageProxies(), session.getOriginalURL());
      } else {
        randProxy = randLettProxy(attempt, session, session.getMarket().getProxies(), session.getOriginalURL());
      }

      CookieStore cookieStore = new BasicCookieStore();

      CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

      if (randProxy != null) {
        if (randProxy.getUser() != null) {
          credentialsProvider.setCredentials(new AuthScope(randProxy.getAddress(), randProxy.getPort()),
              new UsernamePasswordCredentials(randProxy.getUser(), randProxy.getPass()));
        }
      }

      HttpHost proxy = null;
      if (randProxy != null) {
        proxy = new HttpHost(randProxy.getAddress(), randProxy.getPort());
      }

      RequestConfig requestConfig = createRequestConfig(proxy);

      List<Header> headers = getHeadersFromMarket(session.getMarket().getNumber());

      CloseableHttpClient httpclient = HttpClients.custom().setDefaultCookieStore(cookieStore).setUserAgent(randUserAgent)
          .setDefaultRequestConfig(requestConfig).setDefaultHeaders(headers).setDefaultCredentialsProvider(credentialsProvider).build();

      HttpContext localContext = new BasicHttpContext();
      localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

      HttpGet httpGet = new HttpGet(session.getOriginalURL());
      httpGet.setConfig(requestConfig);

      // do request
      closeableHttpResponse = httpclient.execute(httpGet, localContext);

      // analysing the status code
      // if there was some response code that indicates forbidden access or server error we want to
      // try again
      int responseCode = closeableHttpResponse.getStatusLine().getStatusCode();
      if (Integer.toString(responseCode).charAt(0) != '2' && Integer.toString(responseCode).charAt(0) != '3' && responseCode != 404) { // errors
        throw new ResponseCodeException(responseCode);
      }

      localFile = new File(((ImageCrawlerSession) session).getLocalOriginalFileDir());

      // get image bytes
      BufferedInputStream is = new BufferedInputStream(closeableHttpResponse.getEntity().getContent());
      BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(localFile));
      byte[] b = new byte[8 * 1024]; // reading each 8kb
      int read = 0;
      while ((read = is.read(b)) > -1) {
        responseLength += read;
        bout.write(b, 0, read);
      }
      bout.flush();
      bout.close();
      is.close();

      // assembling request information log message
      sendRequestInfoLog(session.getOriginalURL(), GET_REQUEST, randProxy, randUserAgent, session, closeableHttpResponse, responseLength,
          requestHash);

      return localFile;

    } catch (Exception e) {

      sendRequestInfoLog(session.getOriginalURL(), GET_REQUEST, randProxy, randUserAgent, session, closeableHttpResponse, responseLength,
          requestHash);

      if (localFile != null && localFile.exists()) {
        localFile.delete();
      }

      if (e instanceof ResponseCodeException) {
        Logging.printLogWarn(logger, session,
            "Attempt " + attempt + " -> Error performing GET request for image download: " + session.getOriginalURL());
        Logging.printLogWarn(logger, session, e.getMessage());
      } else {
        Logging.printLogWarn(logger, session,
            "Attempt " + attempt + " -> Error performing GET request for image download: " + session.getOriginalURL());
        Logging.printLogWarn(logger, session, e.getMessage());
      }

      if (attempt >= session.getMaxConnectionAttemptsCrawler()) {
        Logging.printLogWarn(logger, session, "Reached maximum attempts for URL [" + session.getOriginalURL() + "]");
        return null;
      } else {
        return downloadImageFromMarket(attempt + 1, session);
      }
    }

  }

  private static List<Header> getHeadersFromMarket(Integer marketId) {
    List<Header> headers = new ArrayList<>();

    if (marketId == 63 || marketId == 62 || marketId == 73) {
      headers.add(new BasicHeader(HttpHeaders.CONNECTION, "keep-alive"));
      headers.add(new BasicHeader(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8"));
      headers.add(new BasicHeader(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate, br"));
      headers.add(new BasicHeader(HttpHeaders.ACCEPT_LANGUAGE, "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6"));

      switch (marketId) {
        case 63:
          headers.add(new BasicHeader(HttpHeaders.HOST, "www.pontofrio-imagens.com.br"));
          break;
        case 62:
          headers.add(new BasicHeader(HttpHeaders.HOST, "www.casasbahia-imagens.com.br"));
          break;
        case 73:
          headers.add(new BasicHeader(HttpHeaders.HOST, "www.extra-imagens.com.br"));
          break;
        default:
          break;
      }
    } else if (marketId != 307) {
      headers.add(new BasicHeader(HttpHeaders.CONTENT_ENCODING, CONTENT_ENCODING));
    } else if (marketId == 307) {
      headers.add(new BasicHeader(HttpHeaders.ACCEPT, "image/jpg, image/apng"));
    }

    return headers;
  }

  /**
   * 
   * @param attempt
   * @param session
   * @param proxyServices
   * @return
   */
  public static LettProxy randLettProxy(int attempt, Session session, List<String> proxyServices, String url) {
    LettProxy nextProxy = getNextProxy(session, attempt, proxyServices);
    session.addRequestProxy(url, nextProxy);

    return nextProxy;
  }

  /**
   * 
   * @param serviceName
   * @param session
   * @return
   */
  public static LettProxy getNextProxy(Session session, int attempt, List<String> proxyServices) {
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
      serviceName = getProxyService(attemptTemp, session, proxyServices);

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
   * Retrieve a random user agent from the user agents array.
   * 
   * @return
   */
  public static String randUserAgent() {
    return userAgents.get(MathUtils.randInt(0, userAgents.size() - 1));
  }

  /**
   * Retrieve a random user agent from the user agents array.
   * 
   * @return
   */
  public static String randMobileUserAgent() {
    return mobileUserAgents.get(MathUtils.randInt(0, mobileUserAgents.size() - 1));
  }

  /**
   * Select a proxy service according to the number of attempt.
   * 
   * @param attempt
   * @param session
   * @param proxyServices
   * @return
   */
  public static String getProxyService(int attempt, Session session, List<String> proxyServices) {
    String service = null;

    Logging.printLogDebug(logger, session, "Selecting a proxy service...connection attempt " + attempt);

    if (session instanceof ImageCrawlerSession) {
      service = GlobalConfigurations.proxies.selectProxy(session.getMarket(), false, attempt);
    } else {
      service = GlobalConfigurations.proxies.selectProxy(session.getMarket(), true, attempt);
    }

    Logging.printLogDebug(logger, session, "Selected proxy: " + service);

    return service;
  }

  /**
   * Splits a cookie value and returns the second part. e.g:
   * 
   * ASP.NET_SessionId=vh2akqijsv0aqzbmn5qxxfbt; first part: ASP.NET_SessionId second part:
   * vh2akqijsv0aqzbmn5qxxfbt
   * 
   * @param headerValue
   */
  public static String splitHeaderValue(String headerValue) {
    int beginIndex = headerValue.indexOf('=') + 1;
    return headerValue.substring(beginIndex, headerValue.length()).trim();
  }

  public static String generateRequestHash(Session session) {
    String s = session.getSessionId() + new DateTime(DateUtils.timeZone).toString("yyyy-MM-dd HH:mm:ss.SSS");
    return DigestUtils.md5Hex(s);
  }

  /**
   * Set request proxy for requests in fetcher
   * 
   * @param session
   * @param response
   * @param url
   */
  public static void setRequestProxyForFetcher(Session session, JSONObject response, String url) {
    if (response.has("statistics")) {
      JSONObject statistics = response.getJSONObject("statistics");

      if (statistics.has("requests")) {
        JSONArray requests = statistics.getJSONArray("requests");

        if (requests.length() > 0) {
          JSONObject succesRequest = requests.getJSONObject(requests.length() - 1);

          if (succesRequest.has("proxy")) {
            JSONObject proxyObj = succesRequest.getJSONObject("proxy");

            if (proxyObj.length() > 0 && proxyObj.has("source")) {
              String source = proxyObj.getString("source");
              List<LettProxy> proxyList = GlobalConfigurations.proxies.getProxy(source);

              String user = null;
              String pass = null;

              if (!proxyList.isEmpty()) {
                LettProxy tempProxy = proxyList.get(0);

                user = tempProxy.getUser();
                pass = tempProxy.getPass();
              }

              LettProxy lettProxy = new LettProxy(source, proxyObj.has("host") ? proxyObj.getString("host") : null,
                  proxyObj.has("port") ? proxyObj.getInt("port") : null, proxyObj.has("location") ? proxyObj.getString("location") : null, user,
                  pass);

              session.addRequestProxy(url, lettProxy);
            }
          }
        }
      }
    }
  }

  /**
   * Determine if the request will be use fetcher api
   * 
   * @param attempt
   * @return boolean
   */
  public static boolean mustUseFetcher(int attempt, Session session) {
    // ZoneId utc = ZoneId.of("America/Sao_Paulo");
    // ZonedDateTime zonedDate = ZonedDateTime.now(utc);
    // int nowHour = zonedDate.getHour();

    // Request via fetcher on first attempt
    // return (attempt == 1 && (nowHour % 4 == 0 && nowHour != 20) &&
    // Main.executionParameters.getUseFetcher());

    boolean mustUseFetcher = attempt == 1 && GlobalConfigurations.executionParameters.getUseFetcher();

    if (mustUseFetcher && attempt == 1) {
      session.setMaxConnectionAttemptsCrawler(2);
    }

    return mustUseFetcher;
  }
}
