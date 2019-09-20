package br.com.lett.crawlernode.database;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.conf.ParamType;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.lett.crawlernode.core.models.CategoriesRanking;
import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.Ranking;
import br.com.lett.crawlernode.core.models.RankingProducts;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.SessionError;
import br.com.lett.crawlernode.core.session.crawler.SeedCrawlerSession;
import br.com.lett.crawlernode.main.GlobalConfigurations;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import dbmodels.Tables;
import dbmodels.tables.Crawler;
import dbmodels.tables.CrawlerCategories;
import dbmodels.tables.CrawlerRanking;
import generation.PostgresJsonBinding;
import models.Behavior;
import models.Marketplace;
import models.Processed;
import models.RatingsReviews;
import models.prices.Prices;

public class Persistence {
  private static final Logger logger = LoggerFactory.getLogger(Persistence.class);

  private static final String MONGO_COLLECTION_DISCOVER_STATS = "RankingDiscoverStats";
  private static final String MONGO_COLLECTION_SERVER_TASK = "ServerTask";

  // Class generated in project DB to convert an object to gson because dialect postgres not accepted
  // this type
  private static final PostgresJsonBinding CONVERT_STRING_GSON = new PostgresJsonBinding();

  /**
   * Persist the product crawled informations on tables crawler and crawler_old
   * 
   * @param product
   * @param session
   */
  public static void persistProduct(Product product, Session session) {
    Logging.printLogDebug(logger, session, "Persisting crawled product...");

    // get crawled information
    boolean available = product.getAvailable();
    String url = product.getUrl();
    String internalId = product.getInternalId();
    String internalPid = product.getInternalPid();
    String name = product.getName();
    Float price = product.getPrice();
    JSONObject prices = product.getPrices() == null ? null : product.getPrices().toJSON();
    String cat1 = product.getCategory1();
    String cat2 = product.getCategory2();
    String cat3 = product.getCategory3();
    String primaryPic = product.getPrimaryImage();
    String secondaryPics = product.getSecondaryImages();
    String description = product.getDescription();
    Marketplace marketplace = product.getMarketplace();
    Integer stock = product.getStock();
    // String ean = product.getEan();
    // List<String> eans = product.getEans();

    String marketplaceString = null;

    if (marketplace != null && !marketplace.isEmpty()) {
      marketplaceString = marketplace.toString();
    }


    // checking fields
    if ((price == null || price.equals(0f)) && available) {
      Logging.printLogError(logger, session, "Erro tentando inserir leitura de produto disponível mas com campo vazio: price");
      return;
    } else if (internalId == null || internalId.isEmpty()) {
      Logging.printLogError(logger, session, "Erro tentando inserir leitura de produto com campo vazio: internal_id");
      return;
    } else if (session.getMarket().getNumber() == 0) {
      Logging.printLogError(logger, session, "Erro tentando inserir leitura de produto com campo vazio: [marketId] ... aborting ...");
      return;
    } else if (url == null || url.isEmpty()) {
      Logging.printLogError(logger, session, "Erro tentando inserir leitura de produto com campo vazio: [url] ... aborting ...");
      return;
    } else if (name == null || name.isEmpty()) {
      Logging.printLogError(logger, session, "Erro tentando inserir leitura de produto com campo vazio: [name] ... aborting ...");
      return;
    }

    if (price != null && price == 0.0) {
      price = null;
    }

    Logging.printLogDebug(logger, session, "Crawled product persisted with success.");

    // Create a fields and values of crawler
    Crawler crawler = Tables.CRAWLER;

    Map<Field<?>, Object> insertMapCrawler = new HashMap<>();

    insertMapCrawler.put(crawler.AVAILABLE, available);
    insertMapCrawler.put(crawler.MARKET, session.getMarket().getNumber());
    insertMapCrawler.put(crawler.INTERNAL_ID, internalId);
    insertMapCrawler.put(crawler.INTERNAL_PID, internalPid);
    insertMapCrawler.put(crawler.URL, url);
    insertMapCrawler.put(crawler.STOCK, stock);
    insertMapCrawler.put(crawler.NAME, name);
    insertMapCrawler.put(crawler.SECONDARY_PICS, secondaryPics);
    insertMapCrawler.put(crawler.CAT1, cat1);
    insertMapCrawler.put(crawler.CAT2, cat2);
    insertMapCrawler.put(crawler.CAT3, cat3);
    insertMapCrawler.put(crawler.PIC, primaryPic);

    if (price != null) {
      insertMapCrawler.put(crawler.PRICE, MathUtils.normalizeTwoDecimalPlaces(price.doubleValue()));
    }

    if (prices != null) {
      insertMapCrawler.put(crawler.PRICES, CONVERT_STRING_GSON.converter().from(prices.toString()));
    }

    if (description != null && !Jsoup.parse(description).text().replace("\n", "").replace(" ", "").trim().isEmpty()) {
      insertMapCrawler.put(crawler.DESCRIPTION, description);
    }

    if (marketplaceString != null) {
      insertMapCrawler.put(crawler.MARKETPLACE, marketplaceString);
    }

    Connection conn = null;
    PreparedStatement pstmt = null;
    String query = GlobalConfigurations.dbManager.jooqPostgres.insertInto(crawler).set(insertMapCrawler).getSQL(ParamType.INLINED);
    try {
      conn = JdbcConnectionFactory.getInstance().getConnection();
      pstmt = conn.prepareStatement(query);

      pstmt.executeUpdate();
    } catch (Exception e) {
      Logging.printLogError(logger, session, "Error inserting product on database on query: " + query);
      Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));

