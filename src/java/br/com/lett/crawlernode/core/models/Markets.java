package br.com.lett.crawlernode.core.models;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.conf.ParamType;
import org.json.JSONArray;
import br.com.lett.crawlernode.database.DatabaseManager;
import br.com.lett.crawlernode.database.JdbcConnectionFactory;
import dbmodels.Tables;

public class Markets {

   private List<Market> marketsList;

   public Markets(DatabaseManager db) {
      this.marketsList = new ArrayList<>();

      init(db);
   }

   public void init(DatabaseManager db) {
      Connection conn = null;
      ResultSet rs = null;
      Statement sta = null;
      try {

         dbmodels.tables.Market marketTable = Tables.MARKET;
         List<Field<?>> fields = new ArrayList<>();
         fields.add(marketTable.ID);
         fields.add(marketTable.CITY);
         fields.add(marketTable.NAME);
         fields.add(marketTable.FULLNAME);
         fields.add(marketTable.CODE);
         fields.add(marketTable.PROXIES);
         fields.add(marketTable.PROXIES_IMAGES);
         fields.add(marketTable.CRAWLER_WEBDRIVER);
         fields.add(marketTable.FIRST_PARTY_REGEX);

         conn = JdbcConnectionFactory.getInstance().getConnection();
         sta = conn.createStatement();
         rs = sta.executeQuery(db.jooqPostgres.select(fields).from(marketTable).getSQL(ParamType.INLINED));

         Result<Record> records = db.jooqPostgres.fetch(rs);
         for (Record r : records) {
            ArrayList<String> proxies = new ArrayList<>();
            ArrayList<String> imageProxies = new ArrayList<>();

            // get the array of proxies from postgres, as a json array
            JSONArray proxiesJSONArray = new JSONArray(r.get(marketTable.PROXIES));

            // populate the proxies array list
            for (int i = 0; i < proxiesJSONArray.length(); i++) {
               proxies.add(proxiesJSONArray.getString(i));
            }

            // get the array of image proxies from postgres
            JSONArray imageProxiesJSONArray = new JSONArray(r.get(marketTable.PROXIES_IMAGES));
            for (int i = 0; i < imageProxiesJSONArray.length(); i++) {
               imageProxies.add(imageProxiesJSONArray.getString(i));
            }

            Market market = new Market(
                  r.get(marketTable.ID).intValue(),
                  r.get(marketTable.CITY),
                  r.get(marketTable.NAME),
                  r.get(marketTable.CODE),
                  r.get(marketTable.FULLNAME),
                  proxies,
                  imageProxies,
                  r.get(marketTable.FIRST_PARTY_REGEX));

            market.setMustUseCrawlerWebdriver(r.get(marketTable.CRAWLER_WEBDRIVER));

            marketsList.add(market);
         }
      } catch (SQLException e) {
         e.printStackTrace();
      } finally {
         JdbcConnectionFactory.closeResource(rs);
         JdbcConnectionFactory.closeResource(sta);
         JdbcConnectionFactory.closeResource(conn);
      }
   }

   /**
    * Search for a market in the markets array.
    * 
    * @param city
    * @param name
    * @return the desired market. If none is found, the method returns null.
    */
   public Market getMarket(String city, String name) {
      for (Market m : marketsList) {
         if (m.getCity().equals(city) && m.getName().equals(name)) {
            return m;
         }
      }

      return null;
   }

   public Market getMarket(int marketId) {
      for (Market m : marketsList) {
         if (m.getNumber() == marketId) {
            return m;
         }
      }

      return null;
   }

   public List<Market> getMarkets() {
      return marketsList;
   }

   public void setMarkets(List<Market> markets) {
      this.marketsList = markets;
   }

   @Override
   public String toString() {
      StringBuilder stringBuilder = new StringBuilder();
      for (Market market : this.marketsList) {
         stringBuilder.append(market.toString());
         stringBuilder.append("\n");
      }
      return stringBuilder.toString();
   }

}