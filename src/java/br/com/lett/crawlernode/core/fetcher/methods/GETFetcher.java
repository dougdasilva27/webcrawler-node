package br.com.lett.crawlernode.core.fetcher.methods;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.codec.binary.Base64;
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

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.DataFetcherRedirectStrategy;
import br.com.lett.crawlernode.core.fetcher.HostNameVerifier;
import br.com.lett.crawlernode.core.fetcher.LettProxy;
import br.com.lett.crawlernode.core.fetcher.PageContent;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.exceptions.ResponseCodeException;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.queue.S3Service;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

public class GETFetcher {
	
	protected static final Logger logger = LoggerFactory.getLogger(GETFetcher.class);
	
	private GETFetcher() {
		super();
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
	public static String fetchPageGET(
			Session session, 
			String url, 
			List<Cookie> cookies, 
			int attempt) {
		
		
		LettProxy randProxy = null;
		String randUserAgent = null;
		CloseableHttpResponse closeableHttpResponse = null;
		int responseLength = 0;
		String requestHash = DataFetcher.generateRequestHash(session);

		try {
			Logging.printLogDebug(logger, session, "Performing GET request: " + url);
			
			// Request via fetcher on first attempt
			if(attempt == 1&& Main.USING_FETCHER) {
				Map<String,String> headers = new HashMap<>();
				
				if(cookies != null && !cookies.isEmpty()) {
					StringBuilder cookiesHeader = new StringBuilder();
					
					for(Cookie c : cookies) {
						cookiesHeader.append(c.getName() + "=" + c.getValue() + ";");
					}
					
					headers.put("Cookie", cookiesHeader.toString());
				}
				
				JSONObject payload = POSTFetcher.fetcherPayloadBuilder(url, "GET", false, null, headers, null);
				JSONObject response = POSTFetcher.requestWithFetcher(session, payload);
				
				return response.getJSONObject("response").getString("body");
			}
			
			randUserAgent = DataFetcher.randUserAgent();
			randProxy = DataFetcher.randLettProxy(attempt, session, session.getMarket().getProxies());
			
			CookieStore cookieStore = DataFetcher.createCookieStore(cookies);

			CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

			if (randProxy != null) {
				session.addRequestProxy(url, randProxy);
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
				

				if (session.getMarket().getName() != null && session.getMarket().getName().equals("bemol")) {
					requestConfig = RequestConfig.custom()
							.setCookieSpec(CookieSpecs.STANDARD)
							.setRedirectsEnabled(true) // set redirect to true
							.setConnectionRequestTimeout(DataFetcher.BEMOL_TIMEOUT)
							.setConnectTimeout(DataFetcher.BEMOL_TIMEOUT)
							.setSocketTimeout(DataFetcher.BEMOL_TIMEOUT)
							.setProxy(proxy)
							.build();
				} else {
					requestConfig = RequestConfig.custom()
							.setCookieSpec(CookieSpecs.STANDARD)
							.setRedirectsEnabled(true) // set redirect to true
							.setConnectionRequestTimeout(DataFetcher.DEFAULT_CONNECTION_REQUEST_TIMEOUT)
							.setConnectTimeout(DataFetcher.DEFAULT_CONNECT_TIMEOUT)
							.setSocketTimeout(DataFetcher.DEFAULT_SOCKET_TIMEOUT)
							.setProxy(proxy)
							.build();
				}

			} else {

				if (session.getMarket().getName() != null && session.getMarket().getName().equals("bemol")) {
					requestConfig = RequestConfig.custom()
							.setCookieSpec(CookieSpecs.STANDARD)
							.setRedirectsEnabled(true) // set redirect to true
							.setConnectionRequestTimeout(DataFetcher.BEMOL_TIMEOUT)
							.setConnectTimeout(DataFetcher.BEMOL_TIMEOUT)
							.setSocketTimeout(DataFetcher.BEMOL_TIMEOUT)
							.build();
				} else {
					requestConfig = RequestConfig.custom()
							.setCookieSpec(CookieSpecs.STANDARD)
							.setRedirectsEnabled(true) // set redirect to true
							.setConnectionRequestTimeout(DataFetcher.DEFAULT_CONNECTION_REQUEST_TIMEOUT)
							.setConnectTimeout(DataFetcher.DEFAULT_CONNECT_TIMEOUT)
							.setSocketTimeout(DataFetcher.DEFAULT_SOCKET_TIMEOUT)
							.build();
				}
			}

			// creating the redirect strategy
			// so we can get the final redirected URL
			DataFetcherRedirectStrategy redirectStrategy = new DataFetcherRedirectStrategy();
			
			List<Header> headers = new ArrayList<>();
			headers.add(new BasicHeader(HttpHeaders.CONTENT_ENCODING, DataFetcher.CONTENT_ENCODING));

			CloseableHttpClient httpclient = HttpClients.custom()
					.setDefaultCookieStore(cookieStore)
					.setUserAgent(randUserAgent)
					.setDefaultRequestConfig(requestConfig)
					.setRedirectStrategy(redirectStrategy)
					.setDefaultCredentialsProvider(credentialsProvider)
					.setDefaultHeaders(headers)
					.setSSLSocketFactory(DataFetcher.createSSLConnectionSocketFactory())
					.setSSLHostnameVerifier(new HostNameVerifier())
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
			
			responseLength = pageContent.getContentData().length;

			// assembling request information log message
			DataFetcher.sendRequestInfoLog(
					url, 
					DataFetcher.GET_REQUEST, 
					randProxy, 
					randUserAgent, 
					session, 
					closeableHttpResponse,
					responseLength,
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
			DataFetcher.sendRequestInfoLog(
					url, 
					DataFetcher.GET_REQUEST, 
					randProxy, 
					randUserAgent, 
					session, 
					closeableHttpResponse,
					responseLength,
					requestHash);

			if (e instanceof ResponseCodeException) {
				Logging.printLogWarn(logger, session, "Tentativa " + attempt + " -> Erro ao fazer requisição GET: " + session.getOriginalURL());
				Logging.printLogWarn(logger, session, CommonMethods.getStackTraceString(e));
			}
			else {
				Logging.printLogError(logger, session, "Tentativa " + attempt + " -> Erro ao fazer requisição GET: " + session.getOriginalURL());
				Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
			}

			if(attempt >= session.getMaxConnectionAttemptsCrawler()) {
				Logging.printLogError(logger, session, "Reached maximum attempts for URL [" + url + "]");
				return "";
			} else {
				return fetchPageGET(session, url, cookies, attempt+1);	
			}

		}
	}
	
	public static String fetchPageGETWithHeaders(
			Session session, 
			String url, 
			List<Cookie> cookies,
			Map<String, String> headers,
			int attempt) {

		LettProxy randProxy = null;
		String randUserAgent = null;
		CloseableHttpResponse closeableHttpResponse = null;
		int responseLength = 0;
		String requestHash = DataFetcher.generateRequestHash(session);

		try {
			Logging.printLogDebug(logger, session, "Performing GET request: " + url);

			// Request via fetcher on first attempt
			if(attempt == 1 && Main.USING_FETCHER) {				
				if(cookies != null && !cookies.isEmpty()) {
					StringBuilder cookiesHeader = new StringBuilder();
					
					for(Cookie c : cookies) {
						cookiesHeader.append(c.getName() + "=" + c.getValue() + ";");
					}
					
					headers.put("Cookie", cookiesHeader.toString());
				}
				
				JSONObject payload = POSTFetcher.fetcherPayloadBuilder(url, "GET", false, null, headers, null);
				JSONObject response = POSTFetcher.requestWithFetcher(session, payload);
				
				return response.getJSONObject("response").getString("body");
			}
			
			randUserAgent = DataFetcher.randUserAgent();
			randProxy = DataFetcher.randLettProxy(attempt, session, session.getMarket().getProxies());

			CookieStore cookieStore = DataFetcher.createCookieStore(cookies);

			CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

			if (randProxy != null) {
				session.addRequestProxy(url, randProxy);
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

				if (session.getMarket().getName() != null && session.getMarket().getName().equals("bemol")) {
					requestConfig = RequestConfig.custom()
							.setCookieSpec(CookieSpecs.STANDARD)
							.setRedirectsEnabled(true) // set redirect to true
							.setConnectionRequestTimeout(5000)
							.setConnectTimeout(5000)
							.setSocketTimeout(5000)
							.setProxy(proxy)
							.build();
				} else {
					requestConfig = RequestConfig.custom()
							.setCookieSpec(CookieSpecs.STANDARD)
							.setRedirectsEnabled(true) // set redirect to true
							.setConnectionRequestTimeout(DataFetcher.DEFAULT_CONNECTION_REQUEST_TIMEOUT)
							.setConnectTimeout(DataFetcher.DEFAULT_CONNECT_TIMEOUT)
							.setSocketTimeout(DataFetcher.DEFAULT_SOCKET_TIMEOUT)
							.setProxy(proxy)
							.build();
				}

			} else {

				if (session.getMarket().getName() != null && session.getMarket().getName().equals("bemol")) {
					requestConfig = RequestConfig.custom()
							.setCookieSpec(CookieSpecs.STANDARD)
							.setRedirectsEnabled(true) // set redirect to true
							.setConnectionRequestTimeout(5000)
							.setConnectTimeout(5000)
							.setSocketTimeout(5000)
							.build();
				} else {
					requestConfig = RequestConfig.custom()
							.setCookieSpec(CookieSpecs.STANDARD)
							.setRedirectsEnabled(true) // set redirect to true
							.setConnectionRequestTimeout(DataFetcher.DEFAULT_CONNECTION_REQUEST_TIMEOUT)
							.setConnectTimeout(DataFetcher.DEFAULT_CONNECT_TIMEOUT)
							.setSocketTimeout(DataFetcher.DEFAULT_SOCKET_TIMEOUT)
							.build();
				}
			}

			// creating the redirect strategy
			// so we can get the final redirected URL
			DataFetcherRedirectStrategy redirectStrategy = new DataFetcherRedirectStrategy();

			List<Header> headerList = new ArrayList<>();
			headerList.add(new BasicHeader(HttpHeaders.CONTENT_ENCODING, DataFetcher.CONTENT_ENCODING));

			for (Entry<String, String> mapEntry : headers.entrySet()) {
				if ("Accept".equals(mapEntry.getKey()) || "Content-Type".equals(mapEntry.getKey())) {
					headerList.add(new BasicHeader(mapEntry.getKey(), mapEntry.getValue()));
				}
			}

			CloseableHttpClient httpclient = HttpClients.custom()
					.setDefaultCookieStore(cookieStore)
					.setUserAgent(randUserAgent)
					.setDefaultRequestConfig(requestConfig)
					.setRedirectStrategy(redirectStrategy)
					.setDefaultCredentialsProvider(credentialsProvider)
					.setDefaultHeaders(headerList)
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
			
			responseLength = pageContent.getContentData().length;

			// assembling request information log message
			DataFetcher.sendRequestInfoLog(
					url, 
					DataFetcher.GET_REQUEST, 
					randProxy, 
					randUserAgent, 
					session, 
					closeableHttpResponse,
					responseLength,
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
			DataFetcher.sendRequestInfoLog(
					url, 
					DataFetcher.GET_REQUEST, 
					randProxy, 
					randUserAgent, 
					session, 
					closeableHttpResponse,
					responseLength,
					requestHash);

			if (e instanceof ResponseCodeException) {
				Logging.printLogWarn(logger, session, "Tentativa " + attempt + " -> Erro ao fazer requisição GET: " + session.getOriginalURL());
				Logging.printLogWarn(logger, session, CommonMethods.getStackTraceString(e));
			}
			else {
				Logging.printLogError(logger, session, "Tentativa " + attempt + " -> Erro ao fazer requisição GET: " + session.getOriginalURL());
				Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
			}

			if(attempt >= session.getMaxConnectionAttemptsCrawler()) {
				Logging.printLogError(logger, session, "Reached maximum attempts for URL [" + url + "]");
				return "";
			} else {
				return fetchPageGETWithHeaders(session, url, cookies, headers, attempt+1);	
			}

		}
	}

}
