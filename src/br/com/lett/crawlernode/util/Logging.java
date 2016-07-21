package br.com.lett.crawlernode.util;

import org.slf4j.Logger;

public class Logging {
	
	/* INFO */
	public static void printLogInfo(Logger logger, String msg) {
		logger.info("[MSG]" + msg.trim());
	}
	
	public static void printLogInfo(Logger logger, String city, String market, String msg) {
		logger.info("[CITY]" + city + " [MARKET]" + market + " [MSG]" + msg.trim());
	}
	
	/* ERROR */
	public static void printLogError(Logger logger, String msg) {
		logger.error("[MSG]" + msg.trim());
	}
	
	public static void printLogError(Logger logger, String city, String market, String msg) {
		logger.error("[CITY]" + city + " [MARKET]" + market + " [MSG]" + msg.trim());
	}
//	
//	/* DEBUG */
	public static void printLogDebug(Logger logger, String msg) {
		logger.debug("[MSG]" + msg.trim());
	}
	
	public static void printLogDebug(Logger logger, String city, String market, String msg) {
		logger.debug("[CITY]" + city + " [MARKET]" + market + " [MSG]" + msg.trim());
	}
	
	/* WARN */
	public static void printLogWarn(Logger logger, String msg) {
		logger.warn("[MSG]" + msg.trim());
	}
	
	public static void printLogWarn(Logger logger, String city, String market, String msg) {
		logger.warn("[CITY]" + city + " [MARKET]" + market + " [MSG]" + msg.trim());
	}
	
	/* TRACE */
	public static void printLogTrace(Logger logger, String msg) {
		logger.trace("[MSG]" + msg.trim());
	}
	
	public static void printLogTrace(Logger logger, String city, String market, String msg) {
		logger.trace("[CITY]" + city + " [MARKET]" + market + " [MSG]" + msg.trim());
	}

}
