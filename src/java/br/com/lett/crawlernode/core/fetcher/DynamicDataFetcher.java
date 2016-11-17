package br.com.lett.crawlernode.core.fetcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.protocol.Protocol;
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
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.openqa.selenium.remote.DesiredCapabilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.core.parser.Parser;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.exceptions.ResponseCodeException;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.test.Test;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;

public class DynamicDataFetcher {

	protected static final Logger logger = LoggerFactory.getLogger(DynamicDataFetcher.class);
	
	private static final String SMART_PROXY_SCRIPT_URL = "http://s3.amazonaws.com/phantomjs-scripts/page_content.js";
	private static final String SMART_PROXY_SCRIPT_MD5 = "3f08999e2f6d7a82bc06c90b754d91e1";

	private static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT = 10000; // ms
	private static final int DEFAULT_CONNECT_TIMEOUT = 10000; // ms
	private static final int DEFAULT_SOCKET_TIMEOUT = 10000; // ms
	
	private static final int MAX_ATTEMPTS_FOR_CONECTION_WITH_PROXY = 2;

	/** 
	 * Most popular agents, retrieved from https://techblog.willshouse.com/2012/01/03/most-common-user-agents/
	 */
	private static List<String> userAgents;

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
	}

	public static String fetchPage(Fetcher fetcherType, String url, Session session) {
		return fetchPage(fetcherType, url, session, 1);
	}
	
	private static String fetchPage(Fetcher fetcherType, String url, Session session, int attempt) {
		if (fetcherType == Fetcher.WEBDRIVER) return fetchPageWebdriver(url, session);
		else return fetchPageSmart(url, session, attempt);
	}

	/**
	 * Use the webdriver to fetch a page and get the html code.
	 * 
	 * @param url
	 * @return the html code of the fetched page
	 */
	private static String fetchPageWebdriver(String url, Session session) {
		Logging.printLogDebug(logger, session, "Fetching " + url + " using webdriver...");

		DesiredCapabilities capabilities = DesiredCapabilitiesBuilder
				.create()
				.setUserAgent(randUserAgent())
				.build();

		CrawlerWebdriver webdriver = new CrawlerWebdriver(capabilities);

		String html = webdriver.loadUrl(url);
		webdriver.terminate();

		return html;
	}

	/**
	 * Use the Charity smart proxy to fetch a page content. This proxy
	 * requires special headers to use the smart functionality, that allows
	 * to execute any javascript code in a page and retrieve the result.
	 * Requests are HTTP GET.
	 * 
	 * @param URL
	 * @param session
	 * @return the HTML code of the fetched page, after javascript code execution
	 */
	private static String fetchPageSmart(String url, Session session, int attempt) {
		LettProxy lettProxy = Test.proxies.getProxy(ProxyCollection.CHARITY).get(0);
		String randUserAgent = randUserAgent();
		CloseableHttpResponse closeableHttpResponse = null;

		try {
			CookieStore cookieStore = new BasicCookieStore();

			CredentialsProvider credentialsProvider = createCredentialsProvider(lettProxy);

			HttpHost proxy = new HttpHost(lettProxy.getAddress(), lettProxy.getPort());

			RequestConfig requestConfig = createRequestConfig(proxy);

			// creating the redirect strategy, so we can get the final redirected URL
			DataFetcherRedirectStrategy redirectStrategy = new DataFetcherRedirectStrategy();

			List<Header> headers = new ArrayList<Header>();
			headers.add(new BasicHeader(HttpHeaders.CONTENT_ENCODING, "compress, gzip"));
						
			CloseableHttpClient httpclient = HttpClients.custom()
					.setDefaultCookieStore(cookieStore)
					.setUserAgent(randUserAgent)
					.setDefaultRequestConfig(requestConfig)
					.setRedirectStrategy(redirectStrategy)
					.setDefaultCredentialsProvider(credentialsProvider)
					.setDefaultHeaders(headers)
					.build();

			HttpContext localContext = createContext(cookieStore);

			HttpGet httpGet = new HttpGet(url);
			httpGet.setConfig(requestConfig);

			// set authentication and configuration headers
			String authenticator = "ff548a45065c581adbb23bbf9253de9b" + ":";
			httpGet.addHeader("Proxy-Authorization", "Basic " + Base64.encodeBase64String(authenticator.getBytes()));
			httpGet.addHeader("X-Proxy-Country", "BR");
			httpGet.addHeader("X-proxy-phantomjs-script-url", SMART_PROXY_SCRIPT_URL);
			httpGet.addHeader("X-proxy-phantomjs-script-md5", SMART_PROXY_SCRIPT_MD5);
			httpGet.addHeader("X-Proxy-Timeout-Soft", "30");
			httpGet.addHeader("X-Proxy-Timeout-Hard", "30");
			
			// perform request
			closeableHttpResponse = httpclient.execute(httpGet, localContext);

			// analysing the status code
			// if there was some response code that indicates forbidden access or server error we want to try again
			int responseCode = closeableHttpResponse.getStatusLine().getStatusCode();
			if( Integer.toString(responseCode).charAt(0) != '2' && 
				Integer.toString(responseCode).charAt(0) != '3' && 
				responseCode != 404 ) {
				throw new ResponseCodeException(responseCode);
			}

			// creating the page content result from the http request
			PageContent pageContent = new PageContent(closeableHttpResponse.getEntity());		// loading information from http entity
			pageContent.setStatusCode(closeableHttpResponse.getStatusLine().getStatusCode());	// geting the status code
			pageContent.setUrl(url); // setting url

			// record the redirected URL on the session
			if (redirectStrategy.getFinalURL() != null && !redirectStrategy.getFinalURL().isEmpty()) {
				session.addRedirection(url, redirectStrategy.getFinalURL());
			}

			// process response and parse
			return processContent(pageContent, session);

		} catch (Exception e) {
			Logging.printLogError(logger, session, "Tentativa " + attempt + " -> Error performing request: " + url);
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));

			if(attempt >= MAX_ATTEMPTS_FOR_CONECTION_WITH_PROXY) {
				Logging.printLogError(logger, session, "Reached maximum attempts for URL [" + url + "]");
				return "";
				
			} else {
				return fetchPageSmart(url, session, attempt+1);	
			}
		}
	}
	
	private static HttpContext createContext(CookieStore cookieStore) {
		HttpContext localContext = new BasicHttpContext();
		localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
		return localContext;
	}
	
	private static CredentialsProvider createCredentialsProvider(LettProxy proxy) {
		CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		if(proxy != null && proxy.getUser() != null) {
			credentialsProvider.setCredentials(
					new AuthScope(proxy.getAddress(), proxy.getPort()),
					new UsernamePasswordCredentials(proxy.getUser(), proxy.getPass())
					);
		}
		return credentialsProvider;
	}
	
	private static RequestConfig createRequestConfig(HttpHost proxy) {
		return RequestConfig.custom()
				.setCookieSpec(CookieSpecs.STANDARD)
				.setRedirectsEnabled(true) // set redirect to true
				.setConnectionRequestTimeout(DEFAULT_CONNECTION_REQUEST_TIMEOUT)
				.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT)
				.setSocketTimeout(DEFAULT_SOCKET_TIMEOUT)
				.setProxy(proxy)
				.build();
	}

	/**
	 * Parse the page content, either to get a HTML or a plain text
	 * In case we are expecting JSONObject or JSONArray response from an API, the content
	 * will be parsed as a plain text. Otherwise it will be parsed as a HTML format.
	 * 
	 * @param pageContent
	 * @param session
	 * @return String with the request response, either in HTML or plain text format
	 */
	private static String processContent(PageContent pageContent, Session session) {		
		Parser parser = new Parser(session);
		parser.parse(pageContent);

		if (pageContent.getHtmlParseData() != null) return pageContent.getHtmlParseData().getHtml();
		if (pageContent.getTextParseData() != null) return pageContent.getTextParseData().getTextContent();

		return "";
	}

	/**
	 * Retrieve a random user agent from the user agents array.
	 * 
	 * @return a String representing an user agent
	 */
	public static String randUserAgent() {
		return userAgents.get(MathCommonsMethods.randInt(0, userAgents.size() - 1));
	}

}
