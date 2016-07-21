
/**
 * Classe auxiliar para fazer requisições http
 * 
 * @author Samir Leão
 */

package br.com.lett.crawlernode.fetcher;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
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
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;

import uk.org.lidalia.slf4jext.Logger;
import uk.org.lidalia.slf4jext.LoggerFactory;
import br.com.lett.crawlernode.base.ExecutionParameters;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

import com.google.common.collect.Lists;

public class DataFetcher {

	protected static final Logger logger = LoggerFactory.getLogger(DataFetcher.class); 

	public static final String GET_REQUEST = "GET";
	public static final String POST_REQUEST = "POST";
	public static final String CONTENT_TYPE_APPLICATION_JSON = "application/json";

	private static final int MAX_ATTEMPTS_FOR_CONECTION_WITH_PROXY = 10;
	private static final int ATTEMPTS_SEMIPREMIUM_PROXIES = 8;
	private static final int ATTEMPTS_REGULAR_PROXIES = 5;

	/**
	 * Fetch a text string from a URL, either by a GET ou POST http request.
	 * @param reqType The type of the http request. GET_REQUEST or POST_REQUEST.
	 * @param url The url from which we will fetch the data.
	 * @param urlParameters The urlParameters, or parameter field of the request, in case of a POST request. null if we have a GET request.
	 * @param marketCrawler An instance of the MarketCrawler class to use the log method, or null if we want to print System.err
	 * @return A String.
	 */
	public static String fetchString(String reqType, String url, String urlParameters, List<Cookie> cookies) {
		return fetchPage(reqType, url, urlParameters, cookies, 1);	
	}

	/**
	 * Fetch a HTML Document from a URL, either by a GET ou POST http request.
	 * @param reqType The type of the http request. GET_REQUEST or POST_REQUEST.
	 * @param url The url from which we will fetch the data.
	 * @param urlParameters The urlParameters, or parameter field of the request, in case of a POST request. null if we have a GET request.
	 * @param marketCrawler An instance of the MarketCrawler class to use the log method, or null if we want to print System.err
	 * @return A Document with the data from the url passed, or null if something went wrong.
	 */
	public static Document fetchDocument(String reqType, String url, String urlParameters, List<Cookie> cookies) {
		return Jsoup.parse(fetchPage(reqType, url, urlParameters, cookies, 1));	
	}

