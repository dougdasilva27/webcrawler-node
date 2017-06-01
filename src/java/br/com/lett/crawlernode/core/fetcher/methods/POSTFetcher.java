package br.com.lett.crawlernode.core.fetcher.methods;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.codec.binary.Base64;
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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.LettProxy;
import br.com.lett.crawlernode.core.fetcher.PageContent;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.exceptions.ResponseCodeException;
import br.com.lett.crawlernode.queue.S3Service;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

public class POSTFetcher {

	protected static final Logger logger = LoggerFactory.getLogger(POSTFetcher.class);

	private static final String FETCHER_CONTENT_TYPE = "application/json";
	private static final String FETCHER_USER = "fetcher";
	private static final String FETCHER_PASSWORD = "lettNasc";
	private static final String FETCHER_HOST = "http://development.j3mv2k6ceh.us-east-1.elasticbeanstalk.com/";
	//private static final String FETCHER_HOST = "http://localhost:3000/";

	private static final String FETCHER_PARAMETER_URL = "url";
	private static final String FETCHER_PARAMETER_METHOD = "request_type";
	private static final String FETCHER_PARAMETER_RETRIEVE_STATISTICS = "retrieve_statistics";
	private static final String FETCHER_PARAMETER_PROXIES = "forced_proxies";
	private static final String FETCHER_PARAMETER_REQUEST_PARAMETERS = "request_parameters";

