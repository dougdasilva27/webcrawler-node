package br.com.lett.crawlernode.core.models;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import br.com.lett.crawlernode.database.JdbcConnectionFactory;

public class Markets {

  private List<Market> marketsList;

  public Markets() {
    this.marketsList = new ArrayList<>();

    init();
  }

  public void init() {
    Connection conn = null;
    ResultSet rs = null;
    Statement sta = null;
    try {
      conn = JdbcConnectionFactory.getInstance().getConnection();
      sta = conn.createStatement();
      rs = sta.executeQuery("SELECT id, city, name, crawler_webdriver, proxies, proxies_images FROM market");

      while (rs.next()) {
        int marketId = rs.getInt("id");
        String city = rs.getString("city");
        String name = rs.getString("name");
        boolean crawlerWebdriver = rs.getBoolean("crawler_webdriver");
        ArrayList<String> proxies = new ArrayList<>();
        ArrayList<String> imageProxies = new ArrayList<>();

        // get the array of proxies from postgres, as a json array
        JSONArray proxiesJSONArray = new JSONArray(rs.getString("proxies"));

        // populate the proxies array list
        for (int i = 0; i < proxiesJSONArray.length(); i++) {
          proxies.add(proxiesJSONArray.getString(i));
        }

        // get the array of image proxies from postgres
        JSONArray imageProxiesJSONArray = new JSONArray(rs.getString("proxies_images"));
        for (int i = 0; i < imageProxiesJSONArray.length(); i++) {
          imageProxies.add(imageProxiesJSONArray.getString(i));
        }

        Market market = new Market(marketId, city, name, proxies, imageProxies);

        market.setMustUseCrawlerWebdriver(crawlerWebdriver);

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
