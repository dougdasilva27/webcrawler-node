package br.com.lett.crawlernode.util;

import java.text.Normalizer;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.main.ExecutionParameters;

/**
 * This class contains static methods to print log messages using the logback lib.
 * Log configurations can be found on logback.xml file. The mthods are customized
 * to be used within crawler-node project.
 * @author Samir Leao
 *
 */

public class Logging {
	protected static final Logger logger = LoggerFactory.getLogger(Logging.class);
	
	/* INFO */
	public static void printLogInfo(Logger logger, String msg) {
		printLogInfo(logger, null, msg);
	}
	
	public static void printLogInfo(Logger logger, CrawlerSession session, String msg) {
		printLogInfo(logger, session, null, msg);
	}
	
	public static void printLogInfo(Logger logger, CrawlerSession session, JSONObject metadata, String msg) {
		logger.info("[MSG]" + sanitizeMessage(msg) + " [METADATA]" + createMetadata(metadata, session).toString());	
	}
	
	/* ERROR */
	public static void printLogError(Logger logger, String msg) {
		printLogError(logger, null, msg);
	}
	
	public static void printLogError(Logger logger, CrawlerSession session, String msg) {
		printLogError(logger, session, null, msg);
	}
	
	public static void printLogError(Logger logger, CrawlerSession session, JSONObject metadata, String msg) {
		logger.error("[MSG]" + sanitizeMessage(msg) + " [METADATA]" + createMetadata(metadata, session).toString());	
	}
	
	
	/* DEBUG */
	public static void printLogDebug(Logger logger, String msg) {
		printLogDebug(logger, null, msg);
	}
	
	public static void printLogDebug(Logger logger, CrawlerSession session, String msg) {
		printLogDebug(logger, session, null, msg);
	}
	
	public static void printLogDebug(Logger logger, CrawlerSession session, JSONObject metadata, String msg) {
		logger.debug("[MSG]" + sanitizeMessage(msg) + " [METADATA]" + createMetadata(metadata, session).toString());	
	}
	
	
	/* WARN */
	public static void printLogWarn(Logger logger, String msg) {
		printLogWarn(logger, null, msg);
	}
	
	public static void printLogWarn(Logger logger, CrawlerSession session, String msg) {
		printLogWarn(logger, session, null, msg);
	}
	
	public static void printLogWarn(Logger logger, CrawlerSession session, JSONObject metadata, String msg) {
		logger.warn("[MSG]" + sanitizeMessage(msg) + " [METADATA]" + createMetadata(metadata, session).toString());	
	}
	
	
	/* TRACE */
	public static void printLogTrace(Logger logger, String msg) {
		printLogTrace(logger, null, msg);
	}
	
	public static void printLogTrace(Logger logger, CrawlerSession session, String msg) {
		printLogTrace(logger, session, null, msg);
	}
	
	public static void printLogTrace(Logger logger, CrawlerSession session, JSONObject metadata, String msg) {
		logger.warn("[MSG]" + sanitizeMessage(msg) + " [METADATA]" + createMetadata(metadata, session).toString());	
	}
	
	
	private static JSONObject createMetadata(JSONObject metadata, CrawlerSession session) {
		
		if(metadata == null || !(metadata instanceof JSONObject) ) metadata = new JSONObject();
		metadata.put("version",  CommonMethods.getVersion());

		if(session != null) {
			metadata.put("city", session.getMarket().getCity());
			metadata.put("market", session.getMarket().getName());
			metadata.put("session", session.getSessionId());
		}
		
		return metadata;
		
	}
	
	/**
	 * Set up MDC variables to be used in logback.xml log config file
	 * @param executionParameters
	 */
	public static void setLogMDC(ExecutionParameters executionParameters) {

		MDC.put("PROCESS_NAME", "webcrawler_node");

		if (executionParameters != null) {

			MDC.put("ENVIRONMENT", executionParameters.getEnvironment());

			if (executionParameters.getDebug()) {
				MDC.put("DEBUG_MODE", "true");
			} else {
				MDC.put("DEBUG_MODE", "false");
			}

		} else {
			Logging.printLogError(logger, "Fatal error during MDC setup: execution parameters are not ready. Please, initialize them first.");
			System.exit(0);
		}
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