	/**
	 * Get the http response code of a URL
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


	private static String fetchPage(String reqType, String url, String urlParameters, List<Cookie> cookies, int attempt) {

		try {

			if (reqType.equals(GET_REQUEST)) {
				return fetchPageGET(url, cookies, attempt);
			} else if (reqType.equals(POST_REQUEST)) {
				if (urlParameters != null) {
					return fetchPagePOST(url, urlParameters, cookies, attempt);
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

			
				Logging.printLogError(logger, "Tentativa " + attempt + " -> Erro ao fazer requisição de Page via " + reqType + ": " + url);
				Logging.printLogError(logger, e.getStackTrace().toString());
			

			if(attempt >= MAX_ATTEMPTS_FOR_CONECTION_WITH_PROXY) {
				Logging.printLogError(logger, "Atingido número máximo de tentativas para a url : " + url);

				return "";
			} else {
				return fetchPage(reqType, url, urlParameters, cookies, attempt+1);	
			}

		}

	}

	private static String fetchPageGET(String url, List<Cookie> cookies, int attempt) {

		try {
			Logging.printLogDebug(logger, "Fazendo requisição GET: " + url);

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

			HttpGet httpGet = new HttpGet(url);
			httpGet.setConfig(requestConfig);

			Logging.printLogDebug(logger, "Fazendo requisição via proxy: " + httpGet.getConfig().getProxy());

			CloseableHttpResponse closeableHttpResponse = httpclient.execute(httpGet, localContext);

			Header contentTypeHeader = closeableHttpResponse.getEntity().getContentType();
			String contentType = null;
			if (contentTypeHeader != null) {
				contentType = contentTypeHeader.toString();
			}

			if (contentType != null && contentType.contains(CONTENT_TYPE_APPLICATION_JSON)) {
				return getPureTextData(closeableHttpResponse.getEntity());
			}

			return parseContentToHtml(closeableHttpResponse.getEntity(), url);

		} catch (Exception e) {
			Logging.printLogError(logger, "Tentativa " + attempt + " -> Erro ao fazer requisição GET: " + url);
			Logging.printLogError(logger, e.getMessage());
			e.printStackTrace();


			if(attempt >= MAX_ATTEMPTS_FOR_CONECTION_WITH_PROXY) {
				Logging.printLogError(logger, "Atingi máximo de tentativas para a url : " + url);
				return "";
			} else {
				return fetchPageGET(url, cookies, attempt+1);	
			}

		}
	}

	private static String fetchPagePOST(String url, String urlParameters, List<Cookie> cookies, int attempt) {

		try {

			
				Logging.printLogDebug(logger, "Fazendo requisição POST: " + url);
			


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

			HttpPost httpPost = new HttpPost(url);
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

			Logging.printLogDebug(logger, "Fazendo requisição via proxy: " + httpPost.getConfig().getProxy());

			CloseableHttpResponse closeableHttpResponse = httpclient.execute(httpPost, localContext);

			Header contentTypeHeader = closeableHttpResponse.getEntity().getContentType();
			String contentType = null;
			if (contentTypeHeader != null) {
				contentType = contentTypeHeader.toString();
			}

			if (contentType != null && contentType.contains(CONTENT_TYPE_APPLICATION_JSON)) {
				return getPureTextData(closeableHttpResponse.getEntity());
			}

			return parseContentToHtml(closeableHttpResponse.getEntity(), url);

		} catch (Exception e) {

			e.printStackTrace();

			Logging.printLogError(logger, "Tentativa " + attempt + " -> Erro ao fazer requisição POST: " + url);
			Logging.printLogError(logger, e.getStackTrace().toString());


			if(attempt >= MAX_ATTEMPTS_FOR_CONECTION_WITH_PROXY) {
				Logging.printLogError(logger, "Atingi máximo de tentativas para a url : " + url);
				return "";
			} else {
				return fetchPagePOST(url, urlParameters, cookies, attempt+1);	
			}

		}
	}


	private static LettProxy randLettProxy(int attempt) {	

		if( Main.executionParameters.getEnvironment().equals(ExecutionParameters.ENVIRONMENT_PRODUCTION) ) {

			ArrayList<LettProxy> proxies = new ArrayList<LettProxy>();

			// If we are in production and in normal mode, use only premium proxies
						if ( Main.executionParameters.getMode().equals(ExecutionParameters.MODE_NORMAL) ) {
							Logging.printLogDebug(logger, "Running in mode normal on production...using only premium proxies on rand.");
							
							proxies.addAll(Main.proxies.premiumProxies);
						}

			else {
				if(attempt < ATTEMPTS_REGULAR_PROXIES+1 && !Main.proxies.regularProxies.isEmpty()){
					proxies.addAll(Main.proxies.regularProxies);
				} else if(attempt < ATTEMPTS_SEMIPREMIUM_PROXIES+1 && !Main.proxies.semiPremiumProxies.isEmpty()){
					proxies.addAll(Main.proxies.semiPremiumProxies);
				} else {
					proxies.addAll(Main.proxies.premiumProxies);
				}
			}

			LettProxy nextProxy = proxies.get(CommonMethods.randInt(0, proxies.size() - 1));


			return nextProxy;

		} else {

			return null;

		}

	}

	private static Proxy randProxy(int attempt) {	

		if( Main.executionParameters.getEnvironment().equals(ExecutionParameters.ENVIRONMENT_PRODUCTION) ) {
			ArrayList<LettProxy> proxies = new ArrayList<LettProxy>();

			// If we are in production and in normal mode, use only premium proxies
			if ( Main.executionParameters.getMode().equals(ExecutionParameters.MODE_NORMAL) ) {
				Logging.printLogDebug(logger, "Running in mode normal on production...using only premium proxies on rand.");
				
				proxies.addAll(Main.proxies.premiumProxies);
			}

			else {
				if(attempt < ATTEMPTS_REGULAR_PROXIES+1 && !Main.proxies.regularProxies.isEmpty()){
					proxies.addAll(Main.proxies.regularProxies);
				} else if(attempt < ATTEMPTS_SEMIPREMIUM_PROXIES+1 && !Main.proxies.semiPremiumProxies.isEmpty()){
					proxies.addAll(Main.proxies.semiPremiumProxies);
				} else {
					proxies.addAll(Main.proxies.premiumProxies);
				}
			}

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

	/**
	 * Parse the content fetched inside the http entity to a String in htlm format
	 * 
	 * @param entity
	 * @param url
	 * @return A string with all the content fetched in html format. An empty string if nothing was fetched.
	 * @throws Exception
	 */
	private static String parseContentToHtml(HttpEntity entity, String url) throws Exception {
		return Jsoup.parse(entity.getContent(), "UTF-8", url).html();
	}

	/**
	 * Get the pure text as a response from a requisition. Used when the page is a pure JSONObject.
	 * When we are fetching a proper html page, we must use the parseContentToHtml before we use the content.
	 * 
	 * @param entity
	 * @return
	 * @throws Exception
	 */
	private static String getPureTextData(HttpEntity entity) throws Exception {
		BufferedReader in = new BufferedReader(new InputStreamReader(entity.getContent()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		return response.toString();
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
