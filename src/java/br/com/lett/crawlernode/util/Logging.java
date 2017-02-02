package br.com.lett.crawlernode.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.MDC;

import br.com.lett.crawlernode.core.session.RankingKeywordsSession;
import br.com.lett.crawlernode.core.session.Session;

import com.amazonaws.util.EC2MetadataUtils;

/**
 * This class contains static methods to print log messages using the logback lib.
 * Log configurations can be found on logback.xml file. The mthods are customized
 * to be used within crawler-node project.
 * @author Samir Leao
 *
 */

public class Logging {
	
	/**
	 * Set up MDC variables to be used in logback.xml log config file
	 */
	public static void setLogMDC() {

		Pattern IPV4_PATTERN = Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
		
		String host = EC2MetadataUtils.getPrivateIpAddress();
		
		// Avoiding ip parse errors in Elasticsearch index
		if(host == null || !IPV4_PATTERN.matcher(host).matches()) {
			host = "0.0.0.0";
		}
		
		MDC.put("PATH", EC2MetadataUtils.getAvailabilityZone());
		MDC.put("SOURCE", EC2MetadataUtils.getInstanceId());
		MDC.put("HOST", host);

	}
		
	/* INFO */
	public static void printLogInfo(Logger logger, String msg) {
		printLogInfo(logger, null, msg);
	}
	
	public static void printLogInfo(Logger logger, Session session, String msg) {
		printLogInfo(logger, session, null, msg);
	}
	
	public static void printLogInfo(Logger logger, Session session, JSONObject metadata, String msg) {
		logger.info("[MSG]" + sanitizeMessage(msg) + " [METADATA]" + createMetadata(metadata, session).toString());	
	}
	
	/* ERROR */
	public static void printLogError(Logger logger, String msg) {
		printLogError(logger, null, msg);
	}
	
	public static void printLogError(Logger logger, Session session, String msg) {
		printLogError(logger, session, null, msg);
	}
	
	public static void printLogError(Logger logger, Session session, JSONObject metadata, String msg) {
		logger.error("[MSG]" + sanitizeMessage(msg) + " [METADATA]" + createMetadata(metadata, session).toString());	
	}
	
	
	/* DEBUG */
	public static void printLogDebug(Logger logger, String msg) {
		printLogDebug(logger, null, msg);
	}
	
	public static void printLogDebug(Logger logger, Session session, String msg) {
		printLogDebug(logger, session, null, msg);
	}
	
	public static void printLogDebug(Logger logger, Session session, JSONObject metadata, String msg) {
		logger.debug("[MSG]" + sanitizeMessage(msg) + " [METADATA]" + createMetadata(metadata, session).toString());	
	}
	
	
	/* WARN */
	public static void printLogWarn(Logger logger, String msg) {
		printLogWarn(logger, null, msg);
	}
	
	public static void printLogWarn(Logger logger, Session session, String msg) {
		printLogWarn(logger, session, null, msg);
	}
	
	public static void printLogWarn(Logger logger, Session session, JSONObject metadata, String msg) {
		logger.warn("[MSG]" + sanitizeMessage(msg) + " [METADATA]" + createMetadata(metadata, session).toString());	
	}
	
	
	/* TRACE */
	public static void printLogTrace(Logger logger, String msg) {
		printLogTrace(logger, null, msg);
	}
	
	public static void printLogTrace(Logger logger, Session session, String msg) {
		printLogTrace(logger, session, null, msg);
	}
	
	public static void printLogTrace(Logger logger, Session session, JSONObject metadata, String msg) {
		logger.warn("[MSG]" + sanitizeMessage(msg) + " [METADATA]" + createMetadata(metadata, session).toString());	
	}
	
	
	private static JSONObject createMetadata(JSONObject metadata, Session session) {
		
		if(metadata == null || !(metadata instanceof JSONObject) ) metadata = new JSONObject();
		metadata.put("version",  CommonMethods.getVersion());

		if(session != null) {
			metadata.put("city", session.getMarket().getCity());
			metadata.put("market", session.getMarket().getName());
			metadata.put("session", session.getSessionId());
			metadata.put("session_type", session.getClass().getSimpleName());
			
			if(session instanceof RankingKeywordsSession) {
				metadata.put("location", ((RankingKeywordsSession)session).getKeyword());
			}
		}
		
		return metadata;
		
	}
	
	/**
	 * Sanitize message before logging then
	 * @param msg
	 */
	public static String sanitizeMessage(String msg) {
		if(msg == null) {
			return "";
		} else {
			msg = Normalizer.normalize(msg, Normalizer.Form.NFD);
			msg = msg.replaceAll("[^\\p{ASCII}]", "").trim();
			
			return msg;
		}
	}

}
