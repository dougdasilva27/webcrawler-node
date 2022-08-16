package br.com.lett.crawlernode.database;

import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.Ranking;
import br.com.lett.crawlernode.core.models.RankingProduct;
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

   // Class generated in project DB to convert an object to gson because dialect postgres not accepted
   // this type
   private static final PostgresJsonBinding CONVERT_STRING_GSON = new PostgresJsonBinding();




   /********************************* Ranking *****************************************************/


   // busca dados no postgres
   public static Long fetchProcessedIdWithInternalId(String internalId, int market, Session session) {

      dbmodels.tables.Processed processed = Tables.PROCESSED;
      Long processedId = null;

         List<Field<?>> fields = new ArrayList<>();
         fields.add(processed.ID);
         fields.add(processed.MASTER_ID);

         List<Condition> conditions = new ArrayList<>();
         conditions.add(processed.MARKET.equal(market));
         conditions.add(processed.INTERNAL_ID.equal(internalId));

         long queryStartTime = System.currentTimeMillis();

         Connection conn = null;
         Statement sta = null;
         ResultSet rs = null;
         try {
            conn = JdbcConnectionFactory.getInstance().getConnection();
            sta = conn.createStatement();
            rs = sta.executeQuery(GlobalConfigurations.dbManager.jooqPostgres.select(fields).from(processed).where(conditions).getSQL(ParamType.INLINED));


            ResultSet finalRs = rs;
            Result<Record> records = Exporter.collectQuery(SqlOperation.SELECT, () -> GlobalConfigurations.dbManager.jooqPostgres.fetch(finalRs));

            for (Record record : records) {
               Long masterId = record.get(processed.MASTER_ID);

               if (masterId != null) {
                 processedId = masterId;
               } else {
                  processedId = record.get(processed.ID);
               }

            }

            JSONObject apacheMetadata = new JSONObject().put("postgres_elapsed_time", System.currentTimeMillis() - queryStartTime)
               .put("query_type", "ranking_fetch_processed_product_with_internalid");

            Logging.logInfo(logger, session, apacheMetadata, "POSTGRES TIMING INFO");
         } catch (Exception e) {
            Logging.printLogError(logger, CommonMethods.getStackTrace(e));
         } finally {
            JdbcConnectionFactory.closeResource(rs);
            JdbcConnectionFactory.closeResource(sta);
            JdbcConnectionFactory.closeResource(conn);
         }

         return processedId;
      }

      public static void insertProductsRanking (Ranking ranking, Session session){
         Connection conn = null;
         Statement sta = null;

         Logging.printLogInfo(logger, session, "Persisting ranking data ...");

         long queryStartTime = System.currentTimeMillis();

         try {
            conn = JdbcConnectionFactory.getInstance().getConnection();
            sta = conn.createStatement();

            CrawlerRanking crawlerRanking = Tables.CRAWLER_RANKING;

            List<RankingProduct> products = ranking.getProducts();

            for (RankingProduct rankingProducts : products) {
               List<Long> processedIds = rankingProducts.getProcessedIds();

               for (Long processedId : processedIds) {

                  Map<Field<?>, Object> mapInsert = new HashMap<>();

                  mapInsert.put(crawlerRanking.RANK_TYPE, ranking.getRankType());
                  mapInsert.put(crawlerRanking.DATE, ranking.getDate());
                  mapInsert.put(crawlerRanking.LOCATION, ranking.getLocation());
                  mapInsert.put(crawlerRanking.POSITION, rankingProducts.getPosition());
                  mapInsert.put(crawlerRanking.PAGE_SIZE, ranking.getStatistics().getPageSize());
                  mapInsert.put(crawlerRanking.PROCESSED_ID, processedId);
                  mapInsert.put(crawlerRanking.TOTAL_SEARCH, ranking.getStatistics().getTotalSearch());
                  mapInsert.put(crawlerRanking.TOTAL_FETCHED, ranking.getStatistics().getTotalFetched());
                  mapInsert.put(crawlerRanking.SCREENSHOT, rankingProducts.getScreenshot());


                  sta.addBatch((GlobalConfigurations.dbManager.jooqPostgres.insertInto(crawlerRanking).set(mapInsert)).getSQL(ParamType.INLINED));
               }
            }

            sta.executeBatch();
            Logging.printLogDebug(logger, session, "Produtos cadastrados no postgres.");

            JSONObject apacheMetadata = new JSONObject().put("postgres_elapsed_time", System.currentTimeMillis() - queryStartTime)
               .put("query_type", "persist_products_crawler_ranking");

            Logging.logInfo(logger, session, apacheMetadata, "POSTGRES TIMING INFO");
         } catch (Exception e) {
            Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
            SessionError error = new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTrace(e));
            session.registerError(error);
         } finally {
            JdbcConnectionFactory.closeResource(sta);
            JdbcConnectionFactory.closeResource(conn);
         }
      }


      /**
       * Update frozen server task
       *
       * @param product
       * @param session
       */
      public static void updateFrozenServerTask(Product product, SeedCrawlerSession session){
         String taskId = session.getTaskId();

         //o que é master id na nova arquitetura?
         //
         //posso fazer conectar no dremio pra pegar as informações do produto, mas não tem o nome do produto

         if (taskId != null) {
            Document taskDocument = new Document().append("updated", new Date()).append("status", "DONE").append("progress", 100);

            Document result = new Document()
               .append("originalName", product.getName()).append("internalId", product.getInternalId())
               .append("url", product.getUrl()).append("status", product.getStatus());

            if (previousProcessedProduct != null) {
               result.append("ect", previousProcessedProduct.getEct()).append("lettId", previousProcessedProduct.getLettId())
                  .append("masterId", previousProcessedProduct.getMasterId()).append("oldName", previousProcessedProduct.getOriginalName())
                  .append("isNew", false);
            } else {
               result.append("ect", new Date()).append("lettId", null).append("masterId", null).append("oldName", null).append("isNew", true);
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
      public static void updateFrozenServerTask (SeedCrawlerSession session, String msg){
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

      public static void updateFrozenServerTask (String taskId, String msg){

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
      public static void updateFrozenServerTaskProgress (SeedCrawlerSession session,int progress){
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
