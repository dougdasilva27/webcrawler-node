package br.com.lett.crawlernode.util;

import java.text.Normalizer;

import org.json.JSONObject;
import org.slf4j.Logger;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.ranking.RankingKeywordsSession;

/**
 * This class contains static methods to print log messages using the logback lib.
 * Log configurations can be found on logback.xml file. The mthods are customized
 * to be used within crawler-node project.
 * @author Samir Leao
 *
 */

public class Logging {

	private static final String VERSION = new Version().getVersion();
	
	private static final String METADATA_TAG = "[METADATA]";

	private Logging() {
		super();
	}

	/* INFO */
	public static void printLogInfo(Logger logger, String msg) {
		printLogInfo(logger, null, msg);
	}

	public static void printLogInfo(Logger logger, Session session, String msg) {
		logInfo(logger, session, null, msg);
	}

	public static void logInfo(Logger logger, Session session, JSONObject metadata, String msg) {
		logger.info(sanitizeMessage(msg) + " " + METADATA_TAG + createMetadata(metadata, session).toString());	
	}

	/* ERROR */
	public static void printLogError(Logger logger, String msg) {
		printLogError(logger, null, msg);
	}

	public static void printLogError(Logger logger, Session session, String msg) {
		logError(logger, session, null, msg);
	}

	public static void logError(Logger logger, Session session, JSONObject metadata, String msg) {
		logger.error(sanitizeMessage(msg) + " " + METADATA_TAG + createMetadata(metadata, session).toString());	
	}


	/* DEBUG */
	public static void printLogDebug(Logger logger, String msg) {
		printLogDebug(logger, null, msg);
	}

	public static void printLogDebug(Logger logger, Session session, String msg) {
		logDebug(logger, session, null, msg);
	}

	public static void logDebug(Logger logger, Session session, JSONObject metadata, String msg) {
		logger.debug(sanitizeMessage(msg) + " " + METADATA_TAG + createMetadata(metadata, session).toString());	
	}


	/* WARN */
	public static void printLogWarn(Logger logger, String msg) {
		printLogWarn(logger, null, msg);
	}

	public static void printLogWarn(Logger logger, Session session, String msg) {
		logWarn(logger, session, null, msg);
	}

	public static void logWarn(Logger logger, Session session, JSONObject metadata, String msg) {
		logger.warn(sanitizeMessage(msg) + " " + METADATA_TAG + createMetadata(metadata, session).toString());	
	}


	/* TRACE */
	public static void printLogTrace(Logger logger, String msg) {
		printLogTrace(logger, null, msg);
	}

	public static void printLogTrace(Logger logger, Session session, String msg) {
		logTrace(logger, session, null, msg);
	}

	public static void logTrace(Logger logger, Session session, JSONObject metadata, String msg) {
		logger.warn(sanitizeMessage(msg) + " " + METADATA_TAG + createMetadata(metadata, session).toString());	
	}


	private static JSONObject createMetadata(JSONObject metadata, Session session) {
		if (metadata == null || !(metadata instanceof JSONObject)) {
			metadata = new JSONObject();
		}

		metadata.put("version",  VERSION);

		if (session != null) {
			metadata.put("city", session.getMarket().getCity());
			metadata.put("market", session.getMarket().getName());
			metadata.put("session", session.getSessionId());
			metadata.put("session_type", session.getClass().getSimpleName());

			if (session instanceof RankingKeywordsSession) {
				metadata.put("location", ((RankingKeywordsSession)session).getLocation());
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
