package br.com.lett.crawlernode.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import dbmodels.tables.SupplierTrackedLett;
import exceptions.MalformedRatingModel;
import exceptions.OfferException;
import org.bson.Document;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mongodb.client.FindIterable;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.LettProxy;
import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import dbmodels.Tables;

public class DatabaseDataFetcher {

   private static final Logger logger = LoggerFactory.getLogger(DatabaseDataFetcher.class);

   private DatabaseManager databaseManager;

   private static final String FETCHER_PROXIES_SOURCE = "source";
   private static final String FETCHER_PROXIES_USERNAME = "user";
   private static final String FETCHER_PROXIES_LOCATION = "location";
   private static final String FETCHER_PROXIES_PASSWORD = "pass";
   private static final String FETCHER_PROXIES_ADDRESSES = "addresses";
   private static final String FETCHER_PROXIES_ADDRESSES_HOST = "host";
   private static final String FETCHER_PROXIES_ADDRESSES_PORT = "port";
   // private static final String FETCHER_PROXIES_ACTIVE = "active";


   public DatabaseDataFetcher(DatabaseManager databaseManager) {
      this.databaseManager = databaseManager;
   }


   /**
    * Fetch the desired market from the database.
    *
    * @param marketCity
    * @param marketName
    * @return
    */
   public Market fetchMarket(String marketCity, String marketName) {
      dbmodels.tables.Market marketTable = Tables.MARKET;

      List<Condition> conditions = new ArrayList<>();
      conditions.add(marketTable.NAME.equal(marketName).and(marketTable.CITY.equal(marketCity)));

      return fetchMarket(conditions);
   }

   /**
    * Fetch the desired market from the database.
    *
    * @param marketId
    * @return
    */
   public Market fetchMarket(Long marketId) {
      dbmodels.tables.Market marketTable = Tables.MARKET;

      List<Condition> conditions = new ArrayList<>();
      conditions.add(marketTable.ID.equal(marketId));

      return fetchMarket(conditions);
   }

   /**
    * Fetch the desired market from the database.
    *
    * @param conditions
    * @return
    */
   private Market fetchMarket(List<Condition> conditions) {

      dbmodels.tables.Market marketTable = Tables.MARKET;

      List<Field<?>> fields = new ArrayList<>();
      fields.add(marketTable.ID);
      fields.add(marketTable.CITY);
      fields.add(marketTable.NAME);
      fields.add(marketTable.FULLNAME);
      fields.add(marketTable.CODE);
      fields.add(marketTable.FIRST_PARTY_REGEX);
      fields.add(marketTable.PROXIES);
      fields.add(marketTable.PROXIES_IMAGES);

      Connection conn = null;
      Statement sta = null;
      ResultSet rs = null;

      try {

         conn = JdbcConnectionFactory.getInstance().getConnection();
         sta = conn.createStatement();
         rs = sta.executeQuery(this.databaseManager.jooqPostgres.select(fields).from(marketTable).where(conditions).getSQL(ParamType.INLINED));

         Result<Record> records = this.databaseManager.jooqPostgres.fetch(rs);

         if (!records.isEmpty()) {
            Record r = records.get(0);

            // get the proxies used in this market
            ArrayList<String> proxies = new ArrayList<>();
            JSONArray proxiesJSONArray = new JSONArray(r.getValue(marketTable.PROXIES));
            for (int i = 0; i < proxiesJSONArray.length(); i++) {
               proxies.add(proxiesJSONArray.getString(i));
            }

            // get the proxies used for images download in this market
            ArrayList<String> imageProxies = new ArrayList<>();
            JSONArray imageProxiesJSONArray = new JSONArray(r.getValue(marketTable.PROXIES_IMAGES));
            for (int i = 0; i < imageProxiesJSONArray.length(); i++) {
               imageProxies.add(imageProxiesJSONArray.getString(i));
            }

            // create market
            return new Market(r.getValue(marketTable.ID).intValue(),
                  r.getValue(marketTable.CITY),
                  r.getValue(marketTable.NAME),
                  r.getValue(marketTable.CODE),
                  r.getValue(marketTable.FULLNAME),
                  proxies,
                  imageProxies,
                  r.getValue(marketTable.FIRST_PARTY_REGEX));
         }

      } catch (Exception e) {
         Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
      } finally {
         JdbcConnectionFactory.closeResource(rs);
         JdbcConnectionFactory.closeResource(sta);
         JdbcConnectionFactory.closeResource(conn);
      }

      return null;
   }

