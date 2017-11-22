package br.com.lett.crawlernode.util;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.cookie.Cookie;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.lett.crawlernode.aws.s3.S3Service;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.LettProxy;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.ranking.RankingSession;
import br.com.lett.crawlernode.core.session.ranking.TestRankingSession;

public class URLBox {

	private URLBox() {
		super();
	}

	private static final Logger logger = LoggerFactory.getLogger(URLBox.class);

	public static String takeAScreenShot(String url, Session session, int page,
			List<Cookie> cookies) {
		String s3Link;

		String urlboxKey = "2hXKGlSeR95wCDVl";
		String urlboxSecret = "98108a7bb45240f3b18ed1ea75906d6f";

		// Set request options
		Map<String, Object> options = new HashMap<>();
		options.put("full_page", "true");

		LettProxy proxy = session.getRequestProxy(url);

		if (proxy != null) {
			if (proxy.getUser() != null) {
				options.put("proxy", proxy.getUser() + "%3A" + proxy.getPass() + "%40" + proxy.getAddress()
						+ "%3A" + proxy.getPort());
			} else if (proxy.getAddress() != null && proxy.getPort() != null) {
				options.put("proxy", proxy.getAddress() + "%3A" + proxy.getPort());
			}
		}


		options.put("use_s3", true);
		options.put("force", true);
		// options.put("save_html", true);

		String date = new DateTime(DateTimeZone.forID("America/Sao_Paulo")).toString("yyyy-MM-dd");
		String dateTime =
				new DateTime(DateTimeZone.forID("America/Sao_Paulo")).toString("yyyy-MM-dd hh:mm:ss");
		String hash = DigestUtils.md5Hex(UUID.randomUUID().toString() + new DateTime().toString());

		StringBuilder urlBuilder = new StringBuilder();
		urlBuilder.append("https://s3.amazonaws.com/" + S3Service.SCREENSHOT_BUCKET_NAME);

		String pathRanking = "ranking";

		if (session instanceof RankingSession) {
			String path =
					"%2F" + pathRanking + "%2F" + ((RankingSession) session).getLocation().replace(" ", "_")
							+ "%2F" + session.getMarket().getName() + "%2F" + date + "%2F" + page + "-" + hash;

			urlBuilder.append(path);
			options.put("s3_path", path);
		} else if (session instanceof TestRankingSession) {
			String path = "%2F" + pathRanking + "%2F"
					+ ((TestRankingSession) session).getLocation().replace(" ", "_") + "%2F"
					+ session.getMarket().getName() + "%2F" + date + "%2F" + page + "-" + hash;

			urlBuilder.append(path);
			options.put("s3_path", path);
		} else {
			String path = "%2Fblackfriday%2F" + session.getProcessedId() + "%2F"
					+ dateTime.replace(" ", "%20").replace(":", "%3A");

			urlBuilder.append(path);
			options.put("s3_path", path);
		}

		urlBuilder.append(".jpeg");
		s3Link = urlBuilder.toString().replace("%2F", "/");

		try {
			Logging.printLogDebug(logger, session, "Take a screenshot for url: " + url);

			// Call generateUrl function of urlbox object
			String apiUrl = generateUrl(url, options, urlboxKey, urlboxSecret, cookies, session);

			Logging.printLogDebug(logger, session, "Api url " + apiUrl);

			DataFetcher.fetchPageAPIUrlBox(apiUrl, session);
		} catch (UnsupportedEncodingException ex) {
			Logging.printLogError(logger, session, "Problem with url encoding");
			Logging.printLogError(logger, CommonMethods.getStackTrace(ex));
		}

		Logging.printLogDebug(logger, session, "Screenshot was send to s3. Link: " + s3Link);

		return s3Link;
	}

