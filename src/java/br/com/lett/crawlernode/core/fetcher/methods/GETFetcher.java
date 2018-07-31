package br.com.lett.crawlernode.core.fetcher.methods;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
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
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.lett.crawlernode.aws.s3.S3Service;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.DataFetcherRedirectStrategy;
import br.com.lett.crawlernode.core.fetcher.HostNameVerifier;
import br.com.lett.crawlernode.core.fetcher.LettProxy;
import br.com.lett.crawlernode.core.fetcher.PageContent;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.exceptions.ResponseCodeException;
import br.com.lett.crawlernode.util.Logging;

public class GETFetcher {

  protected static final Logger logger = LoggerFactory.getLogger(GETFetcher.class);

  private GETFetcher() {
    super();
  }

  /**
   * Fetch a page By default the redirects are enabled in the RequestConfig
   * 
   * @param session
   * @param url
   * @param cookies
   * @param userAgent
   * @param attempt
   * @return
   */
  public static String fetchPageGET(Session session, String url, List<Cookie> cookies, String userAgent, int attempt) {
    return fetchPageGET(session, url, cookies, userAgent, null, attempt);
  }

  /**
   * Fetch a page By default the redirects are enabled in the RequestConfig
   * 
   * @param session
   * @param url
   * @param cookies
   * @param lettProxy
   * @param attempt
   * @return
   */
  public static String fetchPageGET(Session session, String url, List<Cookie> cookies, LettProxy proxy, int attempt) {
    return fetchPageGET(session, url, cookies, DataFetcher.randUserAgent(), proxy, attempt);
  }

  /**
   * Fetch a page By default the redirects are enabled in the RequestConfig
   * 
   * @param session
   * @param url
   * @param cookies
   * @param attempt
   * @return
   */
  public static String fetchPageGET(Session session, String url, List<Cookie> cookies, int attempt) {
    return fetchPageGET(session, url, cookies, DataFetcher.randUserAgent(), null, attempt);
  }