	private POSTFetcher() {
		super();
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
	public static String fetchPagePOST(Session session, String url, String urlParameters, List<Cookie> cookies, int attempt) {
		LettProxy randProxy = null;
		String randUserAgent = null;

		CloseableHttpResponse closeableHttpResponse = null;
		int responseLength = 0;
		String requestHash = DataFetcher.generateRequestHash(session);

		try {
			Logging.printLogDebug(logger, session, "Performing POST request: " + url);

			// Request via fetcher on first attempt
			if(attempt == 1) {
				Map<String,String> headers = new HashMap<>();
				
				if(cookies != null && !cookies.isEmpty()) {
					StringBuilder cookiesHeader = new StringBuilder();
					for(Cookie c : cookies) {
						cookiesHeader.append(c.getName() + "=" + c.getValue() + ";");
					}
					
					headers.put("Cookie", cookiesHeader.toString());
				}
				
				JSONObject payloadFetcher = POSTFetcher.fetcherPayloadBuilder(url, "POST", false, urlParameters, headers, null);
				JSONObject response = POSTFetcher.requestWithFetcher(session, payloadFetcher);
				
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

			RequestConfig requestConfig = getRequestConfig(proxy);

			List<Header> headers = new ArrayList<>();
			headers.add(new BasicHeader(HttpHeaders.CONTENT_ENCODING, DataFetcher.CONTENT_ENCODING));

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
			if (randProxy != null && randProxy.getSource().equals(ProxyCollection.CHARITY)) {
				String authenticator = "ff548a45065c581adbb23bbf9253de9b" + ":";
				String headerValue = "Basic " + Base64.encodeBase64String(authenticator.getBytes());
				httpPost.addHeader("Proxy-Authorization", headerValue);

				// setting header for proxy country
				httpPost.addHeader("X-Proxy-Country", "BR");
			}

			// if we are using azure, we must set header for authentication
			if (randProxy != null && randProxy.getSource().equals(ProxyCollection.AZURE)) {
				httpPost.addHeader("Authorization", "5RXsOBETLoWjhdM83lDMRV3j335N1qbeOfMoyKsD");
			}

			if(urlParameters != null && urlParameters.split("&").length > 0) {
				ArrayList<NameValuePair> postParameters = new ArrayList<>();
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
					DataFetcher.POST_REQUEST, 
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

			// process response and parse
			return DataFetcher.processContent(pageContent, session);

		} catch (Exception e) {
			DataFetcher.sendRequestInfoLog(
					url, 
					DataFetcher.POST_REQUEST, 
					randProxy, 
					randUserAgent, 
					session, 
					closeableHttpResponse,
					responseLength,
					requestHash);

			if (e instanceof ResponseCodeException) {
				Logging.printLogWarn(logger, session, "Tentativa " + attempt + " -> Erro ao fazer requisição POST: " + session.getOriginalURL());
				Logging.printLogWarn(logger, session, CommonMethods.getStackTraceString(e));
			}
			else {
				Logging.printLogError(logger, session, "Tentativa " + attempt + " -> Erro ao fazer requisição POST: " + session.getOriginalURL());
				Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
			}

			if(attempt >= session.getMaxConnectionAttemptsCrawler()) {
				Logging.printLogError(logger, session, "Reached maximum attempts for URL [" + url + "]");
				return "";
			} else {
				return fetchPagePOST(session, url, urlParameters, cookies, attempt+1);	
			}

		}
	}

	public static String fetchJsonPOST(Session session, String url, String payload, List<Cookie> cookies, int attempt) throws Exception {
		Logging.printLogDebug(logger, session, "Fazendo requisição POST com content-type JSON: " + url);
		
		// Request via fetcher on first attempt
		if(attempt == 1) {
			Map<String,String> headers = new HashMap<>();
			
			if(cookies != null && !cookies.isEmpty()) {
				StringBuilder cookiesHeader = new StringBuilder();
				for(Cookie c : cookies) {
					cookiesHeader.append(c.getName() + "=" + c.getValue() + ";");
				}
				
				headers.put("Cookie", cookiesHeader.toString());
			}
			
			JSONObject payloadFetcher = POSTFetcher.fetcherPayloadBuilder(url, "POST", false, payload, headers, null);
			JSONObject response = POSTFetcher.requestWithFetcher(session, payloadFetcher);
			
			return response.getJSONObject("response").getString("body");
		}
		
		String randUserAgent = DataFetcher.randUserAgent();
		LettProxy randProxy = DataFetcher.randLettProxy(attempt, session, session.getMarket().getProxies());

		String requestHash = DataFetcher.generateRequestHash(session);

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

		RequestConfig requestConfig = DataFetcher.createRequestConfig(proxy);

		List<Header> headers = new ArrayList<>();
		headers.add(new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json"));
		headers.add(new BasicHeader(HttpHeaders.CONTENT_ENCODING, DataFetcher.CONTENT_ENCODING));

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
		if (randProxy != null && randProxy.getSource().equals(ProxyCollection.CHARITY)) {
			String authenticator = "ff548a45065c581adbb23bbf9253de9b" + ":";
			String headerValue = "Basic " + Base64.encodeBase64String(authenticator.getBytes());
			httpPost.addHeader("Proxy-Authorization", headerValue);

			// setting header for proxy country
			httpPost.addHeader("X-Proxy-Country", "BR");
		}

		// if we are using azure, we must set header for authentication
		if (randProxy != null && randProxy.getSource().equals(ProxyCollection.AZURE)) {
			httpPost.addHeader("Authorization", "5RXsOBETLoWjhdM83lDMRV3j335N1qbeOfMoyKsD");
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
				ArrayList<NameValuePair> postParameters = new ArrayList<>();
				@SuppressWarnings("rawtypes")
				Iterator iterator = payloadJson.keySet().iterator();

				while(iterator.hasNext()) {
					String key = (String) iterator.next();
					postParameters.add(new BasicNameValuePair(key, payloadJson.get(key).toString()));
				}

				httpPost.setEntity(new UrlEncodedFormEntity(postParameters));
			}

		}

		CloseableHttpResponse closeableHttpResponse = httpclient.execute(httpPost, localContext);

		DataFetcher.sendRequestInfoLog(url, DataFetcher.POST_REQUEST, randProxy, randUserAgent, session, closeableHttpResponse, requestHash);

		// analysing the status code
		// if there was some response code that indicates forbidden access or server error we want to try again
		int responseCode = closeableHttpResponse.getStatusLine().getStatusCode();
		if( Integer.toString(responseCode).charAt(0) != '2' && 
				Integer.toString(responseCode).charAt(0) != '3' && 
				responseCode != 404 ) { // errors
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
		S3Service.uploadCrawlerSessionContentToAmazon(session, requestHash, response.toString());

		return response.toString();

	}

	public static String fetchPagePOSTWithHeaders(
			String url, 
			Session session, 
			String payload, 
			List<Cookie> cookies, 
			int attempt, 
			Map<String,String> headers) {

		LettProxy randProxy = null;
		String randUserAgent = null;
		CloseableHttpResponse closeableHttpResponse = null;
		Integer responseLength = 0;
		String requestHash = DataFetcher.generateRequestHash(session);

		try {

			Logging.printLogDebug(logger, session, "Performing POST request: " + url);
			
			// Request via fetcher on first attempt
			if(attempt == 1) {
				if(cookies != null && !cookies.isEmpty()) {
					StringBuilder cookiesHeader = new StringBuilder();
					for(Cookie c : cookies) {
						cookiesHeader.append(c.getName() + "=" + c.getValue() + ";");
					}
					
					headers.put("Cookie", cookiesHeader.toString());
				}
				
				JSONObject payloadFetcher = POSTFetcher.fetcherPayloadBuilder(url, "POST", false, payload, headers, null);
				JSONObject response = POSTFetcher.requestWithFetcher(session, payloadFetcher);
				
				return response.getJSONObject("response").getString("body");
			}
			
			randUserAgent = DataFetcher.randUserAgent();
			randProxy = DataFetcher.randLettProxy(attempt, session, session.getMarket().getProxies());

			CookieStore cookieStore = DataFetcher.createCookieStore(cookies);

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

			RequestConfig requestConfig = getRequestConfig(proxy);

			List<Header> reqHeaders = new ArrayList<>();
			reqHeaders.add(new BasicHeader(HttpHeaders.CONTENT_ENCODING, DataFetcher.CONTENT_ENCODING));
			if(headers.containsKey("Content-Type")){
				reqHeaders.add(new BasicHeader(HttpHeaders.CONTENT_TYPE, headers.get("Content-Type")));
			}

			CloseableHttpClient httpclient = HttpClients.custom()
					.setDefaultCookieStore(cookieStore)
					.setUserAgent(randUserAgent)
					.setDefaultRequestConfig(requestConfig)
					.setDefaultCredentialsProvider(credentialsProvider)
					.setDefaultHeaders(reqHeaders)
					.build();

			HttpContext localContext = new BasicHttpContext();
			localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

			StringEntity input = new StringEntity(payload);
			input.setContentType(headers.get("Content-Type"));

			HttpPost httpPost = new HttpPost(url);
			httpPost.setEntity(input);

			// if we are using charity engine, we must set header for authentication
			if (randProxy != null && randProxy.getSource().equals(ProxyCollection.CHARITY)) {
				String authenticator = "ff548a45065c581adbb23bbf9253de9b" + ":";
				String headerValue = "Basic " + Base64.encodeBase64String(authenticator.getBytes());
				httpPost.addHeader("Proxy-Authorization", headerValue);

				// setting header for proxy country
				httpPost.addHeader("X-Proxy-Country", "BR");
			}

			// if we are using azure, we must set header for authentication
			if (randProxy != null && randProxy.getSource().equals(ProxyCollection.AZURE)) {
				httpPost.addHeader("Authorization", "5RXsOBETLoWjhdM83lDMRV3j335N1qbeOfMoyKsD");
			}

			if(headers.containsKey("Content-Type")){
				if(payload != null) {
					httpPost.setEntity(new StringEntity(payload, ContentType.create(headers.get("Content-Type"))));
				}
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
					DataFetcher.POST_REQUEST, 
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

			// process response and parse
			return DataFetcher.processContent(pageContent, session);

		} catch (Exception e) {
			DataFetcher.sendRequestInfoLog(
					url, 
					DataFetcher.POST_REQUEST, 
					randProxy, 
					randUserAgent, 
					session, 
					closeableHttpResponse, 
					responseLength, 
					requestHash);

			if (e instanceof ResponseCodeException) {
				Logging.printLogWarn(logger, session, "Tentativa " + attempt + " -> Erro ao fazer requisição POST: " + url);
				Logging.printLogWarn(logger, session, CommonMethods.getStackTraceString(e));
			}
			else {
				Logging.printLogError(logger, session, "Tentativa " + attempt + " -> Erro ao fazer requisição POST: " + url);
				Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
			}

			if(attempt >= session.getMaxConnectionAttemptsCrawler()) {
				Logging.printLogError(logger, session, "Reached maximum attempts for URL [" + url + "]");
				return "";
			} else {
				return fetchPagePOSTWithHeaders(url, session, payload, cookies, attempt+1, headers);	
			}

		}
	}

	/**
	 * Request a url with fetcher
	 * 
	 * @param session
	 * @param payload - JSONObject with all parameters to request fetcher
	 * @return JSONObject - response of fetcher, doc in https://www.notion.so/lett/Fetcher-e63950ab50c849aaa46931d124eba168#303c4ee9f1e347dabab305bd6725f49c
	 * @throws Exception
	 */
	public static JSONObject requestWithFetcher(Session session, JSONObject payload) throws Exception {
		String requestHash = DataFetcher.generateRequestHash(session);

		Logging.printLogDebug(logger, session, "Performing POST request in fetcher to perform a "+ 
												payload.getString(FETCHER_PARAMETER_METHOD) +" request in: " + payload.getString(FETCHER_PARAMETER_URL));

		//Authentication
		URL requestURL = new URI(FETCHER_HOST).toURL();
		String fetcherUrl = requestURL.getProtocol() + "://" + FETCHER_USER + ":" + FETCHER_PASSWORD + "@" + requestURL.getHost();
		
		CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

		RequestConfig requestConfig = RequestConfig.custom()
				.setRedirectsEnabled(true)
				.setConnectionRequestTimeout(DataFetcher.DEFAULT_CONNECTION_REQUEST_TIMEOUT * 2)
				.setConnectTimeout(DataFetcher.DEFAULT_CONNECT_TIMEOUT * 2)
				.setSocketTimeout(DataFetcher.DEFAULT_SOCKET_TIMEOUT * 2)
				.build();

		List<Header> reqHeaders = new ArrayList<>();
		reqHeaders.add(new BasicHeader(HttpHeaders.CONTENT_TYPE, FETCHER_CONTENT_TYPE));

		CloseableHttpClient httpclient = HttpClients.custom()
				.setDefaultRequestConfig(requestConfig)
				.setDefaultCredentialsProvider(credentialsProvider)
				.setDefaultHeaders(reqHeaders)
				.build();

		HttpContext localContext = new BasicHttpContext();

		StringEntity input = new StringEntity(payload.toString());
		input.setContentType(FETCHER_CONTENT_TYPE);

		HttpPost httpPost = new HttpPost(fetcherUrl);
		httpPost.setEntity(input);	
		httpPost.setEntity(new StringEntity(payload.toString(), ContentType.create(FETCHER_CONTENT_TYPE)));
		httpPost.setConfig(requestConfig);

		// do request
		CloseableHttpResponse closeableHttpResponse = httpclient.execute(httpPost, localContext);

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
		pageContent.setUrl(fetcherUrl); // setting url

		Integer responseLength = pageContent.getContentData().length;

		// assembling request information log message
		DataFetcher.sendRequestInfoLog(
				FETCHER_HOST, 
				DataFetcher.POST_REQUEST, 
				null, 
				null, 
				session, 
				closeableHttpResponse, 
				responseLength, 
				requestHash);

		// saving request content result on Amazon
		String content;
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

		// process response and parse
		return new JSONObject(DataFetcher.processContent(pageContent, session));
	}

	/**
	 * Build payload to request 'FETCHER'
	 * 
	 * @param url Url to be request
	 * @param method Get OR Post 
	 * @param retrieveStatistics if you need the statistics of all requests to return your answer
	 * @param payloadPOSTRequest Payload to post requests -- if doesn't needed put null
	 * @param headers Map<String,String> with all headers to be used -- if doesn't needed put null
	 * @inactive anyProxies List<String> with name of proxies to be used -- if doesn't needed put null
	 * @param specificProxy JSONObject Ex:
	 * 			 {"source":"proxy_5","host":"1.1.1.1.1","port":89745,"location":"England","user":"crawler","pass":"12345"} -- if doesn't needed put null
	 * 
	 * @return String with all parameters for request 'FETCHER'
	 */
	public static JSONObject fetcherPayloadBuilder(
			String url, 
			String method, 
			boolean retrieveStatistics, 
			String payloadPOSTRequest, 
			Map<String,String> headers, 
			/*List<String> anyProxies,*/
			JSONObject specificProxy) {

		JSONObject payload = new JSONObject();

		payload.put(FETCHER_PARAMETER_URL, url);
		payload.put(FETCHER_PARAMETER_METHOD, method);
		payload.put(FETCHER_PARAMETER_RETRIEVE_STATISTICS, retrieveStatistics);

		if(payloadPOSTRequest != null ||  headers != null) {
			JSONObject requestParameters = new JSONObject();

			if(payloadPOSTRequest != null) {
				requestParameters.put("payload", payloadPOSTRequest);
			}

			if(headers != null && !headers.isEmpty()) {
				JSONObject headersOBJ = new JSONObject();

				for(Entry<String,String> entry : headers.entrySet()) {
					headersOBJ.put(entry.getKey(), entry.getValue());
				}

				requestParameters.put("headers", headersOBJ);
			}

			payload.put(FETCHER_PARAMETER_REQUEST_PARAMETERS, requestParameters);
		}

		if(specificProxy != null) {
			JSONObject specific = new JSONObject();
			specific.put("specific", specificProxy);

			payload.put(FETCHER_PARAMETER_PROXIES, specific);
		} else {
			JSONObject proxies = new JSONObject();

			JSONArray any = new JSONArray();
			any.put(ProxyCollection.BUY);
			any.put(ProxyCollection.AZURE);
			any.put(ProxyCollection.STORM);
			any.put(ProxyCollection.LUMINATI_SERVER_BR);
			any.put(ProxyCollection.BONANZA);
			any.put(ProxyCollection.LUMINATI_RESIDENTIAL_BR);
			any.put(ProxyCollection.NO_PROXY);

			proxies.put("any", any);
			payload.put(FETCHER_PARAMETER_PROXIES, proxies);
		}

		return payload;
	}
	
	/**
	 * Get requets config
	 * @param proxy
	 * @return
	 */
	private static RequestConfig getRequestConfig(HttpHost proxy) {
		if (proxy != null) {
			return RequestConfig.custom()
					.setCookieSpec(CookieSpecs.STANDARD)
					.setRedirectsEnabled(true)
					.setConnectionRequestTimeout(DataFetcher.DEFAULT_CONNECTION_REQUEST_TIMEOUT)
					.setConnectTimeout(DataFetcher.DEFAULT_CONNECT_TIMEOUT)
					.setSocketTimeout(DataFetcher.DEFAULT_SOCKET_TIMEOUT)
					.setProxy(proxy)
					.build();
		}
			
		return RequestConfig.custom()
				.setCookieSpec(CookieSpecs.STANDARD)
				.setRedirectsEnabled(true)
				.setConnectionRequestTimeout(DataFetcher.DEFAULT_CONNECTION_REQUEST_TIMEOUT)
				.setConnectTimeout(DataFetcher.DEFAULT_CONNECT_TIMEOUT)
				.setSocketTimeout(DataFetcher.DEFAULT_SOCKET_TIMEOUT)
				.build();
	}
}
