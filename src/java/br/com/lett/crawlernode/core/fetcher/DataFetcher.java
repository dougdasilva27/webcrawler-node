package br.com.lett.crawlernode.core.fetcher;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.SSLContext;

import org.apache.commons.codec.binary.Base64;
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
import org.json.JSONObject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import br.com.lett.crawlernode.core.fetcher.methods.GETFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.POSTFetcher;
import br.com.lett.crawlernode.core.parser.Parser;
import br.com.lett.crawlernode.core.session.ImageCrawlerSession;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.TestCrawlerSession;
import br.com.lett.crawlernode.core.session.TestRankingKeywordsSession;
import br.com.lett.crawlernode.exceptions.ResponseCodeException;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.queue.S3Service;
import br.com.lett.crawlernode.test.Test;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.DateConstants;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;


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

	public static final int MAX_ATTEMPTS_FOR_CONECTION_WITH_PROXY = 10;

	public static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT = 10000; // ms
	public static final int DEFAULT_CONNECT_TIMEOUT = 10000; // ms
	public static final int DEFAULT_SOCKET_TIMEOUT = 10000; // ms

	public static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT_IMG = 20000; // ms
	public static final int DEFAULT_CONNECT_TIMEOUT_IMG = 20000; // ms
	public static final int DEFAULT_SOCKET_TIMEOUT_IMG = 20000; // ms

	public static final String CONTENT_ENCODING = "compress, gzip";


	/** Most popular agents, retrieved from https://techblog.willshouse.com/2012/01/03/most-common-user-agents/ */
	public static List<String> userAgents;

	public static List<String> errorCodes;

	/**
	 * Static initialization block
	 * I commented these two user, because in the site homerefil was considering them outdated:
	 *  
	 *  Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; rv:11.0) like Gecko
	 *	Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2
	 * 
	 */
	static {		
		userAgents = Arrays.asList(
				"Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36",
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
				"Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36"
				);

		errorCodes = Arrays.asList("403");
	}

	/**
	 * Fetch a text string from a URL, either by a GET ou POST http request.
	 * 
	 * @param reqType The type of the http request. GET_REQUEST or POST_REQUEST.
	 * @param session
	 * @param url The url from which we will fetch the data.
	 * @param urlParameters The urlParameters, or parameter field of the request, in case of a POST request. null if we have a GET request.
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
	 * @param urlParameters The urlParameters, or parameter field of the request, in case of a POST request. null if we have a GET request.
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
	 * @param urlParameters The urlParameters, or parameter field of the request, in case of a POST request. null if we have a GET request.
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
	 * @param payload The payload, or parameter field of the request, in case of a POST request. null if we have a GET request.
	 * @param cookies
	 * @return A JSONObject with the data from the url passed, or null if something went wrong.
	 */
	public static JSONObject fetchJSONObject(String reqType, Session session, String url, String payload, List<Cookie> cookies) {
		return new JSONObject(fetchJson(reqType, session, url, payload, cookies, 1));
	}

	/**
	 * Fetch a json array from the API, either by a GET ou POST http request.
	 * 
	 * @param reqType The type of the http request. GET_REQUEST or POST_REQUEST.
	 * @param session
	 * @param url The url from which we will fetch the data.
	 * @param payload The payload, or parameter field of the request, in case of a POST request. null if we have a GET request.
	 * @param cookies
	 * @return A JSONArray with the data from the url passed, or null if something went wrong.
	 */
	public static JSONArray fetchJSONArray(String reqType, Session session, String url, String payload, List<Cookie> cookies) {
		return new JSONArray(fetchJson(reqType, session, url, payload, cookies, 1));
	}

	/**
	 * Get the http response code of a URL
	 * 
	 * @param url 
	 * @param session
	 * @return The integer code. Null if we have an exception.
	 */
	public static Integer getUrlResponseCode(String url, Session session) {
		return getUrlResponseCode(url, session, 1);
	}

	public static Integer getUrlResponseCode(String url, Session session, int attempt) {
		try {
			URL urlObject = new URL(url);
			HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection(randProxy(attempt, session, new ArrayList<String>()));
			connection.setRequestMethod("GET");
			connection.connect();

			return connection.getResponseCode();
		} catch (Exception e) {
			Logging.printLogError(logger, session, "Tentativa " + attempt + " -> Erro ao fazer requisição de status code: " + url + " [" + e.getMessage() + "]");
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));

			if(attempt >= MAX_ATTEMPTS_FOR_CONECTION_WITH_PROXY) {
				Logging.printLogError(logger, session, "Reached maximum attempts for URL [" + url + "]");
			} else {
				return getUrlResponseCode(url, session, attempt+1);	
			}

			return null;
		}
	}

	/**
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
			Logging.printLogError(logger, session, "Tentativa " + attempt + " -> Erro ao fazer requisição de JSONObject via " + reqType + ": " + url);
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));


			if(attempt >= MAX_ATTEMPTS_FOR_CONECTION_WITH_PROXY) {
				Logging.printLogError(logger, session, "Reached maximum attempts for URL [" + url + "]");
			} else {
				return fetchJson(reqType, session, url, payload, cookies, attempt+1);	
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
			requestConfig = RequestConfig.custom()
					.setCookieSpec(CookieSpecs.STANDARD)
					.setRedirectsEnabled(true)
					.setConnectionRequestTimeout(DEFAULT_CONNECTION_REQUEST_TIMEOUT)
					.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT)
					.setSocketTimeout(DEFAULT_SOCKET_TIMEOUT)
					.setProxy(proxy)
					.build();
		} else {
			requestConfig = RequestConfig.custom()
					.setCookieSpec(CookieSpecs.STANDARD)
					.setRedirectsEnabled(true)
					.setConnectionRequestTimeout(DEFAULT_CONNECTION_REQUEST_TIMEOUT)
					.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT)
					.setSocketTimeout(DEFAULT_SOCKET_TIMEOUT)
					.build();
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
	private static String fetchPage(
			String reqType, 
			Session session, 
			String url, 
			String urlParameters, 
			List<Cookie> cookies, 
			int attempt) {

		try {

			if (reqType.equals(GET_REQUEST)) {
				return GETFetcher.fetchPageGET(session, url, cookies, attempt);
			} else if (reqType.equals(POST_REQUEST)) {
				if (urlParameters != null) {
					return POSTFetcher.fetchPagePOST(session, url, urlParameters, cookies, attempt);
				} else {
					Logging.printLogError(logger, session, "Parameter payload is null.");
					return "";
				}
			} else {
				Logging.printLogError(logger, session, "Invalid reqType parameter.");
				return "";
			}

		} catch (Exception e) {
			Logging.printLogError(logger, session, "Attempt " + attempt + " -> Error in " + reqType + " request for URL: " + url);
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));

			if(attempt >= MAX_ATTEMPTS_FOR_CONECTION_WITH_PROXY) {
				Logging.printLogError(logger, session, "Reached maximum attempts for URL [" + url + "]");
				return "";

			} else {
				return fetchPage(reqType, session, url, urlParameters, cookies, attempt+1);	
			}

		}

	}
	
	public static SSLConnectionSocketFactory createSSLConnectionSocketFactory() throws NoSuchAlgorithmException, KeyManagementException {
		TrustManager trustManager = new TrustManager();
		SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
		sslContext.init(null, new TrustManager[]{trustManager}, null);
		
		return new SSLConnectionSocketFactory(sslContext);
	}


	/**
	 * Fetch a page
	 * By default the redirects are enabled in the RequestConfig
	 * 
	 * @param session
	 * @param url
	 * @param cookieName
	 * @param cookies
	 * @param attempt
	 * @return the header value. Will return an empty string if the cookie wasn't found.
	 */
	public static Map<String,String> fetchCookies(
			Session session, 
			String url,
			List<Cookie> cookies, 
			int attempt) {

		LettProxy randProxy = null;
		String randUserAgent = null;
		CloseableHttpResponse closeableHttpResponse = null;
		String requestHash = generateRequestHash(session);

		try {
			Logging.printLogDebug(logger, session, "Performing GET request to fetch cookie: " + url);

			randUserAgent = randUserAgent();
			randProxy = randLettProxy(attempt, session, session.getMarket().getProxies());

			CookieStore cookieStore = createCookieStore(cookies);

			CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

			if (randProxy != null) {
				if(randProxy.getUser() != null) {
					credentialsProvider.setCredentials(
							new AuthScope(randProxy.getAddress(), randProxy.getPort()),
							new UsernamePasswordCredentials(randProxy.getUser(), randProxy.getPass())
							);
				}
			}

			HttpHost proxy = null;
			if (randProxy != null) {
				proxy = new HttpHost(randProxy.getAddress(), randProxy.getPort());
			}

			RequestConfig requestConfig = createRequestConfig(proxy);

			List<Header> reqHeaders = new ArrayList<>();
			reqHeaders.add(new BasicHeader(HttpHeaders.CONTENT_ENCODING, "compress, gzip"));

			CloseableHttpClient httpclient = HttpClients.custom()
					.setDefaultCookieStore(cookieStore)
					.setUserAgent(randUserAgent)
					.setDefaultRequestConfig(requestConfig)
					.setDefaultCredentialsProvider(credentialsProvider)
					.setDefaultHeaders(reqHeaders)
					.build();

			HttpContext localContext = new BasicHttpContext();
			localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

			HttpGet httpGet = new HttpGet(url);
			httpGet.setConfig(requestConfig);

			// if we are using charity engine, we must set header for authentication
			if (randProxy != null && randProxy.getSource().equals(ProxyCollection.CHARITY)) {
				String authenticator = "ff548a45065c581adbb23bbf9253de9b" + ":";
				String headerValue = "Basic " + Base64.encodeBase64String(authenticator.getBytes());
				httpGet.addHeader("Proxy-Authorization", headerValue);

				// setting header for proxy country
				httpGet.addHeader("X-Proxy-Country", "BR");
			}

			// if we are using azure, we must set header for authentication
			if (randProxy != null && randProxy.getSource().equals(ProxyCollection.AZURE)) {
				httpGet.addHeader("Authorization", "5RXsOBETLoWjhdM83lDMRV3j335N1qbeOfMoyKsD");
			}

			// do request
			closeableHttpResponse = httpclient.execute(httpGet, localContext);

			// analysing the status code
			// if there was some response code that indicates forbidden access or server error we want to try again
			int responseCode = closeableHttpResponse.getStatusLine().getStatusCode();
			if( Integer.toString(responseCode).charAt(0) != '2' && 
					Integer.toString(responseCode).charAt(0) != '3' && 
					responseCode != 404 ) { // errors
				throw new ResponseCodeException(responseCode);
			}

			// creating the page content result from the http request
			PageContent pageContent = new PageContent(closeableHttpResponse.getEntity());		// loading information from http entity
			pageContent.setStatusCode(closeableHttpResponse.getStatusLine().getStatusCode());	// geting the status code
			pageContent.setUrl(url); // setting url

			// assembling request information log message
			sendRequestInfoLog(url, GET_REQUEST, randProxy, randUserAgent, session, closeableHttpResponse, requestHash);

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

			Map<String,String> cookiesMap = new HashMap<>();

			// get all cookie headers
			Header[] headers = closeableHttpResponse.getHeaders(HTTP_COOKIE_HEADER);

			for (Header header : headers) {
				String cookieHeader = header.getValue();
				String cookieName = cookieHeader.split("=")[0].trim();

				int x = cookieHeader.indexOf(cookieName+"=") + cookieName.length()+1;
				int y = cookieHeader.indexOf(";", x);

				String cookieValue = cookieHeader.substring(x, y).trim();

				cookiesMap.put(cookieName, cookieValue);
			}

			return cookiesMap;


		} catch (Exception e) {
			sendRequestInfoLog(url, GET_REQUEST, randProxy, randUserAgent, session, closeableHttpResponse, requestHash);

			Logging.printLogError(logger, session, "Tentativa " + attempt + " -> Erro ao fazer requisição GET para header: " + url);
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));

			if(attempt >= MAX_ATTEMPTS_FOR_CONECTION_WITH_PROXY) {
				Logging.printLogError(logger, session, "Reached maximum attempts for URL [" + url + "]");
				return null;
			} else {
				return fetchCookies(session, url, cookies, attempt+1);	
			}

		}
	}

	/**
	 * Fetch a page
	 * By default the redirects are enabled in the RequestConfig
	 * 
	 * @param session
	 * @param url
	 * @param cookieName
	 * @param cookies
	 * @param attempt
	 * @return the header value. Will return an empty string if the cookie wasn't found.
	 */
	public static String fetchCookie(
			Session session, 
			String url,
			String cookieName,
			List<Cookie> cookies, 
			int attempt) {

		LettProxy randProxy = null;
		String randUserAgent = null;
		CloseableHttpResponse closeableHttpResponse = null;
		String requestHash = generateRequestHash(session);

		try {
			Logging.printLogDebug(logger, session, "Performing GET request to fetch cookie: " + url);

			randUserAgent = randUserAgent();
			randProxy = randLettProxy(attempt, session, session.getMarket().getProxies());

			CookieStore cookieStore = createCookieStore(cookies);

			CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

			if (randProxy != null) {
				if(randProxy.getUser() != null) {
					credentialsProvider.setCredentials(
							new AuthScope(randProxy.getAddress(), randProxy.getPort()),
							new UsernamePasswordCredentials(randProxy.getUser(), randProxy.getPass())
							);
				}
			}

			HttpHost proxy = null;
			if (randProxy != null) {
				proxy = new HttpHost(randProxy.getAddress(), randProxy.getPort());
			}

			RequestConfig requestConfig = null;
			if (proxy != null) {
				requestConfig = RequestConfig.custom()
						.setCookieSpec(CookieSpecs.STANDARD)
						.setRedirectsEnabled(true) // set redirect to true
						.setConnectionRequestTimeout(DEFAULT_CONNECTION_REQUEST_TIMEOUT)
						.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT)
						.setSocketTimeout(DEFAULT_SOCKET_TIMEOUT)
						.setProxy(proxy)
						.build();
			} else {
				requestConfig = RequestConfig.custom()
						.setCookieSpec(CookieSpecs.STANDARD)
						.setRedirectsEnabled(true) // set redirect to true
						.setConnectionRequestTimeout(DEFAULT_CONNECTION_REQUEST_TIMEOUT)
						.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT)
						.setSocketTimeout(DEFAULT_SOCKET_TIMEOUT)
						.build();
			}


			List<Header> reqHeaders = new ArrayList<>();
			reqHeaders.add(new BasicHeader(HttpHeaders.CONTENT_ENCODING, CONTENT_ENCODING));

			CloseableHttpClient httpclient = HttpClients.custom()
					.setDefaultCookieStore(cookieStore)
					.setUserAgent(randUserAgent)
					.setDefaultRequestConfig(requestConfig)
					.setDefaultCredentialsProvider(credentialsProvider)
					.setDefaultHeaders(reqHeaders)
					.build();

			HttpContext localContext = new BasicHttpContext();
			localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

			HttpGet httpGet = new HttpGet(url);
			httpGet.setConfig(requestConfig);

			// if we are using charity engine, we must set header for authentication
			if (randProxy != null && randProxy.getSource().equals(ProxyCollection.CHARITY)) {
				String authenticator = "ff548a45065c581adbb23bbf9253de9b" + ":";
				String headerValue = "Basic " + Base64.encodeBase64String(authenticator.getBytes());
				httpGet.addHeader("Proxy-Authorization", headerValue);

				// setting header for proxy country
				httpGet.addHeader("X-Proxy-Country", "BR");
			}

			// if we are using azure, we must set header for authentication
			if (randProxy != null && randProxy.getSource().equals(ProxyCollection.AZURE)) {
				httpGet.addHeader("Authorization", "5RXsOBETLoWjhdM83lDMRV3j335N1qbeOfMoyKsD");
			}

			// do request
			closeableHttpResponse = httpclient.execute(httpGet, localContext);

			// analysing the status code
			// if there was some response code that indicates forbidden access or server error we want to try again
			int responseCode = closeableHttpResponse.getStatusLine().getStatusCode();
			if( Integer.toString(responseCode).charAt(0) != '2' && 
					Integer.toString(responseCode).charAt(0) != '3' && 
					responseCode != 404 ) { // errors
				throw new ResponseCodeException(responseCode);
			}

			// creating the page content result from the http request
			PageContent pageContent = new PageContent(closeableHttpResponse.getEntity());		// loading information from http entity
			pageContent.setStatusCode(closeableHttpResponse.getStatusLine().getStatusCode());	// geting the status code
			pageContent.setUrl(url); // setting url

			// assembling request information log message
			sendRequestInfoLog(url, GET_REQUEST, randProxy, randUserAgent, session, closeableHttpResponse, requestHash);

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
			sendRequestInfoLog(url, GET_REQUEST, randProxy, randUserAgent, session, closeableHttpResponse, requestHash);

			Logging.printLogError(logger, session, "Tentativa " + attempt + " -> Erro ao fazer requisição GET para header: " + url);
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));

			if(attempt >= MAX_ATTEMPTS_FOR_CONECTION_WITH_PROXY) {
				Logging.printLogError(logger, session, "Reached maximum attempts for URL [" + url + "]");
				return "";
			} else {
				return fetchCookie(session, url, cookieName, cookies, attempt+1);	
			}

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
	public static void sendRequestInfoLog(
			String url, 
			String requestType, 
			LettProxy proxy,
			String userAgent,
			Session session, 
			CloseableHttpResponse response,
			String requestHash) {

		JSONObject requestMetadata = new JSONObject();

		requestMetadata.put("req_hash", requestHash);
		requestMetadata.put("proxy_name", 	(proxy == null ? ProxyCollection.NO_PROXY 		: proxy.getSource()));
		requestMetadata.put("proxy_ip", 	(proxy == null ? MDC.get("HOST_NAME") 	: proxy.getAddress()));
		requestMetadata.put("user_agent", 	userAgent);
		requestMetadata.put("req_method", requestType);
		requestMetadata.put("req_location", url);
		requestMetadata.put("res_http_code", (response == null) ? 0 : response.getStatusLine().getStatusCode());
		requestMetadata.put("res_length", (response == null) ? 0 : response.getEntity().getContentLength());

		Logging.printLogDebug(logger, session, requestMetadata, "Registrando requisição...");

	}
	
	public static void sendRequestInfoLog(
			String url, 
			String requestType, 
			LettProxy proxy,
			String userAgent,
			Session session, 
			CloseableHttpResponse response,
			Integer responseLength,
			String requestHash) {

		JSONObject requestMetadata = new JSONObject();

		requestMetadata.put("req_hash", requestHash);
		requestMetadata.put("proxy_name", 	(proxy == null ? ProxyCollection.NO_PROXY 		: proxy.getSource()));
		requestMetadata.put("proxy_ip", 	(proxy == null ? MDC.get("HOST_NAME") 	: proxy.getAddress()));
		requestMetadata.put("user_agent", 	userAgent);
		requestMetadata.put("req_method", requestType);
		requestMetadata.put("req_location", url);
		requestMetadata.put("res_http_code", (response == null) ? 0 : response.getStatusLine().getStatusCode());
		requestMetadata.put("res_length", responseLength);

		Logging.printLogDebug(logger, session, requestMetadata, "Registrando requisição...");

	}

	/**
	 * Parse the page content, either to get a html or a plain text
	 * In case we are expecting JSONObject or JSONArray response from an API, the content
	 * will be parsed as a plain text. Otherwise it will be parsed as a htlm format.
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
	private static File downloadImageFromMarket(
			int attempt,
			Session session) {

		File localFile = null;
		LettProxy randProxy = null;
		String randUserAgent = null;
		CloseableHttpResponse closeableHttpResponse = null;
		String requestHash = generateRequestHash(session);

		try {

			// choosing the preferred proxy service
			randUserAgent = randUserAgent();
			if (session instanceof ImageCrawlerSession) {
				randProxy = randLettProxy(attempt, session, session.getMarket().getImageProxies());
			} else {
				randProxy = randLettProxy(attempt, session, session.getMarket().getProxies());
			}

			CookieStore cookieStore = new BasicCookieStore();

			CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

			if (randProxy != null) {
				if(randProxy.getUser() != null) {
					credentialsProvider.setCredentials(
							new AuthScope(randProxy.getAddress(), randProxy.getPort()),
							new UsernamePasswordCredentials(randProxy.getUser(), randProxy.getPass())
							);
				}
			}

			HttpHost proxy = null;
			if (randProxy != null) {
				proxy = new HttpHost(randProxy.getAddress(), randProxy.getPort());
			}

			RequestConfig requestConfig = createRequestConfig(proxy);

			List<Header> headers = new ArrayList<>();
			headers.add(new BasicHeader(HttpHeaders.CONTENT_ENCODING, CONTENT_ENCODING));

			CloseableHttpClient httpclient = HttpClients.custom()
					.setDefaultCookieStore(cookieStore)
					.setUserAgent(randUserAgent)
					.setDefaultRequestConfig(requestConfig)
					.setDefaultHeaders(headers)
					.setDefaultCredentialsProvider(credentialsProvider)
					.build();

			HttpContext localContext = new BasicHttpContext();
			localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

			HttpGet httpGet = new HttpGet(session.getOriginalURL());
			httpGet.setConfig(requestConfig);

			// if we are using charity engine, we must set header for authentication
			if (randProxy != null && randProxy.getSource().equals(ProxyCollection.CHARITY)) {
				String authenticator = "ff548a45065c581adbb23bbf9253de9b" + ":";
				String headerValue = "Basic " + Base64.encodeBase64String(authenticator.getBytes());
				httpGet.addHeader("Proxy-Authorization", headerValue);

				// setting header for proxy country
				httpGet.addHeader("X-Proxy-Country", "BR");
			}

			// if we are using azure, we must set header for authentication
			if (randProxy != null && randProxy.getSource().equals(ProxyCollection.AZURE)) {
				httpGet.addHeader("Authorization", "5RXsOBETLoWjhdM83lDMRV3j335N1qbeOfMoyKsD");
			}

			// do request
			closeableHttpResponse = httpclient.execute(httpGet, localContext);

			// analysing the status code
			// if there was some response code that indicates forbidden access or server error we want to try again
			int responseCode = closeableHttpResponse.getStatusLine().getStatusCode();
			if( Integer.toString(responseCode).charAt(0) != '2' && 
					Integer.toString(responseCode).charAt(0) != '3' && 
					responseCode != 404 ) { // errors
				throw new ResponseCodeException(responseCode);
			}

			// assembling request information log message
			sendRequestInfoLog(session.getOriginalURL(), GET_REQUEST, randProxy, randUserAgent, session, closeableHttpResponse, requestHash);

			localFile = new File(((ImageCrawlerSession)session).getLocalFileDir());

			// get image bytes
			BufferedInputStream is = new BufferedInputStream(closeableHttpResponse.getEntity().getContent());  
			BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(localFile));  
			byte[] b = new byte[8*1024]; // reading each 8kb  
			int read = 0;  
			while((read = is.read(b)) > -1){  
				bout.write(b, 0, read);  
			}  
			bout.flush();  
			bout.close();
			is.close();

			return localFile;

		} catch (Exception e) {			

			sendRequestInfoLog(session.getOriginalURL(), GET_REQUEST, randProxy, randUserAgent, session, closeableHttpResponse, requestHash);

			if (localFile != null && localFile.exists()) {
				localFile.delete();
			}

			if (e instanceof ResponseCodeException) {
				Logging.printLogWarn(logger, session, "Tentativa " + attempt + " -> Erro ao fazer requisição GET para download de imagem: " + session.getOriginalURL());
				Logging.printLogWarn(logger, session, CommonMethods.getStackTraceString(e));
			}
			else {
				Logging.printLogError(logger, session, "Tentativa " + attempt + " -> Erro ao fazer requisição GET para download de imagem: " + session.getOriginalURL());
				Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
			}

			if(attempt >= MAX_ATTEMPTS_FOR_CONECTION_WITH_PROXY) {
				Logging.printLogError(logger, session, "Reached maximum attempts for URL [" + session.getOriginalURL() + "]");
				return null;
			} else {
				return downloadImageFromMarket(attempt+1, session);
			}
		}

	}

	/**
	 * 
	 * @param attempt
	 * @param session
	 * @param proxyServices
	 * @return
	 */
	public static LettProxy randLettProxy(int attempt, Session session, List<String> proxyServices) {
		LettProxy nextProxy = null;
		String serviceName = getProxyService(attempt, session, proxyServices);

		if (serviceName != null) {
			nextProxy = getNextProxy(serviceName, session);
		}

		// request using no proxy
		if (nextProxy == null) {
			return null;
		}

		return nextProxy;
	}

	/**
	 * 
	 * @param attempt
	 * @param session
	 * @param proxyServices
	 * @return
	 */
	public static Proxy randProxy(int attempt, Session session, List<String> proxyServices) {		
		LettProxy nextProxy = null;
		String serviceName = getProxyService(attempt, session, proxyServices);

		if (serviceName != null) {
			nextProxy = getNextProxy(serviceName, session);
		}

		// request using no proxy
		if (nextProxy == null) {
			return null;
		}

		final String nextProxyHost = nextProxy.getAddress();
		final int nextProxyPort = nextProxy.getPort();
		final String nextProxyUser = nextProxy.getUser();
		final String nextProxyPass = nextProxy.getPass();

		if (nextProxyUser != null) {
			Authenticator a = new Authenticator() {
				
				@Override
				public PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(nextProxyUser, nextProxyPass.toCharArray());
				}
			};

			Authenticator.setDefault(a);
		}

		return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(nextProxyHost, nextProxyPort));
	}

	/**
	 * 
	 * @param serviceName
	 * @param session
	 * @return
	 */
	public static LettProxy getNextProxy(String serviceName, Session session) {
		LettProxy nextProxy = null;

		if (session instanceof TestCrawlerSession || session instanceof TestRankingKeywordsSession) { // testing
			List<LettProxy> proxies = Test.proxies.getProxy(serviceName);
			if (!proxies.isEmpty()) {
				nextProxy = proxies.get( MathCommonsMethods.randInt(0, proxies.size()-1) );
			} else {
				Logging.printLogError(logger, session, "Error: using proxy service " + serviceName + ", but there was no proxy fetched for this service.");
			}
		}
		else {
			if (Main.proxies != null) { // production
				List<LettProxy> proxies = Main.proxies.getProxy(serviceName);
				if (!proxies.isEmpty()) {
					nextProxy = proxies.get( MathCommonsMethods.randInt(0, proxies.size()-1) );
				} else {
					Logging.printLogError(logger, session, "Error: using proxy service " + serviceName + ", but there was no proxy fetched for this service.");
				}
			}
		}

		return nextProxy;
	}

	/**
	 * Retrieve a random user agent from the user agents array.
	 * @return
	 */
	public static String randUserAgent() {
		return userAgents.get(MathCommonsMethods.randInt(0, userAgents.size() - 1));
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

		if (session instanceof TestCrawlerSession || session instanceof TestRankingKeywordsSession) {
			service = br.com.lett.crawlernode.test.Test.proxies.selectProxy(session.getMarket(), true, attempt);
		} else {
			if (session instanceof ImageCrawlerSession) {
				service = Main.proxies.selectProxy(session.getMarket(), false, attempt);
			} else {
				service = Main.proxies.selectProxy(session.getMarket(), true, attempt);
			}

		}

		Logging.printLogDebug(logger, session, "Selected proxy: " + service);

		return service;
	}

	/**
	 * Splits a cookie value and returns the second part.
	 * e.g:
	 * 
	 * ASP.NET_SessionId=vh2akqijsv0aqzbmn5qxxfbt;
	 * first part: ASP.NET_SessionId
	 * second part: vh2akqijsv0aqzbmn5qxxfbt
	 * 
	 * @param headerValue
	 */
	public static String splitHeaderValue(String headerValue) {
		int beginIndex = headerValue.indexOf('=') + 1;
		return headerValue.substring(beginIndex, headerValue.length()).trim();
	}

	public static String generateRequestHash(Session session) {
		String s = session.getSessionId() + new DateTime(DateConstants.timeZone).toString("yyyy-MM-dd HH:mm:ss.SSS");
		return DigestUtils.md5Hex(s);
	}

}
