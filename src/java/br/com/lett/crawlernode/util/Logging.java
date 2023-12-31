package br.com.lett.crawlernode.util;

import java.text.Normalizer;

import org.json.JSONObject;
import org.slf4j.Logger;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.ranking.RankingKeywordsSession;
import br.com.lett.crawlernode.main.GlobalConfigurations;

/**
 * This class contains static methods to print log messages using the logback lib. Log
 * configurations can be found on logback.xml file. The mthods are customized to be used within
 * crawler-node project.
 *
 * @author Samir Leao
 */

public class Logging {

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

   public static void printLogError(Logger logger, String msg) {
      printLogError(logger, null, msg);
   }

   public static void printLogError(Logger logger, Session session, String msg) {
      logError(logger, session, null, msg);
   }

   public static void logError(Logger logger, Session session, JSONObject metadata, String msg) {
      logger.error(sanitizeMessage(msg) + " " + METADATA_TAG + createMetadata(metadata, session).toString());
   }

   public static void printLogDebug(Logger logger, String msg) {
      printLogDebug(logger, null, msg);
   }

   public static void printLogDebug(Logger logger, Session session, String msg) {
      logDebug(logger, session, null, msg);
   }

   public static void logDebug(Logger logger, Session session, JSONObject metadata, String msg) {
      logger.debug(sanitizeMessage(msg) + " " + METADATA_TAG + createMetadata(metadata, session).toString());
   }

   public static void printLogWarn(Logger logger, String msg) {
      printLogWarn(logger, null, msg);
   }

   public static void printLogWarn(Logger logger, Session session, String msg) {
      logWarn(logger, session, null, msg);
   }

   public static void logWarn(Logger logger, Session session, JSONObject metadata, String msg) {
      logger.warn(sanitizeMessage(msg) + " " + METADATA_TAG + createMetadata(metadata, session).toString());
   }

   private static JSONObject createMetadata(JSONObject metadata, Session session) {
      if (metadata == null) {
         metadata = new JSONObject();
      }

      if (session != null) {
         metadata.put("market", session.getMarket().getName());
         metadata.put("market_id", session.getMarket().getNumber());
         metadata.put("web_driver", session.isWebDriver());

         String internalId = session.getInternalId();
         if (internalId != null) {
            metadata.put("internal_id", internalId);
         }

         String originalUrl = session.getOriginalURL();
         if (originalUrl != null) {
            metadata.put("url", originalUrl);
         }

         Long supplierId = session.getSupplierId();
         if (supplierId != null) {
            metadata.put("supplier_id", supplierId);
         }

         metadata.put("session", session.getSessionId());
         metadata.put("session_type", session.getClass().getSimpleName());
         metadata.put("env", GlobalConfigurations.executionParameters.getEnvironment());

         if (session instanceof RankingKeywordsSession) {
            metadata.put("location", ((RankingKeywordsSession) session).getLocation());
         }
      }

      return metadata;
   }

   /**
    * Sanitize message before logging then
    */
   public static String sanitizeMessage(String msg) {
      if (msg == null) {
         return "";
      } else {
         msg = Normalizer.normalize(msg, Normalizer.Form.NFD);
         msg = msg.replaceAll("[^\\p{ASCII}]", "").trim();

         return msg;
      }
   }
}
