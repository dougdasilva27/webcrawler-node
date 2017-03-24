package br.com.lett.crawlernode.core.fetcher.methods;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
				requestConfig = RequestConfig.custom()
						.setCookieSpec(CookieSpecs.STANDARD)
						.setRedirectsEnabled(false)
						.setConnectionRequestTimeout(DataFetcher.DEFAULT_CONNECTION_REQUEST_TIMEOUT)
						.setConnectTimeout(DataFetcher.DEFAULT_CONNECT_TIMEOUT)
						.setSocketTimeout(DataFetcher.DEFAULT_SOCKET_TIMEOUT)
						.setProxy(proxy)
						.build();
			} else {
				requestConfig = RequestConfig.custom()
						.setCookieSpec(CookieSpecs.STANDARD)
						.setRedirectsEnabled(false)
						.setConnectionRequestTimeout(DataFetcher.DEFAULT_CONNECTION_REQUEST_TIMEOUT)
						.setConnectTimeout(DataFetcher.DEFAULT_CONNECT_TIMEOUT)
						.setSocketTimeout(DataFetcher.DEFAULT_SOCKET_TIMEOUT)
						.build();
			}

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

			RequestConfig requestConfig = null;
			if (proxy != null) {
				requestConfig = RequestConfig.custom()
						.setCookieSpec(CookieSpecs.STANDARD)
						.setRedirectsEnabled(true)
						.setConnectionRequestTimeout(DataFetcher.DEFAULT_CONNECTION_REQUEST_TIMEOUT)
						.setConnectTimeout(DataFetcher.DEFAULT_CONNECT_TIMEOUT)
						.setSocketTimeout(DataFetcher.DEFAULT_SOCKET_TIMEOUT)
						.setProxy(proxy)
						.build();
			} else {
				requestConfig = RequestConfig.custom()
						.setCookieSpec(CookieSpecs.STANDARD)
						.setRedirectsEnabled(true)
						.setConnectionRequestTimeout(DataFetcher.DEFAULT_CONNECTION_REQUEST_TIMEOUT)
						.setConnectTimeout(DataFetcher.DEFAULT_CONNECT_TIMEOUT)
						.setSocketTimeout(DataFetcher.DEFAULT_SOCKET_TIMEOUT)
						.build();
			}

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

}
