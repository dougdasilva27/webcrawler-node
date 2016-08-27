package br.com.lett.crawlernode.util;

import java.lang.management.ManagementFactory;
import java.text.Normalizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import br.com.lett.crawlernode.kernel.task.CrawlerSession;
import br.com.lett.crawlernode.main.ExecutionParameters;
import br.com.lett.crawlernode.test.TestExecutionParameters;

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
		logger.info("[MSG]" + sanitizeMessage(msg));
	}
	
	public static void printLogInfo(Logger logger, CrawlerSession session, String msg) {
		logger.info("[SESSION]" + session.getSessionId() + " [CITY]" + session.getMarket().getCity() + " [MARKET]" + session.getMarket().getName() + " [MSG]" + sanitizeMessage(msg));
	}
	
	/* ERROR */
	public static void printLogError(Logger logger, String msg) {
		logger.error("[MSG]" + sanitizeMessage(msg));
	}
	
	public static void printLogError(Logger logger, CrawlerSession session, String msg) {
		logger.error("[SESSION]" + session.getSessionId() + " [CITY]" + session.getMarket().getCity() + " [MARKET]" + session.getMarket().getName() + " [MSG]" + sanitizeMessage(msg));
	}
	
	/* DEBUG */
	public static void printLogDebug(Logger logger, String msg) {
		logger.debug("[MSG]" + sanitizeMessage(msg));
	}
	
	public static void printLogDebug(Logger logger, CrawlerSession session, String msg) {
		logger.debug("[SESSION]" + session.getSessionId() + " [CITY]" + session.getMarket().getCity() + " [MARKET]" + session.getMarket().getName() + " [MSG]" + sanitizeMessage(msg));
	}
	
	/* WARN */
	public static void printLogWarn(Logger logger, String msg) {
		logger.warn("[MSG]" + sanitizeMessage(msg));
	}
	
	public static void printLogWarn(Logger logger, CrawlerSession session, String msg) {
		logger.warn("[SESSION]" + session.getSessionId() + " [CITY]" + session.getMarket().getCity() + " [MARKET]" + session.getMarket().getName() + " [MSG]" + sanitizeMessage(msg));
	}
	
	/* TRACE */
	public static void printLogTrace(Logger logger, String msg) {
		logger.trace("[MSG]" + sanitizeMessage(msg));
	}
	
	public static void printLogTrace(Logger logger, CrawlerSession session, String msg) {
		logger.trace("[SESSION]" + session.getSessionId() + " [CITY]" + session.getMarket().getCity() + " [MARKET]" + session.getMarket().getName() + " [MSG]" + sanitizeMessage(msg));
	}
	
	/**
	 * Set up MDC variables to be used in logback.xml log config file
	 * @param executionParameters
	 */
	public static void setLogMDC(ExecutionParameters executionParameters) {
		String pid = ManagementFactory.getRuntimeMXBean().getName().replaceAll("@.*", "");
		String hostName = ManagementFactory.getRuntimeMXBean().getName().replaceAll("\\d+@", "");

		MDC.put("PID", pid);
		MDC.put("HOST_NAME", hostName);
		MDC.put("PROCESS_NAME", "lett");

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
	 * Set up MDC variables when running crawler tests
	 * @param testExecutionParameters
	 */
	public static void setLogMDCTest(TestExecutionParameters testExecutionParameters) {
		String pid = ManagementFactory.getRuntimeMXBean().getName().replaceAll("@.*", "");
		String hostName = ManagementFactory.getRuntimeMXBean().getName().replaceAll("\\d+@", "");

		MDC.put("PID", pid);
		MDC.put("HOST_NAME", hostName);
		MDC.put("PROCESS_NAME", "webcrawler_node");

		if (testExecutionParameters != null) {

			if (testExecutionParameters.getDebug()) {
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
