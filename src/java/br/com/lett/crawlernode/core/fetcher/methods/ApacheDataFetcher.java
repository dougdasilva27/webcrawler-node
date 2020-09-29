package br.com.lett.crawlernode.core.fetcher.methods;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.fluent.Async;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.SocketConfig;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.lett.crawlernode.aws.s3.S3Service;
import br.com.lett.crawlernode.core.fetcher.DataFetcherRedirectStrategy;
import br.com.lett.crawlernode.core.fetcher.FetchUtilities;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.LettProxy;
import br.com.lett.crawlernode.core.fetcher.models.PageContent;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.RequestsStatistics;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.fetcher.models.Response.ResponseBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.crawler.ImageCrawlerSession;
import br.com.lett.crawlernode.core.session.crawler.TestCrawlerSession;
import br.com.lett.crawlernode.exceptions.ResponseCodeException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

public class ApacheDataFetcher implements DataFetcher {

   private static final Logger logger = LoggerFactory.getLogger(ApacheDataFetcher.class);

   @Override
   public Response get(Session session, Request request) {
      return fetch(session, request, FetchUtilities.GET_REQUEST);
   }

   @Override
   public Response post(Session session, Request request) {
      return fetch(session, request, FetchUtilities.POST_REQUEST);
   }

   private Response fetch(Session session, Request request, String method) {
      Response response = new Response();
      List<RequestsStatistics> requests = new ArrayList<>();

      String url = request.getUrl();
      Logging.printLogDebug(logger, session, FetchUtilities.getLoggingMessage(method, url));

      int attempt = 1;

      boolean mustContinue = true;

      long requestsStartTime = System.currentTimeMillis();


      while (attempt <= session.getMaxConnectionAttemptsCrawler() && ((request.bodyIsRequired() && (response.getBody() == null || response.getBody()
            .isEmpty())) || !request.bodyIsRequired()) && mustContinue) {
         RequestsStatistics requestStats = new RequestsStatistics();
         requestStats.setAttempt(attempt);

         LettProxy randProxy = null;
         Map<String, String> headers = request.getHeaders() != null ? request.getHeaders() : new HashMap<>();
         String randUserAgent = headers.containsKey(FetchUtilities.USER_AGENT) ? headers.get(FetchUtilities.USER_AGENT) : FetchUtilities.randUserAgent();
         CloseableHttpResponse closeableHttpResponse = null;
         String requestHash = FetchUtilities.generateRequestHash(session);

         try {
            long requestStartTime = System.currentTimeMillis();

            randProxy = request.getProxy() != null ? request.getProxy() : FetchUtilities.getNextProxy(session, attempt);

            requestStats.setProxy(randProxy);
            session.addRequestProxy(url, randProxy);

            SocketConfig socketConfig = SocketConfig.custom()
                  .setSoKeepAlive(false)
                  .setSoLinger(1)
                  .setSoReuseAddress(true)
                  .setSoTimeout(FetchUtilities.DEFAULT_SOCKET_TIMEOUT)
                  .setTcpNoDelay(true)
                  .build();

            CookieStore cookieStore = createCookieStore(request.getCookies());
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

            if (randProxy != null) {
               Logging.printLogDebug(logger, session, "[ATTEMPT " + attempt + "] Using " + randProxy.getSource() + " (proxy) for this request.");
               if (randProxy.getUser() != null) {
                  credentialsProvider.setCredentials(new AuthScope(randProxy.getAddress(), randProxy.getPort()),
                        new UsernamePasswordCredentials(randProxy.getUser(), randProxy.getPass()));
               }
            } else {
               Logging.printLogDebug(logger, session, "[NO_PROXY ALERT][ATTEMPT " + attempt + "]Using no proxy for this request.");
            }

            List<Header> reqHeaders = new ArrayList<>();
            if (request.mustSendContentEncoding()) {
               reqHeaders.add(new BasicHeader(HttpHeaders.CONTENT_ENCODING, FetchUtilities.CONTENT_ENCODING));
            }

            for (Entry<String, String> mapEntry : headers.entrySet()) {
               reqHeaders.add(new BasicHeader(mapEntry.getKey(), mapEntry.getValue()));
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

            // creating the redirect strategy so we can get the final redirected URL
            DataFetcherRedirectStrategy redirectStrategy = new DataFetcherRedirectStrategy();
            HttpHost proxy = randProxy != null ? new HttpHost(randProxy.getAddress(), randProxy.getPort()) : null;
            RequestConfig requestConfig = FetchUtilities.getRequestConfig(proxy, request.isFollowRedirects(), session);

            CloseableHttpClient httpclient =
                  HttpClients.custom()
                        .setDefaultCookieStore(cookieStore)
                        .setUserAgent(randUserAgent).setDefaultRequestConfig(requestConfig)
                        .setRedirectStrategy(redirectStrategy)
                        .setDefaultCredentialsProvider(credentialsProvider)
                        .setDefaultHeaders(reqHeaders)
                        .setSSLSocketFactory(FetchUtilities.createSSLConnectionSocketFactory())
                        .setSSLHostnameVerifier(hostNameVerifier)
                        .setDefaultSocketConfig(socketConfig)
                        .build();

            HttpContext localContext = new BasicHttpContext();
            localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

            if (method.equals(FetchUtilities.POST_REQUEST)) {
               String payload = request.getPayload();
               StringEntity input = new StringEntity(payload);
               input.setContentType(headers.get(HttpHeaders.CONTENT_TYPE));

               HttpPost httpPost = new HttpPost(url);
               httpPost.setEntity(input);
               httpPost.setConfig(requestConfig);

               if (headers.containsKey(HttpHeaders.CONTENT_TYPE) && payload != null) {
                  httpPost.setEntity(new StringEntity(payload, ContentType.create(headers.get(HttpHeaders.CONTENT_TYPE))));
               }

               for (Entry<String, String> entry : headers.entrySet()) {
                  httpPost.addHeader(entry.getKey(), entry.getValue());
               }

               // do request
               closeableHttpResponse = httpclient.execute(httpPost, localContext);
            } else if (method.equals(FetchUtilities.GET_REQUEST)) {
               HttpGet httpGet = new HttpGet(url);
               httpGet.setConfig(requestConfig);

               // do request
               closeableHttpResponse = httpclient.execute(httpGet, localContext);
            }

            // analysing the status code
            // if there was some response code that indicates forbidden access or server error we want to
            // try again
            int responseCode = closeableHttpResponse != null ? closeableHttpResponse.getStatusLine().getStatusCode() : 0;
            requestStats.setStatusCode(responseCode);
            requestStats.setElapsedTime(System.currentTimeMillis() - requestStartTime);
            if (responseCode == 404 || responseCode == 204) {
               FetchUtilities.sendRequestInfoLog(attempt, request, requestStats, randProxy, method, randUserAgent, session, responseCode, requestHash);
               break;
            } else if (Integer.toString(responseCode).charAt(0) != '2' && Integer.toString(responseCode).charAt(0) != '3') { // errors
               throw new ResponseCodeException(responseCode);
            }

            // creating the page content result from the http request
            PageContent pageContent = new PageContent(closeableHttpResponse.getEntity());
            pageContent.setStatusCode(closeableHttpResponse.getStatusLine().getStatusCode());
            pageContent.setUrl(url);

            // saving request content result on Amazon
            String content = "";
            if (pageContent.getContentData() != null) {
               if (pageContent.getContentCharset() == null) {
                  content = new String(pageContent.getContentData()).trim();
               } else {
                  content = new String(pageContent.getContentData(), pageContent.getContentCharset()).trim();
               }
            }

            S3Service.saveResponseContent(session, requestHash, content);

            if (!content.isEmpty()) {
               FetcherOptions options = request.getFetcherOptions();
               if (options != null) {
                  String selector = options.getForbiddenCssSelector();

                  if (selector != null) {
                     Document doc = Jsoup.parse(content);

                     if (!doc.select(selector).isEmpty()) {
                        Logging.printLogWarn(logger, session, "[ATTEMPT " + attempt + "] Error performing " + method + " request. Error: Forbidden Selector detected.");
                        throw new ResponseCodeException(403);
                     }
                  }
               }

            }

            // see if some code error occured
            // sometimes the remote server doesn't send the http error code on the headers
            // but rater on the page bytes
            for (String errorCode : FetchUtilities.errorCodes) {
               if (content.equals(errorCode)) {
                  requestStats.setStatusCode(Integer.parseInt(errorCode));
                  requests.add(requestStats);
                  throw new ResponseCodeException(Integer.parseInt(errorCode));
               }
            }

            // record the redirected URL on the session
            if (redirectStrategy.getFinalURL() != null && !redirectStrategy.getFinalURL().isEmpty()) {
               session.addRedirection(url, redirectStrategy.getFinalURL());
            }

            Map<String, String> responseHeaders = FetchUtilities.headersToMap(closeableHttpResponse.getAllHeaders());

            response = new ResponseBuilder()
                  .setBody(FetchUtilities.processContent(pageContent, session).trim())
                  .setRedirecturl(redirectStrategy.getFinalURL())
                  .setProxyused(randProxy)
                  .setHeaders(responseHeaders)
                  .setCookies(FetchUtilities.getCookiesFromHeaders(closeableHttpResponse.getHeaders(FetchUtilities.HEADER_SET_COOKIE)))
                  .setLastStatusCode(responseCode)
                  .build();
            mustContinue = false;
            requestStats.setHasPassedValidation(true);
            requests.add(requestStats);
            FetchUtilities.sendRequestInfoLog(attempt, request, requestStats, randProxy, method, randUserAgent, session, responseCode, requestHash);
         } catch (Exception e) {
            int code = e instanceof ResponseCodeException ? ((ResponseCodeException) e).getCode() : 0;

            FetchUtilities.sendRequestInfoLog(attempt, request, requestStats, randProxy, method, randUserAgent, session, code, requestHash);
            requestStats.setHasPassedValidation(false);

            Logging.printLogDebug(logger, session, "[ATTEMPT " + attempt + "] Error performing " + method + " request. Error: " + e.getMessage());
            if (session instanceof TestCrawlerSession) {
               Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e));
            }
         }

         attempt++;
      }