	public static String getUrlForHtmlUrlBox(String url, Session session, int page,
			List<Cookie> cookies) {
		String s3Link = null;

		String urlboxKey = "2hXKGlSeR95wCDVl";
		String urlboxSecret = "98108a7bb45240f3b18ed1ea75906d6f";

		String pathRanking = "ranking";

		// Set request options
		Map<String, Object> options = new HashMap<>();
		options.put("full_page", "true");

		// LettProxy proxy = session.getRequestProxy(url);

		/*
		 * if(proxy != null && !proxy.getSource().contains("luminati")) { if(proxy.getUser() != null) {
		 * options.put("proxy", proxy.getUser() + "%3A" + proxy.getPass() + "%40"+ proxy.getAddress()+
		 * "%3A" + proxy.getPort()); } else if( proxy.getAddress() != null && proxy.getPort() != null) {
		 * options.put("proxy", proxy.getAddress() + "%3A" + proxy.getPort()); } }
		 */

		options.put("use_s3", true);
		options.put("force", true);
		options.put("save_html", true);

		String date = new DateTime(DateTimeZone.forID("America/Sao_Paulo")).toString("yyyy-MM-dd");
		String dateTime =
				new DateTime(DateTimeZone.forID("America/Sao_Paulo")).toString("yyyy-MM-dd hh:mm:ss");

		String hash = DigestUtils.md5Hex(UUID.randomUUID().toString() + new DateTime().toString());

		StringBuilder urlBuilder = new StringBuilder();
		urlBuilder.append("https://s3.amazonaws.com/" + S3Service.SCREENSHOT_BUCKET_NAME);

		if (session instanceof RankingSession) {
			String path =
					"%2F" + pathRanking + "%2F" + ((RankingSession) session).getLocation().replace(" ", "_")
							+ "%2F" + session.getMarket().getName() + "%2F" + date + "%2F" + page + "-" + hash;

			urlBuilder.append(path);
			options.put("s3_path", path);
		} else if (session instanceof TestRankingSession) {
			String path = "%2F" + pathRanking + "%2F"
					+ ((TestRankingSession) session).getLocation().replace(" ", "_") + "%2F"
					+ session.getMarket().getName() + "%2F" + date + "%2F" + page + "-" + hash;

			urlBuilder.append(path);
			options.put("s3_path", path);
		} else {
			String path = "%2Fteste%2F" + session.getProcessedId() + "%2F"
					+ dateTime.replace(" ", "---").replace(":", "-");

			urlBuilder.append(path);
			options.put("s3_path", path);
		}

		urlBuilder.append(".html");
		s3Link = urlBuilder.toString().replace("%2F", "/");

		try {
			Logging.printLogDebug(logger, session, "Take a screenshot for url: " + url);

			// Call generateUrl function of urlbox object
			String apiUrl = generateUrl(url, options, urlboxKey, urlboxSecret, cookies, session);

			Logging.printLogDebug(logger, session, "Api url " + apiUrl);

			DataFetcher.fetchPageAPIUrlBox(apiUrl, session);
		} catch (UnsupportedEncodingException ex) {
			Logging.printLogError(logger, session, "Problem with url encoding");
			Logging.printLogError(logger, CommonMethods.getStackTrace(ex));
		}

		Logging.printLogDebug(logger, session, "Screenshot was send to s3.");

		return s3Link;
	}

	public static String generateUrl(String url, Map<String, Object> options, String key,
			String secret, List<Cookie> cookies, Session session) throws UnsupportedEncodingException {

		String encodedUrl = URLEncoder.encode(url, "UTF-8");
		StringBuilder queryString = new StringBuilder();
		queryString.append(String.format("url=%s", encodedUrl));


		for (Map.Entry<String, Object> entry : options.entrySet()) {
			String queryParam = "&" + entry.getKey() + "=" + entry.getValue();
			queryString.append(queryParam);
		}

		if (cookies != null) {
			for (Cookie c : cookies) {
				queryString.append("&cookie=" + c.getName() + "%3D" + c.getValue());
			}
		}

		String token = generateToken(queryString.toString(), secret, session);
		return String.format("https://api.urlbox.io/v1/%s/%s/jpeg?%s", key, token,
				queryString.toString());
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
