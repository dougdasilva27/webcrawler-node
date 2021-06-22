package br.com.lett.crawlernode.database;

import br.com.lett.crawlernode.core.models.Ranking;
import br.com.lett.crawlernode.core.models.RankingProducts;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.SessionError;
import br.com.lett.crawlernode.core.session.crawler.SeedCrawlerSession;
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

   /**
    * @param newProcessedProduct
    * @param session
    * @return
    */
   public static PersistenceResult persistProcessedProduct(Processed newProcessedProduct, Session session) {

      PersistenceResult persistenceResult = new ProcessedModelPersistenceResult();
      Long id;

      Prices prices = newProcessedProduct.getPrices();

      dbmodels.tables.Processed processedTable = Tables.PROCESSED;

      long queryStartTime = System.currentTimeMillis();

      if (newProcessedProduct.getId() == null) {

         Map<Field<?>, Object> insertMap = new HashMap<>();

         // Column Value
         insertMap.put(processedTable.INTERNAL_ID, newProcessedProduct.getInternalId());
         insertMap.put(processedTable.INTERNAL_PID, newProcessedProduct.getInternalPid());
         insertMap.put(processedTable.ORIGINAL_NAME, newProcessedProduct.getOriginalName());
         insertMap.put(processedTable.CLASS, newProcessedProduct.getProcessedClass());
         insertMap.put(processedTable.BRAND, newProcessedProduct.getBrand());
         insertMap.put(processedTable.RECIPIENT, newProcessedProduct.getRecipient());
         insertMap.put(processedTable.QUANTITY, newProcessedProduct.getQuantity());
         insertMap.put(processedTable.UNIT, newProcessedProduct.getUnit());
         insertMap.put(processedTable.EXTRA, newProcessedProduct.getExtra());
         insertMap.put(processedTable.PIC, newProcessedProduct.getPic());
         insertMap.put(processedTable.URL, newProcessedProduct.getUrl());
         insertMap.put(processedTable.MARKET, newProcessedProduct.getMarket());
         insertMap.put(processedTable.ECT, newProcessedProduct.getEct());
         insertMap.put(processedTable.LMT, newProcessedProduct.getLmt());
         insertMap.put(processedTable.LAT, newProcessedProduct.getLat());
         insertMap.put(processedTable.LRT, newProcessedProduct.getLrt());
         insertMap.put(processedTable.LMS, newProcessedProduct.getLms());
         insertMap.put(processedTable.STATUS, newProcessedProduct.getStatus());
         insertMap.put(processedTable.AVAILABLE, newProcessedProduct.getAvailable());
         insertMap.put(processedTable.VOID, newProcessedProduct.isVoid());
         insertMap.put(processedTable.CAT1, newProcessedProduct.getCat1());
         insertMap.put(processedTable.CAT2, newProcessedProduct.getCat2());
         insertMap.put(processedTable.CAT3, newProcessedProduct.getCat3());
         insertMap.put(processedTable.MULTIPLIER, newProcessedProduct.getMultiplier());
         insertMap.put(processedTable.ORIGINAL_DESCRIPTION, newProcessedProduct.getOriginalDescription());
         insertMap.put(processedTable.PRICE, newProcessedProduct.getPrice());
         insertMap.put(processedTable.STOCK, newProcessedProduct.getStock());
         insertMap.put(processedTable.SECONDARY_PICS, newProcessedProduct.getSecondaryImages());
         insertMap.put(processedTable.EAN, newProcessedProduct.getEan());
         insertMap.put(processedTable.EANS, newProcessedProduct.getEans());

         if (prices != null) {
            insertMap.put(processedTable.PRICES, CONVERT_STRING_GSON.converter().from(prices.toJSON()));
         } else {
            insertMap.put(processedTable.PRICES, null);
         }

         if (newProcessedProduct.getChanges() != null) {
            insertMap.put(processedTable.CHANGES, newProcessedProduct.getChanges().toString());
         } else {
            insertMap.put(processedTable.CHANGES, null);
         }

         if (newProcessedProduct.getDigitalContent() != null) {
            insertMap.put(processedTable.DIGITAL_CONTENT, newProcessedProduct.getDigitalContent().toString());
         } else {
            insertMap.put(processedTable.DIGITAL_CONTENT, null);
         }

         if (newProcessedProduct.getMarketplace() != null && !newProcessedProduct.getMarketplace().isEmpty()) {
            insertMap.put(processedTable.MARKETPLACE, newProcessedProduct.getMarketplace().toString());
         } else {
            insertMap.put(processedTable.MARKETPLACE, null);
         }

         // TODO
         if (newProcessedProduct.getOffers() != null && !newProcessedProduct.getOffers().isEmpty()) {
            insertMap.put(processedTable.OFFERS, CONVERT_STRING_GSON.converter().from(newProcessedProduct.getOffers().toJSON()));
         } else {
            insertMap.put(processedTable.OFFERS, null);
         }

         if (newProcessedProduct.getBehaviour() != null) {
            insertMap.put(processedTable.BEHAVIOUR, newProcessedProduct.getBehaviour().toString());
         } else {
            insertMap.put(processedTable.BEHAVIOUR, null);
         }

         if (newProcessedProduct.getSimilars() != null) {
            insertMap.put(processedTable.SIMILARS, newProcessedProduct.getSimilars().toString());
         } else {
            insertMap.put(processedTable.SIMILARS, null);
         }

         if (newProcessedProduct.getRatingsReviews() != null) {
            insertMap.put(processedTable.RATING, newProcessedProduct.getRatingsReviews().toString());
         } else {
            insertMap.put(processedTable.RATING, null);
         }

         Connection conn = null;
         PreparedStatement pstmt = null;
         String query = GlobalConfigurations.dbManager.jooqPostgres.insertInto(processedTable).set(insertMap).returning(processedTable.ID)
            .getSQL(ParamType.INLINED);
         try {
            conn = JdbcConnectionFactory.getInstance().getConnection();
            pstmt = conn.prepareStatement(query);
            ResultSet rs = pstmt.executeQuery();
            Result<Record> records = Exporter.collectQuery(SqlOperation.SELECT, () -> GlobalConfigurations.dbManager.jooqPostgres.fetch(rs));

            if (!records.isEmpty()) {
               Record r = records.get(0);

               if (r != null) {
                  id = r.get(processedTable.ID);
               } else {
                  id = (long) 0;
               }

               if (id != 0) {
                  newProcessedProduct.setId(id);

                  if (persistenceResult instanceof ProcessedModelPersistenceResult) {
                     ((ProcessedModelPersistenceResult) persistenceResult).addCreatedId(id);
                  }
               }
            }

            JSONObject apacheMetadata = new JSONObject().put("postgres_elapsed_time", System.currentTimeMillis() - queryStartTime)
               .put("query_type", "persist_processed_product");

            Logging.logDebug(logger, session, apacheMetadata, "POSTGRES TIMING INFO");
         } catch (Exception e) {
            Logging.printLogError(logger, session, "Error updating processed product on query: " + query);
            Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));

            session.registerError(new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTraceString(e)));

            return null;
         } finally {
            JdbcConnectionFactory.closeResource(pstmt);
            JdbcConnectionFactory.closeResource(conn);
         }

      } else {
         Map<Field<?>, Object> updateMap = new HashMap<>();

         // Column Value
         updateMap.put(processedTable.INTERNAL_ID, newProcessedProduct.getInternalId());
         updateMap.put(processedTable.INTERNAL_PID, newProcessedProduct.getInternalPid());
         updateMap.put(processedTable.ORIGINAL_NAME, newProcessedProduct.getOriginalName());
         updateMap.put(processedTable.CLASS, newProcessedProduct.getProcessedClass());
         updateMap.put(processedTable.BRAND, newProcessedProduct.getBrand());
         updateMap.put(processedTable.RECIPIENT, newProcessedProduct.getRecipient());
         updateMap.put(processedTable.QUANTITY, newProcessedProduct.getQuantity());
         updateMap.put(processedTable.UNIT, newProcessedProduct.getUnit());
         updateMap.put(processedTable.EXTRA, newProcessedProduct.getExtra());
         updateMap.put(processedTable.PIC, newProcessedProduct.getPic());
         updateMap.put(processedTable.URL, newProcessedProduct.getUrl());
         updateMap.put(processedTable.MARKET, newProcessedProduct.getMarket());
         updateMap.put(processedTable.ECT, newProcessedProduct.getEct());
         updateMap.put(processedTable.LMT, newProcessedProduct.getLmt());
         updateMap.put(processedTable.LAT, newProcessedProduct.getLat());
         updateMap.put(processedTable.LRT, newProcessedProduct.getLrt());
         updateMap.put(processedTable.LMS, newProcessedProduct.getLms());
         updateMap.put(processedTable.STATUS, newProcessedProduct.getStatus());
         updateMap.put(processedTable.AVAILABLE, newProcessedProduct.getAvailable());
         updateMap.put(processedTable.VOID, newProcessedProduct.isVoid());
         updateMap.put(processedTable.CAT1, newProcessedProduct.getCat1());
         updateMap.put(processedTable.CAT2, newProcessedProduct.getCat2());
         updateMap.put(processedTable.CAT3, newProcessedProduct.getCat3());
         updateMap.put(processedTable.MULTIPLIER, newProcessedProduct.getMultiplier());
         updateMap.put(processedTable.ORIGINAL_DESCRIPTION, newProcessedProduct.getOriginalDescription());
         updateMap.put(processedTable.PRICE, newProcessedProduct.getPrice());
         updateMap.put(processedTable.STOCK, newProcessedProduct.getStock());
         updateMap.put(processedTable.SECONDARY_PICS, newProcessedProduct.getSecondaryImages());
         updateMap.put(processedTable.EAN, newProcessedProduct.getEan());
         updateMap.put(processedTable.EANS, newProcessedProduct.getEans());

         if (prices != null) {
            updateMap.put(processedTable.PRICES, CONVERT_STRING_GSON.converter().from(prices.toJSON()));
         } else {
            updateMap.put(processedTable.PRICES, null);
         }

         if (newProcessedProduct.getChanges() != null) {
            updateMap.put(processedTable.CHANGES, newProcessedProduct.getChanges().toString());
         } else {
            updateMap.put(processedTable.CHANGES, null);
         }

         if (newProcessedProduct.getDigitalContent() != null) {
            updateMap.put(processedTable.DIGITAL_CONTENT, newProcessedProduct.getDigitalContent().toString());
         } else {
            updateMap.put(processedTable.DIGITAL_CONTENT, null);
         }

         if (newProcessedProduct.getMarketplace() != null && !newProcessedProduct.getMarketplace().isEmpty()) {
            updateMap.put(processedTable.MARKETPLACE, newProcessedProduct.getMarketplace().toString());
         } else {
            updateMap.put(processedTable.MARKETPLACE, null);
         }

         // TODO
         if (newProcessedProduct.getOffers() != null && !newProcessedProduct.getOffers().isEmpty()) {
            updateMap.put(processedTable.OFFERS, CONVERT_STRING_GSON.converter().from(newProcessedProduct.getOffers().toJSON()));
         } else {
            updateMap.put(processedTable.OFFERS, null);
         }

         if (newProcessedProduct.getBehaviour() != null) {
            updateMap.put(processedTable.BEHAVIOUR, newProcessedProduct.getBehaviour().toString());
         } else {
            updateMap.put(processedTable.BEHAVIOUR, null);
         }

         if (newProcessedProduct.getSimilars() != null) {
            updateMap.put(processedTable.SIMILARS, newProcessedProduct.getSimilars().toString());
         } else {
            updateMap.put(processedTable.SIMILARS, null);
         }

         if (newProcessedProduct.getRatingsReviews() != null) {
            updateMap.put(processedTable.RATING, newProcessedProduct.getRatingsReviews().toString());
         } else {
            updateMap.put(processedTable.RATING, null);
         }

         // get the id of the processed product that already exists
         id = newProcessedProduct.getId();

         List<Condition> conditions = new ArrayList<>();
         conditions.add(processedTable.ID.equal(id));

         if (persistenceResult instanceof ProcessedModelPersistenceResult) {
            ((ProcessedModelPersistenceResult) persistenceResult).addModifiedId(id);
         }


         Connection conn = null;
         PreparedStatement pstmt = null;
         String query = GlobalConfigurations.dbManager.jooqPostgres.update(processedTable).set(updateMap).where(conditions).getSQL(ParamType.INLINED);
         try {
            conn = JdbcConnectionFactory.getInstance().getConnection();
            pstmt = conn.prepareStatement(query);

            Exporter.collectQuery(SqlOperation.UPDATE, pstmt::executeUpdate);

            JSONObject apacheMetadata = new JSONObject().put("postgres_elapsed_time", System.currentTimeMillis() - queryStartTime)
               .put("query_type", "update_processed_product");

            Logging.logDebug(logger, session, apacheMetadata, "POSTGRES TIMING INFO");
         } catch (Exception e) {
            Logging.printLogError(logger, session, "Error updating processed product on query: " + query);
            Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));

            session.registerError(new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTraceString(e)));

            return null;
         } finally {
            JdbcConnectionFactory.closeResource(pstmt);
            JdbcConnectionFactory.closeResource(conn);
         }
      }

      Logging.printLogDebug(logger, session, "Processed product persisted with success.");


      return persistenceResult;
   }

   /**
    * Updates processed Behaviour on processed table. This method is used in active void to include the
    * behavior of void status.
    *
    * @param newBehaviour
    * @param session
    */
   public static void updateProcessedBehaviour(Behavior newBehaviour, Session session, Long id) {
      dbmodels.tables.Processed processedTable = Tables.PROCESSED;

      Map<Field<?>, Object> updateSets = new HashMap<>();

      if (newBehaviour != null) {
         updateSets.put(processedTable.BEHAVIOUR, newBehaviour.toString());
      } else {
         updateSets.put(processedTable.BEHAVIOUR, null);
      }

      List<Condition> conditions = new ArrayList<>();

      if (id != null) {
         conditions.add(processedTable.ID.equal(id));
      } else {
         conditions.add(processedTable.INTERNAL_ID.equal(session.getInternalId()));
         conditions.add(processedTable.MARKET.equal(session.getMarket().getNumber()));
      }

      long queryStartTime = System.currentTimeMillis();

      Connection conn = null;
      PreparedStatement pstmt = null;
      try {
         conn = JdbcConnectionFactory.getInstance().getConnection();
         pstmt = conn.prepareStatement(
            GlobalConfigurations.dbManager.jooqPostgres.update(processedTable).set(updateSets).where(conditions).getSQL(ParamType.INLINED));

         Exporter.collectQuery(SqlOperation.UPDATE, pstmt::executeUpdate);

         Logging.printLogDebug(logger, session, "Processed product with id " + id + " behaviour updated with success. " + "(InternalId: "
            + session.getInternalId() + " - Market: " + session.getMarket().getNumber() + ")");

         JSONObject apacheMetadata = new JSONObject().put("postgres_elapsed_time", System.currentTimeMillis() - queryStartTime)
            .put("query_type", "update_processed_product_behaviour");

         Logging.logDebug(logger, session, apacheMetadata, "POSTGRES TIMING INFO");

      } catch (Exception e) {
         Logging.printLogError(logger, session, "Error updating processed product behaviour.");
         Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));

         session.registerError(new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTraceString(e)));
      } finally {
         JdbcConnectionFactory.closeResource(pstmt);
         JdbcConnectionFactory.closeResource(conn);
      }
   }

   /**
    * Set void value of a processed model. This method sets the following values:
    * <ul>
    * <li>available = false</li>
    * <li>status = "void"</li>
    * <li>void = true</li>
    * <li>marketplace = null</li>
    * <li>price = null</li>
    * <li>prices = new Prices() which is an empty prices model</li>
    * </ul>
    *
    * @param processed
    * @param voidValue A boolean indicating whether the processed product void must be set to true or
    *                  false
    * @param session
    */
   public static void setProcessedVoidTrue(Session session) {
      dbmodels.tables.Processed processedTable = Tables.PROCESSED;

      Map<Field<?>, Object> updateSets = new HashMap<>();

      updateSets.put(processedTable.AVAILABLE, false);
      updateSets.put(processedTable.STATUS, "void");
      updateSets.put(processedTable.VOID, true);
      updateSets.put(processedTable.MARKETPLACE, null);
      updateSets.put(processedTable.OFFERS, null);
      updateSets.put(processedTable.PRICE, null);
      updateSets.put(processedTable.RATING, null);
      updateSets.put(processedTable.PRICES, CONVERT_STRING_GSON.converter().from(new Prices().toJSON()));

      List<Condition> conditions = new ArrayList<>();

      conditions.add(processedTable.INTERNAL_ID.equal(session.getInternalId()));
      conditions.add(processedTable.MARKET.equal(session.getMarket().getNumber()));

      long queryStartTime = System.currentTimeMillis();

      Connection conn = null;
      PreparedStatement pstmt = null;
      try {
         conn = JdbcConnectionFactory.getInstance().getConnection();
         pstmt = conn.prepareStatement(
            GlobalConfigurations.dbManager.jooqPostgres.update(processedTable).set(updateSets).where(conditions).getSQL(ParamType.INLINED));

         pstmt.executeUpdate();
         Exporter.collectQuery(SqlOperation.UPDATE, pstmt::executeUpdate);
         Logging.printLogDebug(logger, session, "Processed product void value updated with success.");

         JSONObject apacheMetadata = new JSONObject().put("postgres_elapsed_time", System.currentTimeMillis() - queryStartTime)
            .put("query_type", "update_void_processed_product");

         Logging.logDebug(logger, session, apacheMetadata, "POSTGRES TIMING INFO");
      } catch (Exception e) {
         Logging.printLogError(logger, session, "Error updating processed product void.");
         Logging.printLogError(logger, session, "InternalId: " + session.getInternalId() + " Market: " + session.getMarket().getNumber());
         Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));

         session.registerError(new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTraceString(e)));
      } finally {
         JdbcConnectionFactory.closeResource(pstmt);
         JdbcConnectionFactory.closeResource(conn);
      }
   }

   /**
    * Updates processed LastReadTime on processed table.
    *
    * @param nowISO
    * @param session
    */
   public static void updateProcessedLRT(String nowISO, Session session) {
      dbmodels.tables.Processed processedTable = Tables.PROCESSED;

      Map<Field<?>, Object> updateSets = new HashMap<>();
      updateSets.put(processedTable.LRT, nowISO);

      List<Condition> conditions = new ArrayList<>();
      conditions.add(processedTable.INTERNAL_ID.equal(session.getInternalId()));
      conditions.add(processedTable.MARKET.equal(session.getMarket().getNumber()));

      long queryStartTime = System.currentTimeMillis();

      Connection conn = null;
      PreparedStatement pstmt = null;
      try {
         conn = JdbcConnectionFactory.getInstance().getConnection();
         pstmt = conn.prepareStatement(
            GlobalConfigurations.dbManager.jooqPostgres.update(processedTable).set(updateSets).where(conditions).getSQL(ParamType.INLINED));

         Exporter.collectQuery(SqlOperation.UPDATE, pstmt::executeUpdate);
         Logging.printLogDebug(logger, session, "Processed product LRT updated with success.");

         JSONObject apacheMetadata = new JSONObject().put("postgres_elapsed_time", System.currentTimeMillis() - queryStartTime)
            .put("query_type", "update_processed_product_lrt");

         Logging.logDebug(logger, session, apacheMetadata, "POSTGRES TIMING INFO");

      } catch (Exception e) {
         Logging.printLogError(logger, session, "Error updating processed product LRT.");
         Logging.printLogError(logger, session, "InternalId: " + session.getInternalId() + " Market: " + session.getMarket().getNumber());
         Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));

         session.registerError(new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTraceString(e)));
      } finally {
         JdbcConnectionFactory.closeResource(pstmt);
         JdbcConnectionFactory.closeResource(conn);
      }
   }

   /**
    * Updates processed LastModifiedTime on processed table.
    *
    * @param nowISO
    * @param session
    */
   public static void updateProcessedLMT(String nowISO, Session session) {
      dbmodels.tables.Processed processedTable = Tables.PROCESSED;

      Map<Field<?>, Object> updateSets = new HashMap<>();
      updateSets.put(processedTable.LMT, nowISO);

      List<Condition> conditions = new ArrayList<>();
      conditions.add(processedTable.INTERNAL_ID.equal(session.getInternalId()));
      conditions.add(processedTable.MARKET.equal(session.getMarket().getNumber()));

      long queryStartTime = System.currentTimeMillis();

      Connection conn = null;
      PreparedStatement pstmt = null;
      try {
         conn = JdbcConnectionFactory.getInstance().getConnection();
         pstmt = conn.prepareStatement(
            GlobalConfigurations.dbManager.jooqPostgres.update(processedTable).set(updateSets).where(conditions).getSQL(ParamType.INLINED));

         Exporter.collectQuery(SqlOperation.UPDATE, pstmt::executeUpdate);
         Logging.printLogDebug(logger, session, "Processed product LMT updated with success.");

         JSONObject apacheMetadata = new JSONObject().put("postgres_elapsed_time", System.currentTimeMillis() - queryStartTime)
            .put("query_type", "update_processed_product_lmt");

         Logging.logDebug(logger, session, apacheMetadata, "POSTGRES TIMING INFO");
      } catch (Exception e) {
         Logging.printLogError(logger, session, "Error updating processed product LMT.");
         Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));

         session.registerError(new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTraceString(e)));
      } finally {
         JdbcConnectionFactory.closeResource(pstmt);
         JdbcConnectionFactory.closeResource(conn);
      }

   }

   /**
    * Updates processed LastModifiedStatus on processed table.
    *
    * @param nowISO
    * @param session
    */
   public static void updateProcessedLMS(String nowISO, Session session) {
      dbmodels.tables.Processed processedTable = Tables.PROCESSED;

      Map<Field<?>, Object> updateSets = new HashMap<>();
      updateSets.put(processedTable.LMS, nowISO);

      List<Condition> conditions = new ArrayList<>();
      conditions.add(processedTable.INTERNAL_ID.equal(session.getInternalId()));
      conditions.add(processedTable.MARKET.equal(session.getMarket().getNumber()));

      long queryStartTime = System.currentTimeMillis();

      Connection conn = null;
      PreparedStatement pstmt = null;
      try {
         conn = JdbcConnectionFactory.getInstance().getConnection();
         pstmt = conn.prepareStatement(
            GlobalConfigurations.dbManager.jooqPostgres.update(processedTable).set(updateSets).where(conditions).getSQL(ParamType.INLINED));

         Exporter.collectQuery(SqlOperation.UPDATE, pstmt::executeUpdate);
         Logging.printLogDebug(logger, session, "Processed product LMS updated with success.");

         JSONObject apacheMetadata = new JSONObject().put("postgres_elapsed_time", System.currentTimeMillis() - queryStartTime)
            .put("query_type", "update_processed_product_lms");

         Logging.logDebug(logger, session, apacheMetadata, "POSTGRES TIMING INFO");
      } catch (Exception e) {
         Logging.printLogError(logger, session, "Error updating processed product LMS.");
         Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));

         session.registerError(new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTraceString(e)));
      } finally {
         JdbcConnectionFactory.closeResource(pstmt);
         JdbcConnectionFactory.closeResource(conn);
      }

   }


   /**
    * Directory creation.
    *
    * @param city
    * @param name
    * @param folder
    */
   private static void processDirectory(String city, String name, String folder) {
      File file;

      if (name == null) {
         file = new File(GlobalConfigurations.executionParameters.getTmpImageFolder() + "/" + city);
      } else if (folder == null) {
         file = new File(GlobalConfigurations.executionParameters.getTmpImageFolder() + "/" + city + "/" + name);
      } else {
         file = new File(GlobalConfigurations.executionParameters.getTmpImageFolder() + "/" + city + "/" + name + "/" + folder);
      }

      if (!file.exists()) {
         boolean fileWasCreated = file.mkdir();
         if (!fileWasCreated) {
            Logging.printLogError(logger, "Failed to create " + file.getAbsolutePath() + " directory!");
         }
      }
   }


   /********************************* Ranking *****************************************************/


   // busca dados no postgres
   public static List<Processed> fetchProcessedIdsWithInternalId(String id, int market, Session session) {
      List<Processed> processeds = new ArrayList<>();

      dbmodels.tables.Processed processed = Tables.PROCESSED;

      List<Field<?>> fields = new ArrayList<>();
      fields.add(processed.ID);
      fields.add(processed.MASTER_ID);
      fields.add(processed.STATUS);
      fields.add(processed.URL);

      List<Condition> conditions = new ArrayList<>();
      conditions.add(processed.MARKET.equal(market));
      conditions.add(processed.INTERNAL_ID.equal(id));

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
            Processed p = new Processed();
            Long masterId = record.get(processed.MASTER_ID);
            p.setVoid(record.get(processed.STATUS).equalsIgnoreCase("void"));
            p.setUrl(record.get(processed.URL));

            if (masterId != null) {
               p.setId(masterId);
            } else {
               p.setId(record.get(processed.ID));
            }

            processeds.add(p);
         }

         JSONObject apacheMetadata = new JSONObject().put("postgres_elapsed_time", System.currentTimeMillis() - queryStartTime)
            .put("query_type", "ranking_fetch_processed_product_with_internalid");

         Logging.logDebug(logger, session, apacheMetadata, "POSTGRES TIMING INFO");
      } catch (Exception e) {
         Logging.printLogError(logger, CommonMethods.getStackTrace(e));
      } finally {
         JdbcConnectionFactory.closeResource(rs);
         JdbcConnectionFactory.closeResource(sta);
         JdbcConnectionFactory.closeResource(conn);
      }

      return processeds;
   }

   public static List<Processed> fetchProcessedIdsWithInternalPid(String pid, int market, Session session) {
      List<Processed> processeds = new ArrayList<>();

      dbmodels.tables.Processed processed = Tables.PROCESSED;

      List<Field<?>> fields = new ArrayList<>();
      fields.add(processed.ID);
      fields.add(processed.MASTER_ID);
      fields.add(processed.STATUS);
      fields.add(processed.URL);

      List<Condition> conditions = new ArrayList<>();
      conditions.add(processed.MARKET.equal(market));
      conditions.add(processed.INTERNAL_PID.equal(pid));

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
            Processed p = new Processed();
            Long masterId = record.get(processed.MASTER_ID);
            p.setVoid(record.get(processed.STATUS).equalsIgnoreCase("void"));
            p.setUrl(record.get(processed.URL));

            if (masterId != null) {
               p.setId(masterId);
            } else {
               p.setId(record.get(processed.ID));
            }

            processeds.add(p);
         }

         JSONObject apacheMetadata = new JSONObject().put("postgres_elapsed_time", System.currentTimeMillis() - queryStartTime)
            .put("query_type", "ranking_fetch_processed_product_with_internalpid");

         Logging.logDebug(logger, session, apacheMetadata, "POSTGRES TIMING INFO");
      } catch (Exception e) {
         Logging.printLogError(logger, CommonMethods.getStackTrace(e));
      } finally {
         JdbcConnectionFactory.closeResource(rs);
         JdbcConnectionFactory.closeResource(sta);
         JdbcConnectionFactory.closeResource(conn);
      }

      return processeds;
   }


   public static List<Long> fetchProcessedIdsWithUrl(String url, int market, Session session) {
      List<Long> processedIds = new ArrayList<>();

      dbmodels.tables.Processed processed = Tables.PROCESSED;

      List<Field<?>> fields = new ArrayList<>();
      fields.add(processed.ID);
      fields.add(processed.MASTER_ID);

      List<Condition> conditions = new ArrayList<>();
      conditions.add(processed.MARKET.equal(market));
      conditions.add(processed.URL.equal(url));

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
               processedIds.add(record.get(processed.MASTER_ID));
            } else {
               processedIds.add(record.get(processed.ID));
            }
         }

         JSONObject apacheMetadata = new JSONObject().put("postgres_elapsed_time", System.currentTimeMillis() - queryStartTime)
            .put("query_type", "ranking_fetch_processed_product_with_internalpid");

         Logging.logDebug(logger, session, apacheMetadata, "POSTGRES TIMING INFO");

      } catch (Exception e) {
         Logging.printLogError(logger, CommonMethods.getStackTrace(e));
      } finally {
         JdbcConnectionFactory.closeResource(rs);
         JdbcConnectionFactory.closeResource(sta);
         JdbcConnectionFactory.closeResource(conn);
      }

      return processedIds;
   }


   public static void insertProductsRanking(Ranking ranking, Session session) {
      Connection conn = null;
      Statement sta = null;

      Logging.printLogInfo(logger, session, "Persisting ranking data ...");

      long queryStartTime = System.currentTimeMillis();

      try {
         conn = JdbcConnectionFactory.getInstance().getConnection();
         sta = conn.createStatement();

         CrawlerRanking crawlerRanking = Tables.CRAWLER_RANKING;

         List<RankingProducts> products = ranking.getProducts();

         for (RankingProducts rankingProducts : products) {
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

         Logging.logDebug(logger, session, apacheMetadata, "POSTGRES TIMING INFO");
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
    * @param previousProcessedProduct
    * @param newProcessedProduct
    * @param session
    */
   public static void updateFrozenServerTask(Processed previousProcessedProduct, Processed newProcessedProduct, SeedCrawlerSession session) {
      String taskId = session.getTaskId();

      if (taskId != null) {
         Document taskDocument = new Document().append("updated", new Date()).append("status", "DONE").append("progress", 100);

         Document result = new Document().append("processedId", newProcessedProduct.getId())
            .append("originalName", newProcessedProduct.getOriginalName()).append("internalId", newProcessedProduct.getInternalId())
            .append("url", newProcessedProduct.getUrl()).append("status", newProcessedProduct.getStatus());

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
   public static void updateFrozenServerTask(SeedCrawlerSession session) {
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
   public static void updateFrozenServerTask(SeedCrawlerSession session, String msg) {
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

   /**
    * Update frozen server task progress
    *
    * @param session
    * @param progress
    */
   public static void updateFrozenServerTaskProgress(SeedCrawlerSession session, int progress) {
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

   public static ScraperInformation fetchScraperInfoToOneMarket(int marketId) {
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