   /**
    * Return all proxies from mongo fetcher
    *
    * @return List<LettProxy>
    */
   public Map<String, List<LettProxy>> fetchProxiesFromMongoFetcher() {
      Map<String, List<LettProxy>> proxiesMap = new HashMap<>();

      FindIterable<Document> documents = databaseManager.connectionFetcher.runFind(null, "Proxies");

      for (Document doc : documents) {
         List<LettProxy> proxies = new ArrayList<>();
         LettProxy proxy = new LettProxy();
         String proxySource = "";

         if (doc.containsKey("active") && !doc.getBoolean("active")) {
            continue;
         }

         if (doc.containsKey(FETCHER_PROXIES_SOURCE)) {
            proxySource = doc.getString(FETCHER_PROXIES_SOURCE);

            if (proxySource.equals("buyproxies.org")) {
               proxySource = ProxyCollection.BUY;
            }

            if (proxySource.equals("proxybonanza")) {
               proxySource = ProxyCollection.BONANZA;
            }

            proxy.setSource(proxySource);
         } else {
            Logging.printLogError(logger, "Proxy without" + FETCHER_PROXIES_SOURCE + " in mongo fetcher.");
            continue;
         }

         if (doc.containsKey(FETCHER_PROXIES_USERNAME)) {
            proxy.setUser(doc.getString(FETCHER_PROXIES_USERNAME));
         }

         if (doc.containsKey(FETCHER_PROXIES_PASSWORD)) {
            proxy.setPass(doc.getString(FETCHER_PROXIES_PASSWORD));
         }

         if (doc.containsKey(FETCHER_PROXIES_LOCATION)) {
            proxy.setLocation(doc.getString(FETCHER_PROXIES_LOCATION));
         }

         if (doc.containsKey(FETCHER_PROXIES_ADDRESSES)) {
            @SuppressWarnings("unchecked")
            List<Document> addressesDocuments = (List<Document>) doc.get(FETCHER_PROXIES_ADDRESSES);

            for (Document addressDocumet : addressesDocuments) {
               LettProxy clonedProxy = proxy.clone();
               if (addressDocumet.containsKey(FETCHER_PROXIES_ADDRESSES_HOST)) {
                  clonedProxy.setAddress(addressDocumet.getString(FETCHER_PROXIES_ADDRESSES_HOST));
               } else {
                  Logging.printLogError(logger, "Proxy " + proxySource + " without" + FETCHER_PROXIES_ADDRESSES_HOST + " in mongo fetcher.");
                  continue;
               }

               if (addressDocumet.containsKey(FETCHER_PROXIES_ADDRESSES_PORT)) {
                  clonedProxy.setPort(addressDocumet.getInteger(FETCHER_PROXIES_ADDRESSES_PORT));
               } else {
                  Logging.printLogError(logger, "Proxy " + proxySource + " without" + FETCHER_PROXIES_ADDRESSES_PORT + " in mongo fetcher.");
                  continue;
               }
               proxies.add(clonedProxy);
            }
         } else {
            Logging.printLogError(logger, "Proxy without" + FETCHER_PROXIES_ADDRESSES + " in mongo fetcher.");
            continue;
         }

         if (!proxies.isEmpty()) {
            proxiesMap.put(proxySource, proxies);
         }
      }

      return proxiesMap;
   }

   public List<Product> fetchInsightsProductsFromMarket(Long marketId, List<Long> suppliersId, boolean isWebdriver) {

      Set<Long> lettIds = fetchLettIdsFromSuppliers(suppliersId);

      return fetchProductsFromLettIds(marketId, lettIds, isWebdriver);
   }

   public Set<Long> fetchLettIdsFromSuppliers(List<Long> suppliersId) {
      Set<Long> trackedLettIds = new HashSet<>();

      for (Long supplierId : suppliersId) {
         Set<Long> result = fetchLettIdsFromSupplierId(supplierId);

         trackedLettIds.addAll(result);
      }
      return trackedLettIds;
   }

