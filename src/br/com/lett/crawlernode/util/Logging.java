package br.com.lett.crawlernode.util;

import java.lang.management.ManagementFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import br.com.lett.crawlernode.base.ExecutionParameters;
import br.com.lett.crawlernode.models.CrawlerSession;

public class Logging {
	protected static final Logger logger = LoggerFactory.getLogger(Logging.class);
	
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
	
	public static void printLogError(Logger logger, CrawlerSession session, String msg) {
		logger.error("[SESSION]" + session.getSessionId() + " [CITY]" + session.getMarket().getCity() + " [MARKET]" + session.getMarket().getName() + " [MSG]" + msg.trim());
	}
	
	/* DEBUG */
	public static void printLogDebug(Logger logger, String msg) {
		logger.debug("[MSG]" + msg.trim());
	}
	
	public static void printLogDebug(Logger logger, String city, String market, String msg) {
		logger.debug("[CITY]" + city + " [MARKET]" + market + " [MSG]" + msg.trim());
	}
	
	public static void printLogDebug(Logger logger, CrawlerSession session, String msg) {
		logger.debug("[SESSION]" + session.getSessionId() + " [CITY]" + session.getMarket().getCity() + " [MARKET]" + session.getMarket().getName() + " [MSG]" + msg.trim());
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
	
	
	public static void setLogMDC(ExecutionParameters executionParameters) {
		String pid = ManagementFactory.getRuntimeMXBean().getName().replaceAll("@.*", "");
		String hostName = ManagementFactory.getRuntimeMXBean().getName().replaceAll("\\d+@", "");

		MDC.put("PID", pid);
		MDC.put("HOST_NAME", hostName);
		MDC.put("PROCESS_NAME", "java");

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

}
