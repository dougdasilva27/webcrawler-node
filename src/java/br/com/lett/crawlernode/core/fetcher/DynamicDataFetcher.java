package br.com.lett.crawlernode.core.fetcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.crawler.TestCrawlerSession;
import br.com.lett.crawlernode.core.session.ranking.TestRankingSession;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.test.Test;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;

public class DynamicDataFetcher {

	protected static final Logger logger = LoggerFactory.getLogger(DynamicDataFetcher.class);

	// private static final String HA_PROXY = "191.235.90.114:3333";
	//
	// private static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT = 10000; // ms
	// private static final int DEFAULT_CONNECT_TIMEOUT = 10000; // ms
	// private static final int DEFAULT_SOCKET_TIMEOUT = 10000; // ms

	/**
	 * Most popular agents, retrieved from
	 * https://techblog.willshouse.com/2012/01/03/most-common-user-agents/
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
				"Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36");
	}

	/**
	 * Use the webdriver to fetch a page.
	 * 
	 * @param url
	 * @param session
	 * @return a webdriver instance with the page already loaded
	 */
	public static CrawlerWebdriver fetchPageWebdriver(String url, Session session) {
		Logging.printLogDebug(logger, session, "Fetching " + url + " using webdriver...");

		String phantomjsPath = null;
		if (session instanceof TestCrawlerSession || session instanceof TestRankingSession) {
			phantomjsPath = Test.phantomjsPath;
		} else {
			phantomjsPath = Main.executionParameters.getPhantomjsPath();
		}

		// choose a proxy randomly
		String proxyString = ProxyCollection.LUMINATI_SERVER_BR;

		// Bifarma block luminati_server
		if (session.getMarket().getName().equals("bifarma")) {
			proxyString = ProxyCollection.BONANZA;
		}

		LettProxy proxy = randomProxy(proxyString, session);

		DesiredCapabilities caps = DesiredCapabilities.phantomjs();
		caps.setJavascriptEnabled(true);
		caps.setCapability("takesScreenshot", true);
		caps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, phantomjsPath);

		//
		// Set proxy via client args
		// Proxy authorization doesnt work with client args
		// we must set the header Authorization or use a custom header
		// that the HAProxy is expecting
		//
		List<String> cliArgsCap = new ArrayList<>();

		if (proxy != null) {
			cliArgsCap.add("--proxy=" + proxy.getAddress() + ":" + proxy.getPort());
			cliArgsCap.add("--proxy-auth=" + proxy.getUser() + ":" + proxy.getPass());
			cliArgsCap.add("--proxy-type=http");
		}

		cliArgsCap.add("--ignore-ssl-errors=true"); // ignore errors in https requests
		cliArgsCap.add("--load-images=false");