      JSONObject apacheMetadata = new JSONObject().put("req_apache_elapsed_time", System.currentTimeMillis() - requestsStartTime)
            .put("req_apache_attempts_number", attempt)
            .put("req_apache_type", "url_request");

      Logging.logInfo(logger, session, apacheMetadata, "APACHE REQUESTS INFO");

      response.setRequests(requests);
      return response;
   }

   @Override
   public File fetchImage(Session session, Request request) {
      File localFile = null;
      int attempt = 1;

      long requestsStartTime = System.currentTimeMillis();

      while (attempt <= session.getMaxConnectionAttemptsImages() && localFile == null) {
         LettProxy randProxy = null;
         String url = request.getUrl();
         Map<String, String> headers = request.getHeaders();
         String randUserAgent = headers.containsKey(FetchUtilities.USER_AGENT) ? headers.get(FetchUtilities.USER_AGENT) : FetchUtilities.randUserAgent();
         CloseableHttpResponse closeableHttpResponse = null;
         String requestHash = FetchUtilities.generateRequestHash(session);
         RequestsStatistics requestStats = new RequestsStatistics();
         requestStats.setAttempt(attempt);

         try {
            long requestStartTime = System.currentTimeMillis();

            randProxy = request.getProxy() != null ? request.getProxy() : FetchUtilities.getNextProxy(session, attempt);
            requestStats.setProxy(randProxy);
            session.addRequestProxy(url, randProxy);

            CookieStore cookieStore = createCookieStore(request.getCookies());
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

            if (randProxy != null) {
               Logging.printLogDebug(logger, session, "Using " + randProxy.getSource() + "(proxy) for this request.");
               if (randProxy.getUser() != null) {
                  credentialsProvider.setCredentials(new AuthScope(randProxy.getAddress(), randProxy.getPort()),
                        new UsernamePasswordCredentials(randProxy.getUser(), randProxy.getPass()));
               }
            } else {
               Logging.printLogDebug(logger, session, "[NO_PROXY ALERT]Using no proxy for this request.");
            }

            List<Header> reqHeaders = new ArrayList<>();
            if (request.mustSendContentEncoding()) {
               reqHeaders.add(new BasicHeader(HttpHeaders.CONTENT_ENCODING, FetchUtilities.CONTENT_ENCODING));
            }

            for (Entry<String, String> mapEntry : headers.entrySet()) {
               reqHeaders.add(new BasicHeader(mapEntry.getKey(), mapEntry.getValue()));
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


            HttpHost proxy = randProxy != null ? new HttpHost(randProxy.getAddress(), randProxy.getPort()) : null;
            RequestConfig requestConfig = FetchUtilities.getRequestConfig(proxy, request.isFollowRedirects(), session);

            SocketConfig socketConfig = SocketConfig.custom()
                  .setSoKeepAlive(false)
                  .setSoLinger(1)
                  .setSoReuseAddress(true)
                  .setSoTimeout(FetchUtilities.DEFAULT_SOCKET_TIMEOUT)
                  .setTcpNoDelay(true)
                  .build();

            CloseableHttpClient httpclient = HttpClients.custom()
                  .setDefaultCookieStore(cookieStore)
                  .setUserAgent(randUserAgent)
                  .setDefaultRequestConfig(requestConfig)
                  .setDefaultHeaders(reqHeaders)
                  .setSSLHostnameVerifier(hostNameVerifier)
                  .setSSLSocketFactory(FetchUtilities.createSSLConnectionSocketFactory())
                  .setDefaultCredentialsProvider(credentialsProvider)
                  .setDefaultSocketConfig(socketConfig)
                  .build();

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
            requestStats.setStatusCode(responseCode);
            requestStats.setElapsedTime(System.currentTimeMillis() - requestStartTime);
            if (Integer.toString(responseCode).charAt(0) != '2' && Integer.toString(responseCode).charAt(0) != '3' && responseCode != 404) { // errors
               throw new ResponseCodeException(responseCode);
            }

            localFile = new File(((ImageCrawlerSession) session).getLocalOriginalFileDir());

            // get image bytes
            BufferedInputStream is = new BufferedInputStream(closeableHttpResponse.getEntity().getContent());
            FileOutputStream file = new FileOutputStream(localFile);
            BufferedOutputStream bout = new BufferedOutputStream(file);
            byte[] b = new byte[8 * 1024]; // reading each 8kb
            int read = 0;
            while ((read = is.read(b)) > -1) {
               bout.write(b, 0, read);
            }
            bout.flush();
            bout.close();
            file.close();
            is.close();

            FetchUtilities.sendRequestInfoLog(attempt, request, requestStats, randProxy, FetchUtilities.GET_REQUEST, randUserAgent, session, responseCode, requestHash);
         } catch (Exception e) {
            int code = e instanceof ResponseCodeException ? ((ResponseCodeException) e).getCode() : 0;
            Logging.printLogWarn(logger, session, "Attempt " + attempt + " -> Error performing GET request: " + e.getMessage());

            FetchUtilities.sendRequestInfoLog(attempt, request, requestStats, randProxy, FetchUtilities.GET_REQUEST, randUserAgent, session, code, requestHash);

            if (localFile != null && localFile.exists()) {
               localFile.delete();
            }

         }
         attempt++;
      }

      JSONObject apacheMetadata = new JSONObject().put("req_apache_elapsed_time", System.currentTimeMillis() - requestsStartTime)
            .put("req_apache_attempts_number", attempt)
            .put("req_apache_type", "images_download");

      Logging.logInfo(logger, session, apacheMetadata, "APACHE REQUESTS INFO");

      return localFile;
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

   /**
    * This function make a asynchronous get request on a url
    * 
    * @param url
    * @param session
    * @return Future<Content>
    */
   public Future<Content> getAsyncHttp(final String url, Session session) {
      try {
         Logging.printLogDebug(logger, session, "Performing a async request on: " + url);
         URI requestURL = null;
         try {
            requestURL = new URI(url);
         } catch (URISyntaxException use) {
            Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(use));
         }

         ExecutorService threadpool = Executors.newFixedThreadPool(1);
         Async async = Async.newInstance().use(threadpool);
         final org.apache.http.client.fluent.Request request = org.apache.http.client.fluent.Request
               .Get(requestURL)
               .socketTimeout(5000)
               .connectTimeout(5000);


         return async.execute(request, new FutureCallback<Content>() {
            @Override
            public void failed(final Exception e) {
               Logging.printLogWarn(logger, CommonMethods.getStackTrace(e));
               threadpool.shutdown();
            }

            @Override
            public void completed(final Content content) {
               Logging.printLogInfo(logger, "Request: " + request.toString() + " is completed!");
               threadpool.shutdown();
            }

            @Override
            public void cancelled() {
               threadpool.shutdown();
            }
         });
      } catch (Exception e) {
         Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
      }

      return null;
   }
}
