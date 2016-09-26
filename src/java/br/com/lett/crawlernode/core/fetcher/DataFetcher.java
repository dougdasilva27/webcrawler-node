package br.com.lett.crawlernode.core.fetcher;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import br.com.lett.crawlernode.core.parser.Parser;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.server.S3Service;
import br.com.lett.crawlernode.test.Test;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;


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

	private static final int MAX_ATTEMPTS_FOR_CONECTION_WITH_PROXY = 10;
	private static final int MAX_ATTEMPTS_PER_PROXY = 2;

	private static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT = 10000; // ms
	private static final int DEFAULT_CONNECT_TIMEOUT = 10000; // ms
	private static final int DEFAULT_SOCKET_TIMEOUT = 10000; // ms

	/** Most popular agents, retrieved from https://techblog.willshouse.com/2012/01/03/most-common-user-agents/ */
	private static List<String> userAgents;

	private static List<String> errorCodes;

	/**
	 * Static initialization block
	 */
	static {		
		userAgents = Arrays.asList(
				"Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2",
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
				"Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; rv:11.0) like Gecko",
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
	public static String fetchString(String reqType, CrawlerSession session, String url, String urlParameters, List<Cookie> cookies) {
		return fetchPage(reqType, session, url, urlParameters, cookies, 1);	
	}

	/**
	 * 
	 * @param session
	 * @param localFileDir
	 * @return
	 * @throws IOException
	 */
	public static BufferedImage fetchImage(CrawlerSession session, String localFileDir) throws IOException {
		File imageFile = downloadImageFromMarket(1, session, localFileDir);
		if(imageFile != null) return ImageIO.read(imageFile);
		return null;
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
	public static Document fetchDocument(String reqType, CrawlerSession session, String url, String urlParameters, List<Cookie> cookies) {
		return Jsoup.parse(fetchPage(reqType, session, url, urlParameters, cookies, 1));	
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
	public static JSONObject fetchJSONObject(String reqType, CrawlerSession session, String url, String payload, List<Cookie> cookies) {
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
	public static JSONArray fetchJSONArray(String reqType, CrawlerSession session, String url, String payload, List<Cookie> cookies) {
		return new JSONArray(fetchJson(reqType, session, url, payload, cookies, 1));
	}

	/**
	 * Get the http response code of a URL
	 * 
	 * @param url 
	 * @param session
	 * @return The integer code. Null if we have an exception.
	 */
	public static Integer getUrlResponseCode(String url, CrawlerSession session) {
		return getUrlResponseCode(url, session, 1);
	}

	public static Integer getUrlResponseCode(String url, CrawlerSession session, int attempt) {
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

	private static String fetchJson(String reqType, CrawlerSession session, String url, String payload, List<Cookie> cookies, int attempt) {
		try {

			if (reqType.equals(GET_REQUEST)) {
				return fetchPageGET(session, url, cookies, attempt);
			} else if (reqType.equals(POST_REQUEST)) {
				if (payload != null) {
					return fetchJsonPOST(session, url, payload, cookies, attempt);
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

		return null;

	}

	private static String fetchJsonPOST(CrawlerSession session, String url, String payload, List<Cookie> cookies, int attempt) throws Exception {
		Logging.printLogDebug(logger, session, "Fazendo requisição POST com content-type JSON: " + url);

		String randUserAgent = randUserAgent();
		LettProxy randProxy = randLettProxy(attempt, session, session.getMarket().getProxies());

		String requestHash = generateRequestHash(session);

		CookieStore cookieStore = new BasicCookieStore();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				cookieStore.addCookie(cookie);
			}
		}

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


		Header header = new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json");
		List<Header> headers = new ArrayList<Header>();
		headers.add(header);

		CloseableHttpClient httpclient = HttpClients.custom()
				.setDefaultCookieStore(cookieStore)
				.setUserAgent(randUserAgent)
				.setDefaultRequestConfig(requestConfig)
				.setDefaultCredentialsProvider(credentialsProvider)
				.setDefaultHeaders(headers)
				.build();

		HttpContext localContext = new BasicHttpContext();
		localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

		HttpPost httpPost = new HttpPost(url);
		httpPost.setConfig(requestConfig);

		// if we are using charity engine, we must set header for authentication
		if (randProxy != null && randProxy.getSource().equals(Proxies.CHARITY)) {
			String authenticator = "ff548a45065c581adbb23bbf9253de9b" + ":";
			String headerValue = "Basic " + Base64.encodeBase64String(authenticator.getBytes());
			httpPost.addHeader("Proxy-Authorization", headerValue);

			// setting header for proxy country
			httpPost.addHeader("X-Proxy-Country", "BR");
		}


		if(payload != null) {

			JSONObject payloadJson = null;

			try {
				payloadJson = new JSONObject(payload);
			} catch (Exception e) {
				Logging.printLogError(logger, session, "Tentativa " + attempt + " -> Erro ao fazer requisição POST JSON, pois não consegui converter o payload em json: " + payload);
				Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
			}

			if(payloadJson != null && payloadJson.length() > 0) {
				ArrayList<NameValuePair> postParameters = new ArrayList<NameValuePair>();
				Iterator iterator = payloadJson.keySet().iterator();

				while(iterator.hasNext()) {
					String key = (String) iterator.next();
					postParameters.add(new BasicNameValuePair(key, payloadJson.get(key).toString()));
				}

				httpPost.setEntity(new UrlEncodedFormEntity(postParameters));
			}

		}

		CloseableHttpResponse closeableHttpResponse = httpclient.execute(httpPost, localContext);

		sendRequestInfoLog(url, POST_REQUEST, randProxy, session, closeableHttpResponse, requestHash);

		// analysing the status code
		// if there was some response code that indicates forbidden access or server error we want to try again
		int responseCode = closeableHttpResponse.getStatusLine().getStatusCode();
		if(Integer.toString(responseCode).charAt(0) != '2' && Integer.toString(responseCode).charAt(0) != '3' && responseCode != 404) { // errors
			throw new ResponseCodeException(responseCode);
		}

		BufferedReader in = new BufferedReader(new InputStreamReader(closeableHttpResponse.getEntity().getContent()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		// saving request content result on Amazon
		S3Service.uploadContentToAmazon(session, requestHash, response.toString());

		return response.toString();

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
			CrawlerSession session, 
			String url, 
			String urlParameters, 
			List<Cookie> cookies, 
			int attempt) {

		try {

			if (reqType.equals(GET_REQUEST)) {
				return fetchPageGET(session, url, cookies, attempt);
			} else if (reqType.equals(POST_REQUEST)) {
				if (urlParameters != null) {
					return fetchPagePOST(session, url, urlParameters, cookies, attempt);
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

	/**
	 * Fetch a page
	 * By default the redirects are enabled in the RequestConfig
	 * 
	 * @param session
	 * @param url
	 * @param cookies
	 * @param attempt
	 * @return
	 */
	private static String fetchPageGET(
			CrawlerSession session, 
			String url, 
			List<Cookie> cookies, 
			int attempt) {

		LettProxy randProxy = null;
		CloseableHttpResponse closeableHttpResponse = null;
		String requestHash = generateRequestHash(session);

		try {
			Logging.printLogDebug(logger, session, "Performing GET request: " + url);

			String randUserAgent = randUserAgent();
			randProxy = randLettProxy(attempt, session, session.getMarket().getProxies());

			CookieStore cookieStore = new BasicCookieStore();
			if (cookies != null) {
				if (cookies.size() > 0) {
					for (Cookie cookie : cookies) {
						cookieStore.addCookie(cookie);
					}
				}
			}

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

			CloseableHttpClient httpclient = HttpClients.custom()
					.setDefaultCookieStore(cookieStore)
					.setUserAgent(randUserAgent)
					.setDefaultRequestConfig(requestConfig)
					.setDefaultCredentialsProvider(credentialsProvider)
					.build();

			HttpContext localContext = new BasicHttpContext();
			localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

			HttpGet httpGet = new HttpGet(url);
			httpGet.setConfig(requestConfig);

			// if we are using charity engine, we must set header for authentication
			if (randProxy != null && randProxy.getSource().equals(Proxies.CHARITY)) {
				String authenticator = "ff548a45065c581adbb23bbf9253de9b" + ":";
				String headerValue = "Basic " + Base64.encodeBase64String(authenticator.getBytes());
				httpGet.addHeader("Proxy-Authorization", headerValue);

				// setting header for proxy country
				httpGet.addHeader("X-Proxy-Country", "BR");
			}

			// do request
			closeableHttpResponse = httpclient.execute(httpGet, localContext);

			// analysing the status code
			// if there was some response code that indicates forbidden access or server error we want to try again
			int responseCode = closeableHttpResponse.getStatusLine().getStatusCode();
			if(Integer.toString(responseCode).charAt(0) != '2' && Integer.toString(responseCode).charAt(0) != '3' && responseCode != 404) { // errors
				throw new ResponseCodeException(responseCode);
			}

			// creating the page content result from the http request
			PageContent pageContent = new PageContent(closeableHttpResponse.getEntity());		// loading information from http entity
			pageContent.setStatusCode(closeableHttpResponse.getStatusLine().getStatusCode());	// geting the status code
			pageContent.setUrl(url); // setting url

			// assembling request information log message
			sendRequestInfoLog(url, GET_REQUEST, randProxy, session, closeableHttpResponse, requestHash);

			// saving request content result on Amazon
			String content = "";
			if (pageContent.getContentCharset() == null) {
				content = new String(pageContent.getContentData());
			} else {
				content = new String(pageContent.getContentData(), pageContent.getContentCharset());
			}
			S3Service.uploadContentToAmazon(session, requestHash, content);

			// see if some code error occured
			// sometimes the remote server doesn't send the http error code on the headers
			// but rater on the page bytes
			content = content.trim();
			for (String errorCode : errorCodes) {
				if (content.equals(errorCode)) {
					throw new ResponseCodeException(Integer.parseInt(errorCode));
				}
			}

			// process response and parse
			return processContent(pageContent, session);

		} catch (Exception e) {
			sendRequestInfoLog(url, GET_REQUEST, randProxy, session, closeableHttpResponse, requestHash);

			Logging.printLogError(logger, session, "Tentativa " + attempt + " -> Erro ao fazer requisição GET: " + url);
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));

			if(attempt >= MAX_ATTEMPTS_FOR_CONECTION_WITH_PROXY) {
				Logging.printLogError(logger, session, "Reached maximum attempts for URL [" + url + "]");
				return "";
			} else {
				return fetchPageGET(session, url, cookies, attempt+1);	
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
			CrawlerSession session, 
			String url,
			String cookieName,
			List<Cookie> cookies, 
			int attempt) {

		LettProxy randProxy = null;
		CloseableHttpResponse closeableHttpResponse = null;
		String requestHash = generateRequestHash(session);

		try {
			Logging.printLogDebug(logger, session, "Performing GET request to fetch cookie: " + url);

			String randUserAgent = randUserAgent();
			randProxy = randLettProxy(attempt, session, session.getMarket().getProxies());

			CookieStore cookieStore = new BasicCookieStore();
			if (cookies != null) {
				if (cookies.size() > 0) {
					for (Cookie cookie : cookies) {
						cookieStore.addCookie(cookie);
					}
				}
			}

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

			CloseableHttpClient httpclient = HttpClients.custom()
					.setDefaultCookieStore(cookieStore)
					.setUserAgent(randUserAgent)
					.setDefaultRequestConfig(requestConfig)
					.setDefaultCredentialsProvider(credentialsProvider)
					.build();

			HttpContext localContext = new BasicHttpContext();
			localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

			HttpGet httpGet = new HttpGet(url);
			httpGet.setConfig(requestConfig);

			// if we are using charity engine, we must set header for authentication
			if (randProxy != null && randProxy.getSource().equals(Proxies.CHARITY)) {
				String authenticator = "ff548a45065c581adbb23bbf9253de9b" + ":";
				String headerValue = "Basic " + Base64.encodeBase64String(authenticator.getBytes());
				httpGet.addHeader("Proxy-Authorization", headerValue);

				// setting header for proxy country
				httpGet.addHeader("X-Proxy-Country", "BR");
			}

			// do request
			closeableHttpResponse = httpclient.execute(httpGet, localContext);

			// analysing the status code
			// if there was some response code that indicates forbidden access or server error we want to try again
			int responseCode = closeableHttpResponse.getStatusLine().getStatusCode();
			if(Integer.toString(responseCode).charAt(0) != '2' && Integer.toString(responseCode).charAt(0) != '3' && responseCode != 404) { // errors
				throw new ResponseCodeException(responseCode);
			}

			// creating the page content result from the http request
			PageContent pageContent = new PageContent(closeableHttpResponse.getEntity());		// loading information from http entity
			pageContent.setStatusCode(closeableHttpResponse.getStatusLine().getStatusCode());	// geting the status code
			pageContent.setUrl(url); // setting url

			// assembling request information log message
			sendRequestInfoLog(url, GET_REQUEST, randProxy, session, closeableHttpResponse, requestHash);

			// saving request content result on Amazon
			String content = "";
			if (pageContent.getContentCharset() == null) {
				content = new String(pageContent.getContentData());
			} else {
				content = new String(pageContent.getContentData(), pageContent.getContentCharset());
			}
			S3Service.uploadContentToAmazon(session, requestHash, content);

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
			sendRequestInfoLog(url, GET_REQUEST, randProxy, session, closeableHttpResponse, requestHash);

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
	 * @param session
	 * @param url
	 * @param urlParameters
	 * @param cookies
	 * @param attempt
	 * @return
	 */
	private static String fetchPagePOST(CrawlerSession session, String url, String urlParameters, List<Cookie> cookies, int attempt) {
		LettProxy randProxy = null;

		CloseableHttpResponse closeableHttpResponse = null;
		String requestHash = generateRequestHash(session);

		try {
			Logging.printLogDebug(logger, session, "Performing POST request: " + url);

			String randUserAgent = randUserAgent();
			randProxy = randLettProxy(attempt, session, session.getMarket().getProxies());

			CookieStore cookieStore = new BasicCookieStore();
			if (cookies != null) {
				if (cookies.size() > 0) {
					for (Cookie cookie : cookies) {
						cookieStore.addCookie(cookie);
					}
				}
			}

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
						.setRedirectsEnabled(false)
						.setConnectionRequestTimeout(DEFAULT_CONNECTION_REQUEST_TIMEOUT)
						.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT)
						.setSocketTimeout(DEFAULT_SOCKET_TIMEOUT)
						.setProxy(proxy)
						.build();
			} else {
				requestConfig = RequestConfig.custom()
						.setCookieSpec(CookieSpecs.STANDARD)
						.setRedirectsEnabled(false)
						.setConnectionRequestTimeout(DEFAULT_CONNECTION_REQUEST_TIMEOUT)
						.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT)
						.setSocketTimeout(DEFAULT_SOCKET_TIMEOUT)
						.build();
			}

			CloseableHttpClient httpclient = HttpClients.custom()
					.setDefaultCookieStore(cookieStore)
					.setUserAgent(randUserAgent)
					.setDefaultRequestConfig(requestConfig)
					.setDefaultCredentialsProvider(credentialsProvider)
					.build();

			HttpContext localContext = new BasicHttpContext();
			localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

			HttpPost httpPost = new HttpPost(url);
			httpPost.setConfig(requestConfig);

			// if we are using charity engine, we must set header for authentication
			if (randProxy != null && randProxy.getSource().equals(Proxies.CHARITY)) {
				String authenticator = "ff548a45065c581adbb23bbf9253de9b" + ":";
				String headerValue = "Basic " + Base64.encodeBase64String(authenticator.getBytes());
				httpPost.addHeader("Proxy-Authorization", headerValue);

				// setting header for proxy country
				httpPost.addHeader("X-Proxy-Country", "BR");
			}

			if(urlParameters != null && urlParameters.split("&").length > 0) {
				ArrayList<NameValuePair> postParameters = new ArrayList<NameValuePair>();
				String[] urlParametersSplitted = urlParameters.split("&");

				for(String p: urlParametersSplitted) {
					if(p.split("=").length > 1) {
						postParameters.add(new BasicNameValuePair(p.split("=")[0], p.split("=", 2)[1]));
					}
				}

				httpPost.setEntity(new UrlEncodedFormEntity(postParameters));

			}

			// do request
			closeableHttpResponse = httpclient.execute(httpPost, localContext);

			// analysing the status code
			// if there was some response code that indicates forbidden access or server error we want to try again
			int responseCode = closeableHttpResponse.getStatusLine().getStatusCode();
			if(Integer.toString(responseCode).charAt(0) != '2' && Integer.toString(responseCode).charAt(0) != '3' && responseCode != 404) { // errors
				throw new ResponseCodeException(responseCode);
			}

			// creating the page content result from the http request
			PageContent pageContent = new PageContent(closeableHttpResponse.getEntity());		// loading information from http entity
			pageContent.setStatusCode(closeableHttpResponse.getStatusLine().getStatusCode());	// geting the status code
			pageContent.setUrl(url); // setting url

			// assembling request information log message
			sendRequestInfoLog(url, POST_REQUEST, randProxy, session, closeableHttpResponse, requestHash);

			// saving request content result on Amazon
			String content = "";
			if (pageContent.getContentCharset() == null) {
				content = new String(pageContent.getContentData());
			} else {
				content = new String(pageContent.getContentData(), pageContent.getContentCharset());
			}
			S3Service.uploadContentToAmazon(session, requestHash, content);

			// see if some code error occured
			// sometimes the remote server doesn't send the http error code on the headers
			// but rater on the page bytes
			content = content.trim();
			for (String errorCode : errorCodes) {
				if (content.equals(errorCode)) {
					throw new ResponseCodeException(Integer.parseInt(errorCode));
				}
			}

			// process response and parse
			return processContent(pageContent, session);

		} catch (Exception e) {
			sendRequestInfoLog(url, POST_REQUEST, randProxy, session, closeableHttpResponse, requestHash);

			Logging.printLogError(logger, session, "Attempt " + attempt + " -> Error in POST request: " + url);
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));

			if(attempt >= MAX_ATTEMPTS_FOR_CONECTION_WITH_PROXY) {
				Logging.printLogError(logger, session, "Reached maximum attempts for URL [" + url + "]");
				return "";
			} else {
				return fetchPagePOST(session, url, urlParameters, cookies, attempt+1);	
			}

		}
	}

	public static String fetchPagePOSTWithHeaders(
			String url, 
			CrawlerSession session, 
			String urlParameters, 
			List<Cookie> cookies, 
			int attempt, 
			Map<String,String> headers) {

		LettProxy randProxy = null;
		CloseableHttpResponse closeableHttpResponse = null;
		String requestHash = generateRequestHash(session);

		try {

			Logging.printLogDebug(logger, session, "Performing POST request: " + url);

			String randUserAgent = randUserAgent();
			randProxy = randLettProxy(attempt, session, session.getMarket().getProxies());

			CookieStore cookieStore = new BasicCookieStore();
			if (cookies != null) {
				for (Cookie cookie : cookies) {
					cookieStore.addCookie(cookie);
				}
			}

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

			CloseableHttpClient httpclient = HttpClients.custom()
					.setDefaultCookieStore(cookieStore)
					.setUserAgent(randUserAgent)
					.setDefaultRequestConfig(requestConfig)
					.setDefaultCredentialsProvider(credentialsProvider)
					.build();

			HttpContext localContext = new BasicHttpContext();
			localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

			StringEntity input = new StringEntity(urlParameters);
			input.setContentType(headers.get("Content-Type"));

			HttpPost httpPost = new HttpPost(url);
			httpPost.setEntity(input);

			// if we are using charity engine, we must set header for authentication
			if (randProxy != null && randProxy.getSource().equals(Proxies.CHARITY)) {
				String authenticator = "ff548a45065c581adbb23bbf9253de9b" + ":";
				String headerValue = "Basic " + Base64.encodeBase64String(authenticator.getBytes());
				httpPost.addHeader("Proxy-Authorization", headerValue);

				// setting header for proxy country
				httpPost.addHeader("X-Proxy-Country", "BR");
			}

			for(String key : headers.keySet()){
				httpPost.addHeader(key, headers.get(key));
			}

			httpPost.setConfig(requestConfig);

			// do request
			closeableHttpResponse = httpclient.execute(httpPost, localContext);

			// analysing the status code
			// if there was some response code that indicates forbidden access or server error we want to try again
			int responseCode = closeableHttpResponse.getStatusLine().getStatusCode();
			if(Integer.toString(responseCode).charAt(0) != '2' && Integer.toString(responseCode).charAt(0) != '3' && responseCode != 404) { // errors
				throw new ResponseCodeException(responseCode);
			}

			// creating the page content result from the http request
			PageContent pageContent = new PageContent(closeableHttpResponse.getEntity());		// loading information from http entity
			pageContent.setStatusCode(closeableHttpResponse.getStatusLine().getStatusCode());	// geting the status code
			pageContent.setUrl(url); // setting url

			// assembling request information log message
			sendRequestInfoLog(url, POST_REQUEST, randProxy, session, closeableHttpResponse, requestHash);

			// saving request content result on Amazon
			String content = "";
			if (pageContent.getContentCharset() == null) {
				content = new String(pageContent.getContentData());
			} else {
				content = new String(pageContent.getContentData(), pageContent.getContentCharset());
			}
			S3Service.uploadContentToAmazon(session, requestHash, content);

			// see if some code error occured
			// sometimes the remote server doesn't send the http error code on the headers
			// but rater on the page bytes
			content = content.trim();
			for (String errorCode : errorCodes) {
				if (content.equals(errorCode)) {
					throw new ResponseCodeException(Integer.parseInt(errorCode));
				}
			}

			// process response and parse
			return processContent(pageContent, session);

		} catch (Exception e) {
			sendRequestInfoLog(url, POST_REQUEST, randProxy, session, closeableHttpResponse, requestHash);

			Logging.printLogError(logger, session, "Attempt " + attempt + " -> Error in POST request for URL: " + url);
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));

			if(attempt >= MAX_ATTEMPTS_FOR_CONECTION_WITH_PROXY) {
				Logging.printLogError(logger, session, "Reached maximum attempts for URL [" + url + "]");
				return "";
			} else {
				return fetchPagePOSTWithHeaders(url, session, urlParameters, cookies, attempt+1, headers);	
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
	private static void sendRequestInfoLog(
			String url, 
			String requestType, 
			LettProxy proxy, 
			CrawlerSession session, 
			CloseableHttpResponse response,
			String requestHash) {

		JSONObject request_metadata = new JSONObject();

		request_metadata.put("req_hash", requestHash);
		request_metadata.put("proxy_name", 	(proxy == null ? Proxies.NO_PROXY 		: proxy.getSource()));
		request_metadata.put("proxy_ip", 	(proxy == null ? MDC.get("HOST_NAME") 	: proxy.getAddress()));
		request_metadata.put("req_method", requestType);
		request_metadata.put("req_location", url);
		request_metadata.put("res_http_code", (response == null) ? 0 : response.getStatusLine().getStatusCode());
		request_metadata.put("res_length", (response == null) ? 0 : response.getEntity().getContentLength());

		Logging.printLogDebug(logger, session, request_metadata, "Registrando requisição...");

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
	private static String processContent(PageContent pageContent, CrawlerSession session) {		
		Parser parser = new Parser(session);
		parser.parse(pageContent);

		if (pageContent.getHtmlParseData() != null) return pageContent.getHtmlParseData().getHtml();
		if (pageContent.getTextParseData() != null) return pageContent.getTextParseData().getTextContent();

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
			CrawlerSession session, 
			String localFileDir) {

		LettProxy randProxy = null;
		CloseableHttpResponse closeableHttpResponse = null;
		String requestHash = generateRequestHash(session);

		try {

			// choosing the preferred proxy service
			String randUserAgent = randUserAgent();
			randProxy = randLettProxy(attempt, session, session.getMarket().getProxies());

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

			CloseableHttpClient httpclient = HttpClients.custom()
					.setDefaultCookieStore(cookieStore)
					.setUserAgent(randUserAgent)
					.setDefaultRequestConfig(requestConfig)
					.setDefaultCredentialsProvider(credentialsProvider)
					.build();

			HttpContext localContext = new BasicHttpContext();
			localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

			HttpGet httpGet = new HttpGet(session.getUrl());
			httpGet.setConfig(requestConfig);

			// if we are using charity engine, we must set header for authentication
			if (randProxy != null && randProxy.getSource().equals(Proxies.CHARITY)) {
				String authenticator = "ff548a45065c581adbb23bbf9253de9b" + ":";
				String headerValue = "Basic " + Base64.encodeBase64String(authenticator.getBytes());
				httpGet.addHeader("Proxy-Authorization", headerValue);

				// setting header for proxy country
				httpGet.addHeader("X-Proxy-Country", "BR");
			}

			// do request
			closeableHttpResponse = httpclient.execute(httpGet, localContext);

			// analysing the status code
			// if there was some response code that indicates forbidden access or server error we want to try again
			int responseCode = closeableHttpResponse.getStatusLine().getStatusCode();
			if(Integer.toString(responseCode).charAt(0) != '2' && Integer.toString(responseCode).charAt(0) != '3' && responseCode != 404) { // errors
				throw new ResponseCodeException(responseCode);
			}

			// assembling request information log message
			sendRequestInfoLog(session.getUrl(), GET_REQUEST, randProxy, session, closeableHttpResponse, requestHash);

			File localFile = new File(localFileDir);

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

			Logging.printLogDebug(logger, session, " Download OK!");

			return localFile;

		} catch (Exception e) {			

			sendRequestInfoLog(session.getUrl(), GET_REQUEST, randProxy, session, closeableHttpResponse, requestHash);

			Logging.printLogError(logger, session, "Tentativa " + attempt + " -> Erro ao fazer requisição GET para download de imagem: " + session.getUrl());
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));

			if(attempt >= MAX_ATTEMPTS_FOR_CONECTION_WITH_PROXY) {
				Logging.printLogError(logger, session, "Reached maximum attempts for URL [" + session.getUrl() + "]");
				return null;
			} else {
				return downloadImageFromMarket(attempt + 1, session, localFileDir);
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
	private static LettProxy randLettProxy(int attempt, CrawlerSession session, ArrayList<String> proxyServices) {
		LettProxy nextProxy = null;
		String serviceName = getProxyService(attempt, proxyServices);

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
	private static Proxy randProxy(int attempt, CrawlerSession session, ArrayList<String> proxyServices) {		
		LettProxy nextProxy = null;
		String serviceName = getProxyService(attempt, proxyServices);

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
	private static LettProxy getNextProxy(String serviceName, CrawlerSession session) {
		LettProxy nextProxy = null;

		// when not testing
		if (Main.proxies != null) {
			if (serviceName.equals(Proxies.BONANZA)) { // bonanza
				if (Main.proxies.bonanzaProxies.size() > 0) {
					nextProxy = Main.proxies.bonanzaProxies.get(CommonMethods.randInt(0, Main.proxies.bonanzaProxies.size() - 1));
				} else {
					Logging.printLogError(logger, session, "Error: using proxy service " + Proxies.BONANZA + ", but there was no proxy fetched for this service.");
				}
			} 
			else if (serviceName.equals(Proxies.BUY)) { // buy
				if (Main.proxies.buyProxies.size() > 0) {
					nextProxy = Main.proxies.buyProxies.get(CommonMethods.randInt(0, Main.proxies.buyProxies.size() - 1));
				} else {
					Logging.printLogError(logger, session, "Error: using proxy service " + Proxies.BUY + ", but there was no proxy fetched for this service.");
				}
			}
			else if (serviceName.equals(Proxies.SHADER)) { // shader
				if (Main.proxies.shaderProxies.size() > 0) {
					nextProxy = Main.proxies.shaderProxies.get(CommonMethods.randInt(0, Main.proxies.shaderProxies.size() - 1));
				} else {
					Logging.printLogError(logger, session, "Error: using proxy service " + Proxies.SHADER + ", but there was no proxy fetched for this service.");
				}
			}
			else if (serviceName.equals(Proxies.STORM)) { // storm
				if (Main.proxies.storm.size() > 0) {
					nextProxy = Main.proxies.storm.get(CommonMethods.randInt(0, Main.proxies.storm.size() - 1));
				} else {
					Logging.printLogError(logger, session, "Error: using proxy service " + Proxies.STORM + ", but there was no proxy fetched for this service.");
				}
			}
			else if (serviceName.equals(Proxies.CHARITY)) { // charity
				if (Main.proxies.charity.size() > 0) {
					nextProxy = Main.proxies.charity.get(CommonMethods.randInt(0, Main.proxies.charity.size() - 1));
				} else {
					Logging.printLogError(logger, session, "Error: using proxy service " + Proxies.CHARITY + ", but there was no proxy fetched for this service.");
				}
			}
		}

		// when testing
		else {
			if (serviceName.equals(Proxies.BONANZA)) { // bonanza
				if (Test.proxies.bonanzaProxies.size() > 0) {
					nextProxy = Test.proxies.bonanzaProxies.get(CommonMethods.randInt(0, Test.proxies.bonanzaProxies.size() - 1));
				} else {
					Logging.printLogError(logger, session, "Error: using proxy service " + Proxies.BONANZA + ", but there was no proxy fetched for this service.");
				}
			} 
			else if (serviceName.equals(Proxies.BUY)) { // buy
				if (Test.proxies.buyProxies.size() > 0) {
					nextProxy = Test.proxies.buyProxies.get(CommonMethods.randInt(0, Test.proxies.buyProxies.size() - 1));
				} else {
					Logging.printLogError(logger, session, "Error: using proxy service " + Proxies.BUY + ", but there was no proxy fetched for this service.");
				}
			}
			else if (serviceName.equals(Proxies.SHADER)) { // shader
				if (Test.proxies.shaderProxies.size() > 0) {
					nextProxy = Test.proxies.shaderProxies.get(CommonMethods.randInt(0, Test.proxies.shaderProxies.size() - 1));
				} else {
					Logging.printLogError(logger, session, "Error: using proxy service " + Proxies.SHADER + ", but there was no proxy fetched for this service.");
				}
			}
			else if (serviceName.equals(Proxies.STORM)) { // storm
				if (Test.proxies.storm.size() > 0) {
					nextProxy = Test.proxies.storm.get(CommonMethods.randInt(0, Test.proxies.storm.size() - 1));
				} else {
					Logging.printLogError(logger, session, "Error: using proxy service " + Proxies.STORM + ", but there was no proxy fetched for this service.");
				}
			}
			else if (serviceName.equals(Proxies.CHARITY)) { // charity
				if (Test.proxies.charity.size() > 0) {
					nextProxy = Test.proxies.charity.get(CommonMethods.randInt(0, Test.proxies.charity.size() - 1));
				} else {
					Logging.printLogError(logger, session, "Error: using proxy service " + Proxies.CHARITY + ", but there was no proxy fetched for this service.");
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
		return userAgents.get(CommonMethods.randInt(0, userAgents.size() - 1));
	}

	private static String getProxyService(int attempt, ArrayList<String> proxyServices) {
		String service = null;

		if (proxyServices == null || proxyServices.size() == 0) { // there is no proxy...this should not happen...for no proxy we still must have a string in the ArrayList
			service = Proxies.NO_PROXY;
		}
		else if (attempt <= MAX_ATTEMPTS_PER_PROXY) { // first interval of attempts...the first proxy service on the list
			service = proxyServices.get(0);
		}
		else if (attempt > MAX_ATTEMPTS_PER_PROXY && attempt <= MAX_ATTEMPTS_PER_PROXY*2) { // second interval of attempts
			if (proxyServices.size() > 1) {
				service = proxyServices.get(1);
			}
		}
		else if (attempt > MAX_ATTEMPTS_PER_PROXY*2 && attempt <= MAX_ATTEMPTS_PER_PROXY*3) { // third interval of attempts
			if (proxyServices.size() > 2) {
				service = proxyServices.get(2);
			}
		}
		else if (attempt > MAX_ATTEMPTS_PER_PROXY*3 && attempt <= MAX_ATTEMPTS_PER_PROXY*4) { // fourth interval of attempts
			if (proxyServices.size() > 3) {
				service = proxyServices.get(3);
			}
		}
		else if (attempt > MAX_ATTEMPTS_PER_PROXY*4 && attempt <= MAX_ATTEMPTS_PER_PROXY*5) { // fourth interval of attempts
			if (proxyServices.size() > 4) {
				service = proxyServices.get(4);
			}
		}
		else {
			service = Proxies.NO_PROXY;
		}

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

	private static String generateRequestHash(CrawlerSession session) {
		String s = session.getSessionId() + new DateTime(DateTimeZone.forID("America/Sao_Paulo")).toString("yyyy-MM-dd HH:mm:ss.SSS");
		return DigestUtils.md5Hex(s);
	}

}
