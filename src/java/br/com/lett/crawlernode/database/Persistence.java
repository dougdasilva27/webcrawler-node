package br.com.lett.crawlernode.database;

import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.Ranking;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.SkuStatus;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.SessionError;
import br.com.lett.crawlernode.core.session.crawler.SeedCrawlerSession;
import br.com.lett.crawlernode.database.model.SqlOperation;
import br.com.lett.crawlernode.main.GlobalConfigurations;
import br.com.lett.crawlernode.metrics.Exporter;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.ScraperInformation;
import dbmodels.Tables;
import dbmodels.tables.CrawlerRanking;
import generation.PostgresJsonBinding;
import models.Behavior;
import models.Processed;
import models.prices.Prices;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.conf.ParamType;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class Persistence {
   private static final Logger logger = LoggerFactory.getLogger(Persistence.class);

   private static final String MONGO_COLLECTION_SERVER_TASK = "ServerTask";

      /**
       * Update frozen server task
       *
       * @param product
       * @param session
       */
      public static void updateFrozenServerTask(Product product, JSONObject productJson, SeedCrawlerSession session){
         String taskId = session.getTaskId();
         SkuStatus status = CommonMethods.getSkuStatus(product);

         if (taskId != null) {
            Document taskDocument = new Document().append("updated", new Date()).append("status", "DONE").append("progress", 100);

            Document result = new Document()
               .append("original_name", product.getName()).append("internal_id", product.getInternalId())
               .append("url", product.getUrl()).append("status", status.toString());

            if (productJson != null || !productJson.isEmpty()) {
               result.append("created", productJson.optString("created")).append("lett_id", productJson.optString("lett_id"))
                  .append("is_master", productJson.optString("unification_is_master")).append("old_name", productJson.optString("name"))
                  .append("is_new", false);
            } else {
               result.append("created", new Date()).append("lett_id", null).append("is_master", null).append("old_name", null).append("is_new", true);
            }

            taskDocument.append("result", result);

            long queryStartTime = System.currentTimeMillis();

            try {
               GlobalConfigurations.dbManager.connectionFrozen.updateOne(new Document("_id", new ObjectId(taskId)), new Document("$set", taskDocument),
                  MONGO_COLLECTION_SERVER_TASK);

               JSONObject apacheMetadata = new JSONObject().put("mongo_elapsed_time", System.currentTimeMillis() - queryStartTime)
                  .put("query_type", "update_product_frozen_seed_servertask");

               Logging.logInfo(logger, session, apacheMetadata, "MONGO TIMING INFO");
            } catch (Exception e) {
               Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
            }
         }
      }



      /**
       * Update frozen server task
       *
       * @param session
       */
      public static void updateFrozenServerTask(SeedCrawlerSession session){
         String taskId = session.getTaskId();

         if (taskId != null) {
            Document taskDocument = new Document().append("updated", new Date()).append("progress", 100);

            StringBuilder errors = new StringBuilder();

            if (!session.getErrors().isEmpty()) {
               for (SessionError error : session.getErrors()) {
                  errors.append(error.getErrorContent()).append("\n");
               }
               taskDocument.append("status", "ERROR");
            } else {
               errors.append("Not a product page!");
               taskDocument.append("status", "DONE");
            }

            taskDocument.append("result", new Document().append("error", errors.toString()));

            long queryStartTime = System.currentTimeMillis();

            try {
               GlobalConfigurations.dbManager.connectionFrozen.updateOne(new Document("_id", new ObjectId(taskId)), new Document("$set", taskDocument),
                  MONGO_COLLECTION_SERVER_TASK);

               JSONObject apacheMetadata = new JSONObject().put("mongo_elapsed_time", System.currentTimeMillis() - queryStartTime)
                  .put("query_type", "update_error_frozen_seed_servertask");

               Logging.logInfo(logger, session, apacheMetadata, "MONGO TIMING INFO");
            } catch (Exception e) {
               Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
            }
         }
      }

      /**
       * Update frozen server task
       *
       * @param session
       */
      public static void updateFrozenServerTask(SeedCrawlerSession session, String msg){
         String taskId = session.getTaskId();

         if (taskId != null) {
            Document taskDocument = new Document().append("updated", new Date()).append("progress", 100);

            StringBuilder errors = new StringBuilder();
            errors.append(msg);
            taskDocument.append("status", "ERROR");

            taskDocument.append("result", new Document().append("error", errors.toString()));

            long queryStartTime = System.currentTimeMillis();

            try {
               GlobalConfigurations.dbManager.connectionFrozen.updateOne(new Document("_id", new ObjectId(taskId)), new Document("$set", taskDocument),
                  MONGO_COLLECTION_SERVER_TASK);

               JSONObject apacheMetadata = new JSONObject().put("mongo_elapsed_time", System.currentTimeMillis() - queryStartTime)
                  .put("query_type", "update_error_frozen_seed_servertask");

               Logging.logInfo(logger, session, apacheMetadata, "MONGO TIMING INFO");
            } catch (Exception e) {
               Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
            }
         }
      }

      public static void updateFrozenServerTask(String taskId, String msg){

         if (taskId != null) {
            Document taskDocument = new Document().append("updated", new Date()).append("progress", 100);

            taskDocument.append("status", "ERROR");

            taskDocument.append("result", new Document().append("error", msg));

            long queryStartTime = System.currentTimeMillis();

            try {
               GlobalConfigurations.dbManager.connectionFrozen.updateOne(new Document("_id", new ObjectId(taskId)), new Document("$set", taskDocument),
                  MONGO_COLLECTION_SERVER_TASK);

               JSONObject apacheMetadata = new JSONObject().put("mongo_elapsed_time", System.currentTimeMillis() - queryStartTime)
                  .put("query_type", "update_error_frozen_seed_servertask");

            } catch (Exception e) {
               Logging.printLogError(logger, CommonMethods.getStackTrace(e));
            }
         }
      }


      /**
       * Update frozen server task progress
       *
       * @param session
       * @param progress
       */
      public static void updateFrozenServerTaskProgress(SeedCrawlerSession session,int progress){
         String taskId = session.getTaskId();

         if (taskId != null) {
            Document taskDocument = new Document("$set", new Document().append("updated", new Date()).append("progress", progress));

            long queryStartTime = System.currentTimeMillis();
            try {
               GlobalConfigurations.dbManager.connectionFrozen.updateOne(new Document("_id", new ObjectId(taskId)), taskDocument,
                  MONGO_COLLECTION_SERVER_TASK);

               JSONObject apacheMetadata = new JSONObject().put("mongo_elapsed_time", System.currentTimeMillis() - queryStartTime)
                  .put("query_type", "update_progress_frozen_seed_servertask");

               Logging.logInfo(logger, session, apacheMetadata, "MONGO TIMING INFO");
            } catch (Exception e) {
               Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
            }
         }
      }

      public static ScraperInformation fetchScraperInfoToOneMarket ( int marketId){
         Connection conn = null;
         Statement sta = null;
         ScraperInformation scraperInformation = null;

         try {

            String query = "WITH market_informations AS (" +
               "SELECT scraper.market_id, scraper.\"options\" , scraper.scraper_class_id, scraper.\"type\", " +
               "scraper.use_browser, market.fullname, market.first_party_regex, market.code, market.name, market.proxies " +
               "FROM market JOIN scraper ON (market.id = scraper.market_id) " +
               "AND market.id = '" + marketId + "') " +
               "SELECT market_informations.\"options\" as options_scraper, scraper_class.\"options\", " +
               "public.scraper_class.\"class\", market_informations.market_id, market_informations.use_browser, market_informations.proxies, " +
               "market_informations.first_party_regex, market_informations.code, market_informations.fullname, market_informations.name " +
               "FROM market_informations JOIN scraper_class " +
               "ON (market_informations.scraper_class_id = scraper_class.id) " +
               "WHERE market_informations.\"type\" = 'CORE'";


            conn = JdbcConnectionFactory.getInstance().getConnection();
            sta = conn.createStatement();
            ResultSet rs = sta.executeQuery(query);

            while (rs.next()) {

               scraperInformation = CommonMethods.getScraperInformation(rs);


            }

         } catch (Exception e) {
            Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
         } finally {
            JdbcConnectionFactory.closeResource(sta);
            JdbcConnectionFactory.closeResource(conn);
         }

         return scraperInformation;
      }

   }
