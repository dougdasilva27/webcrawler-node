package br.com.lett.crawlernode.fetcher;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import br.com.lett.crawlernode.base.ExecutionParameters;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.models.CrawlerSession;
import br.com.lett.crawlernode.parser.Parser;
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

	public static final String GET_REQUEST = "GET";
	public static final String POST_REQUEST = "POST";

	private static final int MAX_ATTEMPTS_FOR_CONECTION_WITH_PROXY = 10;
	private static final int ATTEMPTS_SEMIPREMIUM_PROXIES = 8;
	private static final int ATTEMPTS_REGULAR_PROXIES = 5;

	/**
	 * Fetch a text string from a URL, either by a GET ou POST http request.
	 * 
	 * @param reqType The type of the http request. GET_REQUEST or POST_REQUEST.
	 * @param url The url from which we will fetch the data.
	 * @param urlParameters The urlParameters, or parameter field of the request, in case of a POST request. null if we have a GET request.
	 * @param marketCrawler An instance of the MarketCrawler class to use the log method, or null if we want to print System.err
	 * @return A String.
	 */
	public static String fetchString(String reqType, CrawlerSession session, String urlParameters, List<Cookie> cookies) {
		return fetchPage(reqType, session, urlParameters, cookies, 1);	
	}

	/**
	 * Fetch a HTML Document from a URL, either by a GET ou POST http request.
	 * 
	 * @param reqType The type of the http request. GET_REQUEST or POST_REQUEST.
	 * @param url The url from which we will fetch the data.
	 * @param urlParameters The urlParameters, or parameter field of the request, in case of a POST request. null if we have a GET request.
	 * @param marketCrawler An instance of the MarketCrawler class to use the log method, or null if we want to print System.err
	 * @return A Document with the data from the url passed, or null if something went wrong.
	 */
	public static Document fetchDocument(String reqType, CrawlerSession session, String urlParameters, List<Cookie> cookies) {
		return Jsoup.parse(fetchPage(reqType, session, urlParameters, cookies, 1));	
	}

	/**
	 * Fetch a json object from the API, either by a GET ou POST http request.
	 * 
	 * @param reqType The type of the http request. GET_REQUEST or POST_REQUEST.
	 * @param url The url from which we will fetch the data.
	 * @param payload The payload, or parameter field of the request, in case of a POST request. null if we have a GET request.
	 * @param marketCrawler An instance of the MarketCrawler class to use the log method, or null if we want to print System.err
	 * @return A JSONObject with the data from the url passed, or null if something went wrong.
	 */
	public static JSONObject fetchJSONObject(String reqType, CrawlerSession session, String payload, List<Cookie> cookies) {
		return new JSONObject(fetchJson(reqType, session, payload, cookies, 1));
	}

	/**
	 * Fetch a json array from the API, either by a GET ou POST http request.
	 * 
	 * @param reqType The type of the http request. GET_REQUEST or POST_REQUEST.
	 * @param url The url from which we will fetch the data.
	 * @param payload The payload, or parameter field of the request, in case of a POST request. null if we have a GET request.
	 * @param marketCrawler An instance of the MarketCrawler class to use the log method, or null if we want to print System.err
	 * @return A JSONArray with the data from the url passed, or null if something went wrong.
	 */
	public static JSONArray fetchJSONArray(String reqType, CrawlerSession session, String payload, List<Cookie> cookies) {
		return new JSONArray(fetchJson(reqType, session, payload, cookies, 1));
	}

	/**
	 * Get the http response code of a URL
	 * 
	 * @param url 
	 * @param marketCrawler An instance of the MarketCrawler class to use the log method, or null if we want to print System.err
	 * @return The integer code. Null if we have an exception.
	 */
	public static Integer getUrlResponseCode(String url) {
		return getUrlResponseCode(url, 1);
	}

	private static Integer getUrlResponseCode(String url, int attempt) {
		try {
			URL urlObject = new URL(url);
			HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection(randProxy(attempt));
			connection.setRequestMethod("GET");
			connection.connect();

			return connection.getResponseCode();
		} catch (Exception e) {


			Logging.printLogError(logger, "Tentativa " + attempt + " -> Erro ao fazer requisição de status code: " + url);
			Logging.printLogError(logger, e.getStackTrace().toString());


			if(attempt >= MAX_ATTEMPTS_FOR_CONECTION_WITH_PROXY) {
				Logging.printLogError(logger, "Atingido número máximo de tentativas para a url : " + url);
			} else {
				return getUrlResponseCode(url, attempt+1);	
			}

			return null;
		}
	}

	private static String fetchJson(String reqType, CrawlerSession session, String payload, List<Cookie> cookies, int attempt) {
		try {

			if (reqType.equals(GET_REQUEST)) {
				return fetchPageGET(session, cookies, attempt);
			} else if (reqType.equals(POST_REQUEST)) {
				if (payload != null) {
					return fetchJsonPOST(session, payload, cookies, attempt);
				} else {
					Logging.printLogWarn(logger, "Parametro payload está null.");
				}
			} else {
				Logging.printLogWarn(logger, "Parametro reqType é inválido.");
			}

		} catch (Exception e) {

			Logging.printLogError(logger, "Tentativa " + attempt + " -> Erro ao fazer requisição de JSONObject via " + reqType + ": " + session.getUrl());
			Logging.printLogError(logger, e.getStackTrace().toString());


			if(attempt >= MAX_ATTEMPTS_FOR_CONECTION_WITH_PROXY) {
				Logging.printLogError(logger, "Atingido número máximo de tentativas para a url : " + session.getUrl());
			} else {
				return fetchJson(reqType, session, payload, cookies, attempt+1);	
			}

		}

		return null;

	}

	private static String fetchJsonPOST(CrawlerSession session, String payload, List<Cookie> cookies, int attempt) throws Exception {
		Logging.printLogDebug(logger, "Fazendo requisição POST com content-type JSON: " + session.getUrl());

		String randUserAgent = randUserAgent();
		LettProxy randProxy = randLettProxy(attempt);

		CookieStore cookieStore = new BasicCookieStore();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				cookieStore.addCookie(cookie);
			}
		}

		CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

		if(randProxy.getUser() != null) {
			credentialsProvider.setCredentials(
					new AuthScope(randProxy.getAddress(), randProxy.getPort()),
					new UsernamePasswordCredentials(randProxy.getUser(), randProxy.getPass())
					);
		}

		HttpHost proxy = new HttpHost(randProxy.getAddress(), randProxy.getPort());

		RequestConfig requestConfig = RequestConfig.custom()
				.setCookieSpec(CookieSpecs.STANDARD)
				.setRedirectsEnabled(false)
				.setProxy(proxy)
				.build();


		Header header = new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json");
		List<Header> headers = Lists.newArrayList(header);

		CloseableHttpClient httpclient = HttpClients.custom()
				.setDefaultCookieStore(cookieStore)
				.setUserAgent(randUserAgent)
				.setDefaultRequestConfig(requestConfig)
				.setDefaultCredentialsProvider(credentialsProvider)
				.setDefaultHeaders(headers)
				.build();

		HttpContext localContext = new BasicHttpContext();
		localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

		HttpPost httpPost = new HttpPost(session.getUrl());
		httpPost.setConfig(requestConfig);


		if(payload != null) {

			JSONObject payloadJson = null;

			try {
				payloadJson = new JSONObject(payload);
			} catch (Exception e) {
				Logging.printLogError(logger, "Tentativa " + attempt + " -> Erro ao fazer requisição POST JSON, pois não consegui converter o payload em json: " + payload);
				Logging.printLogError(logger, e.getStackTrace().toString());
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

		Logging.printLogDebug(logger, "Fazendo requisição via proxy: " + httpPost.getConfig().getProxy());

		CloseableHttpResponse closeableHttpResponse = httpclient.execute(httpPost, localContext);

		BufferedReader in = new BufferedReader(new InputStreamReader(closeableHttpResponse.getEntity().getContent()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		return response.toString();

	}


	private static String fetchPage(String reqType, CrawlerSession session, String urlParameters, List<Cookie> cookies, int attempt) {
		try {

			if (reqType.equals(GET_REQUEST)) {
				return fetchPageGET(session, cookies, attempt);
			} else if (reqType.equals(POST_REQUEST)) {
				if (urlParameters != null) {
					return fetchPagePOST(session, urlParameters, cookies, attempt);
				} else {
					Logging.printLogError(logger, "Parametro payload está null.");
					return "";
				}
			} else {
				Logging.printLogError(logger, "Parametro reqType é inválido.");
				return "";
			}

		} catch (Exception e) {
			e.printStackTrace();

			Logging.printLogError(logger, "Tentativa " + attempt + " -> Erro ao fazer requisição de Page via " + reqType + ": " + session.getUrl());
			Logging.printLogError(logger, e.getStackTrace().toString());

			if(attempt >= MAX_ATTEMPTS_FOR_CONECTION_WITH_PROXY) {
				Logging.printLogError(logger, "Atingido número máximo de tentativas para a url : " + session.getUrl());
				return "";

			} else {
				return fetchPage(reqType, session, urlParameters, cookies, attempt+1);	
			}

		}

	}

	private static String fetchPageGET(CrawlerSession session, List<Cookie> cookies, int attempt) {

		try {
			Logging.printLogDebug(logger, "Fazendo requisição GET: " + session.getUrl());

			String randUserAgent = randUserAgent();
			LettProxy randProxy = randLettProxy(attempt);

			CookieStore cookieStore = new BasicCookieStore();
			if (cookies != null) {
				for (Cookie cookie : cookies) {
					cookieStore.addCookie(cookie);
				}
			}

			CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

			if(randProxy.getUser() != null) {
				credentialsProvider.setCredentials(
						new AuthScope(randProxy.getAddress(), randProxy.getPort()),
						new UsernamePasswordCredentials(randProxy.getUser(), randProxy.getPass())
						);
			}

			HttpHost proxy = new HttpHost(randProxy.getAddress(), randProxy.getPort());

			RequestConfig requestConfig = RequestConfig.custom()
					.setCookieSpec(CookieSpecs.STANDARD)
					.setRedirectsEnabled(false)
					.setProxy(proxy)
					.build();

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

			Logging.printLogDebug(logger, "Fazendo requisição via proxy: " + httpGet.getConfig().getProxy());

			// do request
			CloseableHttpResponse closeableHttpResponse = httpclient.execute(httpGet, localContext);

			// creating the page content result from the http request
			PageContent pageContent = new PageContent(closeableHttpResponse.getEntity());		// loading information from http entity
			pageContent.setStatusCode(closeableHttpResponse.getStatusLine().getStatusCode());	// geting the status code
			pageContent.setUrl(session.getUrl()); // setting url

			// process response and parse
			return processContent(pageContent);

		} catch (Exception e) {

			e.printStackTrace();

			Logging.printLogError(logger, "Tentativa " + attempt + " -> Erro ao fazer requisição GET: " + session.getUrl());
			Logging.printLogError(logger, e.getMessage());
			e.printStackTrace();


			if(attempt >= MAX_ATTEMPTS_FOR_CONECTION_WITH_PROXY) {
				Logging.printLogError(logger, "Atingi máximo de tentativas para a url : " + session.getUrl());
				return "";
			} else {
				return fetchPageGET(session, cookies, attempt+1);	
			}

		}
	}

	private static String fetchPagePOST(CrawlerSession session, String urlParameters, List<Cookie> cookies, int attempt) {
		try {
			Logging.printLogDebug(logger, "Fazendo requisição POST: " + session.getUrl());

			String randUserAgent = randUserAgent();
			LettProxy randProxy = randLettProxy(attempt);

			CookieStore cookieStore = new BasicCookieStore();
			if (cookies != null) {
				for (Cookie cookie : cookies) {
					cookieStore.addCookie(cookie);
				}
			}

			CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

			if(randProxy.getUser() != null) {
				credentialsProvider.setCredentials(
						new AuthScope(randProxy.getAddress(), randProxy.getPort()),
						new UsernamePasswordCredentials(randProxy.getUser(), randProxy.getPass())
						);
			}

			HttpHost proxy = new HttpHost(randProxy.getAddress(), randProxy.getPort());

			RequestConfig requestConfig = RequestConfig.custom()
					.setCookieSpec(CookieSpecs.STANDARD)
					.setRedirectsEnabled(false)
					.setProxy(proxy)
					.build();

			CloseableHttpClient httpclient = HttpClients.custom()
					.setDefaultCookieStore(cookieStore)
					.setUserAgent(randUserAgent)
					.setDefaultRequestConfig(requestConfig)
					.setDefaultCredentialsProvider(credentialsProvider)
					.build();

			HttpContext localContext = new BasicHttpContext();
			localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

			HttpPost httpPost = new HttpPost(session.getUrl());
			httpPost.setConfig(requestConfig);

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

			Logging.printLogDebug(logger, "Request using proxy: " + httpPost.getConfig().getProxy());

			// do request
			CloseableHttpResponse closeableHttpResponse = httpclient.execute(httpPost, localContext);

			// creating the page content result from the http request
			PageContent pageContent = new PageContent(closeableHttpResponse.getEntity());		// loading information from http entity
			pageContent.setStatusCode(closeableHttpResponse.getStatusLine().getStatusCode());	// geting the status code
			pageContent.setUrl(session.getUrl()); // setting url

			// process response and parse
			return processContent(pageContent);

		} catch (Exception e) {

			e.printStackTrace();


			Logging.printLogError(logger, "Tentativa " + attempt + " -> Erro ao fazer requisição POST: " + session.getUrl());
			Logging.printLogError(logger, e.getStackTrace().toString());


			if(attempt >= MAX_ATTEMPTS_FOR_CONECTION_WITH_PROXY) {
				Logging.printLogError(logger, "Atingi máximo de tentativas para a url : " + session.getUrl());
				return "";
			} else {
				return fetchPagePOST(session, urlParameters, cookies, attempt+1);	
			}

		}
	}

	/**
	 * Parse the page content, either to get a html or a plain text
	 * In case we are expecting JSONObject or JSONArray response from an API, the content
	 * will be parsed as a plain text. Otherwise it will be parsed as a htlm format.
	 * 
	 * @param pageContent
	 * @return String with the request response, either in html or plain text format
	 */
	private static String processContent(PageContent pageContent) {		
		Parser parser = new Parser();
		parser.parse(pageContent);

		if (pageContent.getHtmlParseData() != null) return pageContent.getHtmlParseData().getHtml();
		if (pageContent.getTextParseData() != null) return pageContent.getTextParseData().getTextContent();

		return "";
	}


	private static LettProxy randLettProxy(int attempt) {
		String env = Main.executionParameters.getEnvironment();

		if(env.equals(ExecutionParameters.ENVIRONMENT_PRODUCTION)) {

			ArrayList<LettProxy> proxies = new ArrayList<LettProxy>();

			// If we are in mode insights or placeholder, than we must use premium proxy always
			Logging.printLogDebug(logger, "Using only premium proxies on rand.");
			proxies.addAll(Main.proxies.premiumProxies);


			//			else {
			//				if(attempt < ATTEMPTS_REGULAR_PROXIES+1 && !Main.proxies.regularProxies.isEmpty()){
			//					proxies.addAll(Main.proxies.regularProxies);
			//				} else if(attempt < ATTEMPTS_SEMIPREMIUM_PROXIES+1 && !Main.proxies.semiPremiumProxies.isEmpty()){
			//					proxies.addAll(Main.proxies.semiPremiumProxies);
			//				} else {
			//					proxies.addAll(Main.proxies.premiumProxies);
			//				}
			//			}

			LettProxy nextProxy = proxies.get(CommonMethods.randInt(0, proxies.size() - 1));


			return nextProxy;

		} else {

			return null;

		}

	}

	private static Proxy randProxy(int attempt) {
		String env = Main.executionParameters.getEnvironment();

		if(env.equals(ExecutionParameters.ENVIRONMENT_PRODUCTION)) {

			ArrayList<LettProxy> proxies = new ArrayList<LettProxy>();

			// If we are in mode insights or placeholder, than we must use premium proxy always
			Logging.printLogDebug(logger, "Using only premium proxies on rand.");
			proxies.addAll(Main.proxies.premiumProxies);


			//			else {
			//				if(attempt < ATTEMPTS_REGULAR_PROXIES+1 && !Main.proxies.regularProxies.isEmpty()){
			//					proxies.addAll(Main.proxies.regularProxies);
			//				} else if(attempt < ATTEMPTS_SEMIPREMIUM_PROXIES+1 && !Main.proxies.semiPremiumProxies.isEmpty()){
			//					proxies.addAll(Main.proxies.semiPremiumProxies);
			//				} else {
			//					proxies.addAll(Main.proxies.premiumProxies);
			//				}
			//			}

			LettProxy nextProxy = proxies.get(CommonMethods.randInt(0, proxies.size() - 1));

			final String nextProxyHost = nextProxy.getAddress();
			final int nextProxyPort = nextProxy.getPort();
			final String nextProxyUser = nextProxy.getUser();
			final String nextProxyPass = nextProxy.getPass();

			if(nextProxyUser != null){
				//Autenticação de usuário e senha do proxy e seta a porta e o host para conexão
				Authenticator a = new Authenticator() {
					public PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(nextProxyUser, nextProxyPass.toCharArray());
					}
				};

				Authenticator.setDefault(a);
			}

			return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(nextProxyHost, nextProxyPort));

		} else {

			Authenticator.setDefault(null);

			return null;

		}

	}


	public static String randUserAgent() {

		// Lista dos mais populares retirada de https://techblog.willshouse.com/2012/01/03/most-common-user-agents/

		List<String> userAgents = new ArrayList<String>();

		userAgents.add("Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
		userAgents.add("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36");
		userAgents.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36");
		userAgents.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_5) AppleWebKit/601.6.17 (KHTML, like Gecko) Version/9.1.1 Safari/601.6.17");
		userAgents.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36");
		userAgents.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36");
		userAgents.add("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:46.0) Gecko/20100101 Firefox/46.0");
		userAgents.add("Mozilla/5.0 (Windows NT 10.0; WOW64; rv:46.0) Gecko/20100101 Firefox/46.0");
		userAgents.add("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.84 Safari/537.36");
		userAgents.add("Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36");
		userAgents.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.84 Safari/537.36");
		userAgents.add("Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; rv:11.0) like Gecko");
		userAgents.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36");
		userAgents.add("Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:46.0) Gecko/20100101 Firefox/46.0");
		userAgents.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_4) AppleWebKit/601.5.17 (KHTML, like Gecko) Version/9.1 Safari/601.5.17");
		userAgents.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.84 Safari/537.36");
		userAgents.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.11; rv:46.0) Gecko/20100101 Firefox/46.0");
		userAgents.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36");

		return userAgents.get(CommonMethods.randInt(0, userAgents.size() - 1));

	}


}