   Set<Long> fetchLettIdsFromSupplierId(Long supplierId) {
      Set<Long> lettIds = new HashSet<>();

      SupplierTrackedLett supplierTrackedLett = Tables.SUPPLIER_TRACKED_LETT;

      String query = "SELECT DISTINCT lett_id, (tracked_full.tracked_by_supplier <> tracked_full.lett_supplier_id) AS competitor " +
         "FROM (SELECT lett_id, tracked_by_supplier, supplier.id AS lett_supplier_id " +
         "FROM (SELECT lett_id, supplier_id AS tracked_by_supplier " +
         "FROM supplier_tracked_lett " +
         "JOIN supplier_tracked_markets " +
         "USING (supplier_id) " +
         "WHERE supplier_id = " + supplierId + ") AS tracked " +
         "LEFT JOIN lett ON " +
         "tracked.lett_id = lett.id " +
         "LEFT JOIN brand ON " +
         "lett.brand_id = brand.id " +
         "LEFT JOIN supplier ON " +
         "brand.supplier_id = supplier.id) AS tracked_full " +
         "WHERE tracked_by_supplier = " + supplierId;

      Result<Record> result = fetchQuery(query);

      if (result != null) {
         lettIds = result
            .stream()
            .filter(record -> (!(Boolean) record.get("competitor")))
            .map(record -> (record.get(supplierTrackedLett.LETT_ID)))
            .collect(Collectors.toSet());
      }

      return lettIds;
   }

   Set<String> fetchKeywordsFromSupplierId(Long supplierId) {
      Set<String> keywords = new HashSet<>();



      return keywords;
   }

   List<Product> fetchProductsFromLettIds(Long marketId, Set<Long> lettIds, boolean isWebdriver) {

      List<Product> products = new ArrayList<>();

      dbmodels.tables.Processed processed = Tables.PROCESSED;
      dbmodels.tables.Unification unification = Tables.UNIFICATION;
      dbmodels.tables.Market marketTable = Tables.MARKET;

      Condition condition = isWebdriver ? marketTable.CRAWLER_WEBDRIVER.isTrue() : marketTable.CRAWLER_WEBDRIVER.isFalse();

      String query = DSL.select(
         processed.ID.as("processed_id"),
         processed.INTERNAL_ID,
         processed.ORIGINAL_NAME,
         processed.URL,
         processed.MARKET,
         processed.INTERNAL_PID,
         processed.AVAILABLE,
         processed.VOID,
         processed.PIC,
         processed.SECONDARY_PICS,
         processed.CAT1,
         processed.CAT2,
         processed.CAT3,
         processed.ORIGINAL_DESCRIPTION,
         processed.RATING,
         processed.EANS
      )
         .from(processed)
         .rightJoin(unification).on(processed.INTERNAL_ID.eq(unification.INTERNAL_ID)
            .and(processed.MARKET.eq(unification.MARKET_ID.cast(Integer.class))))
         .join(marketTable).on(marketTable.ID.eq(unification.MARKET_ID))
         .where(unification.MARKET_ID.eq(marketId))
         .and(unification.LETT_ID.in(lettIds))
         .and(condition)
         .toString();

      try {
         Result<Record> result = fetchQuery(query);

         for (Record record : result) {
            Product product = recordToProduct(record);
            products.add(product);
         }
      } catch (Exception e) {
         Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
      }

      return products;
   }

   private Product recordToProduct(Record record) throws MalformedProductException, OfferException, MalformedRatingModel {
      Map<String, Object> m = record.intoMap();

      JSONObject json = new JSONObject(m);

      return Product.fromJSON(json);
   }

   Result<Record> fetchQuery(String query) {
      Connection conn = null;
      Statement sta = null;
      ResultSet rs = null;

      Result<Record> result = null;

      try {
         conn = JdbcConnectionFactory.getInstance().getConnection();
         sta = conn.createStatement();
         rs = sta.executeQuery(query);

         result = this.databaseManager.jooqPostgres.fetch(rs);

      } catch (Exception e) {
         Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
      } finally {
         JdbcConnectionFactory.closeResource(rs);
         JdbcConnectionFactory.closeResource(sta);
         JdbcConnectionFactory.closeResource(conn);
      }
      return result;
   }
}