      session.registerError(new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTraceString(e)));
    } finally {
      JdbcConnectionFactory.closeResource(pstmt);
      JdbcConnectionFactory.closeResource(conn);
    }
  }

  public static void updateRating(RatingsReviews ratingReviews, String internalId, Session session) {
    dbmodels.tables.Processed processedTable = Tables.PROCESSED;

    Map<Field<?>, Object> updateSets = new HashMap<>();

    if (ratingReviews != null) {
      updateSets.put(processedTable.RATING, CONVERT_STRING_GSON.converter().from(ratingReviews.getJSON().toString()));
    } else {
      updateSets.put(processedTable.RATING, null);
    }

    if (internalId != null) {
      List<Condition> conditions = new ArrayList<>();
      conditions.add(processedTable.INTERNAL_ID.equal(internalId));
      conditions.add(processedTable.MARKET.equal(session.getMarket().getNumber()));

      Connection conn = null;
      PreparedStatement pstmt = null;
      try {
        conn = JdbcConnectionFactory.getInstance().getConnection();
        pstmt = conn.prepareStatement(
            GlobalConfigurations.dbManager.jooqPostgres.update(processedTable).set(updateSets).where(conditions).getSQL(ParamType.INLINED));

        pstmt.executeUpdate();
        Logging.printLogDebug(logger, session, "Processed product rating updated with success.");

      } catch (Exception e) {
        Logging.printLogError(logger, session, "Error updating processed product rating.");
        Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));

        session.registerError(new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTraceString(e)));
      } finally {
        JdbcConnectionFactory.closeResource(pstmt);
        JdbcConnectionFactory.closeResource(conn);
      }
    }
  }

  /**
   * 
   * @param newProcessedProduct
   * @param session
   * @return
   */
  public static PersistenceResult persistProcessedProduct(Processed newProcessedProduct, Session session) {
    Logging.printLogDebug(logger, session, "Persisting processed product...");

    PersistenceResult persistenceResult = new ProcessedModelPersistenceResult();
    Long id;

    Prices prices = newProcessedProduct.getPrices();

    dbmodels.tables.Processed processedTable = Tables.PROCESSED;

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
        Result<Record> records = GlobalConfigurations.dbManager.jooqPostgres.fetch(rs);

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
      } catch (Exception e) {
        Logging.printLogError(logger, session, "Error updating processed product on query: " + query);
        Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));

        session.registerError(new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTraceString(e)));

        return null;
      } finally {
        JdbcConnectionFactory.closeResource(pstmt);
        JdbcConnectionFactory.closeResource(conn);
      }

    } else

    {
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

        pstmt.executeUpdate();
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

    Connection conn = null;
    PreparedStatement pstmt = null;
    try {
      conn = JdbcConnectionFactory.getInstance().getConnection();
      pstmt = conn.prepareStatement(
          GlobalConfigurations.dbManager.jooqPostgres.update(processedTable).set(updateSets).where(conditions).getSQL(ParamType.INLINED));

      pstmt.executeUpdate();
      Logging.printLogDebug(logger, session, "Processed product with id " + id + " behaviour updated with success. " + "(InternalId: "
          + session.getInternalId() + " - Market: " + session.getMarket().getNumber() + ")");

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
   *        false
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
    updateSets.put(processedTable.PRICES, CONVERT_STRING_GSON.converter().from(new Prices().toJSON()));

    List<Condition> conditions = new ArrayList<>();

    conditions.add(processedTable.INTERNAL_ID.equal(session.getInternalId()));
    conditions.add(processedTable.MARKET.equal(session.getMarket().getNumber()));

    Connection conn = null;
    PreparedStatement pstmt = null;
    try {
      conn = JdbcConnectionFactory.getInstance().getConnection();
      pstmt = conn.prepareStatement(
          GlobalConfigurations.dbManager.jooqPostgres.update(processedTable).set(updateSets).where(conditions).getSQL(ParamType.INLINED));

      pstmt.executeUpdate();
      Logging.printLogDebug(logger, session, "Processed product void value updated with success.");

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

    Connection conn = null;
    PreparedStatement pstmt = null;
    try {
      conn = JdbcConnectionFactory.getInstance().getConnection();
      pstmt = conn.prepareStatement(
          GlobalConfigurations.dbManager.jooqPostgres.update(processedTable).set(updateSets).where(conditions).getSQL(ParamType.INLINED));

      pstmt.executeUpdate();
      Logging.printLogDebug(logger, session, "Processed product LRT updated with success.");

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

    Connection conn = null;
    PreparedStatement pstmt = null;
    try {
      conn = JdbcConnectionFactory.getInstance().getConnection();
      pstmt = conn.prepareStatement(
          GlobalConfigurations.dbManager.jooqPostgres.update(processedTable).set(updateSets).where(conditions).getSQL(ParamType.INLINED));

      pstmt.executeUpdate();
      Logging.printLogDebug(logger, session, "Processed product LMT updated with success.");

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

    Connection conn = null;
    PreparedStatement pstmt = null;
    try {
      conn = JdbcConnectionFactory.getInstance().getConnection();
      pstmt = conn.prepareStatement(
          GlobalConfigurations.dbManager.jooqPostgres.update(processedTable).set(updateSets).where(conditions).getSQL(ParamType.INLINED));

      pstmt.executeUpdate();
      Logging.printLogDebug(logger, session, "Processed product LMS updated with success.");

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
   * 
   * @param markets
   */
  public static void initializeImagesDirectories(Markets markets) {
    Logging.printLogDebug(logger, "Initializing temp images directory at:" + GlobalConfigurations.executionParameters.getTmpImageFolder() + "...");

    List<Market> marketsList = markets.getMarkets();

    String[] subdirectories = new String[] {
        "images"
    };

    int counter = 0;

    // create folder for each market
    for (Market m : marketsList) {
      processDirectory(m.getCity(), null, null);
      processDirectory(m.getCity(), m.getName(), null);
      for (String folder : subdirectories) {
        processDirectory(m.getCity(), m.getName(), folder);
      }
      counter++;
    }

    Logging.printLogDebug(logger, "Initialized directory for " + counter + " markets.");
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
  public static CategoriesRanking fecthCategories(int id) {
    CrawlerCategories crawlerCategories = Tables.CRAWLER_CATEGORIES;

    List<Field<?>> fields = new ArrayList<>();
    fields.add(crawlerCategories.CAT1);
    fields.add(crawlerCategories.CAT2);
    fields.add(crawlerCategories.CAT3);
    fields.add(crawlerCategories.URL);

    List<Condition> conditions = new ArrayList<>();
    conditions.add(crawlerCategories.ID.equal((long) id));

    Connection conn = null;
    Statement sta = null;
    ResultSet rs = null;
    try {
      conn = JdbcConnectionFactory.getInstance().getConnection();
      sta = conn.createStatement();
      rs = sta.executeQuery(
          GlobalConfigurations.dbManager.jooqPostgres.select(fields).from(crawlerCategories).where(conditions).getSQL(ParamType.INLINED));

      Result<Record> records = GlobalConfigurations.dbManager.jooqPostgres.fetch(rs);

      CategoriesRanking cat = new CategoriesRanking();

      for (Record record : records) {
        cat.setCat1(record.get(crawlerCategories.CAT1));
        cat.setCat2(record.get(crawlerCategories.CAT2));
        cat.setCat3(record.get(crawlerCategories.CAT3));
        cat.setUrl(record.get(crawlerCategories.URL));
      }

      return cat;

    } catch (Exception e) {
      Logging.printLogError(logger, CommonMethods.getStackTrace(e));
    } finally {
      JdbcConnectionFactory.closeResource(rs);
      JdbcConnectionFactory.closeResource(sta);
      JdbcConnectionFactory.closeResource(conn);
    }

    return null;
  }

  // busca dados no postgres
  public static List<Processed> fetchProcessedIdsWithInternalId(String id, int market) {
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

    Connection conn = null;
    Statement sta = null;
    ResultSet rs = null;
    try {
      conn = JdbcConnectionFactory.getInstance().getConnection();
      sta = conn.createStatement();
      rs = sta.executeQuery(GlobalConfigurations.dbManager.jooqPostgres.select(fields).from(processed).where(conditions).getSQL(ParamType.INLINED));

      Result<Record> records = GlobalConfigurations.dbManager.jooqPostgres.fetch(rs);

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


    } catch (Exception e) {
      Logging.printLogError(logger, CommonMethods.getStackTrace(e));
    } finally {
      JdbcConnectionFactory.closeResource(rs);
      JdbcConnectionFactory.closeResource(sta);
      JdbcConnectionFactory.closeResource(conn);
    }

    return processeds;
  }

  public static List<Processed> fetchProcessedIdsWithInternalPid(String pid, int market) {
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

    Connection conn = null;
    Statement sta = null;
    ResultSet rs = null;
    try {
      conn = JdbcConnectionFactory.getInstance().getConnection();
      sta = conn.createStatement();
      rs = sta.executeQuery(GlobalConfigurations.dbManager.jooqPostgres.select(fields).from(processed).where(conditions).getSQL(ParamType.INLINED));

      Result<Record> records = GlobalConfigurations.dbManager.jooqPostgres.fetch(rs);

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


    } catch (Exception e) {
      Logging.printLogError(logger, CommonMethods.getStackTrace(e));
    } finally {
      JdbcConnectionFactory.closeResource(rs);
      JdbcConnectionFactory.closeResource(sta);
      JdbcConnectionFactory.closeResource(conn);
    }

    return processeds;
  }



  public static List<Long> fetchProcessedIdsWithUrl(String url, int market) {
    List<Long> processedIds = new ArrayList<>();

    dbmodels.tables.Processed processed = Tables.PROCESSED;

    List<Field<?>> fields = new ArrayList<>();
    fields.add(processed.ID);
    fields.add(processed.MASTER_ID);

    List<Condition> conditions = new ArrayList<>();
    conditions.add(processed.MARKET.equal(market));
    conditions.add(processed.URL.equal(url));

    Connection conn = null;
    Statement sta = null;
    ResultSet rs = null;
    try {
      conn = JdbcConnectionFactory.getInstance().getConnection();
      sta = conn.createStatement();
      rs = sta.executeQuery(GlobalConfigurations.dbManager.jooqPostgres.select(fields).from(processed).where(conditions).getSQL(ParamType.INLINED));

      Result<Record> records = GlobalConfigurations.dbManager.jooqPostgres.fetch(rs);

      for (Record record : records) {
        Long masterId = record.get(processed.MASTER_ID);

        if (masterId != null) {
          processedIds.add(record.get(processed.MASTER_ID));
        } else {
          processedIds.add(record.get(processed.ID));
        }
      }


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
   * Queries in database panel
   */

  // insere dados do ranking no mongo
  public static void persistDiscoverStats(Ranking r) {
    SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd");

    // se não conseguir inserir tenta atualizar
    try {
      Document filter = new Document("location", r.getLocation()).append("market", r.getMarketId()).append("rank_type", r.getRankType())
          .append("date", ft.format(new Date()));

      if (GlobalConfigurations.dbManager.connectionFrozen.countFind(filter, MONGO_COLLECTION_DISCOVER_STATS) > 0) {

        Document update = new Document("$set", new Document(r.getDocumentUpdate()));
        GlobalConfigurations.dbManager.connectionFrozen.updateOne(filter, update, MONGO_COLLECTION_DISCOVER_STATS);
        Logging.printLogDebug(logger, "Dados atualizados com sucesso!");

      } else {

        GlobalConfigurations.dbManager.connectionFrozen.insertOne(r.getDocument(), MONGO_COLLECTION_DISCOVER_STATS);
        Logging.printLogDebug(logger, "Dados cadastrados com sucesso!");

      }

    } catch (Exception e) {
      Logging.printLogError(logger, CommonMethods.getStackTrace(e));
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

      try {
        GlobalConfigurations.dbManager.connectionFrozen.updateOne(new Document("_id", new ObjectId(taskId)), new Document("$set", taskDocument),
            MONGO_COLLECTION_SERVER_TASK);
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

      try {
        GlobalConfigurations.dbManager.connectionFrozen.updateOne(new Document("_id", new ObjectId(taskId)), new Document("$set", taskDocument),
            MONGO_COLLECTION_SERVER_TASK);
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

      try {
        GlobalConfigurations.dbManager.connectionFrozen.updateOne(new Document("_id", new ObjectId(taskId)), new Document("$set", taskDocument),
            MONGO_COLLECTION_SERVER_TASK);
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
      try {
        GlobalConfigurations.dbManager.connectionFrozen.updateOne(new Document("_id", new ObjectId(taskId)), taskDocument,
            MONGO_COLLECTION_SERVER_TASK);
      } catch (Exception e) {
        Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
      }
    }
  }
}
