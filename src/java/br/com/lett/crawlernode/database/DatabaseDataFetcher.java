package br.com.lett.crawlernode.database;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.LettProxy;
import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.crawler.SeedCrawlerSession;
import br.com.lett.crawlernode.main.GlobalConfigurations;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import dbmodels.Tables;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.conf.ParamType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class DatabaseDataFetcher {

   private static final Logger logger = LoggerFactory.getLogger(DatabaseDataFetcher.class);

   private static DatabaseManager databaseManager;

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
            return new Market(
               r.getValue(marketTable.ID).intValue(),
               r.getValue(marketTable.NAME),
               r.getValue(marketTable.FULLNAME),
               r.getValue(marketTable.CODE),
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

   public static JSONArray fetchProxiesFromMongoFetcher(JSONArray proxiesNames) {
      JSONArray proxies = new JSONArray();
      LettProxy proxy = new LettProxy();

      MongoCollection<Document> collection = databaseManager.connectionFetcher.getCollection("Proxies");

      for (Object proxyName : proxiesNames) {
         Bson query = Filters.and(
            Filters.eq("source", proxyName));

         FindIterable<Document> documents = collection.find(query);

         for (Document doc : documents) {
            String proxySource = "";

            if (doc.containsKey("active") && !doc.getBoolean("active")) {
               continue;
            }

            if (doc.containsKey(FETCHER_PROXIES_SOURCE)) {
               proxySource = doc.getString(FETCHER_PROXIES_SOURCE);

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
               Collections.shuffle(addressesDocuments);
               if (addressesDocuments.size() > 0) {
                  Document addressDocument = addressesDocuments.get(0);

                  if (addressDocument.containsKey(FETCHER_PROXIES_ADDRESSES_HOST)) {
                     proxy.setAddress(addressDocument.getString(FETCHER_PROXIES_ADDRESSES_HOST));
                  } else {
                     Logging.printLogError(logger, "Proxy " + proxySource + " without" + FETCHER_PROXIES_ADDRESSES_HOST + " in mongo fetcher.");
                     continue;
                  }

                  if (addressDocument.containsKey(FETCHER_PROXIES_ADDRESSES_PORT)) {
                     proxy.setPort(addressDocument.getInteger(FETCHER_PROXIES_ADDRESSES_PORT));
                  } else {
                     Logging.printLogError(logger, "Proxy " + proxySource + " without" + FETCHER_PROXIES_ADDRESSES_PORT + " in mongo fetcher.");
                     continue;
                  }
                  proxies.put(proxy.toJson());

               } else {
                  Logging.printLogError(logger, "Proxy without" + FETCHER_PROXIES_ADDRESSES + " in mongo fetcher.");
                  continue;
               }
            }
         }
      }

      return proxies;

   }

   public static JSONObject fetchProductInElastic(Product product, Session session) {
      DatabaseManager dbManagerElastic = new DatabaseManager(GlobalConfigurations.dbCredentials, session instanceof SeedCrawlerSession);
      JSONObject productJson = new JSONObject();

      String[] sources = new String[4];
      sources[0] = "created";
      sources[1] = "unification_is_master";
      sources[2] = "name";
      sources[3] = "lett_id";

      BoolQueryBuilder query = QueryBuilders.boolQuery();
      query.must(QueryBuilders.termQuery("internal_id", product.getInternalId()));
      query.must(QueryBuilders.termQuery("market_id", session.getMarket().getId()));

      long queryStartTime = System.currentTimeMillis();

      try {
         SearchResponse searchResponse = dbManagerElastic.connectionElasticSearch.searchResponse(sources, query);
         SearchHit[] searchHits = searchResponse.getHits().getHits();
         if (searchHits.length > 0) {
            productJson = new JSONObject(searchHits[0].getSourceAsString());
         }

         JSONObject apacheMetadata = new JSONObject().put("elasticsearch_elapsed_time", System.currentTimeMillis() - queryStartTime)
            .put("query_type", "fetch_product_in_elastic");

         Logging.logInfo(logger, session, apacheMetadata, "ELASTICSEARCH TIMING INFO");

      } catch (IOException e) {
         Logging.printLogError(logger, session, "Error fetching product in elasticsearch to product: internalId  " + product.getInternalId() + " in market: " + session.getMarket().getId());
         throw new RuntimeException(e);
      } finally {
         dbManagerElastic.connectionElasticSearch.closeConnection();
      }

      return productJson;

   }

}
