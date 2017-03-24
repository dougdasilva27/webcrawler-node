package br.com.lett.crawlernode.util;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.session.Session;

public class URLBox {

	private URLBox() {
		super();
	}
	
	private static final Logger logger = LoggerFactory.getLogger(URLBox.class);
	
	public static void takeAScreenShot(String url, Session session) {
		String urlboxKey = "2hXKGlSeR95wCDVl";
		String urlboxSecret = "98108a7bb45240f3b18ed1ea75906d6f";

		// Set request options
		Map<String, Object> options = new HashMap<>();
		options.put("full_page", "true");
		options.put("proxy", "proxylett%3AJ7Rs9rnq%40187.45.228.130%3A60099");
		options.put("use_s3", true);
		
		String date = new DateTime(DateTimeZone.forID("America/Sao_Paulo")).toString("yyyy-MM-dd_HH-mm-ss");
		
		options.put("s3_path", "%2Fteste_24-04%2F" + session.getProcessedId() + "%2F" + date);
		
		options.put("force", true);
		options.put("save_html", true);

		try {
			Logging.printLogDebug(logger, session, "Take a screenshot for url: " + url);
			
			// Call generateUrl function of urlbox object
			String apiUrl = generateUrl(url , options, urlboxKey, urlboxSecret, session);
			
			Logging.printLogDebug(logger, session, "Api url " + apiUrl);
			
			DataFetcher.fetchPageAPIUrlBox(apiUrl);
		} catch (UnsupportedEncodingException ex) {
			Logging.printLogError(logger, session, "Problem with url encoding");
			Logging.printLogError(logger, CommonMethods.getStackTrace(ex));
		}
		
		Logging.printLogDebug(logger, session, "Screenshot was send to s3.");
	}

	public static String generateUrl(String url, Map<String,Object> options, String key, String secret, Session session) throws UnsupportedEncodingException {

		String encodedUrl = URLEncoder.encode(url, "UTF-8");
		StringBuilder queryString = new StringBuilder();
		queryString.append(String.format("url=%s", encodedUrl));

		for (Map.Entry<String, Object> entry : options.entrySet()) {
			String queryParam = "&"+entry.getKey()+"="+entry.getValue(); 
			queryString.append(queryParam);
		}

		String token = generateToken(queryString.toString(), secret, session);
		return String.format("https://api.urlbox.io/v1/%s/%s/jpg?%s", key, token, queryString.toString());
	}

	private static String generateToken(String input, String key, Session session) {
		String lSignature = "None";
		try {
			Mac lMac = Mac.getInstance("HmacSHA1");
			SecretKeySpec lSecret = new SecretKeySpec(key.getBytes(), "HmacSHA1");
			lMac.init(lSecret);

			byte[] lDigest = lMac.doFinal(input.getBytes());
			BigInteger lHash = new BigInteger(1, lDigest);
			lSignature = lHash.toString(16);
			if ((lSignature.length() % 2) != 0) {
				lSignature = "0" + lSignature;
			}
		} catch (NoSuchAlgorithmException | InvalidKeyException lEx) {
			Logging.printLogError(logger, session, "Problems calculating HMAC");
			Logging.printLogError(logger, session, CommonMethods.getStackTrace(lEx));
		} 
		
		return lSignature;
	}
}