  /**
   * Fetch a page By default the redirects are enabled in the RequestConfig
   * 
   * @param session
   * @param url
   * @param cookies
   * @param userAgent
   * @param lettProxy
   * @param attempt
   * @return
   */
  public static String fetchPageGET(Session session, String url, List<Cookie> cookies, String userAgent, LettProxy lettProxy, int attempt) {
    Logging.printLogDebug(logger, session, getLoggingMessage("GET", url));
    LettProxy randProxy = null;
    CloseableHttpResponse closeableHttpResponse = null;
    int responseLength = 0;
    String requestHash = DataFetcher.generateRequestHash(session);

    try {

      // Request via fetcher on first attempt
      if (DataFetcher.mustUseFetcher(attempt, session)) {
        Map<String, String> headers = new HashMap<>();

        if (cookies != null && !cookies.isEmpty()) {
          StringBuilder cookiesHeader = new StringBuilder();

          for (Cookie c : cookies) {
            cookiesHeader.append(c.getName() + "=" + c.getValue() + ";");
          }

          headers.put("Cookie", cookiesHeader.toString());
        }

        if (userAgent != null) {
          headers.put("User-Agent", userAgent);
        }

        JSONObject payload = POSTFetcher.fetcherPayloadBuilder(url, DataFetcher.GET_REQUEST, true, null, headers, lettProxy);
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

          return content;
        } else {
          Logging.printLogError(logger, session, "Fetcher did not returned the expected response.");
          throw new ResponseCodeException(500);
        }
      }

      randProxy = lettProxy != null ? lettProxy : DataFetcher.randLettProxy(attempt, session, session.getMarket().getProxies(), url);

      CookieStore cookieStore = DataFetcher.createCookieStore(cookies);

      CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

      if (randProxy != null) {
        Logging.printLogDebug(logger, session, "Using " + randProxy.getSource() + "(proxy) for this request.");
        session.addRequestProxy(url, randProxy);
        if (randProxy.getUser() != null) {
          credentialsProvider.setCredentials(new AuthScope(randProxy.getAddress(), randProxy.getPort()),
              new UsernamePasswordCredentials(randProxy.getUser(), randProxy.getPass()));
        }
      } else {
        Logging.printLogWarn(logger, session, "Using no proxy for this request.");
      }

      HttpHost proxy = null;
      if (randProxy != null) {
        proxy = new HttpHost(randProxy.getAddress(), randProxy.getPort());
      }

      RequestConfig requestConfig = null;
      if (proxy != null) {

        if (session.getMarket().getName() != null && DataFetcher.highTimeoutMarkets.contains(session.getMarket().getName())) {
          requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).setRedirectsEnabled(true) // set // true
              .setConnectionRequestTimeout(DataFetcher.THIRTY_SECONDS_TIMEOUT).setConnectTimeout(DataFetcher.THIRTY_SECONDS_TIMEOUT)
              .setSocketTimeout(DataFetcher.THIRTY_SECONDS_TIMEOUT).setProxy(proxy).build();
        } else {
          requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).setRedirectsEnabled(true) // set // true
              .setConnectionRequestTimeout(DataFetcher.DEFAULT_CONNECTION_REQUEST_TIMEOUT).setConnectTimeout(DataFetcher.DEFAULT_CONNECT_TIMEOUT)
              .setSocketTimeout(DataFetcher.DEFAULT_SOCKET_TIMEOUT).setProxy(proxy).build();
        }

      } else {
        if (session.getMarket().getName() != null && DataFetcher.highTimeoutMarkets.contains(session.getMarket().getName())) {
          requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).setRedirectsEnabled(true) // set // true
              .setConnectionRequestTimeout(DataFetcher.THIRTY_SECONDS_TIMEOUT).setConnectTimeout(DataFetcher.THIRTY_SECONDS_TIMEOUT)
              .setSocketTimeout(DataFetcher.THIRTY_SECONDS_TIMEOUT).build();
        } else {
          requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).setRedirectsEnabled(true) // set // true
              .setConnectionRequestTimeout(DataFetcher.DEFAULT_CONNECTION_REQUEST_TIMEOUT).setConnectTimeout(DataFetcher.DEFAULT_CONNECT_TIMEOUT)
              .setSocketTimeout(DataFetcher.DEFAULT_SOCKET_TIMEOUT).build();
        }
      }

      // creating the redirect strategy
      // so we can get the final redirected URL
      DataFetcherRedirectStrategy redirectStrategy = new DataFetcherRedirectStrategy();

      List<Header> headers = new ArrayList<>();
      headers.add(new BasicHeader(HttpHeaders.CONTENT_ENCODING, DataFetcher.CONTENT_ENCODING));

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

      CloseableHttpClient httpclient =
          HttpClients.custom().setDefaultCookieStore(cookieStore).setUserAgent(userAgent).setDefaultRequestConfig(requestConfig)
              .setRedirectStrategy(redirectStrategy).setDefaultCredentialsProvider(credentialsProvider).setDefaultHeaders(headers)
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
      PageContent pageContent = new PageContent(closeableHttpResponse.getEntity()); // loading
      pageContent.setStatusCode(closeableHttpResponse.getStatusLine().getStatusCode()); // geting
      pageContent.setUrl(url); // setting url

      responseLength = pageContent.getContentData().length;

      // assembling request information log message
      DataFetcher.sendRequestInfoLog(url, DataFetcher.GET_REQUEST, randProxy, userAgent, session, closeableHttpResponse, responseLength, requestHash);

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
      for (String errorCode : DataFetcher.errorCodes) {
        if (content.equals(errorCode)) {
          throw new ResponseCodeException(Integer.parseInt(errorCode));
        }
      }

      // record the redirected URL on the session
      if (redirectStrategy.getFinalURL() != null && !redirectStrategy.getFinalURL().isEmpty()) {
        session.addRedirection(url, redirectStrategy.getFinalURL());
      }

      // process response and parse
      return DataFetcher.processContent(pageContent, session);

    } catch (Exception e) {
      DataFetcher.sendRequestInfoLog(url, DataFetcher.GET_REQUEST, randProxy, userAgent, session, closeableHttpResponse, responseLength, requestHash);

      Logging.printLogError(logger, session, "Error performing GET request [url: " + session.getOriginalURL() + " , attempt: " + attempt + "]");
      Logging.printLogError(logger, session, e.getMessage());

      if (attempt >= session.getMaxConnectionAttemptsCrawler()) {
        Logging.printLogError(logger, session, "Reached maximum attempts for URL [" + url + "]");
        return "";
      } else {
        return fetchPageGET(session, url, cookies, userAgent, lettProxy, attempt + 1);
      }

    }
  }

  public static String fetchPageGETWithHeaders(Session session, String url, List<Cookie> cookies, Map<String, String> headers, int attempt) {
    Logging.printLogDebug(logger, session, getLoggingMessage("GET", url));

    LettProxy randProxy = null;
    String randUserAgent = null;
    CloseableHttpResponse closeableHttpResponse = null;
    int responseLength = 0;
    String requestHash = DataFetcher.generateRequestHash(session);

    try {


      // Request via fetcher on first attempt
      if (DataFetcher.mustUseFetcher(attempt, session)) {
        if (cookies != null && !cookies.isEmpty()) {
          StringBuilder cookiesHeader = new StringBuilder();

          for (Cookie c : cookies) {
            cookiesHeader.append(c.getName() + "=" + c.getValue() + ";");
          }

          headers.put("Cookie", cookiesHeader.toString());
        }

        JSONObject payload = POSTFetcher.fetcherPayloadBuilder(url, "GET", true, null, headers, null);
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

          return content;
        } else {
          Logging.printLogError(logger, session, "Fetcher did not returned the expected response.");
          throw new ResponseCodeException(500);
        }
      }

      randUserAgent = headers.containsKey("User-Agent") ? headers.get("User-Agent") : DataFetcher.randUserAgent();
      randProxy = DataFetcher.randLettProxy(attempt, session, session.getMarket().getProxies(), url);

      CookieStore cookieStore = DataFetcher.createCookieStore(cookies);

      CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

      if (randProxy != null) {
        Logging.printLogDebug(logger, session, "Using " + randProxy.getSource() + "(proxy) for this request.");
        session.addRequestProxy(url, randProxy);
        if (randProxy.getUser() != null) {
          credentialsProvider.setCredentials(new AuthScope(randProxy.getAddress(), randProxy.getPort()),
              new UsernamePasswordCredentials(randProxy.getUser(), randProxy.getPass()));
        }
      } else {
        Logging.printLogWarn(logger, session, "Using no proxy for this request.");
      }

      HttpHost proxy = null;
      if (randProxy != null) {
        proxy = new HttpHost(randProxy.getAddress(), randProxy.getPort());
      }

      RequestConfig requestConfig = null;
      if (proxy != null) {

        if (session.getMarket().getName() != null && DataFetcher.highTimeoutMarkets.contains(session.getMarket().getName())) {
          requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).setRedirectsEnabled(true) // set
              .setConnectionRequestTimeout(5000).setConnectTimeout(5000).setSocketTimeout(5000).setProxy(proxy).build();
        } else {
          requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).setRedirectsEnabled(true) // set
              .setConnectionRequestTimeout(DataFetcher.DEFAULT_CONNECTION_REQUEST_TIMEOUT).setConnectTimeout(DataFetcher.DEFAULT_CONNECT_TIMEOUT)
              .setSocketTimeout(DataFetcher.DEFAULT_SOCKET_TIMEOUT).setProxy(proxy).build();
        }

      } else {

        if (session.getMarket().getName() != null && DataFetcher.highTimeoutMarkets.contains(session.getMarket().getName())) {
          requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).setRedirectsEnabled(true) // set
              .setConnectionRequestTimeout(5000).setConnectTimeout(5000).setSocketTimeout(5000).build();
        } else {
          requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).setRedirectsEnabled(true) // set
              .setConnectionRequestTimeout(DataFetcher.DEFAULT_CONNECTION_REQUEST_TIMEOUT).setConnectTimeout(DataFetcher.DEFAULT_CONNECT_TIMEOUT)
              .setSocketTimeout(DataFetcher.DEFAULT_SOCKET_TIMEOUT).build();
        }
      }

      // creating the redirect strategy
      // so we can get the final redirected URL
      DataFetcherRedirectStrategy redirectStrategy = new DataFetcherRedirectStrategy();

      List<Header> headerList = new ArrayList<>();
      headerList.add(new BasicHeader(HttpHeaders.CONTENT_ENCODING, DataFetcher.CONTENT_ENCODING));

      for (Entry<String, String> mapEntry : headers.entrySet()) {
        headerList.add(new BasicHeader(mapEntry.getKey(), mapEntry.getValue()));
      }

      CloseableHttpClient httpclient =
          HttpClients.custom().setDefaultCookieStore(cookieStore).setUserAgent(randUserAgent).setDefaultRequestConfig(requestConfig)
              .setRedirectStrategy(redirectStrategy).setDefaultCredentialsProvider(credentialsProvider).setDefaultHeaders(headerList).build();

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
      PageContent pageContent = new PageContent(closeableHttpResponse.getEntity()); // loading
      pageContent.setStatusCode(closeableHttpResponse.getStatusLine().getStatusCode()); // geting
      pageContent.setUrl(url); // setting url

      responseLength = pageContent.getContentData().length;

      // assembling request information log message
      DataFetcher.sendRequestInfoLog(url, DataFetcher.GET_REQUEST, randProxy, randUserAgent, session, closeableHttpResponse, responseLength,
          requestHash);

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
      for (String errorCode : DataFetcher.errorCodes) {
        if (content.equals(errorCode)) {
          throw new ResponseCodeException(Integer.parseInt(errorCode));
        }
      }

      // record the redirected URL on the session
      if (redirectStrategy.getFinalURL() != null && !redirectStrategy.getFinalURL().isEmpty()) {
        session.addRedirection(url, redirectStrategy.getFinalURL());
      }

      // process response and parse
      return DataFetcher.processContent(pageContent, session);

    } catch (Exception e) {
      DataFetcher.sendRequestInfoLog(url, DataFetcher.GET_REQUEST, randProxy, randUserAgent, session, closeableHttpResponse, responseLength,
          requestHash);

      if (e instanceof ResponseCodeException) {
        Logging.printLogWarn(logger, session, "Attempt " + attempt + " -> Error performing GET request: " + session.getOriginalURL());
        Logging.printLogWarn(logger, session, e.getMessage());
      } else {
        Logging.printLogError(logger, session, "Attempt " + attempt + " -> Error performing GET request: " + session.getOriginalURL());
        Logging.printLogError(logger, session, e.getMessage());
      }

      if (attempt >= session.getMaxConnectionAttemptsCrawler()) {
        Logging.printLogError(logger, session, "Reached maximum attempts for URL [" + url + "]");
        return "";
      } else {
        return fetchPageGETWithHeaders(session, url, cookies, headers, attempt + 1);
      }

    }
  }

  private static String getLoggingMessage(String requestType, String requestEndpoint) {
    return new StringBuilder().append("Request : [").append(requestType).append(", ").append(requestEndpoint).append("]").toString();
  }

}
