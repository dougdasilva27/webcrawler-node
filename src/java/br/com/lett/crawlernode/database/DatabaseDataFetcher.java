package br.com.lett.crawlernode.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.conf.ParamType;
import org.json.JSONArray;
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

    List<Field<?>> fields = new ArrayList<>();
    fields.add(marketTable.ID);
    fields.add(marketTable.CITY);
    fields.add(marketTable.NAME);
    fields.add(marketTable.PROXIES);
    fields.add(marketTable.PROXIES_IMAGES);

    List<Condition> conditions = new ArrayList<>();
    conditions.add(marketTable.NAME.equal(marketName).and(marketTable.CITY.equal(marketCity)));

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
        return new Market(r.getValue(marketTable.ID).intValue(), r.getValue(marketTable.CITY), r.getValue(marketTable.NAME), proxies, imageProxies);
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
   * Return all processeds from crawler_ranking for this location and market from yesterday
   * 
   * @param location
   * @param market
   * @param date
   * @return
   */
  public static Long fetchCountOfProcessedsFromCrawlerRanking(String location, int market, String today, String yesterday) {
    Long count = 0l;

    StringBuilder sql = new StringBuilder();

    sql.append("SELECT COUNT(crawler_ranking.id) AS count FROM crawler_ranking, processed ")
        .append("WHERE crawler_ranking.processed_id = processed_id ").append("AND processed.market = ").append(market)
        .append(" AND crawler_ranking.location = '").append(location).append("' AND crawler_ranking.date BETWEEN '").append(yesterday)
        .append("' AND '").append(today).append("'");

    Connection conn = null;
    ResultSet rs = null;
    Statement sta = null;
    try {
      conn = JdbcConnectionFactory.getInstance().getConnection();
      sta = conn.createStatement();
      rs = sta.executeQuery(sql.toString());

      count = (Long) rs.getObject("count");

    } catch (Exception e) {
      Logging.printLogWarn(logger, CommonMethods.getStackTrace(e));
    } finally {
      JdbcConnectionFactory.closeResource(rs);
      JdbcConnectionFactory.closeResource(sta);
      JdbcConnectionFactory.closeResource(conn);
    }

    return count;
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
}