		caps.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, cliArgsCap);
		// caps.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_CUSTOMHEADERS_PREFIX + "x-a",
		// "5RXsOBETLoWjhdM83lDMRV3j335N1qbeOfMoyKsD"); // authentication

		//
		// Set a random user agent
		//
		caps.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_CUSTOMHEADERS_PREFIX + "User-Agent",
				"Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");

		//
		// Tell the HAProxy which proxy service we want to use
		//
		// String proxyServiceName = session.getMarket().getProxies().get(0);
		// caps.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_CUSTOMHEADERS_PREFIX + "x-type",
		// proxyServiceName);

		CrawlerWebdriver webdriver = new CrawlerWebdriver(caps, session);

		if (!(session instanceof TestCrawlerSession || session instanceof TestRankingSession)) {
			Main.server.incrementWebdriverInstances();
		}

		webdriver.loadUrl(url);

		return webdriver;
	}

	private static LettProxy randomProxy(String proxyService, Session session) {
		List<LettProxy> proxies;

		if (session instanceof TestRankingSession || session instanceof TestCrawlerSession) {
			proxies = Test.proxies.getProxy(proxyService);
		} else {
			proxies = Main.proxies.getProxy(proxyService);
		}

		if (!proxies.isEmpty()) {
			int i = MathCommonsMethods.randInt(0, proxies.size() - 1);
			return proxies.get(i);
		}
		return null;
	}

	/**
	 * 
	 * @param webdriver
	 * @param url
	 * @return
	 */
	public static Document fetchPage(CrawlerWebdriver webdriver, String url, Session session) {
		try {
			Document doc = new Document(url);
			webdriver.loadUrl(url);

			session.addRedirection(url, webdriver.getCurURL());

			String docString = webdriver.getCurrentPageSource();

			if (docString != null) {
				doc = Jsoup.parse(docString);
			}

			return doc;
		} catch (Exception e) {
			Logging.printLogError(logger,
					"Erro ao realizar requisição: " + CommonMethods.getStackTraceString(e));
			return new Document(url);
		}
	}

	/**
	 * Use the Charity smart proxy to fetch a page content. This proxy requires special headers to use
	 * the smart functionality, that allows to execute any javascript code in a page and retrieve the
	 * result. Requests are HTTP GET.
	 * 
	 * @param URL
	 * @param session
	 * @return the HTML code of the fetched page, after javascript code execution
	 */
	// private static String fetchPageSmart(String url, Session session, int attempt) {
	// LettProxy lettProxy = Main.proxies.getProxy(ProxyCollection.CHARITY).get(0);
	// String randUserAgent = randUserAgent();
	// CloseableHttpResponse closeableHttpResponse = null;
	//
	// try {
	// CookieStore cookieStore = new BasicCookieStore();
	//
	// CredentialsProvider credentialsProvider = createCredentialsProvider(lettProxy);
	//
	// HttpHost proxy = new HttpHost(lettProxy.getAddress(), lettProxy.getPort());
	//
	// RequestConfig requestConfig = createRequestConfig(proxy);
	//
	// // creating the redirect strategy, so we can get the final redirected URL
	// DataFetcherRedirectStrategy redirectStrategy = new DataFetcherRedirectStrategy();
	//
	// List<Header> headers = new ArrayList<>();
	// headers.add(new BasicHeader(HttpHeaders.CONTENT_ENCODING, "compress, gzip"));
	//
	// CloseableHttpClient httpclient = HttpClients.custom()
	// .setDefaultCookieStore(cookieStore)
	// .setUserAgent(randUserAgent)
	// .setDefaultRequestConfig(requestConfig)
	// .setRedirectStrategy(redirectStrategy)
	// .setDefaultCredentialsProvider(credentialsProvider)
	// .setDefaultHeaders(headers)
	// .build();
	//
	// HttpContext localContext = createContext(cookieStore);
	//
	// HttpGet httpGet = new HttpGet(url);
	// httpGet.setConfig(requestConfig);
	//
	// // set authentication and configuration headers
	// String authenticator = "ff548a45065c581adbb23bbf9253de9b" + ":";
	// httpGet.addHeader("Proxy-Authorization", "Basic " +
	// Base64.encodeBase64String(authenticator.getBytes()));
	// httpGet.addHeader("X-Proxy-Country", "BR");
	// httpGet.addHeader("X-proxy-phantomjs-script-url", SMART_PROXY_SCRIPT_URL);
	// httpGet.addHeader("X-proxy-phantomjs-script-md5", SMART_PROXY_SCRIPT_MD5);
	// httpGet.addHeader("X-Proxy-Timeout-Soft", "30");
	// httpGet.addHeader("X-Proxy-Timeout-Hard", "30");
	//
	// // perform request
	// closeableHttpResponse = httpclient.execute(httpGet, localContext);
	//
	// // analysing the status code
	// // if there was some response code that indicates forbidden access or server error we want to
	// try again
	// int responseCode = closeableHttpResponse.getStatusLine().getStatusCode();
	// if( Integer.toString(responseCode).charAt(0) != '2' &&
	// Integer.toString(responseCode).charAt(0) != '3' &&
	// responseCode != 404 ) {
	// throw new ResponseCodeException(responseCode);
	// }
	//
	// // creating the page content result from the http request
	// PageContent pageContent = new PageContent(closeableHttpResponse.getEntity()); // loading
	// information from http entity
	// pageContent.setStatusCode(closeableHttpResponse.getStatusLine().getStatusCode()); // geting the
	// status code
	// pageContent.setUrl(url); // setting url
	//
	// // record the redirected URL on the session
	// if (redirectStrategy.getFinalURL() != null && !redirectStrategy.getFinalURL().isEmpty()) {
	// session.addRedirection(url, redirectStrategy.getFinalURL());
	// }
	//
	// // process response and parse
	// String response = processContent(pageContent, session);
	//
	// if(response != null && response.trim().isEmpty()) {
	// throw new ResponseCodeException(0);
	// }
	//
	// return response;
	//
	// } catch (Exception e) {
	// Logging.printLogError(logger, session, "Tentativa " + attempt + " -> Error performing request:
	// " + url);
	// Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
	//
	// if(attempt >= MAX_ATTEMPTS_FOR_CONECTION_WITH_PROXY) {
	// Logging.printLogError(logger, session, "Reached maximum attempts for URL [" + url + "]");
	// return "";
	//
	// } else {
	// return fetchPageSmart(url, session, attempt+1);
	// }
	// }
	// }

	// private static HttpContext createContext(CookieStore cookieStore) {
	// HttpContext localContext = new BasicHttpContext();
	// localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
	// return localContext;
	// }
	//
	// private static CredentialsProvider createCredentialsProvider(LettProxy proxy) {
	// CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
	// if(proxy != null && proxy.getUser() != null) {
	// credentialsProvider.setCredentials(
	// new AuthScope(proxy.getAddress(), proxy.getPort()),
	// new UsernamePasswordCredentials(proxy.getUser(), proxy.getPass())
	// );
	// }
	// return credentialsProvider;
	// }
	//
	// private static RequestConfig createRequestConfig(HttpHost proxy) {
	// return RequestConfig.custom()
	// .setCookieSpec(CookieSpecs.STANDARD)
	// .setRedirectsEnabled(true) // set redirect to true
	// .setConnectionRequestTimeout(DEFAULT_CONNECTION_REQUEST_TIMEOUT)
	// .setConnectTimeout(DEFAULT_CONNECT_TIMEOUT)
	// .setSocketTimeout(DEFAULT_SOCKET_TIMEOUT)
	// .setProxy(proxy)
	// .build();
	// }

	// /**
	// * Parse the page content, either to get a HTML or a plain text
	// * In case we are expecting JSONObject or JSONArray response from an API, the content
	// * will be parsed as a plain text. Otherwise it will be parsed as a HTML format.
	// *
	// * @param pageContent
	// * @param session
	// * @return String with the request response, either in HTML or plain text format
	// */
	// private static String processContent(PageContent pageContent, Session session) {
	// Parser parser = new Parser(session);
	// parser.parse(pageContent);
	//
	// if (pageContent.getHtmlParseData() != null) {
	// return pageContent.getHtmlParseData().getHtml();
	// }
	// if (pageContent.getTextParseData() != null) {
	// return pageContent.getTextParseData().getTextContent();
	// }
	//
	// return "";
	// }

	/**
	 * Retrieve a random user agent from the user agents array.
	 * 
	 * @return a String representing an user agent
	 */
	public static String randUserAgent() {
		return userAgents.get(MathCommonsMethods.randInt(0, userAgents.size() - 1));
	}

}
